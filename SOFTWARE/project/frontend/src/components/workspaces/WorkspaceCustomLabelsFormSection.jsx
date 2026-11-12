import { Box, Card, Collapse, colors, DialogContentText, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from "@mui/material";
import AddRoundedIcon from '@mui/icons-material/AddRounded';

import FormLabelComponent from "../common/FormLabelComponent";
import { CustomLabelColours, getRandomCustomLabelColour } from "../../api/colours";
import ColourPreviewBox from "../common/ColourPreviewBox";
import ContainedIconButton from "../common/ContainedIconButton";
import WorkspaceCustomLabelsContent from "./WorkspaceCustomLabelsContent";
import { checkWorkspaceCustomLabelNameValidity } from "../../utilities/workspaces/validateWorkspace";
import { useCallback, useEffect, useState } from "react";
import { debounce } from "../../utilities/functionHandlers";
import { normaliseText } from "../../utilities/stringHandlers";
import { useParams } from "react-router-dom";
import { useSnackbar } from "../contexts/SnackbarContext";
import api from "../../api/axios";

export default function WorkspaceCustomLabelsFormSection({
    showSubheading = false,
    showConfirmationDialogs = true,
    workspaceCustomLabels,
    setWorkspaceCustomLabels,
    workspaceCustomLabelTooltipText = "",
    editable = false,
    removable = false
}) {

    const MAX_WORKSPACE_CUSTOM_LABELS = 15;

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [currentCustomLabelName, setCurrentCustomLabelName] = useState("");
    const [currentCustomLabelColour, setCurrentCustomLabelColour] = useState(getRandomCustomLabelColour);
    const [addCustomLabelLoading, setAddCustomLabelLoading] = useState(false);
    
    /* ---------- Validity Checks ---------- */

    const [currentCustomLabelNameHelperText, setCurrentCustomLabelNameHelperText] = useState("");
    const [isCurrentCustomLabelNameValid, setIsCurrentCustomLabelNameValid] = useState(true);

    /** 
     * Presence and format check for custom workspace label input.
     * Used mostly while typing
     */
    function checkCustomLabelName(labelName, focussed, isBlurCall=false) {
        let [validityState, helperMessage] = checkWorkspaceCustomLabelNameValidity(labelName, focussed, isBlurCall);
        setIsCurrentCustomLabelNameValid(validityState);
        setCurrentCustomLabelNameHelperText(helperMessage);
        return validityState;
    }

    /* ---------- Update handler methods using useCallback for efficiency. ---------- */

    //To run immediately to be ready.
    const debounceCheckCurrentCustomLabelName = useCallback(debounce((value) => checkCustomLabelName(value, true), 250), []);

    const handleSetCurrentCustomLabelName = useCallback((e) => {
        setCurrentCustomLabelName(e.target.value);
        debounceCheckCurrentCustomLabelName(e.target.value);
    }, []);


    /* ---------- Workspace Custom Label Functions ---------- */

    async function handleAddCustomLabel() {
        setAddCustomLabelLoading(true);

        //Validate not reached max custom labels.
        if (workspaceCustomLabels.length >= MAX_WORKSPACE_CUSTOM_LABELS) {
            setCurrentCustomLabelNameHelperText("Workspace has reached maximum custom labels");
            setAddCustomLabelLoading(false);
            return;
        }

        //Validate presence and format
        if (!checkCustomLabelName(currentCustomLabelName)) {
            setAddCustomLabelLoading(false);
            return;
        }

        //Validate unique label name.
        if (workspaceCustomLabels.some(label => (
            normaliseText(label.name) === normaliseText(currentCustomLabelName)
        ))) {
            setCurrentCustomLabelNameHelperText("Label with this name has already been added")
            setIsCurrentCustomLabelNameValid(false);
            setAddCustomLabelLoading(false);
            return;
        }

        if (!showConfirmationDialogs) {
            //Add to custom labels list
            setWorkspaceCustomLabels((prev) => 
                [...prev, {name: currentCustomLabelName.trim(), colour: currentCustomLabelColour}]
            );
        } else {
            try {
                //Request to add with name and colour, handle persist and update
                let response = await api.post(`/workspaces/${pathParams.id}/configure/labels`, {
                    name: currentCustomLabelName.trim(),
                    colour: currentCustomLabelColour
                });

                //Successful if reached here, show success and update current labels.
                console.log(response);
                setWorkspaceCustomLabels(response.data);
                displaySnackbar("success", "Successfully Added Workspace Label");

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
        setCurrentCustomLabelName("");
        setCurrentCustomLabelColour(getRandomCustomLabelColour);
        setAddCustomLabelLoading(false);
    }
    
    /**
     * Function to handle removing a custom label by 
     * filtering it out.
     * No confirmation needed here since this is the creation page.
     * @param {Number} iRemove index to remove from list.
     */
    function handleRemoveCustomLabel(iRemove) {
        setWorkspaceCustomLabels((prev) =>
            prev.filter((_, i) => i !== iRemove)
        )
    }

    return (<>

        {showSubheading && 
            <FormLabelComponent
                asLabel={false}
                label="Custom Workspace Labels (Optional)" 
                tooltipText={workspaceCustomLabelTooltipText}
            />
        }
        <Box display="flex" flexDirection="column" gap={3}>

            <Typography 
                variant="body2" 
                color="text.secondary" 
                mt={showSubheading ? -1.5 : -0.5}
            >
                Workspace labels can be attached to reports and bugs to help them stand out.
                {/* You have the freedom to define your own custom labels for your workspace. */}
            </Typography>

            <Box display="flex" gap="8px" alignItems="start" flexWrap="wrap">

                <TextField 
                    label="Label Name"
                    value={currentCustomLabelName}
                    onChange={handleSetCurrentCustomLabelName}
                    onBlur={(e) => checkCustomLabelName(e.target.value, false, true)}
                    error={!isCurrentCustomLabelNameValid}
                    helperText={currentCustomLabelNameHelperText}
                    disabled={workspaceCustomLabels.length >= MAX_WORKSPACE_CUSTOM_LABELS}
                    sx={{width: "225px", m: "0"}}
                />
                
                <Box display="flex">
                    <FormControl>
                        <InputLabel>Label Colour</InputLabel>
                        <Select
                            label="Label Colour"
                            sx={{
                                width: "175px", 
                                textTransform: "capitalize",
                                borderTopRightRadius: "0",
                                borderBottomRightRadius: "0"
                            }}
                            value={currentCustomLabelColour}
                            onChange={(e) => setCurrentCustomLabelColour(e.target.value)}
                            disabled={workspaceCustomLabels.length >= MAX_WORKSPACE_CUSTOM_LABELS}
                        >
                            {Object.entries(CustomLabelColours).map(([name, colour]) => (
                                <MenuItem value={name} dense 
                                    sx={{display: "flex", gap: "5px", alignItems: "center", textTransform: "capitalize"}}
                                >
                                    {name}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    
                    <ColourPreviewBox borderTopLeftRadius="0" borderBottomLeftRadius="0"
                        colour={CustomLabelColours[currentCustomLabelColour]} 
                    />
                </Box>

                {/* Add Custom Label Button */}
                <ContainedIconButton
                    onClick={handleAddCustomLabel}
                    loading={addCustomLabelLoading}
                    icon={<AddRoundedIcon/>}
                    variant={"success"}
                    tooltip="Add custom label to workspace"
                    {...(workspaceCustomLabels.length >= MAX_WORKSPACE_CUSTOM_LABELS && {
                        disabledTooltip:`You can only add up to ${MAX_WORKSPACE_CUSTOM_LABELS} custom labels.`
                    })}
                    disabled={workspaceCustomLabels.length >= MAX_WORKSPACE_CUSTOM_LABELS || currentCustomLabelName === ""}
                />
                
            </Box>
        </Box>

        {/* Show preview with remove option, not show if none */}
        <Collapse in={workspaceCustomLabels.length > 0}>
            <Card 
                variant="outlined" 
                // sx={{borderRadius: 2}}
            >
                <Box padding="10px" display="flex" gap="5px" flexWrap="wrap">
                    <WorkspaceCustomLabelsContent
                        workspaceCustomLabels={workspaceCustomLabels}
                        setWorkspaceCustomLabels={setWorkspaceCustomLabels}
                        showConfirmationDialogs={showConfirmationDialogs}
                        removable={removable}
                        {...(removable && {removeMethod: handleRemoveCustomLabel})}
                        editable={editable}
                    />
                </Box>
            </Card>
        </Collapse>
    </>);
}