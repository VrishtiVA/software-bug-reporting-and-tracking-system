
import axios from 'axios'

/**
 * Method to validate email
 * @param {*} email The email input
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
function checkEmailValidity(email, focussed) {
    let emailInput = email.trim();
    let emailFormatCheck = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]+$/.test(emailInput);
    if (!focussed && !emailFormatCheck) {
        return [false, "Email is invalid"]
    } else {
        return [true, ""]
    }
}

/**
 * Method to validate username
 * @param {*} username The username input
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
async function checkUsernameValidity(username, focussed) {
    let usernameInput = username.trim();
    
    let noUnsupportedCheck = /^[A-Za-z0-9_]*$/.test(usernameInput);
    let minLengthCheck = usernameInput.length >= 3;
    let maxLengthCheck = usernameInput.length < 20;

    console.log(noUnsupportedCheck, minLengthCheck, maxLengthCheck);
    if (!noUnsupportedCheck) {
        //Find which character was unsupported.
        let unsupportedCharacter = usernameInput.match(/[^A-Za-z0-9_]/g)[0]
        return [false, `Username contains an unsupported character: '${unsupportedCharacter}'`];
    } else if (!maxLengthCheck) {
        //If username is too long
        return [false, "Username length should be within 20 characters"];
    } else if (!focussed && !minLengthCheck) {
        //If username is too short
        return [false, "Username length should be at least 3 characters"];
    } else {
        if (minLengthCheck) {
            //Check unique
            try {
                const availabilityCheck = await axios.get("/api/auth/validate/is-username-available", {
                    params: {username: usernameInput}
                });
                if (!availabilityCheck.data) {
                    return [false, "Username is already taken"];
                } else {
                    return [true, ""];
                }
            } catch (err) {
                //Will catch validation afterwards.
                return [true, ""];
            }
        } else {
            return [true, ""];
        }
    }
}

/**
 * Method to validate password, ensuring
 * - at least 8 characters.
 * - within maximum of 25 characters.
 * - no unsupported characters
 * @param {string} password The input password
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
function checkPasswordValidity(password, focussed) {

    let passwordInput = password;

    // Validation check
    let noUnsupportedCheck = /^[A-Za-z0-9!@#$%^&*_-]*$/.test(passwordInput);
    let maxLengthCheck = passwordInput.length < 64;
    let minLengthCheck = passwordInput.length >= 8;

    if (!noUnsupportedCheck) {
        //Find which character was unsupported.
        let unsupportedCharacter = passwordInput.match(/[^A-Za-z0-9!@#$%^&*_-]/g)[0]
        return [false, `Password contains an unsupported character: '${unsupportedCharacter}'`];
    } else if (!maxLengthCheck) {
        //If password is too long
        return [false, "Password length should be within 64 characters"];
    } else if (!focussed && !minLengthCheck) {
        //If password is too short
        return [false, "Password length should be at least 8 characters"];
    } else {
        return [true, ""];        
    }
}

/**
 * Method to validate password matches confirmed password.
 * @param {*} password The password input
 * @param {*} passwordConfirm The confirm password input
 * @param {boolean} focussed If the field is currently focussed
 * @returns [validityState: bool, helperText: string]
 */
function checkPasswordConfirmValidity(password, passwordConfirm, focussed) {

    let passwordInput = password.trim();
    let passwordConfirmInput = passwordConfirm.trim();

    if (!focussed && passwordInput !== passwordConfirmInput) {
        return [false, "Confirm password does not match the password"];
    } else {
        return [true, ""];
    }
}

export { checkEmailValidity, checkUsernameValidity, checkPasswordValidity, checkPasswordConfirmValidity };