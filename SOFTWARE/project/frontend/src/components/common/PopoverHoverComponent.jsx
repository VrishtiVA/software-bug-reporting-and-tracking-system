import { Card, CardContent, Paper, Popover } from "@mui/material";

export default function PopoverHoverComponent({
    anchorEl,
    open,
    onClose,
    anchorOrigin,
    transformOrigin,
    popoverContent
}) {
    return (<>
        <Popover 
            anchorEl={anchorEl}
            open={open}
            onClose={onClose}
            anchorOrigin={anchorOrigin}
            transformOrigin={transformOrigin}
            elevation={3}
            disableRestoreFocus
            slotProps={{
                paper: {
                    sx: {
                        p: 1,
                        maxHeight: "40vh",
                        overflowY: "auto"
                    }
                }
            }}

        >   
            {popoverContent}
        </Popover>
    </>);
}