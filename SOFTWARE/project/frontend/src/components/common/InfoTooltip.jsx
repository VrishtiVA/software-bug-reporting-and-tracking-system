import { IconButton, Tooltip } from "@mui/material";
import InfoOutlineIcon from '@mui/icons-material/InfoOutlineRounded';

export default function InfoTooltip({title}) {
    return (<>
        <Tooltip title={title}>
            <IconButton component="span" size="small" sx={{zIndex: 1}}>
                <InfoOutlineIcon fontSize="inherit" />
            </IconButton>
        </Tooltip>
    </>)
}