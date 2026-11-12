import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, IconButton } from "@mui/material";
import CloseIcon from '@mui/icons-material/CloseRounded';

export default function DialogComponent({
    open, 
    closeDialogMethod, 
    dialogTitle, 
    dialogContent, 
    showCancel = false, 
    showDismiss = false,
    mainActionName = null, 
    mainActionMethod, 
    mainActionType,
    mainActionDisabled = false,
    mainActionLoading = false
}) {

    return (<>
        <Dialog
            open={open}
            onClose={closeDialogMethod}
            fullWidth
        >
            <DialogTitle sx={{
                display: "flex", 
                justifyContent: "space-between", 
                alignItems: "center"
            }}>
                {dialogTitle}
                <IconButton aria-label="Close" onClick={closeDialogMethod}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent dividers>
                {dialogContent}
            </DialogContent>
            <DialogActions>
                {showCancel &&
                    <Button 
                        variant="outlined" 
                        onClick={closeDialogMethod}
                        disabled={mainActionLoading}
                    > 
                        Cancel 
                    </Button>
                }
                {mainActionName &&
                    <Button 
                        variant="contained" 
                        color={mainActionType} 
                        onClick={mainActionMethod} 
                        autoFocus
                        disabled={mainActionDisabled}
                        loading={mainActionLoading}
                    > 
                        {mainActionName} 
                    </Button>
                }
                {showDismiss &&
                    <Button variant="outlined" onClick={closeDialogMethod}> Close </Button>
                }
            </DialogActions>
        </Dialog>
    </>);
}