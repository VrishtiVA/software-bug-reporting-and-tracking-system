import { useParams } from "react-router-dom";
import { useSnackbar } from "../contexts/SnackbarContext";
import DialogComponent from "../common/DialogComponent";
import { useCallback, useEffect, useState } from "react";
import { Box, Card, Chip, Collapse, FormControl, FormHelperText, MenuItem, Select, Typography } from "@mui/material";
import { CustomLabelColours } from "../../api/colours";
import ContainedIconButton from "../common/ContainedIconButton";
import WorkspaceCustomLabelsContent from "../workspaces/WorkspaceCustomLabelsContent";
import FormLabelComponent from "../common/FormLabelComponent";
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import api from "../../api/axios";

export default function EditBugLabelsDialog({
    open,
    onClose,
    currentLabels,
    setBugLabels,
    workspaceCustomLabels
}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();
    const [isUpdateLoading, setIsUpdateLoading] = useState(false);

    const [displayLabels, setDisplayLabels] = useState(currentLabels);
    const [selectedLabel, setSelectedLabel] = useState("None");
    const [isAddLabelLoading, setIsAddLabelLoading] = useState(false);
    const [currentLabelHelperText, setCurrentLabelHelperText] = useState("");

    const [addLabels, setAddLabels] = useState([]);
    const [removeLabels, setRemoveLabels] = useState([]);

    const handleSetSelectedLabel = useCallback((e) => {
        setSelectedLabel(e.target.value);
        setCurrentLabelHelperText("");
    })

    const handleAddLabel = () => {
        setIsAddLabelLoading(true);

        //Validate presence, otherwise show helper text.
        if (selectedLabel == "None") {
            setCurrentLabelHelperText("Select from labels here");
            setIsAddLabelLoading(false);
            return;
        }
        //Validate unique label, otherwise show helper text.
        if (displayLabels.some(label => (label.name === selectedLabel.name))) {
            setCurrentLabelHelperText("This label has already been added");
            setIsAddLabelLoading(false);
            return;
        }

        //Add Label
        setDisplayLabels((prev) => [...prev, selectedLabel]);
        if (removeLabels.includes(selectedLabel.name)) {
            //If in remove labels, remove from remove labels since no change. //<-bug
            setRemoveLabels(prev => prev.filter(label => label !== selectedLabel.name));
        } else {
            //Otherwise add to add labels
            setAddLabels(prev => [...prev, selectedLabel.name]);
        }
        
        //Reset view
        setSelectedLabel("None");
        setCurrentLabelHelperText("");
        setIsAddLabelLoading(false);
    }

    const handleRemoveLabel = (iRemove) => {
        let removeLabel = displayLabels[iRemove];
        if (addLabels.includes(removeLabel.name)) {
            //If in add labels, remove from add labels since no change //<-- bug
            setAddLabels(prev => prev.filter(label => label !== removeLabel.name));
        } else {
            //Otherwsie add to remove labels
            setRemoveLabels(prev => [...prev, removeLabel.name]);
        }
        //Remove label
        setDisplayLabels((prev) => prev.filter((_, i) => i !== iRemove));
    }

    /** Closing dialog while resetting changes. */
    function handleClose() {
        onClose();
        setAddLabels([]);
        setRemoveLabels([]);
    }

    useEffect(() => {
        if (!open) return;
        setDisplayLabels(currentLabels);
    }, [open])

    async function handleUpdateLabels() {
        try {
            setIsUpdateLoading(true);

            //Try update
            console.log(addLabels, removeLabels);
            let response = await api.patch(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/labels`, {
                add: addLabels,
                remove: removeLabels
            })

            //Here if success
            console.log(response);
            setBugLabels(response?.data);
            setDisplayLabels(response?.data);
            setAddLabels([]);
            setRemoveLabels([]);
            displaySnackbar("success", "Successfully Updated Bug Labels");
            handleClose();
        } catch (error) {
            console.log(error);
        } finally {
            setIsUpdateLoading(false);
        }
    }

    return (<>
        <DialogComponent
            open={open}
            closeDialogMethod={handleClose}
            dialogTitle={"Edit Labels"}
            showCancel={true}
            mainActionType={"success"}
            mainActionName={"Update"}
            mainActionLoading={isUpdateLoading}
            mainActionMethod={handleUpdateLabels}
            mainActionDisabled={addLabels.length === 0 && removeLabels.length === 0}
            dialogContent={
                <Box display="flex" flexDirection="column" gap="8px">
                    <FormControl>
                        <FormLabelComponent label={"Labels"}/>
                        <Box display="flex" gap="8px" flexWrap="wrap">
                            <Select
                                sx={{width: "215px"}}
                                value={selectedLabel}
                                onChange={handleSetSelectedLabel}
                            >
                                <MenuItem value="None" disabled dense>
                                    <Typography>Select Label</Typography>
                                </MenuItem>
                                {workspaceCustomLabels.map((v, i) => (
                                    <MenuItem value={v} dense key={i}>
                                        {/* Turn into a component label chip */}
                                        <Chip 
                                            label={v.name} 
                                            variant="soft-square" 
                                            size="small" sx={{
                                                bgcolor: CustomLabelColours[v.colour], 
                                                color: "white"
                                            }}
                                        />
                                    </MenuItem>
                                ))}
                            </Select>
                            {/* Add label button */}
                            <ContainedIconButton
                                onClick={handleAddLabel}
                                loading={isAddLabelLoading}
                                icon={<AddRoundedIcon/>}
                                variant={"success"}
                                tooltip="Add label to bug"
                                disabled={selectedLabel === "None"}
                                disabledTooltip={"Select a label to add to this bug."}
                            />
                        </Box>
                        <FormHelperText sx={{mr: 0}}>{currentLabelHelperText}</FormHelperText>
                    </FormControl>
                    {/* Preview display */}
                    <Collapse in={displayLabels.length > 0}>
                        <Card variant="outlined">
                            <Box padding="10px" display="flex" gap="5px" flexWrap="wrap">
                                <WorkspaceCustomLabelsContent
                                    workspaceCustomLabels={displayLabels}
                                    removable={true}
                                    removeMethod={handleRemoveLabel}
                                />
                            </Box>
                        </Card>
                    </Collapse>
                </Box>
            }
        />
    </>);

}