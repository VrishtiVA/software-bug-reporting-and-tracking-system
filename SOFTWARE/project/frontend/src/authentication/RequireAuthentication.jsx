
import { Navigate, useLocation } from "react-router-dom";
import { useAuthentication } from "./AuthenticationContext";

/**
 * If the user is authenticated, allow children (page entry and display),
 * otherwise redirect to signin, replacing where they were in the history stack.
 * Remember target URL incase side-quested to obtain authentication, 
 * so that the user can be taken straight where they wanted to go once completed.
 */
export function RequireAuthentication({children}) {

    const targetLocation = useLocation(); //Requested target URL.
    const { isAuthenticated, isSigningOut } = useAuthentication();

    return (
        isAuthenticated 
            ? children 
            : isSigningOut
                ? <Navigate to={`/signin`} replace /> 
                : <Navigate to={`/signin?redirect=${targetLocation.pathname}`} replace />
    );
}