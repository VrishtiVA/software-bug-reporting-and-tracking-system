import { Avatar, Box, Chip, DialogContentText, FormControl, InputLabel, MenuItem, Select, Typography } from "@mui/material";
import { AvatarColours } from "../../api/colours";
import MoreVertIcon from "@mui/icons-material/MoreVertRounded"
import { useState } from "react";
import MenuComponent from "../common/MenuComponent";
import DialogComponent from "../common/DialogComponent";
import { useSnackbar } from "../contexts/SnackbarContext";
import { useParams } from "react-router-dom";
import api from "../../api/axios";
import PersonRemoveIcon from '@mui/icons-material/PersonRemoveRounded';
import EditIcon from '@mui/icons-material/EditRounded';
import { WorkspaceRoleDescriptions } from "../../api/options";

/** 
 * Component to provide workspace member content. 
 * This may be editable or for preview only.
 */
export default function WorkspaceMembersContent({
    roleFilter,
    showConfirmationDialogs = false,
    workspaceMembers,
    setWorkspaceMembers,
    userRole,
    removable = false,
    editable = false,
    removeMethod = undefined,
}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [currentSelectedRole, setCurrentSelectedRole] = useState("Reporter"); 
    const [activeMember, setActiveMember] = useState({
        id: null,
        colour: "",
        username: "",
        role: "Reporter"
    });
    const resetActiveMember = () => setActiveMember({
        id: null,
        colour: "",
        username: "",
        role: "Reporter"
    })
    const [anchorElMember, setAnchorElMember] = useState(null);
    const handleMemberClick = (element, id, colour, username) => {
        setActiveMember({
            id: id,
            colour: colour,
            username: username,
            role: roleFilter
        });
        setAnchorElMember(element);
    };
    const handleMemberMenuClose = () => setAnchorElMember(null);

    /* ---------- Dialogs ---------- */
    const [displayRemoveWorkspaceMemberDialog, setDisplayRemoveWorkspaceMemberDialog] = useState(false);
    const openRemoveWorkspaceMemberDialog = () => setDisplayRemoveWorkspaceMemberDialog(true);
    const closeRemoveWorkspaceMemberDialog = () => setDisplayRemoveWorkspaceMemberDialog(false);
    const [displayEditWorkspaceMemberDialog, setDisplayEditWorkspaceMemberDialog] = useState(false);
    const openEditWorkspaceMemberDialog = () => {
        setCurrentSelectedRole(activeMember.role);
        setDisplayEditWorkspaceMemberDialog(true);
    };
    const closeEditWorkspaceMemberDialog = () => setDisplayEditWorkspaceMemberDialog(false);
    
    /* ---------- Edit and Remove Persistant Methods ---------- */

    const handleRemoveMember = async () => {
        closeRemoveWorkspaceMemberDialog();

        try {
            let memberToRemove = activeMember.username;
            let response = await api.delete(`/workspaces/${pathParams.id}/configure/members/${memberToRemove}`);
            //Successful if reached here, show success and update members display.
            console.log(response);
            resetActiveMember();
            setWorkspaceMembers(response.data);
            displaySnackbar("success", "Successfully Removed Workspace Member");
            
        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                displaySnackbar(
                    "error", 
                    err.response.data.message || "Workspace Update Failed",
                    err.response.data.details
                )
            } else {
                displaySnackbar("error", "Workspace Update Failed", "Unable to reach server.")
            }
        }
    }

    const handleEditMember = async () => {
        closeEditWorkspaceMemberDialog();

        try {
            let memberToEdit = activeMember.username;
            let response = await api.patch(`/workspaces/${pathParams.id}/configure/members`, {
                username: memberToEdit,
                role: currentSelectedRole
            });
            //Successful if reached here, show success and update members display.
            console.log(response);
            setWorkspaceMembers(response.data);
            displaySnackbar("success", "Successfully Updated Workspace Member");

        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                displaySnackbar(
                    "error", 
                    err.response.data.message || "Workspace Update Failed",
                    err.response.data.details
                )
            } else {
                displaySnackbar("error", "Workspace Update Failed", "Unable to reach server.")
            }
        }
    }

    return (<>
        {workspaceMembers
            .filter((v) => (v.role === roleFilter))
            .map((v) => (
                <Chip 
                    key={v.user_id}
                    avatar={
                        <Avatar sx={{
                            bgcolor: AvatarColours[v.user_colour],
                            color: "white !important"
                        }}> 
                            {v.username?.slice(0,1).toUpperCase() ?? "-"}
                        </Avatar>
                    }
                    label={v.username}
                    variant="soft-square"
                    strength="weak"
                    {...( 
                        (userRole === "ADMIN" && roleFilter === "ADMIN") ?
                            undefined : 
                            removable ? 
                                editable ? {
                                    onDelete: (event) => handleMemberClick(event.currentTarget, v.user_id, v.user_colour, v.username),
                                    deleteIcon: <MoreVertIcon/>
                                } : { 
                                    onDelete: () => removeMethod(v.user_id) 
                                } 
                                : undefined
                    )}
                />
            )
        )}

        {removable && editable &&
            <MenuComponent
                anchorEl={anchorElMember}
                open={Boolean(anchorElMember)}
                onClose={handleMemberMenuClose}
                //Anchor menu to bottom-right of icon
                anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
                //Menu top-right meets anchor
                transformOrigin={{horizontal: 'right', vertical: 'top'}}
                actions={[
                    {
                        name: "Edit Member Role",
                        method: openEditWorkspaceMemberDialog,
                        icon: <EditIcon/>
                    },
                    {
                        name: "Remove Member",
                        method: openRemoveWorkspaceMemberDialog,
                        icon: <PersonRemoveIcon/>
                    }
                ]}
            />
        }

        {removable && showConfirmationDialogs && <DialogComponent 
            open={displayRemoveWorkspaceMemberDialog}
            closeDialogMethod={closeRemoveWorkspaceMemberDialog}
            dialogTitle={"Are you sure?"}
            dialogContent={
                <DialogContentText>
                    This action cannot be undone.       
                </DialogContentText>
            }
            showCancel={true}
            mainActionName={"Yes, Remove"}
            mainActionMethod={handleRemoveMember}
            mainActionType={"error"}
        />}

        {editable && showConfirmationDialogs && activeMember.id != null && <DialogComponent
            open={displayEditWorkspaceMemberDialog}
            closeDialogMethod={closeEditWorkspaceMemberDialog}
            dialogTitle={"Edit Member Role"}
            dialogContent={<>
                <Box display="flex" alignItems="center" justifyContent="center" gap="12px" flexWrap="wrap">
                    <Chip 
                        avatar={
                            <Avatar sx={{
                                bgcolor: AvatarColours[activeMember.colour],
                                color: "white !important"
                            }}> 
                                {activeMember.username?.slice(0,1).toUpperCase() ?? "-"}
                            </Avatar>
                        }
                        label={activeMember.username}
                        variant="soft-square"
                        strength="weak"
                    />
                    <FormControl>
                        <InputLabel>Role</InputLabel>
                        <Select 
                            label="Role"
                            sx={{width: "175px", textTransform: "capitalize"}}
                            value={currentSelectedRole}
                            onChange={(element) => setCurrentSelectedRole(element.target.value)}
                        >
                            {/* Exclude admin if userRole provided and is ADMIN */}
                            {(userRole === "ADMIN" ?
                                ["Developer", "Reporter"] :
                                ["Admin", "Developer", "Reporter"]
                            ).map((role, i) => (
                                <MenuItem value={role.toUpperCase()} dense 
                                    sx={{display: "flex", gap: "5px", alignItems: "center", textTransform: "capitalize"}}
                                >
                                    {role}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Box>
                <Typography variant="subtitle2" color="textSecondary" mt={1} fontSize={"12px"}>
                    {WorkspaceRoleDescriptions[currentSelectedRole]}
                </Typography>
            </>}
            showCancel={true}
            mainActionName={"Save Changes"}
            mainActionMethod={handleEditMember}
            mainActionType={"success"}
            mainActionDisabled={activeMember.role === currentSelectedRole}
        />}
    </>);
}