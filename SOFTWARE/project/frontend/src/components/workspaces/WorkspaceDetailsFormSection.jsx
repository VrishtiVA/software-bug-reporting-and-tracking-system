import { useCallback, useEffect } from "react";
import { debounce } from "../../utilities/functionHandlers";
import { Box, FormControl, FormHelperText, MenuItem, OutlinedInput, Select, ToggleButton, ToggleButtonGroup } from "@mui/material";
import FormLabelComponent from "../common/FormLabelComponent";
import { WorkspaceColours } from "../../api/colours";
import ColourPreviewBox from "../common/ColourPreviewBox";

import PublicRoundedIcon from '@mui/icons-material/PublicRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';

export default function WorkspaceDetailsFormSection({
    privilegedOnly = false,
    userRole,
    setWorkspaceDetailsChangesDetected = undefined,
    initialWorkspaceDetails = null,

    workspaceName,
    setWorkspaceName,
    workspaceNameTooltipText,
    checkWorkspaceName,
    isWorkspaceNameValid,
    workspaceNameHelperText,

    workspaceDescription = "",
    setWorkspaceDescription,
    workspaceDescriptionTooltipText,
    checkWorkspaceDescription,
    isWorkspaceDescriptionValid,
    workspaceDescriptionHelperText,

    workspaceIsPublic,
    setWorkspaceIsPublic,
    workspaceVisibilityTooltipText,
    
    workspaceColour,
    setWorkspaceColour,
    workspaceColourTooltipText
}) {

    /* ---------- Update handler methods using useCallback for efficiency. ---------- */

    useEffect((
        debounce(() => {
            if (!setWorkspaceDetailsChangesDetected || !initialWorkspaceDetails) return;
            let hasWorkspaceDetailsChanged = (
                workspaceName.trim() !== initialWorkspaceDetails.current.name ||
                (workspaceDescription ? workspaceDescription.trim() : "") !== (initialWorkspaceDetails.current.description || "")  ||
                workspaceColour !== initialWorkspaceDetails.current.colour ||
                workspaceIsPublic !== initialWorkspaceDetails.current.is_public
            );
            // console.log(initialWorkspaceDetails.current, workspaceName, workspaceDescription, workspaceColour, workspaceIsPublic);
            setWorkspaceDetailsChangesDetected(hasWorkspaceDetailsChanged);
        }, 250)
    ), [workspaceName, workspaceDescription, workspaceColour, workspaceIsPublic, initialWorkspaceDetails?.current]);

    //To run immediately to be ready.
    const debounceCheckWorkspaceName = useCallback(debounce((value) => {
        checkWorkspaceName(value, true);
    }, 250), []);
    const debounceCheckWorkspaceDescription = useCallback(debounce((value) => {
        checkWorkspaceDescription(value, true);
    }, 250), []);

    const handleSetWorkspaceName = useCallback((e) => {
        setWorkspaceName(e.target.value);
        debounceCheckWorkspaceName(e.target.value);
    }, []);

    const handleSetWorkspaceDescription = useCallback((e) => {
        setWorkspaceDescription(e.target.value);
        debounceCheckWorkspaceDescription(e.target.value);
    }, []);

    const handleSetWorkspaceVisibility = useCallback((e, newValue) => {
        if (newValue != null) {
            setWorkspaceIsPublic(newValue);
        }
    }, []);

    const handleSetWorkspaceColour = useCallback((e) => {
        setWorkspaceColour(e.target.value);
    }, []);

    return (<>
        <Box display="flex" flexDirection="column" gap={1.5}>
            <Box display="flex" justifyContent="space-between" gap="10px" flexWrap="wrap">

                {/* Workspace Name */}
                <FormControl required 
                    error={!isWorkspaceNameValid}
                    sx={{width: "300px"}}
                    {...(privilegedOnly && userRole && userRole === "ADMIN" && {
                        disabled: true
                    })}
                >
                    <FormLabelComponent 
                        label="Workspace Name" 
                        tooltipText={(privilegedOnly && userRole && userRole === "ADMIN") ? 
                            `Only the workspace owner can edit the name of the workspace.`
                        : workspaceNameTooltipText}
                    />
                    <OutlinedInput
                        placeholder="Workspace Name"
                        value={workspaceName}
                        onChange={handleSetWorkspaceName}
                        onBlur={(e) => checkWorkspaceName(e.target.value, false)}
                    />
                    <FormHelperText>{workspaceNameHelperText}</FormHelperText>
                </FormControl>

                {/* Workspace Visibility */}
                <FormControl required
                    {...(privilegedOnly && userRole && userRole === "ADMIN" && {
                        disabled: true
                    })}
                >
                    <FormLabelComponent 
                        label="Workspace Visibility" 
                        tooltipText={(userRole && userRole === "ADMIN") ? 
                            `Only the workspace owner can change the workspace visibility.` 
                        : workspaceVisibilityTooltipText}
                    />
                    <ToggleButtonGroup 
                        color="primary" 
                        exclusive={true}
                        value={workspaceIsPublic} 
                        {...(userRole && userRole === "ADMIN" && {
                            disabled: true
                        })}
                        onChange={(event, newValue) => handleSetWorkspaceVisibility(event, newValue)}
                    >
                        <ToggleButton value={true}>
                            <PublicRoundedIcon fontSize="small"/> Public
                        </ToggleButton>
                        <ToggleButton value={false}>
                            <LockRoundedIcon fontSize="small"/> Private
                        </ToggleButton>
                    </ToggleButtonGroup>
                </FormControl>
            </Box>

            {/* Workspace Description */}
            <FormControl error={!isWorkspaceDescriptionValid}>
                <FormLabelComponent 
                    label="Workspace Description (Optional)" 
                    tooltipText={workspaceDescriptionTooltipText}
                />
                <OutlinedInput multiline rows={4} fullWidth
                    placeholder="Optional Workspace Description"
                    value={workspaceDescription || ""}
                    onChange={handleSetWorkspaceDescription}
                    onBlur={(e) => checkWorkspaceDescription(e.target.value, false)}
                />
                <FormHelperText>{workspaceDescriptionHelperText}</FormHelperText>
            </FormControl>

            {/* Workspace Colour */}
            <FormControl required>
                <FormLabelComponent 
                    label="Workspace Colour" 
                    tooltipText={workspaceColourTooltipText}
                />
                {/* Select Colour and Display Preview */}
                <Box display="flex">
                    <Select 
                        value={workspaceColour}
                        onChange={handleSetWorkspaceColour}
                        sx={{
                            width: "175px",
                            borderTopRightRadius: "0",
                            borderBottomRightRadius: "0",
                            textTransform: "capitalize"
                        }}
                    >
                        { Object.entries(WorkspaceColours).map(([name, colour]) => (
                            <MenuItem value={name} dense 
                                sx={{display: "flex", gap: "5px", alignItems: "center", textTransform: "capitalize"}}
                            >
                                {name}
                            </MenuItem>
                        ))}
                    </Select>
                    <ColourPreviewBox borderTopLeftRadius="0" borderBottomLeftRadius="0"
                        colour={WorkspaceColours[workspaceColour]} 
                    />
                </Box>
            </FormControl>

        </Box>
    </>);
}