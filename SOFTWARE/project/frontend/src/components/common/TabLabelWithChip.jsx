import { Box, Chip } from "@mui/material";

export default function TabLabelWithChip({tabLabel, chipLabel, hideChip = false}) {
    
    return (<>
        <Box display="flex" alignItems="center" gap="5px">
            {tabLabel}
            {!hideChip &&
                <Chip label={chipLabel} strength="weak" size="small"/>
            }
        </Box>
    </>);
}