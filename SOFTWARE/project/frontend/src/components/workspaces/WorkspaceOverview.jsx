import { Box, Card, CardActionArea, Chip, Tooltip, Typography } from "@mui/material";

import BugIcon from '@mui/icons-material/BugReportRounded';
import PeopleIcon from '@mui/icons-material/PeopleRounded';
import PrivateIcon from '@mui/icons-material/LockRounded';
import PublicIcon from '@mui/icons-material/PublicRounded'
import { Link } from "react-router-dom";
import { findArticle, toCamelCase } from "../../utilities/stringHandlers";
import { WorkspaceColours } from "../../api/colours";

export default function WorkspaceOverview({workspace, height = "100px"}) {

    const {
        id, 
        name, 
        colour, 
        is_public, 
        role, 
        member_count,
        bug_count
    } = workspace;

    return(<>
        <Card variant="outlined" sx={{backgroundColor: WorkspaceColours[colour]}}>
            <CardActionArea
                component={Link}
                to={"/workspaces/"+id}
            >
                <Box padding="10px" height={height} display="flex" flexDirection="column" justifyContent="space-between">
                    <Box display="flex" justifyContent={role ? "space-between" : "end"} flexWrap="wrap">
                        {role &&
                            <Box display="flex" alignItems="center">
                                <Tooltip title={"You are "+findArticle(role)+" "+ role.toLowerCase() +" in this workspace"}>
                                    <Chip variant="soft-square" label={toCamelCase(role)} size="small"/>
                                </Tooltip>
                            </Box>
                        }
                        <Box display="flex" gap="4px">
                            <Box display="flex" alignItems="center" gap="1px">
                                <PeopleIcon fontSize="small"/>
                                {member_count}
                            </Box>
                            <Box display="flex" alignItems="center" gap="1px">
                                <BugIcon fontSize="small"/>
                                {bug_count}
                            </Box>
                        </Box>
                    </Box>
                    <Box display="flex" alignItems="center" gap="4px">
                        <Tooltip title={"This workspace is " + (is_public ? "public" : "private")}>
                            {is_public ? <PublicIcon fontSize="small"/> : <PrivateIcon fontSize="small"/>}
                        </Tooltip>
                        {/* No wrap adds elipses */}
                        <Typography noWrap>{name}</Typography>
                    </Box>
                </Box>
            </CardActionArea>
        </Card>
    </>)

}