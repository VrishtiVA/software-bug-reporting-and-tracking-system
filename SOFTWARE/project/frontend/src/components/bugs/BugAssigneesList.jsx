import { Box, Chip, List, ListItem, ListItemAvatar, Typography } from "@mui/material";
import React from "react";
import UserAvatar from "../users/UserAvatar";
import { toCamelCase } from "../../utilities/stringHandlers";

/**
 * Component to display a list of bug assignees, provided workspace member objects.
 */
function BugAssigneesList({bugAssignees}) {
    return (<>
        <List disablePadding>
            {(bugAssignees == null || bugAssignees.length == 0) ? 
                <Typography px={0.5} variant="subtitle2">No assignees</Typography>
                :
                bugAssignees.map((v, i) => (
                    <ListItem dense disableGutters sx={{px: 1}} key={v.user.user_id}>
                        <ListItemAvatar>
                            <UserAvatar size="24px" username={v.user.username} avatarColour={v.user.user_colour}/>
                        </ListItemAvatar>
                        <Box display="flex" gap={0.75} alignItems={"center"}>
                            <Typography variant="body2">{v.user.username}</Typography>
                            <Chip size="small" variant="role" label={toCamelCase(v.user_role)} />
                        </Box>
                    </ListItem>
                ))
            }
        </List>
    </>);
}

export default React.memo(BugAssigneesList)