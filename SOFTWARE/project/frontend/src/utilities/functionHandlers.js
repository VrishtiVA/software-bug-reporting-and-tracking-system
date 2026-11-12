
/* 
Useful reading for debouncing in JavaScript in:
- https://www.geeksforgeeks.org/javascript/debouncing-in-javascript/ 
*/

/**
 * Function to debounce/delay the runtime of a function.
 * This is helpful for improving performance.
 * @param {Function} fn The function to delay.
 * @param {*} delay The number of milliseconds to delay by.
 * @returns Function wrapped with timeout handling.
 */
export function debounce(fn, delay) {
    let timeout;
    return (...args) => { 
        clearTimeout(timeout); //Cancel previous calls.
        timeout = setTimeout(() => fn.apply(this, args), delay);
    }
}

/**
 * Function to check if elapsed time since timestamp is within the last 2 minutes.
 * @param {String} timestamp The timestamp as a string.
 * @returns true elapsed time since timestamp is within the last 2 minutes, otherwise false.
 */
export function isTimestampWithinLast2Minutes(timestamp) {
    let sentTime = new Date(timestamp).getTime(); //Get Unix Epoch
    let currentTime = Date.now() //is also Unix Epoch

    console.log(sentTime, currentTime, currentTime - sentTime <= (2 * 60 * 1000))

    //Return if elapsed time is within 2 minutes.
    return currentTime - sentTime <= (2 * 60 * 1000)
}