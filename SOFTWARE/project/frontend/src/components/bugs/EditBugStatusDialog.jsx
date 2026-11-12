import { useParams } from "react-router-dom";
import DialogComponent from "../common/DialogComponent";
import { useSnackbar } from "../contexts/SnackbarContext";
import { useEffect, useState } from "react";
import api from "../../api/axios";
import { FormControl, FormHelperText, MenuItem, Select } from "@mui/material";
import { StatusDescriptions, StatusOptions } from "../../api/options";
import StatusDot from "../common/StatusDot";
import { StatusColours } from "../../api/colours";

export default function EditBugStatusDialog({
    open,
    onClose,
    currentStatus,
    setBugStatus
}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [isUpdateLoading, setIsUpdateLoading] = useState(false);
    const [newStatus, setNewStatus] = useState(currentStatus);

    async function handleUpdateStatus() {
        try {
            setIsUpdateLoading(true);
            //Try update
            let response = await api.patch(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/status`, null, {
                params: {
                    status: newStatus
                }
            })
            //Here if success
            console.log(response);
            setBugStatus(response?.data);
            displaySnackbar("success", "Successfully Updated Bug Status");
            onClose();
        } catch (error) {
            console.log(error);
        } finally {
            setIsUpdateLoading(false);
        }
    }

    useEffect(() => {
        if (!open) return;
        setNewStatus(currentStatus);
    }, [open])

    return (<>
        <DialogComponent
            open={open}
            closeDialogMethod={onClose}
            dialogTitle={"Update Bug Status"}
            showCancel={true}
            mainActionType={"success"}
            mainActionName={"Update"}
            mainActionLoading={isUpdateLoading}
            mainActionMethod={handleUpdateStatus}
            mainActionDisabled={currentStatus === newStatus}
            dialogContent={
                <FormControl required fullWidth>
                    <Select
                        value={newStatus}
                        onChange={(e) => setNewStatus(e.target.value)}
                    >
                        { Object.keys(StatusOptions).map((v, i) => (
                            <MenuItem value={v} dense 
                                sx={{display: "flex", gap: "5px", alignItems: "center"}}
                            >
                                <StatusDot colour={StatusColours[v]} size="10px"/>
                                {StatusOptions[v]}
                            </MenuItem>
                        ))}
                    </Select>
                    <FormHelperText>{StatusDescriptions[newStatus]}</FormHelperText>
                </FormControl>
            }
        />
    </>)

}