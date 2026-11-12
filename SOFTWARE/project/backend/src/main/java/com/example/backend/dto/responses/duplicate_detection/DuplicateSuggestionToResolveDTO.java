package com.example.backend.dto.responses.duplicate_detection;

import com.example.backend.dto.responses.reports.ReportDTO;
import com.example.backend.model.reports.DuplicateSuggestion;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DuplicateSuggestionToResolveDTO(
    @JsonProperty("to_report") ReportDTO toReport,
    @JsonProperty("similarity_order") Integer similarityOrder,
    @JsonProperty("similarity_score") Double similarityScore
) {
    public static DuplicateSuggestionToResolveDTO fromEntity(DuplicateSuggestion duplicateSuggestion) {
        return new DuplicateSuggestionToResolveDTO(
            ReportDTO.fromEntity(duplicateSuggestion.getToReport()),
            duplicateSuggestion.getSimilarityOrder(),
            duplicateSuggestion.getSimilarityScore()
        );
    }
}
