
import axios from "axios";
import { jwtDecode } from "jwt-decode";

let displaySnackbarMethod = null;
let navigationMethod = null;
let signoutMethod = null;
let updateErrorMethod = null;

/**
 * Set the same method to display a snackbar from snackbar provider.
 * Allow use from here, which is outside of a react component.
 */ 
export function setSnackbarHandlerInAxiosJS(method) {displaySnackbarMethod = method;}

/**
 * Set the same method to navigate from useNavigate. 
 * Allow use from here, which is outside of a react component.
 */ 
export function setNavigationMethodInAxiosJS(method) {navigationMethod = method;}

/**
 * Method to update authentication context from here.
 * Useful for if token expired.
 */
export function setSignOutMethodInAxiosJS(method) {signoutMethod = method;}

/** Method to update error context from here. */
export function setUpdateErrorContextInAxiosJS(method) {updateErrorMethod = method;}

const TOKEN_KEY = "jwtToken";

//Creating centralised axios instance for easier management.
//Currently using /api through defined proxy configuration.
const api = axios.create({
    baseURL: "/api"
})

//Request interceptor to add JWT to header for requests
api.interceptors.request.use(
    //Adding JWT if found to header before request is sent
    (config) => {
        const token = localStorage.getItem(TOKEN_KEY);
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        //Return modified configuration
        return config;
    },
    //If an error occurs, provide this error which can be caught.
    (error) => Promise.reject(error)
)

/**
 * Function to handle signout from here.
 */
function signOut() {
    if (signoutMethod) {
        signoutMethod();
    } else {
        console.log("Simple signout method not found in axios.js. Will use fallback.");
        localStorage.removeItem(TOKEN_KEY);
    }
}

//Intercepting important errors
api.interceptors.response.use(
    (response) => response, //Allow successful responses through.
    (error) => {

        /* ----- 403 FORBIDDEN ----- */
        //e.g. no token, token expired, or no permitted to access endpoint. 
        //Returning to signin or error page depending on situation.
        if (error.response?.status === 403 && !(error.config?.url).includes("/signin") && !(error.config?.url).includes("/signup")) {

            //Find JWT Token
            let token = localStorage.getItem(TOKEN_KEY);
            if (token) {
                //Check if JWT expired
                let isJwtExpired = false;
                try {
                    //payload.exp will be the no.seconds after unix epoch.
                    let payload = jwtDecode(token);
                    //Converting now to seconds too to match, and
                    //has been offset to consider client clock and prevent using token that is about to expire.
                    let now = Math.floor(Date.now() / 1000) + 30;
                    isJwtExpired = payload.exp < now;
                    console.log("Is JWT Expired?: " + isJwtExpired);

                } catch (err) { 
                    //Signout if malformed token
                    signOut();
                    console.log("Malformed token")
                    navigationMethod("/signin");
                    return Promise.reject(err);
                }

                //If expired
                if (isJwtExpired) {
                    console.log("Expired token")
                    //Signout
                    signOut();
                    //Send a snackbar with a message to let the user know what happened.
                    if (displaySnackbarMethod) {
                        displaySnackbarMethod("warning", "You were signed out", "Please sign in again.");
                    } else {
                        console.log("Display snackbar method not found in axios.js.")
                    }
                    //Go to signin page
                    navigationMethod("/signin");
                    return Promise.reject(error);
                } else {
                    //Response from a forbidden endpoint, set error message and go to error page.
                    console.log("Forbidden endpoint")
                    updateErrorMethod(403, error.response.data?.message ?? null, error.response.data?.details ?? null);
                    navigationMethod("/error")
                    return Promise.reject(error);
                }
            } else { 
                //Otherwise, if user has no token, signout and go to signin page.
                console.log("No token found")
                signOut();
                navigationMethod("/signin");
                return Promise.reject(error);
            }

        /* ----- 404 NOT FOUND ----- */
        } else if (error.response?.status === 404 && error.response.data?.redirect === true) {
            //set error message and go to error page
            updateErrorMethod(404, error.response.data?.message ?? null, error.response.data?.details ?? null);
            navigationMethod("/error")
            return Promise.reject(error);
        }
        
        /* ----- 500 SERVER ERROR ----- */
        else if (error.response?.status === 500 && !(error.config?.url).includes("/signin") && !(error.config?.url).includes("/signup")) {
            if (error.response.data?.redirect === true) {
                //Redirect if requested to by server, set error message if there is one.
                updateErrorMethod(500, error.response.data?.message ?? null, error.response.data?.details ?? null);
                navigationMethod("/error")
                return Promise.reject(error);
            } else if (error.response.data?.redirect === false) {
                //Server error occured but it's fine, just display snackbar to avoid distruption.
                displaySnackbarMethod("error", "Server Error", "A server error occurred.");
                return Promise.reject(error);
            } else {
                //Unable to reach server.
                updateErrorMethod(500, "Oops!", "Sorry, we're unable to reach the server at the moment.");
                navigationMethod("/error")
                return Promise.reject(error);
            }
        }
        
        //Passed error filter and send back mysterious error to code.
        return Promise.reject(error);
    }
)

export default api;