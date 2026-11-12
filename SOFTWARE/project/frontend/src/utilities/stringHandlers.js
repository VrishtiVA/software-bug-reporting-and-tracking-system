/**
 * Function to find the insignificant article of a word.
 * (reminds me the gcse days where I wrote pages about its significance)..
 * @param {String} word A word.
 * @returns an article "a" or "an" depending on vowel of word.
 */
export function findArticle(word) {
    if (!word) return "";
    return ['a', 'e', 'i', 'o', 'u'].includes(word[0].toLowerCase()) ? "an" : "a";
}

/** Function to covert a word into camel case. */
export function toCamelCase(word) {
    return word.toUpperCase()[0] + word.toLowerCase().slice(1);
}

/** Function to remove empty whitespace from string and set to lowercase. */
export function normaliseText(string) {
    return string.trim().replace(/\s+/g, " ").toLowerCase();
}

/**
 * Function to convert a float into a 2dp percentage, 
 * so that it can be used in a string. 
 */
export function to2dpPercentage(float) {
    return Math.round(float*10000)/100
}