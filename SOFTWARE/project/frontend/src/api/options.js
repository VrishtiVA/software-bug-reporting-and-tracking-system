
export const StatusOptions = {
    UNCONFIRMED: "Unconfirmed",
    CONFIRMED: "Confirmed",
    UNREPRODUCIBLE: "Unreproducible",
    UNFIXABLE: "Unfixable",
    RESOLVED: "Resolved"
}

export const StatusDescriptions = {
    UNCONFIRMED: "This means that the bug is yet to be confirmed to exist.",
    CONFIRMED: "This means that the bug has been confirmed to exist.",
    UNREPRODUCIBLE: "This means that the bug cannot be reproduced to know if it is an actual bug.",
    UNFIXABLE: "This means that this bug cannot be debugged and resolved.",
    RESOLVED: "This means that the bug has been successfully debugged."
}

export const ProgressOptions = {
    OPEN: "Open",
    IN_PROGRESS: "In Progress",
    IN_TESTING: "In Testing",
    IN_REVIEW: "In Review",
    CLOSED: "Closed"
}

export const ProgressDescription = {
    OPEN: "This means that the bug isn't being handled yet.",
    IN_PROGRESS: "This means that the bug is currently in the process of being debugged.",
    IN_TESTING: "This means that the bug fix is undergoing testing now.",
    IN_REVIEW: "This means that the bug fix is now being reviewed by others to ensure that the issue has been properly resolved.",
    CLOSED: "This means that there is nothing more that can be added to this bug."
}

export const FilterOptionTooltips = {
    "Progress": "Select to only display bugs with selected progress levels",
    "Status": "Select to only display bugs with selected statuses",
    "Labels": "Select to only display bugs with at least one of the selected labels"
}

export const BugSortOptions = {
    "relevance": "Relevance", //Set to this sort when searching.
    "priority": "Priority", //Otherwise default
    "hotness": "Trending",
    "firstReportedTimestamp": "First Reported",
    "frequency": "Frequency",
    "suggestions": "Suggestions"
}

export const BugPriorityLabelDescriptions = {
    P4: "P4: Lowest Priority",
    P3: "P3: Low Priority",
    P2: "P2: Medium Priority",
    P1: "P1: High Priority",
    P0: "P0: Critical Highest Priority"
}

export const BugFieldDescriptions = {
    PRIORITY: "The priority category provided for the bug, after considering its average severity, user impact, report frequency, and the time since first reported.",
    FREQUENCY: "The number of times this bug was reported, by considering the number of duplicate reports (reports refering to the same bug).",
    SUGGESTIONS: "The number of relevant duplicate report suggestions there are available related to this bug. These suggestions need to be resolved."
}

export const WorkspaceMetricDescriptions = {
    DUPLICATE_SUGGESTIONS: "The number of reports that have been detected as potential duplicates.",
    PROGRESS_DISTRIBUTION: "This pie chart shows the distribution of bug progress across the workspace.",
    PRIORITY_DISTRIBUTION: "This pie chart shows the distribution of bug priorities across the workspace.",
    LABEL_USAGE_DISTRIBUTION: "This bar chart shows the usage count of workspace labels in bugs across the workspace.",
    PRIORITY_FREQUENCY_SPREAD: "This bubble chart groups bugs by similar priority and frequency. The size of each bubble shows how many bugs fall into a priority and frequency range. Bigger bubbles indicate more bugs sharing the same combination."
}

export const WorkspaceRoleDescriptions = {
    ADMIN: "This role can edit workspace details (excluding workspace name and privacy), manage developers and reporters, resolve duplicate suggestions, update bugs, and edit assignment of both admins and developers.",
    DEVELOPER: "This role can resolve duplicate suggestions, update bugs, edit assignments for developers, and make bug reports.",
    REPORTER: "This role can report bugs, view bugs, and view the workspace dashboard. They will see a simpler version of the bug report form.",
}

export const PermissionCategories = {
    "CONFIGURE_WORKSPACE_NAME_AND_PRIVACY": "Edit WS Name & Privacy",
    "CONFIGURE_WORKSPACE_DETAILS": "Edit WS Details",
    "ADD_EDIT_REMOVE_MEMBERS": "Add, Edit, Remove Members",
    "RESOLVE_DUPLICATES": "Resolve Duplicate Suggestions",
    "EDIT_ASSIGNMENTS": "Edit Bug Assignments",
    "UPDATE_BUGS": "Update Bugs",
    "VIEW_WORKSPACE_DASHBOARD": "View WS Dashboard",
    "REPORT_BUGS": "Report Bugs",
    "VIEW_BUGS": "View Bugs"
}

export const WorkspaceRolePermissionDetails = {
    OWNER: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 1,
        CONFIGURE_WORKSPACE_DETAILS: 1,
        ADD_EDIT_REMOVE_MEMBERS: "Admins, Developers, Reporters",
        RESOLVE_DUPLICATES: 1,
        EDIT_ASSIGNMENTS: "Owner, Admins, Developers",
        UPDATE_BUGS: 1,
        VIEW_WORKSPACE_DASHBOARD: 1,
        REPORT_BUGS: 1,
        VIEW_BUGS: 1
    },
    ADMIN: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 0,
        CONFIGURE_WORKSPACE_DETAILS: 1,
        ADD_EDIT_REMOVE_MEMBERS: "Developers, Reporters",
        RESOLVE_DUPLICATES: 1,
        EDIT_ASSIGNMENTS: "Admins, Developers",
        UPDATE_BUGS: 1,
        VIEW_WORKSPACE_DASHBOARD: 1,
        REPORT_BUGS: 1,
        VIEW_BUGS: 1
    },
    DEVELOPER: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 0,
        CONFIGURE_WORKSPACE_DETAILS: 0,
        ADD_EDIT_REMOVE_MEMBERS: "Reporters",
        RESOLVE_DUPLICATES: 1,
        EDIT_ASSIGNMENTS: "Developers",
        UPDATE_BUGS: 1,
        VIEW_WORKSPACE_DASHBOARD: 1,
        REPORT_BUGS: 1,
        VIEW_BUGS: 1
    },
    REPORTER: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 0,
        CONFIGURE_WORKSPACE_DETAILS: 0,
        ADD_EDIT_REMOVE_MEMBERS: 0,
        RESOLVE_DUPLICATES: 0,
        EDIT_ASSIGNMENTS: 0,
        UPDATE_BUGS: 0,
        VIEW_WORKSPACE_DASHBOARD: 1,
        REPORT_BUGS: 1,
        VIEW_BUGS: 1
    },
    PUBLIC_WORKSPACE_NONMEMBER: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 0,
        CONFIGURE_WORKSPACE_DETAILS: 0,
        ADD_EDIT_REMOVE_MEMBERS: 0,
        RESOLVE_DUPLICATES: 0,
        EDIT_ASSIGNMENTS: 0,
        UPDATE_BUGS: 0,
        VIEW_WORKSPACE_DASHBOARD: 0,
        REPORT_BUGS: 1,
        VIEW_BUGS: 1
    },
    PRIVATE_WORKSPACE_NONMEMBER: {
        CONFIGURE_WORKSPACE_NAME_AND_PRIVACY: 0,
        CONFIGURE_WORKSPACE_DETAILS: 0,
        ADD_EDIT_REMOVE_MEMBERS: 0,
        RESOLVE_DUPLICATES: 0,
        EDIT_ASSIGNMENTS: 0,
        UPDATE_BUGS: 0,
        VIEW_WORKSPACE_DASHBOARD: 0,
        REPORT_BUGS: 0,
        VIEW_BUGS: 0
    }
}