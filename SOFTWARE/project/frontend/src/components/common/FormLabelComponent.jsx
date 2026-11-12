import React from "react";
import { Box, FormLabel, IconButton, Tooltip, Typography } from "@mui/material";
import InfoOutlineIcon from '@mui/icons-material/InfoOutlineRounded';
import InfoTooltip from "./InfoTooltip";

const FormLabelComponent = React.memo(function FormLabelComponent({
    label, 
    keyword = null,
    tooltipText = null, 
    asLabel = true,
    margins = true
}) {

    /**
     * Handle bolding a word in the label when keyword given.
     */
    function HighlightedLabel() {
        if (keyword === null || keyword === "") {
            return label;
        } else {
            //gi = global ignore case, to match all occurances of the keyward in the label.
            //using RegExp constructor for dynamic regular expression.
            const segments = label.split(new RegExp(`(${keyword})`, "gi"))
            return (<>
                {segments.map((segment, i) => (
                    segment.toLowerCase() === keyword.toLowerCase() ? 
                        <b key={i}>{segment}</b> 
                        : 
                        segment
                ))}
            </>);
        }
    }

    return (<>
        <Box display="flex" alignItems="center" gap="1px" 
            sx={{mb: (margins ? 0.25 : 0)}}
        >
            {asLabel ?
                <FormLabel sx={{
                    color: "rgba(0,0,0,0.8)",
                    pointerEvents:"none"
                }}> 
                    <HighlightedLabel/>
                </FormLabel>
            :
                <Typography sx={{
                    color: "rgba(0,0,0,0.8)",
                    pointerEvents:"none"
                }}>
                    <HighlightedLabel/>
                </Typography>
            }
            {tooltipText && 
                <InfoTooltip title={tooltipText}/>
            }
        </Box>
    </>);
});

export default FormLabelComponent;