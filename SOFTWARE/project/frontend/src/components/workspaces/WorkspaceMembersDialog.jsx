import { Box, Chip, Skeleton, Tooltip, Typography } from "@mui/material";
import DialogComponent from "../common/DialogComponent";
import WorkspaceMembersTabbedDisplay from "./WorkspaceMembersTabbedDisplay";

export default function WorkspaceMembersDialog({
    openState,
    closeMethod,
    workspaceMembers,
    workspaceMembersLoading
}) {

    return (<>
        <DialogComponent
            open={openState}
            closeDialogMethod={closeMethod}
            dialogTitle={
                <Box display="flex" alignItems="center" gap={1}>
                    <Typography variant="h6">Workspace Members</Typography>
                    {workspaceMembers?.length - 1 > 0 &&
                        <Tooltip title={`This workspace has ${workspaceMembers?.length-1} member${workspaceMembers?.length-1 !== 1 ? "s": ""} (excluding the owner)`}>
                            <Chip
                                size="small"
                                color="primary"
                                label={workspaceMembers?.length-1}
                            />
                        </Tooltip>
                    }
                </Box>
            }
            dialogContent={!workspaceMembers || workspaceMembersLoading ?
                <Skeleton/>
            : (workspaceMembers.length-1 === 0) ?
                <Typography variant="subtitle2">
                    No members have been added to this workspace yet, but 
                    you could add new members through configuration.
                </Typography>
            :<>
                <Box mt={-1}>
                    <WorkspaceMembersTabbedDisplay workspaceMembers={workspaceMembers}/>
                </Box>
            </>}
            showDismiss={true}
        />
    </>);

}