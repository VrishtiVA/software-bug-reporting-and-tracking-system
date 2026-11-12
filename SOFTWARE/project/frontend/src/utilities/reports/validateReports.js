
/**
 * Method to validate the report summary.
 * - maximum 50 characters,
 * - minimum 10 characters (!focussed)
 * @param {*} summary The summary to validate.
 * @param {*} focussed If the field is currently focussed.
 * @returns [validityState: bool, helperText: string]
 */
export function checkReportSummaryValidity(summary, focussed) {
    let summaryInput = summary.trim();
    let minLengthCheck = summaryInput.length >= 10;
    let maxLengthCheck = summaryInput.length <= 50;
    if (!maxLengthCheck) {
        return [false, "Input length should be within 50 characters"];
    } else if (!focussed && !minLengthCheck) {
        return [false, "Input length should be at least 10 characters"];
    } else {
        return [true, ""];
    }
}

/**
 * Method to validate report long text inputs.
 * - minimum 10 characters unless optional (!focussed)
 * - maximum 500 characters
 * @param {*} text The input to validate.
 * @param {*} optional If the field is optional, in which case minimum does not have to be enforced.
 * @param {*} focussed If the field is currently focussed.
 * @returns [validityState: bool, helperText: string]
 */
export function checkReportLongTextValidity(text, optional = false, focussed) {
    let textInput = text.trim();
    let minLengthCheck = textInput.length >= 10;
    let maxLengthCheck = textInput.length <= 500;
    if (!maxLengthCheck) {
        return [false, "Input length should be within 500 characters"];
    } else if (!optional && !focussed && !minLengthCheck) {
        return [false, "Input length should be at least 10 characters"];
    } else {
        return [true, ""];
    }
}

/**
 * Method to validate report long text inputs.
 * - maximum 100 characters
 * @param {*} text The input to validate.
 * @param {*} focussed If the field is currently focussed.
 * @returns [validityState: bool, helperText: string]
 */
export function checkCaptionValidity(caption, focussed) {
    let captionInput = caption.trim();
    let maxLengthCheck = captionInput.length <= 100;
    if (!maxLengthCheck) {
        return [false, "Input length should be within 100 characters"];
    } else {
        return [true, ""];
    }
}