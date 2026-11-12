import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { 
    Button, 
    TextField, 
    Container, 
    Typography, 
    Box, 
    InputAdornment, 
    IconButton, 
    Stack, 
    Card,
    CardContent
} from "@mui/material";
import VisibilityIcon from '@mui/icons-material/VisibilityRounded';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOffRounded';
import LoginIcon from '@mui/icons-material/LoginRounded';

import api from '../../api/axios.js';

import { checkEmailValidity, checkUsernameValidity, checkPasswordValidity, checkPasswordConfirmValidity } from '../../authentication/signupValidation.js';
import { useAuthentication } from '../../authentication/AuthenticationContext.jsx';
import ErrorAlertComponent from '../../components/common/ErrorAlertComponent.jsx';

export default function SignupPage() {

    const navigate = useNavigate();
    const { setSignedIn } = useAuthentication(); 
    const [queryParams, setQueryParams] = useSearchParams();
    const targetLocation = queryParams.get("redirect") || "/";

    //Signup Data
    const [email, setEmail] = useState('')
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState('');

    //Show/hide for passwords
    const [showPassword, setShowPassword] = useState(false);
    const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
    const [submitEnabled, setSubmitEnabled] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);

    //Error Object
    const [errorObject, setErrorObject] = useState({
        show: false,
        message: "",
        details: ""
    });
    const resetErrorObject = () => {setErrorObject({show: false, message: "", details: ""});}
    const hideErrorObject = (prev) => {setErrorObject({...prev, show: false})}

    //Validation display states
    const [emailHelperText, setEmailHelperText] = useState('');
    const [isEmailValid, setEmailValid] = useState(true);
    const [usernameHelperText, setUsernameHelperText] = useState('');
    const [isUsernameValid, setUsernameValid] = useState(true);
    const [passwordHelperText, setPasswordHelperText] = useState('');
    const [isPasswordValid, setPasswordValid] = useState(true);
    const [passwordConfirmHelperText, setPasswordConfirmHelperText] = useState('');
    const [isPasswordConfirmValid, setPasswordConfirmValid] = useState(true);

    //Run after render every time username updates, 
    //to debounce check while typing to prevent overloading server.
    useEffect(() => {
        //Validate after 250ms of no change
        const timeout = setTimeout(async () => {
            await checkUsername(username, true);
        }, 250);
        //Otherwise cancel timeout
        return () => clearTimeout(timeout);
    }, [username]);

    /**
     * Method to check email validity.
     * @param {*} email The email input.
     * @param {*} focussed Whether the input box is currently selected or not.
     * @returns if valid or not.
     */
    function checkEmail(email, focussed) {
        let [validityState, helperMessage] = checkEmailValidity(email, focussed);
        setEmailValid(validityState);
        setEmailHelperText(helperMessage);
        return (validityState);
    }

    /**
     * Method to check username validity.
     * @param {*} username The username input.
     * @param {*} focussed Whether the input box is currently selected or not.
     * @returns if valid or not
     */
    async function checkUsername(username, focussed) {
        const [validityState, helperMessage] = await checkUsernameValidity(username, focussed);
        setUsernameValid(validityState);
        setUsernameHelperText(helperMessage);
        return (validityState);
    }

    /**
     * Method to check password password validity.
     * @param {string} password The password input.
     * @param {boolean} focussed Whether the input box is currently selected or not.
     * @returns if valid or not.
     */
    function checkPassword(password, focussed) {
        let [validityState, helperMessage] = checkPasswordValidity(password, focussed);
        setPasswordValid(validityState);
        setPasswordHelperText(helperMessage);
        return (validityState);
    }

    /**
     * Method to check password confirm validity.
     * @param {*} passwordConfirm The input password confirm.
     * @param {*} focussed Whether the input box is currently selected or not.
     * @returns if valid or not.
     */
    function checkPasswordConfirm(password, passwordConfirm, focussed) {
        let [validityState, helperMessage] = checkPasswordConfirmValidity(password, passwordConfirm, focussed);
        setPasswordConfirmValid(validityState);
        setPasswordConfirmHelperText(helperMessage);
        return (validityState);
    }

    //Handle form submission
    const handleSubmit = async (event) => {
        event.preventDefault();
        hideErrorObject();
        setSubmitEnabled(false);

        //Validate
        let valid = true;
        valid = valid && checkEmail(email, false);
        valid = valid && await checkUsername(username, false);
        valid = valid && checkPassword(password, false);
        valid = valid && checkPasswordConfirm(password, passwordConfirm, false);
    
        //If not valid
        if (!valid) {
            //Reenable button
            setSubmitEnabled(true);
            return;
        }

        //Handle submit
        try {
            //Set button to loading.
            setIsSubmitting(true);
            let emailInput = email.trim();
            let usernameInput = username.trim();
            let passwordInput = password.trim();
            let signupResponse = await api.post("/auth/signup", {
                email: emailInput, 
                username: usernameInput, 
                password: passwordInput
            })

            //Successful if reached here
            resetErrorObject();
            //Set signed in
            setSignedIn(signupResponse.data.jwtToken, signupResponse.data.currentUser, signupResponse.data.avatarColour);
            //Redirect to target location
            navigate(targetLocation);

            //In case user still here
            setIsSubmitting(false);

        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Sign up failed",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Sign up failed",
                    details: "Unable to reach server."
                });
            }
            setIsSubmitting(false);
            setSubmitEnabled(true);
        }

    }

    return (<>

        <Container maxWidth="xs" style={{ marginTop: "5vh" }} >

            <Card 
                variant="outlined"
                // elevation={2}
                sx={{ borderRadius: "10px", padding: "5px 15px" }}
            >
                <CardContent>
                    <Typography variant="overline" color="primary" fontWeight="bold"> MY BUGSPACE </Typography>
                    <Typography variant="h4"> Welcome. </Typography>
                    <Typography variant="subtitle1"> Sign up to continue </Typography>
                    
                    {/* Error alert appears here */}
                    <ErrorAlertComponent
                        errorObject={errorObject}
                        closeMethod={hideErrorObject}
                        marginTop={"0.5rem"}
                    />

                    <Box component="form" noValidate onSubmit={handleSubmit} style={{ marginTop: "1.25rem" }}>

                        <Stack spacing={2} alignItems="flex-start">
                            
                            <TextField label="Email" name="email" type="email" variant="outlined" fullWidth required
                                value={email}
                                onChange={(element) => {
                                    setEmail(element.target.value)
                                    checkEmail(element.target.value, true)
                                }}
                                onBlur={(element) => checkEmail(element.target.value, false)}
                                error={!isEmailValid}
                                helperText={emailHelperText}
                            />

                            <TextField label="Username" name="username" type="text" variant="outlined" fullWidth required
                                value={username}
                                onChange={async (element) => {
                                    setUsername(element.target.value);
                                    //Check is debounced through useEffect, since includes to availability check.
                                }}
                                onBlur={async (element) => checkUsername(element.target.value, false)}
                                error={!isUsernameValid}
                                helperText={usernameHelperText}
                            />

                            <TextField label="Password" name="password" variant="outlined" fullWidth required
                                value={password}
                                onInput={(element) => {
                                    setPassword(element.target.value);
                                    checkPassword(element.target.value, true);
                                    if (passwordConfirm !== "") checkPasswordConfirm(element.target.value, passwordConfirm, false);
                                }}
                                onBlur={(element) => checkPassword(element.target.value, false)}
                                error={!isPasswordValid}
                                helperText={passwordHelperText}
                                type={showPassword ? "text" : "password"}
                                slotProps={{
                                    input: {
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    edge="end"
                                                    onClick={() => setShowPassword(!showPassword)}
                                                    title={showPassword ? "Hide password" : "Show password"}
                                                    aria-label={showPassword ? "Hide password" : "Show password"}
                                                >
                                                    {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                                </IconButton>
                                            </InputAdornment>
                                        )
                                    }
                                }}
                                //Hide browser eye
                                sx={{
                                    "& input::-ms-reveal": {display: "none"},
                                    "& input::-ms-clear": {display: "none"}
                                }}
                            />

                            <TextField label="Confirm Password" name="passwordConfirm" variant="outlined" fullWidth required
                                value={passwordConfirm}
                                onChange={(element) => {
                                    setPasswordConfirm(element.target.value);
                                    checkPasswordConfirm(password, element.target.value, false);
                                }}
                                onBlur={(element) => {checkPasswordConfirm(password, element.target.value, false)}}
                                error={!isPasswordConfirmValid}
                                helperText={passwordConfirmHelperText}
                                type={showPasswordConfirm ? "text" : "password"}
                                slotProps={{ 
                                    input: {
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    edge="end"
                                                    onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                                                    title={showPasswordConfirm ? "Hide password" : "Show password"}
                                                    aria-label={showPasswordConfirm ? "Hide password" : "Show password"}
                                                >
                                                    {showPasswordConfirm ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                                </IconButton>
                                            </InputAdornment>
                                        )
                                    }
                                }}
                                //Hide browser eye
                                sx={{
                                    "& input::-ms-reveal": {display: "none"},
                                    "& input::-ms-clear": {display: "none"}
                                }}
                            />

                        </Stack>

                        <Stack spacing={1} marginTop="1.5rem">
                            <Button type="submit" variant="contained" startIcon={<LoginIcon />} disabled={!submitEnabled} loading={isSubmitting}>
                                Sign Up
                            </Button>

                            <Typography variant="subtitle2">
                                Already registered? 
                                <Button component={Link} to={targetLocation == "/" ? `/signin` : `/signin?redirect=${targetLocation}`} variant="text" size="small"> Sign In </Button> 
                            </Typography>
                        </Stack>
                        
                    </Box>
                </CardContent>
            </Card>
        </Container>

    </>);

}