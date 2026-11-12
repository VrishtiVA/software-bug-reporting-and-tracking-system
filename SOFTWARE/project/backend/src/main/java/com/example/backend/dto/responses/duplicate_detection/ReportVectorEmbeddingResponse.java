package com.example.backend.dto.responses.duplicate_detection;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportVectorEmbeddingResponse(
        @JsonProperty("report_id") int reportId,
        @JsonProperty("vector_embedding") String vectorEmbedding //Will arrive as a base64 string
) {}
