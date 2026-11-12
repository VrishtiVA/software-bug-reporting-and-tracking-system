import { Box, Chip, Typography } from "@mui/material";
import React from "react";
import { toCamelCase } from "../../utilities/stringHandlers";
import dayjs from "dayjs"; //Uses users local timezone by default.

function ReportMetadataLine({
    user,
    userRole = null,
    creationTimestamp
}) {
    return (<>
        <Box display="flex" alignItems="center" gap={0.5} flexWrap="wrap">
            <Typography variant="body2">Reported by @{user.username} </Typography>
            {userRole !== null &&
                <Chip size="small" label={toCamelCase(userRole)}/>
            }
            <Typography variant="caption" color="text.secondary">&bull; {dayjs(creationTimestamp).format("MMM D, YYYY h:mm A")}</Typography>
        </Box>
    </>);
}

export default React.memo(ReportMetadataLine);