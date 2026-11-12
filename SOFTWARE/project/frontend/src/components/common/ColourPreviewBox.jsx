import { Box } from "@mui/material";
import theme from "../../styles/theme";

/**
 * Coloured box component.
 */
export default function ColourPreviewBox({
    colour, 
    size = "40px",
    borderRadius = "4px",
    borderTopLeftRadius = "4px",
    borderTopRightRadius = "4px",
    borderBottomLeftRadius = "4px",
    borderBottomRightRadius = "4px",
}) {
    return (<>
        <Box
            bgcolor = {colour}
            width = {size}
            height = {size}
            borderRadius = {borderRadius}
            sx = {{
                borderTopLeftRadius: borderTopLeftRadius,
                borderTopRightRadius: borderTopRightRadius,
                borderBottomLeftRadius: borderBottomLeftRadius,
                borderBottomRightRadius: borderBottomRightRadius
            }}
            border = {"1px solid " + theme.palette.divider}
            borderLeft={borderTopLeftRadius === "0" && borderBottomLeftRadius === "0" ? "none" : ""}
            borderRight={borderTopRightRadius === "0" && borderBottomRightRadius === "0" ? "none" : ""}
        />
    </>)
}