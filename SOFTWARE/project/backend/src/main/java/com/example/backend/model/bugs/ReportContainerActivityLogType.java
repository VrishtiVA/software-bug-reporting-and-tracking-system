package com.example.backend.model.bugs;

public enum ReportContainerActivityLogType {
    /** When bug was found, with report ID. */
    CREATION,
    /** When bug summary/labels are modified by a user */
    MODIFICATION,
    /** When bug progress/status is updated, by a user */
    PROGRESSION,
    /** When assignees is edited, by a user. */
    ASSIGNMENT,
    /**
     * When new report is resolved as a duplicate, by a user.
     * E.g. Report _ID:X_ discovered as a duplicate, merging in Y new reports.
     */
    DUPLICATE_RESOLVED,
    /**
     * When a report is automatically resolved as a duplicate.
     * E.g. Report _ID:X_ automatically resolved as a duplicate, merging in Y new reports.
     */
    DUPLICATE_AUTO_RESOLVED
}
