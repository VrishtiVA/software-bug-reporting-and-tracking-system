package com.example.backend.services.microservices;

import com.example.backend.dto.responses.duplicate_detection.DuplicateDetectionResponse;
import com.example.backend.dto.responses.duplicate_detection.ReportVectorEmbeddingResponse;
import com.example.backend.model.reports.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    //URL to access FastAPI Microservice
    WebClient webClient = WebClient.create("http://localhost:8000");

    /**
     * Method to asynchronously send a new report for to the microservice to compute a new vector embedding for it.
     * @param report The report to compute a vector embedding for
     * @return The vector embedding that was computed.
     */
    @Async
    public CompletableFuture<ReportVectorEmbeddingResponse> computeVectorEmbedding(Report report) {
        log.info("Sending request to compute vector embedding endpoint with new report id: {}", report.getId());
        try {
            long startTime = System.currentTimeMillis();
            //Call FastAPI Service, and return the response.
            return webClient.post()
                .uri("/micro/duplicate-detection/embedding")
                .bodyValue(Map.of(
                    "report_id", report.getId(),
                    "summary", report.getSummaryTitle(),
                    "steps", report.getStepsToReproduce(),
                    "expected", report.getExpectedResult(),
                    "actual", report.getActualResult()
                )) //Send request body in JSON
                .retrieve()
                .bodyToMono(ReportVectorEmbeddingResponse.class) //Read body as string
                .toFuture() //Wait for result, sync within async container, to not block main controller.
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("Failed in computeVectorEmbedding(Report report)", ex);
                    }
                    long endTime = System.currentTimeMillis();
                    log.info("computeVectorEmbedding(Report report) Turn Around Time (TAT): {}", (endTime - startTime));
                });
        } catch (Exception e) {
            log.error("Failed in computeVectorEmbedding(Report report)", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Method to asynchronously send a new report for to the microservice for duplicate detection for checking.
     * This should compute a new vector embedding, and return some duplicate suggestions for the report if there are any.
     */
    @Async
    public CompletableFuture<DuplicateDetectionResponse> detectDuplicates(Report report) {
        log.info("Sending request to duplicate detection endpoint with new report id: {}", report.getId());
        try {
            long startTime = System.currentTimeMillis();
            //Call FastAPI Service, and return the response.
            return webClient.post()
                .uri("/micro/duplicate-detection/suggestions")
                .bodyValue(Map.of(
                    "workspace_id", report.getWorkspace().getId(),
                    "report_id", report.getId()
                )) //Send request body in JSON
                .retrieve()
                .bodyToMono(DuplicateDetectionResponse.class) //Read body as string
                .toFuture() //Wait for result, sync within async container, to not block main controller.
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("Failed in detectDuplicates(Report report)", ex);
                    } else {
                        log.info("FastAPI Response: {}", response);
                    }
                    long endTime = System.currentTimeMillis();
                    log.info("detectDuplicates(Report report) Turn Around Time (TAT): {}", (endTime - startTime));
                });
        } catch (Exception e) {
            log.error("Failed in detectDuplicates(Report report)", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
