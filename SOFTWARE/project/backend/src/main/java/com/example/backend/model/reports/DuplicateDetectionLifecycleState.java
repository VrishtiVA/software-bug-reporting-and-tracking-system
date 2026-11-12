package com.example.backend.model.reports;

public enum DuplicateDetectionLifecycleState {
    /** The report was just created and has not undergone duplicate detection yet. */
    NEW,
    /** A vector embedding has been computed for the report, and it is ready for duplicate detection */
    READY,
    /** This report may be a duplicate report and has duplicate suggestions. */
    OUTSTANDING,
    /** This report was either approved or rejected as a duplicate by a user, and is waiting to be contained. */
    RESOLVED,
    /** The report has been placed in an unstable container */
    STAGED,
    /** The report has been placed into a confirmed container. */
    CONTAINED
}
