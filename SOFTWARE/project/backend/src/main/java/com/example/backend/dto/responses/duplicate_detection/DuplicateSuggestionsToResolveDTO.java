package com.example.backend.dto.responses.duplicate_detection;

import com.example.backend.dto.responses.reports.ReportDTO;
import com.example.backend.model.reports.DuplicateSuggestion;
import com.example.backend.model.reports.Report;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record DuplicateSuggestionsToResolveDTO(
    @JsonProperty("claim_timestamp") Instant claimTimestamp,
    @JsonProperty("from_report") ReportDTO fromReport,
    @JsonProperty("duplicate_suggestions") List<DuplicateSuggestionToResolveDTO> duplicateSuggestions
) {
    public static DuplicateSuggestionsToResolveDTO fromEntity(List<DuplicateSuggestion> duplicateSuggestions) {
        //Extract from report
        Report fromReport = duplicateSuggestions.getFirst().getFromReport();
        //Fetch duplicate suggestions in format
        List<DuplicateSuggestionToResolveDTO> duplicateSuggestionsDTOList = new ArrayList<>();
        for (DuplicateSuggestion duplicateSuggestion : duplicateSuggestions) {
            duplicateSuggestionsDTOList.add(DuplicateSuggestionToResolveDTO.fromEntity(duplicateSuggestion));
        }
        //Return full DTO.
        return new DuplicateSuggestionsToResolveDTO(
            fromReport.getDuplicateSuggestionsBeingManuallyResolvedTimestamp(),
            ReportDTO.fromEntity(fromReport),
            duplicateSuggestionsDTOList
        );
    }
}
