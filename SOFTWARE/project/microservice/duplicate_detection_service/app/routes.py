
from fastapi import APIRouter
from .services import computeVector, cleanReport, detectDuplicates
from .dtos import NewReportRequestBody, ReportEmbeddingResponseBody, ReportRequestBody, ReportDuplicateDetectionResponseBody, DuplicateSuggestionResponseBody
from .database import fetchReportEmbeddingTimestampAndVectorEmbedding, fetchCandidateReports
import time
import base64

router = APIRouter()

@router.post("/embedding")
def computeReportVectorEmbedding(requestBody: NewReportRequestBody) -> ReportEmbeddingResponseBody:
    """
    Request endpoint to compute a vector embedding for a new report

    :param requestBody: The report details to compute the vector embedding for and with.
    :type requestBody: NewReportRequestBody
    :return: The report ID and the vector embedding.
    :rtype: ReportEmbeddingResponseBody
    """
    # Timing the endpoint
    start_time = time.perf_counter()

    # Clean report data
    report_data = cleanReport(
        requestBody.summary,
        requestBody.steps,
        requestBody.expected,
        requestBody.actual
    )

    # Compute vector of new report first.
    vector_embedding = computeVector(report_data)

    # Send as base64 encoded byte string that spring can decode and store.
    # Float32 since the model uses 32-bit floats in the vector.
    # The decode() here just removes the byte notation so I can pass it through JSON.
    # This makes it utf-8 by default.
    vector_embedding = vector_embedding.astype("float32").tobytes()
    base64_vector_embedding = base64.b64encode(vector_embedding).decode()

    end_time = time.perf_counter()
    print("Compute vector embedding for report took %.2fms" % ((end_time - start_time)*1000))

    # Return report ID and vector embedding
    return ReportEmbeddingResponseBody(
        report_id = requestBody.report_id,
        vector_embedding = base64_vector_embedding
    )


@router.post("/suggestions")
def detectReportDuplicates(requestBody: ReportRequestBody) -> ReportDuplicateDetectionResponseBody:
    """
    Request endpoint to suggest duplicates for the report.

    :param requestBody: The report details to suggest duplicates for and with.
    :type requestBody: ReportRequestBody
    :return: The report ID, and duplicate suggestions.
    :rtype: ReportDuplicateDetectionResponseBody
    """    
    start_time = time.perf_counter()

    # Get current report embedding
    report_embedding_timestamp, report_vector_embedding = fetchReportEmbeddingTimestampAndVectorEmbedding(
        requestBody.report_id
    )

    # Fetch certain reports with their vector embeddings
    indexed_candidate_report_ids, candidate_report_embeddings = fetchCandidateReports(
        requestBody.workspace_id,
        requestBody.report_id,  
        report_embedding_timestamp
    )

    # If no candidates return, otherwise find any duplicate suggestions
    duplicate_suggestions = []
    if len(candidate_report_embeddings) > 0:
        detection_results: list[tuple[int, float, bool]] = detectDuplicates(report_vector_embedding, candidate_report_embeddings, indexed_candidate_report_ids)

        # Format duplicate suggestions in order
        for i, suggestion in enumerate(detection_results):
            print(f"Suggestion from: {requestBody.report_id}, to: {suggestion[0]}, score: {suggestion[1]}, order: {i+1}")
            duplicate_suggestions.append(DuplicateSuggestionResponseBody(
                to_report_id = suggestion[0],
                similarity_order = i+1, # Plus 1 to make 1 indexed
                similarity_score = suggestion[1],
                is_confident = suggestion[2] # If over the confidence threshold (upperbound)
            ))

    # Return report ID, and duplicate suggestions
    end_time = time.perf_counter()
    print("Detect duplicates for report took %.2fms" % ((end_time - start_time)*1000))
    return ReportDuplicateDetectionResponseBody(
        from_report_id = requestBody.report_id,
        duplicate_suggestions = duplicate_suggestions
    )