import { Alert, Collapse, Typography } from "@mui/material";

export default function ErrorAlertComponent({
    errorObject,
    closeMethod,
    marginBottom = null,
    marginTop = null,
}) {
    return (<>
        <Collapse in={errorObject.show} timeout={300}>
            <Alert
                severity="error"
                onClose={closeMethod}
                sx={{
                    ...(marginBottom && {
                        mb: marginBottom
                    }),
                    ...(marginTop && {
                        mt: marginTop
                    })
                }}
            >
                <Typography variant="body1">{errorObject.message}</Typography>
                <Typography variant="subtitle2">{errorObject.details}</Typography>
            </Alert>
        </Collapse>
    </>);
}