
export const AvatarColours = {
    "red": "#e66262",
    "orange": "#ec9a60",
    "yellow": "#ead63f",
    "lime": "#aee35f",
    "green": "#24d665",
    "sky": "#53c8e5",
    "blue": "#7ba9f2",
    "indigo": "#9076f1",
    "violet": "#c078ed",
    "pink": "#f97db3"
}

export const WorkspaceColours = {
    "red": "#f8cece",
    "orange": "#f5dfd0",
    "yellow": "#f5f1c9",
    "lime": "#e0efc3",
    "green": "#c9ead5",
    "sky": "#cfebf1",
    "blue": "#d2e0f1",
    "indigo": "#dcd7f0",
    "violet": "#e6d7f0",
    "pink": "#f7d5e4",
}

export const WorkspaceStrongColours = {
    "red": "#a92828",
    "orange": "#d56f2b",
    "yellow": "#c9ad2f",
    "lime": "#8fbd33",
    "green": "#1e954a",
    "sky": "#32b0cc",
    "blue": "#2f6db9",
    "indigo": "#5038af",
    "violet": "#854daa",
    "pink": "#c43a76",
}

/** Utility function to select a random workspace colour. */
export const getRandomWorkspaceColour = () => {
    let randomNumber = Math.floor(Math.random() * 10);
    return Object.keys(WorkspaceColours)[randomNumber];
}

export const CustomLabelColours = {
    red: "#b23d3d",
    orange: "#b87745",
    yellow: "#b4b028",
    lime: "#85b53f",
    green: "#1a9f4c",
    sky: "#3d9ccf",
    blue: "#5c88d1",
    indigo: "#6b54d1",
    violet: "#8b4bbf",
    pink: "#d3588f",
}

/** Utility function to select a random custom label colour. */
export const getRandomCustomLabelColour = () => {
    let randomNumber = Math.floor(Math.random() * 10);
    return Object.keys(CustomLabelColours)[randomNumber];
}

export const RatingColour = [
    "#aeaeae",
    "#6a9700",
    "#b49f00",
    "#bf8f00",
    "#cf6926",
    "#bc1b1b"
]

export const ProgressColours = {
    OPEN: "#0ea5e9",
    IN_PROGRESS: "#f59e0b",
    IN_TESTING: "#13b3ab",
    IN_REVIEW: "#8b63f1",
    CLOSED: "#9ca3af"
}

export const StatusColours = {
    UNCONFIRMED: "#9ca3af",
    CONFIRMED: "#2162e4",
    UNREPRODUCIBLE: "#e75d13",
    UNFIXABLE: "#c113db",
    RESOLVED: "#25a01a",
    NOT_A_BUG: "#2c3341"
}

export const BugAuditLogTypeColours = {
    CREATION: "#71c678",
    MODIFICATION: "#e693c4",
    PROGRESSION: "#b393e6",
    ASSIGNMENT: "#7a97e0",
    DUPLICATE_RESOLVED: "#e0be7a",
    DUPLICATE_AUTO_RESOLVED: "#e0847a"
}

export const NotificationTypeColours = {
    ADDED_TO_WORKSPACE: "#6e9d72",
    REMOVED_FROM_WORKSPACE: "#c86e64",
    WORKSPACE_UPDATE: "#8c72b8",
    WORKSPACE_DELETED: "#c86e64",
    ASSIGNED_TO_BUG: "#6e9d72",
    UNASSIGNED_FROM_BUG: "#c86e64",
    BUG_UPDATE: "#8c72b8",
    NEW_DUPLICATE_REPORTS: "#6780bf",
    NEW_DUPLICATE_SUGGESTIONS: "#ce9c37",
    REPORT_DISCOVERED_AS_DUPLICATE: "#cc88b0"
}

export const BugPriorityLabelColours = {
    P4: "#5b9609",
    P3: "#92a016",
    P2: "#b9a314",
    P1: "#c05711",
    P0: "#b50c0c"
}