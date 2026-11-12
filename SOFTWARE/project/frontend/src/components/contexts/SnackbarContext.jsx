import { useCallback, createContext, useState, useContext, useEffect } from "react";
import { Alert, AlertTitle, Snackbar, Typography } from "@mui/material";
import { setSnackbarHandlerInAxiosJS } from "../../api/axios";

const SnackbarContext = createContext({
    displaySnackbar: () => {},
    closeSnackbar: () => {},
    displayNotificationToast: () => {}
});

export function useSnackbar() {
    return useContext(SnackbarContext);
}

/**
 * Snackbar context provider to wrap around application endpoints and 
 * provide access to methods here from components across the application,
 * e.g. to display snackbars/toasts.
 */
export function SnackbarProvider({children}) {

    //Snackbar details
    const [snackbar, setSnackbar] = useState({
        open: false,
        severity: "info",
        variant: "filled",
        duration: 3000,
        message: "",
        details: "",
        icon: null,
        timestamp: null,
        anchorOriginVertical: "top",
        anchorOriginHorizontal: "center"
    })

    //Function to display snackbar for 3 seconds.
    const displaySnackbar = useCallback((
        severity = "info", 
        message, 
        details, 
        duration = 3000
    ) => {
        setSnackbar({ 
            open: true, 
            severity, 
            variant: "filled",
            duration,
            message,
            details,
            icon: null,
            timestamp: Date.now(), //For uniqueness
            anchorOriginVertical: "top",
            anchorOriginHorizontal: "center"
        })
    }, []);

    //Function to display noyification snackbar.
    const displayNotificationToast = useCallback((
        message,
        details,
        icon
    ) => {
        setSnackbar({
            open: true,
            severity: "primary",
            variant: "standard",
            duration: 5000,
            message,
            details,
            icon: icon,
            timestamp: Date.now(),
            anchorOriginVertical: "bottom",
            anchorOriginHorizontal: "right"
        })
    }) 

    //Function to close snackbar.
    const closeSnackbar = () => {
        setSnackbar((prev) => ({...prev, open: false}));
    }

    //Set snackbar handler in axios.js too once it is ready
    useEffect(() => {
        setSnackbarHandlerInAxiosJS(displaySnackbar);
    }, [])

    return (
        <SnackbarContext.Provider value={{displaySnackbar, closeSnackbar, displayNotificationToast}}>
            {children}
            <Snackbar
                key={snackbar.timestamp}
                open={snackbar.open}
                autoHideDuration={snackbar.duration}
                onClose={closeSnackbar}
                anchorOrigin={{vertical: snackbar.anchorOriginVertical, horizontal: snackbar.anchorOriginHorizontal}}
            >
                <Alert
                    variant={snackbar.variant}
                    severity={snackbar.severity}
                    onClose={closeSnackbar}
                    elevation={3}
                    {...snackbar.icon != null && {
                        icon: snackbar.icon
                    }}
                >
                    <Typography variant="body1">{snackbar.message}</Typography>
                    <Typography variant="subtitle2">{snackbar.details}</Typography>
                </Alert>
            </Snackbar>
        </SnackbarContext.Provider>
    );

}