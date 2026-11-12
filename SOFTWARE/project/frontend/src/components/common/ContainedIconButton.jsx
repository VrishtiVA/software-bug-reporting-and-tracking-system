import { IconButton, Tooltip } from "@mui/material";
import theme from "../../styles/theme";

/**
 * Component to create a Contained version of the IconButton.
 */
export default function ContainedIconButton({
    icon, 
    onClick, 
    loading, 
    disabled=false, 
    tooltip = null, 
    disabledTooltip = null,
    variant = null,
    noBorderLeft = false,
    noBorderRight = false,
    colourGrey = false
}) {

    const tooltipText = disabled 
        ? (disabledTooltip ?? "") 
        : (tooltip ?? "");

    const borderColour = colourGrey ? "silver" : theme.palette.divider

    return (<>
        <Tooltip title={tooltipText}>
            <span style={{
                cursor: tooltipText ? "help" : "default"
            }}>
                <IconButton
                    onClick={onClick}
                    loading={loading}
                    disabled={disabled}
                    sx={{
                        color: !disabled && variant != null && "white",
                        boxShadow: 
                            (noBorderLeft && noBorderRight) ? `inset 0 1px 0 0 ${borderColour}, inset 0 -1px 0 0 ${borderColour}`
                            : (noBorderLeft) ? `inset 0 1px 0 0 ${borderColour}, inset 0 -1px 0 0 ${borderColour}, inset -1px 0 0 0 ${borderColour}`
                            : (noBorderRight) ? `inset 0 1px 0 0 ${borderColour}, inset 0 -1px 0 0 ${borderColour}, inset 1px 0 0 0 ${borderColour}`
                            : "inset 0 0 0 1px " + borderColour,
                        // borderRadius: "4px",
                        borderTopLeftRadius: noBorderLeft ? "0" : "4px",
                        borderBottomLeftRadius: noBorderLeft ? "0" : "4px",
                        borderTopRightRadius: noBorderRight ? "0" : "4px",
                        borderBottomRightRadius: noBorderRight ? "0" : "4px",
                        backgroundColor: !disabled && (
                            (variant==="success" && theme.palette.success.main) || 
                            (variant==="danger" && theme.palette.error.main)
                        ),
                        "&:hover": {
                            backgroundColor: !disabled && (
                                (variant==="success" && theme.palette.success.dark) ||
                                (variant==="danger" && theme.palette.error.dark)
                            )
                        }
                    }}
                >
                    {icon}
                </IconButton>
            </span>
        </Tooltip>
    </>);
}