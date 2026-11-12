import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { setUpdateErrorContextInAxiosJS } from "../../api/axios";

const ErrorContext = createContext({
    errorCode: 500,
    errorTitle: "Oops",
    errorMessage: "Sorry, something went wrong.",
    setError: () => {}
})

//Custom hook to use easily in children
export function useError() {
    return useContext(ErrorContext);
}

const defaultErrorMessages = {
    403: "Sorry, you don't have permission to access this page.",
    404: "Sorry, the page you are looking for doesn't exist.",
    500: "Sorry, we couldn't find what you were looking for.",
}

/**
 * Method to find default error message for a certain error code.
 * @param {Number} erCode The error code
 * @returns The default error message.
 */
const getDefaultErrorMessage = (erCode) => (
    (!erCode || erCode === 500) ? defaultErrorMessages[500] :
    (erCode === 403) ? defaultErrorMessages[403] :
    (erCode === 404) ? defaultErrorMessages[404] : 
    defaultErrorMessages[500]
);

export function ErrorProvider({children}) {

    //Fetching back from local storage to survive page refresh on error page.
    const [errorCode, setErrorCode] = useState(500);
    const [errorTitle, setErrorTitle] = useState("Oops!");
    const [errorMessage, setErrorMessage] = useState(getDefaultErrorMessage(errorCode));
    
    /** Set error method provided by this context */
    const setError = useCallback((code = 500, title, message) => {
        let newErrorCode = Number(code) || 500;
        let newErrorTitle = title || "Oops!"; 
        let newErrorMessage = message || getDefaultErrorMessage(500);

        setErrorCode(newErrorCode);
        setErrorTitle(newErrorTitle);
        setErrorMessage(newErrorMessage);
        
        console.log("Updated error context.");
    }, []);

    //Set update error method in axios.js too once ready.
    useEffect(() => {
        setUpdateErrorContextInAxiosJS(setError);
    }, [])

    //Error context values to pass to children.
    const errorContext = useMemo(() => ({
        errorCode, errorTitle, errorMessage, setError
    }), [errorCode, errorTitle, errorMessage, setError]);

    return (
        <ErrorContext.Provider value={errorContext}>
            {children}
        </ErrorContext.Provider>
    );

}