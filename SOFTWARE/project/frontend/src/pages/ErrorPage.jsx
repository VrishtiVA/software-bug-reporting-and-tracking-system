import { Container, Typography, Box, Button, Stack } from "@mui/material";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useError } from "../components/contexts/ErrorContext";

export default function ErrorPage() {

    const { errorCode, errorTitle, errorMessage } = useError();
    const navigate = useNavigate();

    return (<>
        <Container maxWidth="sm" sx={{alignContent:"center", height:"50vh"}}>
            <Stack textAlign="center" spacing={1} >
                <Typography variant="h3"> {errorTitle} </Typography>
                <Typography variant="body1" textAlign="center">
                    {errorMessage}
                </Typography>
                <Box>
                    <Button variant="contained" onClick={() => navigate("/workspaces")} sx={{mt: 1}}>
                        Return Home
                    </Button>
                </Box>
            </Stack>
        </Container>
    </>);

}