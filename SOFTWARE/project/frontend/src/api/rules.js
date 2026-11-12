
/**
 * Function to get roles that a user is allowed to edit assignments for.
 * Using switch case statement without break to build up list appropriately.
 * @param {string} role The role of the user. 
 * @returns A list of roles that the user is allowed to edit assignments for.
 */
export function getBugEditAssigneesPermittedRoles(role) {
    let roles = [];
    switch(role) {
        case "OWNER": roles.push("OWNER");
        case "ADMIN": roles.push("ADMIN");
        case "DEVELOPER": roles.push("DEVELOPER");
    }
    return roles;
}