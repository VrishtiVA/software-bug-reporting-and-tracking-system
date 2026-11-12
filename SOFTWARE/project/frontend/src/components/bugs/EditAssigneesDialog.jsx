import React, { useEffect, useState } from "react";
import DialogComponent from "../common/DialogComponent";
import { Avatar, Badge, Box, Chip, Divider, IconButton, List, ListItem, ListItemAvatar, ListItemButton, ListItemIcon, Skeleton, styled, Tooltip, Typography, useMediaQuery } from "@mui/material";
import api from "../../api/axios";
import { useParams } from "react-router-dom";
import UserAvatar from "../users/UserAvatar";
import { toCamelCase } from "../../utilities/stringHandlers";
import { useSnackbar } from "../contexts/SnackbarContext";
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import ClearRoundedIcon from '@mui/icons-material/ClearRounded';
import theme from "../../styles/theme";


export default function EditAssigneesDialog({
    open,
    onClose, 
    permittedRoles,
    setBugAssignees,
    updateIfUserIsAssignee
}) {

    const pathParams = useParams();
    const {displaySnackbar} = useSnackbar();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"));

    const [isUpdateLoading, setIsUpdateLoading] = useState(false);
    const [newAssignments, setNewAssignments] = useState([]);
    const [newUnassignments, setNewUnassignments] = useState([]);
    const [displayAssignees, setDisplayAssignees] = useState(null);
    const [displayAssignable, setDisplayAssignable] = useState(null);

    /** Function to register a new assignment. */
    const addNewAssignment = (v) => setNewAssignments(prev => [...prev, v]);
    /** Function to register a new unassignment. */
    const addNewUnassignment = (v) => setNewUnassignments(prev => [...prev, v]);
    /** Function to register undoing a new assignment. */
    const removeNewAssignment = (userId) => setNewAssignments(prev => prev.filter(v => v.user.user_id != userId));
    /** Function to register undoing a new unassignment. */
    const removeNewUnassignment = (userId) => setNewUnassignments(prev => prev.filter(v => v.user.user_id != userId));

    /** Function to move assigned to unassigned list display */
    const moveAssignedToUnassignedDisplay = (v) => {
        setDisplayAssignable(prev => [...prev, v]);
        setDisplayAssignees(prev => prev.filter(a => a.user.user_id !== v.user.user_id));
    }
    /** Function to move unassigned to assigned list display */
    const moveUnassignedToAssignedDisplay = (v) => {
        setDisplayAssignees(prev => [...prev, v]);
        setDisplayAssignable(prev => prev.filter(a => a.user.user_id !== v.user.user_id));
    }

    /** Function to handle editting assignment display and registering changes. */
    const handleEditAssignment = (v, to) => {
        if (to === "assign") {
            moveUnassignedToAssignedDisplay(v);
            let isRevert = newUnassignments.some(a => a.user.user_id === v.user.user_id);
            if (isRevert === true) {
                //If unassignment was previously an assignment, no change.
                removeNewUnassignment(v.user.user_id);
            } else if (isRevert === false) {
                //Otherwise note change
                addNewAssignment(v);
            }
        } else if (to === "unassign") {
            moveAssignedToUnassignedDisplay(v);
            let isRevert = newAssignments.some(a => a.user.user_id === v.user.user_id)
            if (isRevert === true) {
                //If assignment was previously non assigned, no change
                removeNewAssignment(v.user.user_id);
            } else if (isRevert === false) {
                //Otherwise note change
                addNewUnassignment(v);
            }
        }
    }

    /** 
     * Function to make request to update the assignments.
     * Does by only sending over assignment changes for backend to validate and process.
     * Close dialog once complete.
     */
    async function handleUpdateAssignees() {
        if (isUpdateLoading) return; 
        setIsUpdateLoading(true);
        try {
            let request = {
                "assign": newAssignments.map((v, _) => v.user.user_id),
                "unassign": newUnassignments.map((v, _) => v.user.user_id)
            }

            //Request
            let response = await api.patch(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/assignees`, request);

            //Here if success.
            console.log(response);
            setBugAssignees(response?.data);
            updateIfUserIsAssignee(permittedRoles, response?.data);
            displaySnackbar("success", "Successfully Updated Assignees");
            handleClose();

        } catch (error) {
            console.log(error);
        } finally {
            setIsUpdateLoading(false);
        }
    }

    /**
     * Fetch assignment details including assigned and assignable members on edit assignments dialog open.
     */
    useEffect(() => {
        if (!open) return;
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/assignees`)
            .then(({data}) => {
                console.log("Fetched edit assignment details");
                console.log(data);
                setDisplayAssignees(data.assignees);
                setDisplayAssignable(data.assignable);

            }).catch(error => console.log(error));
    }, [open]);

    /** 
     * Function to close edit assignments dialog
     * Additionally resets assignment changes for a freshly fetched view next time.
     */
    const handleClose = () => {
        setNewAssignments([]);
        setNewUnassignments([]);
        onClose();
    }

    /** Function to get dynamic assignment count tooltip text */
    const assignmentCountTooltipText = (count) => count == 0 
        ? "This member has not yet been assigned any bugs in this workspace" 
        : `This member has already been assigned ${count == 1 
            ? "1 bug" 
            : count + " bugs"
        } in this workspace`;
        
    /** Styled assignment count badge */
    const StyledBadge = styled(Badge)(({theme}) => ({
        "& .MuiBadge-badge": {
            right: 0,
            top: 20,
            fontSize: "11px",
            border: `1.25px solid ${theme.palette.background.paper}`,
            padding: "0 4px",
            scale: 0.8
        }
    }));

    return (<>
        <DialogComponent
            open={open}
            closeDialogMethod={handleClose}
            dialogTitle={"Edit Assignees"}
            showCancel={true}
            mainActionName={"Update"}
            mainActionType={"success"}
            mainActionMethod={handleUpdateAssignees}
            mainActionLoading={isUpdateLoading}
            mainActionDisabled={newAssignments.length === 0 && newUnassignments.length === 0}
            dialogContent={ (displayAssignable == null || displayAssignees == null) ?
                <Skeleton/>
                : <>
                <Box 
                    display="flex" 
                    gap={"10px"} 
                    sx={{ 
                        flexDirection:{ xs: "column-reverse", md: "row" },
                        border: `1px solid ${theme.palette.divider}`,
                        bgcolor: "neutral.main",
                        borderRadius: "4px",
                        px: 1.5,
                        py: 1
                    }}
                > 
                    {/* List of assignable. */}
                    <List dense disablePadding sx={{flex: 1}}>
                        <Typography mb={0.75} variant="subtitle2" color="text.secondary">Available</Typography>
                        {displayAssignable.length === 0 ? 
                            <Typography variant="body2">None</Typography>
                        : 
                            displayAssignable.map((v, i) => (
                                <ListItem
                                    key={v.user.user_id}
                                    disableGutters
                                    disablePadding
                                    sx={{pb: 0.25}}
                                >
                                    <ListItemButton
                                        disableGutters
                                        onClick= {() => handleEditAssignment(v, "assign")}
                                        sx={{
                                            px: 0.5,
                                            borderRadius: "4px",
                                            display: "flex",
                                            justifyContent: "space-between"
                                        }}
                                    >
                                        <Box display="flex" alignItems={"center"} gap={0.5}>
                                            <ListItemAvatar>
                                                <Tooltip title={assignmentCountTooltipText(v.member_assignment_count)}>
                                                    <StyledBadge badgeContent={v.member_assignment_count} color="warning">
                                                        <UserAvatar size="24px" username={v.user.username} avatarColour={v.user.user_colour}/>
                                                    </StyledBadge>
                                                </Tooltip>
                                            </ListItemAvatar>
                                            <Box display="flex" gap={1} alignItems={"center"}>
                                                <Typography variant="body2">{v.user.username}</Typography>
                                                <Chip size="small" variant="role" label={toCamelCase(v.user_role)} />
                                            </Box>
                                        </Box>
                                        {permittedRoles.includes(v.user_role) &&
                                            <ListItemIcon sx={{minWidth: 0}}>
                                                <AddRoundedIcon />
                                            </ListItemIcon>
                                        }
                                    </ListItemButton>
                                </ListItem>
                            ))
                        }
                    </List>

                    <Divider orientation={isSmallScreen ? "horizontal" : "vertical"} flexItem={!isSmallScreen} sx={{m: isSmallScreen ? 0 : 0.15}}/>

                    {/* List of assignees. */}
                    <List dense disablePadding sx={{flex: 1}}>
                        <Typography mb={0.75} variant="subtitle2" color="text.secondary">Assigned</Typography>
                        {displayAssignees.length === 0 ? 
                            <Typography variant="body2">None</Typography> 
                        : 
                            displayAssignees.map((v, i) => (
                                <ListItem
                                    key={v.user.user_id}
                                    disableGutters
                                    disablePadding
                                    sx={{pb: 0.25}}
                                >
                                    <ListItemButton
                                        disableGutters
                                        //Only allow move if permitted.
                                        {...(permittedRoles.includes(v.user_role) ? {
                                            onClick: () => handleEditAssignment(v, "unassign")
                                        } : {
                                            disabled: true
                                        })}
                                        sx={{
                                            px: 0.5,
                                            borderRadius: "4px",
                                            display: "flex",
                                            justifyContent: "space-between"
                                        }}
                                    >
                                        <Box display="flex" alignItems={"center"} gap={0.5}>
                                            <ListItemAvatar>
                                                <Tooltip title={assignmentCountTooltipText(v.member_assignment_count)}>
                                                    <StyledBadge badgeContent={v.member_assignment_count} color="warning">
                                                        <UserAvatar size="24px" username={v.user.username} avatarColour={v.user.user_colour}/>
                                                    </StyledBadge>
                                                </Tooltip>
                                            </ListItemAvatar>
                                            <Box display="flex" gap={1} alignItems={"center"}>
                                                <Typography variant="body2">{v.user.username}</Typography>
                                                <Chip size="small" variant="role" label={toCamelCase(v.user_role)} />
                                            </Box>
                                        </Box>
                                        {permittedRoles.includes(v.user_role) &&
                                            <ListItemIcon sx={{minWidth: 0}}>
                                                <ClearRoundedIcon fontSize="small"/>
                                            </ListItemIcon>
                                        }
                                    </ListItemButton>
                                </ListItem>
                            ))
                        }
                    </List>

                </Box>
            </>}
        />
    </>);

}