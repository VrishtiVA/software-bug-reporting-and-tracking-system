import { Button, styled } from "@mui/material";

/**
 * A more plainly styled button to match input boxes.
 */
export const DefaultButton = styled(Button)(() => ({
    height: "40px",
    minWidth: "85px",
    color: "black",
    border: `1px solid silver`,
    ".MuiSvgIcon-root": {
        color: "rgba(0,0,0,0.54)"
    }
}));