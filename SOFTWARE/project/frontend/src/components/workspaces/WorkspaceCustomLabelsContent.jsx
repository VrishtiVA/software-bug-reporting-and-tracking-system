
import { Box, Chip, DialogContentText, FormControl, InputLabel, MenuItem, Select } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVertRounded"
import { CustomLabelColours } from "../../api/colours";
import DialogComponent from "../common/DialogComponent";
import { useState } from "react";
import MenuComponent from "../common/MenuComponent";
import ColourPreviewBox from "../common/ColourPreviewBox";
import { useSnackbar } from "../contexts/SnackbarContext";
import api from "../../api/axios";
import { useParams } from "react-router-dom";
import DeleteForeverIcon from '@mui/icons-material/DeleteForeverRounded';
import EditIcon from '@mui/icons-material/EditRounded';

/** 
 * Component to provide workspace custom labels content. 
 * This may be editable or for preview only.
 */
export default function WorkspaceCustomLabelsContent({
    workspaceCustomLabels,
    setWorkspaceCustomLabels,
    showConfirmationDialogs = false,
    removable = false,
    editable = false,
    removeMethod = undefined
}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [currentSelectedColour, setCurrentSelectedColour] = useState("blue");
    const [activeCustomLabelIndex, setActiveCustomLabelIndex] = useState(null);
    const [anchorElCustomLabel, setAnchorElCustomLabel] = useState(null);
    const handleCustomLabelClick = (element, index) => {
        setActiveCustomLabelIndex(index);
        setAnchorElCustomLabel(element);
    };
    const handleCustomLabelMenuClose = () => setAnchorElCustomLabel(null);

    /* ---------- Dialogs ---------- */
    const [displayRemoveWorkspaceCustomLabelDialog, setDisplayRemoveWorkspaceCustomLabelDialog] = useState(false);
    const openRemoveWorkspaceCustomLabelDialog = () => setDisplayRemoveWorkspaceCustomLabelDialog(true);
    const closeRemoveWorkspaceCustomLabelDialog = () => setDisplayRemoveWorkspaceCustomLabelDialog(false);
    const [displayEditWorkspaceCustomLabelDialog, setDisplayEditWorkspaceCustomLabelDialog] = useState(false);
    const openEditWorkspaceCustomLabelDialog = () => {
        setCurrentSelectedColour(workspaceCustomLabels[activeCustomLabelIndex].colour);
        setDisplayEditWorkspaceCustomLabelDialog(true);
    };
    const closeEditWorkspaceCustomLabelDialog = () => setDisplayEditWorkspaceCustomLabelDialog(false);

    /* ---------- Edit and Remove Persistant Methods ---------- */
    const handleRemoveCustomLabel = async () => {
        closeRemoveWorkspaceCustomLabelDialog();

        try {
            let customLabelToRemove = workspaceCustomLabels[activeCustomLabelIndex].name;            
            let response = await api.delete(`/workspaces/${pathParams.id}/configure/labels/${customLabelToRemove}`);
            //Successful if reached here, show success and update current labels display.
            console.log(response);
            setActiveCustomLabelIndex(0);
            setWorkspaceCustomLabels(response.data);
            displaySnackbar("success", "Successfully Removed Workspace Label");

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

    const handleEditCustomLabel = async () => {
        closeEditWorkspaceCustomLabelDialog();

        try {
            let customLabelToEdit = workspaceCustomLabels[activeCustomLabelIndex].name;
            let response = await api.patch(`/workspaces/${pathParams.id}/configure/labels`, {
                name: customLabelToEdit,
                colour: currentSelectedColour
            });
            //Successful if reached here, show success and update current labels display.
            console.log(response);
            setWorkspaceCustomLabels(response.data);
            displaySnackbar("success", "Successfully Updated Workspace Label");

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
        {workspaceCustomLabels.map((v, i) => (
            <Chip 
                key={i}
                label={v.name} 
                size="small"
                {...(removable ? 
                        editable ? {
                            onDelete: (event) => handleCustomLabelClick(event.currentTarget, i),
                            deleteIcon: <MoreVertIcon/>
                        } : { 
                            onDelete: () => removeMethod(i) 
                        } 
                    : undefined
                )}
                sx={{
                    bgcolor: CustomLabelColours[v.colour],
                    color: "white",
                    fontSize: "14px",
                    borderRadius: "4px"
                }}
            />
        ))}

        {removable && editable &&
            <MenuComponent
                anchorEl={anchorElCustomLabel}
                open={Boolean(anchorElCustomLabel)}
                onClose={handleCustomLabelMenuClose}
                //Anchor menu to bottom-right of icon
                anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
                //Menu top-right meets anchor
                transformOrigin={{horizontal: 'right', vertical: 'top'}}
                actions={[
                    {
                        name: "Edit Label Colour",
                        method: openEditWorkspaceCustomLabelDialog,
                        icon: <EditIcon/>
                    },
                    {
                        name: "Remove Label",
                        method: openRemoveWorkspaceCustomLabelDialog,
                        icon: <DeleteForeverIcon/>
                    }
                ]}
            />
        }

        {removable && showConfirmationDialogs && <DialogComponent 
            open={displayRemoveWorkspaceCustomLabelDialog}
            closeDialogMethod={closeRemoveWorkspaceCustomLabelDialog}
            dialogTitle={"Are you sure?"}
            dialogContent={
                <DialogContentText>
                    By removing this custom workspace label, 
                    it will be removed from any bug reports where used.
                    This action cannot be undone.              
                </DialogContentText>
            }
            showCancel={true}
            mainActionName={"Yes, Remove"}
            mainActionMethod={handleRemoveCustomLabel}
            mainActionType={"error"}
        />}

        {editable && showConfirmationDialogs && activeCustomLabelIndex !== null && <DialogComponent
            open={displayEditWorkspaceCustomLabelDialog}
            closeDialogMethod={closeEditWorkspaceCustomLabelDialog}
            dialogTitle={"Edit Label Colour"}
            dialogContent={<>
                <Box display="flex" alignItems="center" justifyContent="center" gap="12px" flexWrap="wrap">
                    <Chip 
                        label={workspaceCustomLabels[activeCustomLabelIndex]?.name} 
                        sx={{
                            bgcolor: CustomLabelColours[currentSelectedColour],
                            color: "white",
                            fontSize: "16px",
                            borderRadius: "8px"
                        }}
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
                                value={currentSelectedColour}
                                onChange={(element) => setCurrentSelectedColour(element.target.value)}
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
                            colour={CustomLabelColours[currentSelectedColour]} 
                        />
                    </Box>
                </Box>
            </>}
            showCancel={true}
            mainActionName={"Save Changes"}
            mainActionMethod={handleEditCustomLabel}
            mainActionType={"success"}
            mainActionDisabled={currentSelectedColour === workspaceCustomLabels[activeCustomLabelIndex]?.colour}
        />}
    </>);
}