import api from "../../api/axios";

/**
 * Function to validate workspace name.
 * @param {*} name The workspace name input.
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
export function checkWorkspaceNameValidity(name, focussed) {
    let nameInput = name.trim();

    let noUnsupportedCheck = /^[A-Za-z0-9_\- ]*$/.test(nameInput);
    let atLeastOneLetterCheck = /[A-Za-z]/.test(nameInput);
    let minLengthCheck = nameInput.length >= 3;
    let maxLengthCheck = nameInput.length <= 30;

    if (!noUnsupportedCheck) {
        //If contains unsupported character, find which it was and show.
        let unsupportedCharacter = nameInput.match(/[^A-Za-z0-9_\- ]/g)[0]
        return [false, `Workspace name contains an unsupported character: ${unsupportedCharacter}`];
    } else if (!maxLengthCheck) {
        //If name is too long.
        return [false, "Workspace name length should be within 30 characters"];
    } else if (!focussed && !minLengthCheck) {
        //If name is too short when not focussed.
        return [false, "Workspace name should contain at least 3 characters"];
    } else if (!focussed && !atLeastOneLetterCheck) {
        //If there is not at least 1 letter when not focussed.
        return [false, "Workspace name should contain at least 1 letter"];
    } else {
        return [true, ""];
    }
}

/**
 * Function to validate workspace description.
 * @param {*} description The workspace description input
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
export function checkWorkspaceDescriptionValidity(description, focussed) {
    let descriptionInput = description.trim();

    let maxLengthCheck = descriptionInput.length <= 500;
    if (!maxLengthCheck) {
        return [false, "Description length should be within 500 characters"];
    } else {
        return [true, ""];
    }
}

/**
 * Function to validate custom workspace label input
 * @param {*} labelName The custom workspace label input.
 * @param {boolean} focussed If the field is currently focussed
 * @param {boolean} isBlurCall If was called onBlur.
 * @returns [validityState: bool, helperText: string]
 */
export function checkWorkspaceCustomLabelNameValidity(labelName, focussed, isBlurCall) {
    let nameInput = labelName.trim();

    let noUnsupportedCheck = /^[A-Za-z0-9_\- ]*$/.test(nameInput);
    let minLengthCheck = nameInput.length >= 1;
    let maxLengthCheck = nameInput.length <= 20;

    if (!noUnsupportedCheck) {
        //If contains unsupported character, find which it was and show.
        let unsupportedCharacter = nameInput.match(/[^A-Za-z0-9_\- ]/g)[0]
        return [false, `Label contains an unsupported character: ${unsupportedCharacter}`];
    } else if (!maxLengthCheck) {
        //If label is too long.
        return [false, "Label length should be within 20 characters"];
    } else if (!focussed && !minLengthCheck) {
        //If label is too short when not focussed.
        return [isBlurCall, "Label should include at least 1 character"];
    } else {
        return [true, ""];
    }
}

/**
 * Function to validate if a user can be added to a workspace on creation.
 * @param {*} username The member username input.
 * @returns [validityState: bool, user summary object | error message]
 */
export async function checkIfUserCanBecomeMember(username) {
    username = username.trim();
    try {
        let userExistsResponse = await api.get("/workspaces/create/validate/member/"+username);
        return [true, userExistsResponse];

    } catch (err) {
        console.log(err);
        if (err.response?.status === 400) {
            console.log("You can't add yourself")
            return [false, err.response.data.details];
        } else if (err.response?.status === 404) {
            console.log("User doesn't exist, will set error and helper text.")
            return [false, err.response.data.details];
        } else {
            console.log("Something went wrong");
            return [false, "Something went wrong."];
        }
    }
}

/**
 * Function to perform just a presence, length and format check on 
 * the member username input.
 * @param {*} username The member username input
 * @param {boolean} isBlurCall if it was called onBlur
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
export function checkWorkspaceMemberUsernameValidity(username, focussed, isBlurCall) {
    let usernameInput = username.trim();

    let noUnsupportedCheck = /^[A-Za-z0-9_]*$/.test(usernameInput);    
    let minLengthCheck = username.length >= 3;
    let maxLengthCheck = username.length <= 20;
    if (!noUnsupportedCheck) {
        //Find which character was unsupported.
        let unsupportedCharacter = usernameInput.match(/[^A-Za-z0-9_]/g)[0]
        return [false, `Input contains an unsupported character: '${unsupportedCharacter}'`];
    } else if (!maxLengthCheck) {
        //If username is too long
        return [false, "Input length should be within 20 characters"];
    } else if (!focussed && !minLengthCheck) {
        //If username is too short
        return [isBlurCall, "Input length should be atleast 3 characters"];
    } else {
        return [true, ""];
    }
}