import { Box, FormControl, InputLabel, MenuItem, Select, TextField, useMediaQuery } from "@mui/material";
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import FormLabelComponent from "../common/FormLabelComponent";
import ContainedIconButton from "../common/ContainedIconButton";
import WorkspaceMembersTabbedDisplay from "./WorkspaceMembersTabbedDisplay";
import { useCallback, useState } from "react";
import { checkIfUserCanBecomeMember, checkWorkspaceMemberUsernameValidity } from "../../utilities/workspaces/validateWorkspace";
import { debounce } from "../../utilities/functionHandlers";
import { useParams } from "react-router-dom";
import { useSnackbar } from "../contexts/SnackbarContext";
import api from "../../api/axios";
import WorkspaceRolesLearnMoreDialog from "./WorkspaceRolesLearnMoreDialog";
import theme from "../../styles/theme";

export default function WorkspaceMembersFormSection({
    showSubheading = false,
    showConfirmationDialogs = true,
    workspaceMembers,
    includesOwner = true,
    setWorkspaceMembers,
    workspaceMembersTooltipText = "",
    removable = false,
    editable = false,
    userRole
}) {
    const MAX_WORKSPACE_MEMBERS = 50
    const MAX_WORKSPACE_MEMBERS_CALC = MAX_WORKSPACE_MEMBERS - (includesOwner ? 0 : 1);

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    const [currentWorkspaceMemberName, setCurrentWorkspaceMemberName] = useState("");
    const [currentWorkspaceMemberRole, setCurrentWorkspaceMemberRole] = useState("Reporter");
    const [addWorkspaceMemberLoading, setAddWorkspaceMemberLoading] = useState(false);

    /* ---------- Validity Checks ---------- */

    const [currentWorkspaceMemberNameHelperText, setCurrentWorkspaceMemberNameHelperText] = useState("");
    const [isCurrentWorkspaceMemberNameValid, setIsCurrentWorkspaceMemberNameValid] = useState(true);
    
    /** 
     * Presence, length and format check for workspace member input.
     * Used mostly while typing.
     */
    function checkCurrentWorkspaceMemberName(name, focussed, isBlurCall=false) {
        let [validityState, helperMessage] = checkWorkspaceMemberUsernameValidity(name, focussed, isBlurCall);
        setIsCurrentWorkspaceMemberNameValid(validityState);
        setCurrentWorkspaceMemberNameHelperText(helperMessage);
        return validityState;
    }

    /* ---------- Update handler methods using useCallback for efficiency. ---------- */

    //To run immediately to be ready.
    const debounceCheckCurrentMemberName = useCallback(debounce((value) => checkCurrentWorkspaceMemberName(value, true), 250), []);

    const handleSetCurrentMemberName = useCallback((e) => {
        setCurrentWorkspaceMemberName(e.target.value);
        debounceCheckCurrentMemberName(e.target.value);
    }, []);
    
    /* ---------- Workspace Member Functions ---------- */

    async function handleAddWorkspaceMember() {
        setAddWorkspaceMemberLoading(true);

        //Validate not reached max members
        if (workspaceMembers.length >= MAX_WORKSPACE_MEMBERS_CALC) {
            setCurrentWorkspaceMemberNameHelperText("Workspace has reached maximum members");
            setAddWorkspaceMemberLoading(false);
            return;
        }

        //Validate length and format.
        if (!checkCurrentWorkspaceMemberName(currentWorkspaceMemberName)) {
            setAddWorkspaceMemberLoading(false);
            return;
        }

        //Validate not already added to members collection.
        if (workspaceMembers.some(member => (
            member.username.trim() === currentWorkspaceMemberName.trim()
        ))) {
            setCurrentWorkspaceMemberNameHelperText("Member has already been added")
            setIsCurrentWorkspaceMemberNameValid(false);
            setAddWorkspaceMemberLoading(false);
            return;
        }

        //Validate is used can become member.
        let [userExists, userResponse] = await checkIfUserCanBecomeMember(currentWorkspaceMemberName);
        if (!userExists) {
            setCurrentWorkspaceMemberNameHelperText(userResponse);
            setIsCurrentWorkspaceMemberNameValid(false);
            setAddWorkspaceMemberLoading(false);
            return;
        }

        let memberUsername = userResponse.data.username;
        let memberColour = userResponse.data.user_colour;
        let memberId = userResponse.data.user_id;            

        if (!showConfirmationDialogs) {
            //Add to list.
            setWorkspaceMembers((prev) => 
                [...prev, {
                    username: memberUsername, 
                    role: currentWorkspaceMemberRole.toUpperCase(), 
                    user_colour: memberColour, 
                    user_id: memberId
                }]
            );
        } else {
            try {
                let response = await api.post(`/workspaces/${pathParams.id}/configure/members`, {
                    username: memberUsername,
                    role: currentWorkspaceMemberRole
                });

                //Successful if reached here, show success and update current labels.
                console.log(response);
                setWorkspaceMembers(response.data);
                displaySnackbar("success", "Successfully Added Workspace Member");

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

        //Reset inputs.
        setCurrentWorkspaceMemberName("");
        setAddWorkspaceMemberLoading(false);
    }

    /**
     * Method to handle removing a workspace member by filtering them out.
     * No confirmation needed here since this is the creation page.
     * @param {*} idRemove The id of the user member to remove from collection.
     */
    function handleRemoveWorkspaceMember(idRemove) {
        setWorkspaceMembers((prev) =>
            prev.filter((v) => v.user_id !== idRemove)
        )
    }

    return (<>

        {showSubheading && <>
            <Box 
                display="flex" 
                gap={isSmallScreen ? 0 : 0.25}
                justifyContent={"space-between"}
                alignItems={isSmallScreen ? "start" : "center"}
                flexDirection={isSmallScreen ? "column" : "row"}
            >
                <FormLabelComponent 
                    asLabel={false}
                    label="Workspace Members (Optional)" 
                    tooltipText={workspaceMembersTooltipText}
                />
                <WorkspaceRolesLearnMoreDialog/>
            </Box>
        </>}
        {!showSubheading && 
            <Box 
                position={isLargeScreen ? "absolute" : "relative"}
                right={isLargeScreen ? "18px" : 0}
                mt={-0.5}
            >
                <WorkspaceRolesLearnMoreDialog/>
            </Box>
        }

        <Box display="flex" flexDirection="column" gap={0.2}>
            {/* Add members */}
            <Box display="flex" gap="8px" alignItems="start" flexWrap="wrap">

                <TextField 
                    label="Username"
                    value={currentWorkspaceMemberName}
                    onChange={handleSetCurrentMemberName}
                    onBlur={(e) => checkCurrentWorkspaceMemberName(e.target.value, false, true)}
                    error={!isCurrentWorkspaceMemberNameValid}
                    helperText={currentWorkspaceMemberNameHelperText}
                    disabled={workspaceMembers.length >= MAX_WORKSPACE_MEMBERS_CALC}
                    sx={{width: "225px", m: "0"}}
                />

                <FormControl>
                    <InputLabel>Role</InputLabel>
                    <Select 
                        label="Role"
                        sx={{width: "175px", textTransform: "capitalize"}}
                        value={currentWorkspaceMemberRole}
                        disabled={workspaceMembers.length >= MAX_WORKSPACE_MEMBERS_CALC}
                        onChange={(element) => setCurrentWorkspaceMemberRole(element.target.value)}
                    >
                        {/* Exclude admin if userRole provided and is ADMIN */}
                        {(userRole === "ADMIN" ?
                            ["Developer", "Reporter"] :
                            ["Admin", "Developer", "Reporter"]
                        ).map((role, i) => (
                            <MenuItem value={role} dense 
                                sx={{display: "flex", gap: "5px", alignItems: "center", textTransform: "capitalize"}}
                            >
                                {role}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <ContainedIconButton
                    onClick={handleAddWorkspaceMember}
                    loading={addWorkspaceMemberLoading}
                    icon={<AddRoundedIcon/>}
                    variant={"success"}
                    tooltip="Add custom label to workspace"
                    {...(workspaceMembers.length >= MAX_WORKSPACE_MEMBERS_CALC && {
                        disabledTooltip: `You can only have up to ${MAX_WORKSPACE_MEMBERS} workspace members (including yourself).`
                    })}
                    disabled={workspaceMembers.length >= MAX_WORKSPACE_MEMBERS_CALC || currentWorkspaceMemberName === ""}
                />
            </Box>

            {/* Show tabbed box with removables */}
            <WorkspaceMembersTabbedDisplay
                workspaceMembers={workspaceMembers}
                setWorkspaceMembers={setWorkspaceMembers}
                showConfirmationDialogs={showConfirmationDialogs}
                userRole={userRole}
                removable={removable}
                {...(removable && {removeMethod: handleRemoveWorkspaceMember})}
                editable={editable}
            />
        </Box>

    </>);
}