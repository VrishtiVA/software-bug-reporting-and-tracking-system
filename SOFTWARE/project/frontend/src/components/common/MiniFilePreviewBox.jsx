import { Box, Skeleton } from "@mui/material";
import theme from "../../styles/theme";
import PlayArrowIcon from '@mui/icons-material/PlayArrowRounded';

export default function MiniFilePreviewBox({
    preview,
    type,
    size = "40px",
    borderRadius = "4px",
    borderTopLeftRadius = "4px",
    borderTopRightRadius = "4px",
    borderBottomLeftRadius = "4px",
    borderBottomRightRadius = "4px",
}) {
    
    return (
        <Box
            width = {size}
            height = {size}
            borderRadius = {borderRadius}
            overflow="hidden"
            sx = {{
                borderTopLeftRadius: borderTopLeftRadius,
                borderTopRightRadius: borderTopRightRadius,
                borderBottomLeftRadius: borderBottomLeftRadius,
                borderBottomRightRadius: borderBottomRightRadius
            }}
            border = {"1px solid " + theme.palette.divider}
            borderLeft={borderTopLeftRadius === "0" && borderBottomLeftRadius === "0" ? "none" : ""}
            borderRight={borderTopRightRadius === "0" && borderBottomRightRadius === "0" ? "none" : ""}
        >
            { type !== null ?
                 type === "image" ?
                    <Box 
                        component="img"
                        src={preview}
                        alt="Image Upload Mini Preview"
                        sx={{
                            width: size,
                            height: size,
                            objectFit: "cover"
                        }}
                    />
                    :
                    <Box display="flex" height="100%" alignItems="center" justifyContent="center" bgcolor="#fafafa">
                        <PlayArrowIcon sx={{color: "grey"}}/>
                    </Box>
                :
                <Skeleton variant="rectangle" width="100%" height="100%"/>
            }
        </Box>
    );
}