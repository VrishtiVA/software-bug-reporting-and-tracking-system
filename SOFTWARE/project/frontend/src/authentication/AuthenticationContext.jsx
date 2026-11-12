import { useCallback, createContext, useState, useMemo, useContext, useEffect } from "react";
import { useSnackbar } from "../components/contexts/SnackbarContext";
import { setSignOutMethodInAxiosJS } from "../api/axios";
import { EventSourcePolyfill } from "event-source-polyfill";
import { isTimestampWithinLast2Minutes } from "../utilities/functionHandlers";
import { useNavigate } from "react-router-dom";

const TOKEN_KEY = "jwtToken";
const USERNAME_KEY = "currentUser";
const AVATARCOLOUR_KEY = "avatarColour";
const LAST_TOASTED_NOTIFICATION_ID_KEY = "lastToastedNotificationId";

//Authentication context
const AuthenticationContext = createContext({
    currentUser: null,
    avatarColour: null,
    isAuthenticated: false,
    unreadNotificationsCount: 0,
    latestNotification: null,
    latestNotificationToast: null,
    isSigningOut: false,
    setLatestNotificationToast: () => {},
    setSignedIn: () => {},
    setSignedOut: () => {}
});

//A custom hook to use this authentication context.
export const useAuthentication = () => {
    return useContext(AuthenticationContext);
};

//Authentication provider
export function AuthenticationProvider({children}) {

    const { displaySnackbar, displayNotificationToast } = useSnackbar();
    const navigate = useNavigate();

    const [currentUser, setCurrentUser] = useState(localStorage.getItem(USERNAME_KEY) || null);
    const [avatarColour, setAvatarColour] = useState(localStorage.getItem(AVATARCOLOUR_KEY) || null);
    const [isAuthenticated, setIsAuthenticated] = useState(currentUser ? true : false);
    const [jwtToken, setJwtToken] = useState(localStorage.getItem(TOKEN_KEY) || null);
    const [unreadNotificationsCount, setUnreadNotificationsCount] = useState(0);
    const [latestNotification, setLatestNotification] = useState(null);
    const [latestNotificationToast, setLatestNotificationToast] = useState(null);
    const [isSigningOut, setIsSigningOut] = useState(false);

    //Caching this function between re-renders with useCallback.
    //Method to set signed in state
    const setSignedIn = useCallback((token, username, colour) => {
        setIsSigningOut(false);
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USERNAME_KEY, username);
        localStorage.setItem(AVATARCOLOUR_KEY, colour);
        setCurrentUser(username);
        setAvatarColour(colour);
        setIsAuthenticated(true);
        setJwtToken(token);
        console.log("Signed In")
        displaySnackbar("success", "Signed In Successfully", null);
    }, []);

    //Handle notification arrivals via connection.
    useEffect(() => {
        if (!jwtToken) return;
        console.log("Connecting to notification stream...")

        //Establish SSE connection
        const notificationEmitter = new EventSourcePolyfill("/notifications/stream", {
            headers: {
                Authorization: `Bearer ${jwtToken}`
            }
        });

        //Listen for heartbeats
        notificationEmitter.addEventListener("heartbeat", () => {
            console.log("SSE Heartbeat");
        })

        //Listen for notifications
        notificationEmitter.addEventListener("notification", (event) => {
            const notificationsData = JSON.parse(event.data);
            setUnreadNotificationsCount(notificationsData.unread_count);
            setLatestNotification(notificationsData.notification);

            //Only display notification toast once, using local storage to remember last one that was shown.
            let currentNotificationId = String(notificationsData.notification?.id) ?? null;
            let lastToastedNotificationId = localStorage.getItem(LAST_TOASTED_NOTIFICATION_ID_KEY) || null;
            if ((
                    lastToastedNotificationId == null || 
                    lastToastedNotificationId == undefined || 
                    lastToastedNotificationId != currentNotificationId
                ) && 
                notificationsData.notification != null && isTimestampWithinLast2Minutes(notificationsData.notification.timestamp)
            ) {
                setLatestNotificationToast(notificationsData.notification);
                localStorage.setItem(LAST_TOASTED_NOTIFICATION_ID_KEY, currentNotificationId);
            }
        })

        notificationEmitter.onerror = (error) => {
            console.log("Closing Notification Stream: ", error)
            notificationEmitter.close();
        }

        return () => notificationEmitter.close();
        
    }, [jwtToken]);

    //Caching this function between re-renders with useCallback.
    //Method to set signed out state
    const setSignedOut = useCallback(() => {
        setIsSigningOut(true);
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        localStorage.removeItem(AVATARCOLOUR_KEY);
        setCurrentUser(null);
        setAvatarColour(null);
        setJwtToken(null);
        setIsAuthenticated(false);
        console.log("Signed out");
        displaySnackbar("success", "Signed Out Successfully", null);
    }, []);

    useEffect(() => {
        //Set signout method in axios.js too once ready.
        const setSignedOutSimple = () => {
            setIsSigningOut(true);
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USERNAME_KEY);
            localStorage.removeItem(AVATARCOLOUR_KEY);
            setCurrentUser(null);
            setAvatarColour(null);
            setIsAuthenticated(false);
            setJwtToken(null);
            console.log("Signed out (simple)");
        };
        setSignOutMethodInAxiosJS(setSignedOutSimple);
    }, []);

    //Values but cached, updates only if they change
    const authenticationContext = useMemo(() => ({
        currentUser, 
        avatarColour,
        isAuthenticated,
        unreadNotificationsCount,
        latestNotification,
        latestNotificationToast,
        isSigningOut,
        setLatestNotificationToast,
        setSignedIn,
        setSignedOut
    }), [
        isAuthenticated, 
        currentUser, 
        avatarColour, 
        unreadNotificationsCount, 
        latestNotification, 
        latestNotificationToast, 
        isSigningOut,
        setLatestNotificationToast, 
        setSignedIn, 
        setSignedOut
    ]);

    //Return authentication context provider wrapped around the other children.
    return (
        <AuthenticationContext.Provider value={authenticationContext}>
            {children}
        </AuthenticationContext.Provider>
    );

}