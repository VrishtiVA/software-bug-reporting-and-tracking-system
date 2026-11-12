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

import api from '../../api/axios';

import { useAuthentication } from '../../authentication/AuthenticationContext';
import ErrorAlertComponent from '../../components/common/ErrorAlertComponent';

export default function SigninPage() {

    const navigate = useNavigate();
    const { setSignedIn } = useAuthentication();
    const [queryParams, setQueryParams] = useSearchParams();
    const targetLocation = queryParams.get("redirect") || "/";

    //Signin Data
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    //Validation
    const [usernameHelperText, setUsernameHelperText] = useState('');
    const [isUsernameValid, setUsernameValid] = useState(true);
    const [passwordHelperText, setPasswordHelperText] = useState('');
    const [isPasswordValid, setPasswordValid] = useState(true);

    //Error Object
    const [errorObject, setErrorObject] = useState({
        show: false,
        message: "",
        details: ""
    });
    const resetErrorObject = () => {setErrorObject({show: false, message: "", details: ""});}
    const hideErrorObject = (prev) => {setErrorObject({...prev, show: false})}

    //Visibility State
    const [showPassword, setShowPassword] = useState(false);
    const [submitEnabled, setSubmitEnabled] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);

    //Handle form submission
    const handleSubmit = async (event) => {
        event.preventDefault();
        hideErrorObject();
        setSubmitEnabled(false);

        let usernameInput = username.trim();
        let passwordInput = password.trim();

        let valid = true;
        if (usernameInput === "") {
            valid = false;
            setUsernameHelperText("This field should be filled");
            setUsernameValid(false);
        } 
        if (passwordInput === "") {
            valid = false;
            setPasswordHelperText("This field should be filled");
            setPasswordValid(false);
        }
        if (valid) {
            setUsernameHelperText("");
            setPasswordHelperText("");
            setUsernameValid(true);
            setPasswordValid(true);
        } else {
            setSubmitEnabled(true);
            return;
        }

        //Handle submit
        try {
            //Set button to loading.
            setIsSubmitting(true);
            let signinResponse = await api.post("/auth/signin", {
                username: usernameInput,
                password: passwordInput
            });

            //Successful if reached here
            resetErrorObject();
            //Set signed in
            setSignedIn(signinResponse.data.jwtToken, signinResponse.data.currentUser, signinResponse.data.avatarColour);
            //Redirect to target location
            navigate(targetLocation);

            //In case user still here
            setIsSubmitting(false);

        } catch (err) {
            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Sign in failed",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Sign in failed",
                    details: "Unable to reach server."
                });
            }
            setIsSubmitting(false);
            setSubmitEnabled(true);
        }

    }

    return (<>
        <Container maxWidth="xs" style={{ marginTop: "5vh" }}>
            <Card 
                variant="outlined"
                // elevation={2}
                sx={{borderRadius: "10px", padding: "5px 15px"}}
            >
                <CardContent>
                    <Typography variant="overline" color="primary" fontWeight="bold"> MY BUGSPACE </Typography>
                    <Typography variant="h4"> Welcome Back. </Typography>
                    <Typography variant="subtitle1"> Sign in to continue </Typography>
                    
                    {/* Alert appears here */}
                    <ErrorAlertComponent
                        errorObject={errorObject}
                        closeMethod={hideErrorObject}
                        marginTop={"0.5rem"}
                    />

                    <Box component="form" onSubmit={handleSubmit} style={{ marginTop: "1.25rem" }}>
                        <Stack spacing={2} alignItems="flex-start">
                            
                            <TextField label="Username" name="text" type="text" variant="outlined" fullWidth
                                value={username}
                                error={!isUsernameValid}
                                helperText={usernameHelperText}
                                onChange={(element) => {
                                    setUsername(element.target.value)
                                    setUsernameValid(true);
                                    setUsernameHelperText("");
                                }}
                            />

                            <TextField label="Password" name="password" variant="outlined" fullWidth
                                value={password}
                                error={!isPasswordValid}
                                helperText={passwordHelperText}
                                onChange={(element) => {
                                    setPassword(element.target.value)
                                    setPasswordValid(true);
                                    setPasswordHelperText("");
                                }}
                                type={showPassword ? "text" : "password"}
                                slotProps={{ input: {
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
                                }}}
                                //Hide browser eye
                                sx={{
                                    "& input::-ms-reveal": {display: "none"},
                                    "& input::-ms-clear": {display: "none"}
                                }}
                            />

                        </Stack>
                        <Stack spacing={1} marginTop="1.5rem">
                            <Button type="submit" variant="contained" startIcon={<LoginIcon />} disabled={!submitEnabled} loading={isSubmitting}>
                                Sign In
                            </Button>
                            <Typography variant="subtitle2">
                                Not yet registered? 
                                <Button 
                                    component={Link} 
                                    to={targetLocation == "/" ? "/signup" : `/signup?redirect=${targetLocation}`} 
                                    variant="text" 
                                    size="small"
                                > Sign Up </Button> 
                            </Typography>
                        </Stack>
                    </Box>
                </CardContent>
            </Card>
        </Container>
    </>);
}