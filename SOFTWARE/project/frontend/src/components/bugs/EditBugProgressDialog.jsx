import { useParams } from "react-router-dom";
import DialogComponent from "../common/DialogComponent";
import { useSnackbar } from "../contexts/SnackbarContext";
import { useEffect, useState } from "react";
import api from "../../api/axios";
import { FormControl, FormHelperText, MenuItem, Select } from "@mui/material";
import StatusDot from "../common/StatusDot";
import { ProgressDescription, ProgressOptions } from "../../api/options";
import { ProgressColours } from "../../api/colours";

export default function EditBugProgressDialog({
    open,
    onClose,
    currentProgress,
    setBugProgress
}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [isUpdateLoading, setIsUpdateLoading] = useState(false);
    const [newProgress, setNewProgress] = useState(currentProgress);

    async function handleUpdateProgress() {
        try {
            setIsUpdateLoading(true);
            //Try update
            let response = await api.patch(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/progress`, null, {
                params: {
                    progress: newProgress
                }
            })
            //Here if success
            console.log(response);
            setBugProgress(response?.data);
            displaySnackbar("success", "Successfully Updated Bug Progress");
            onClose();
        } catch (error) {
            console.log(error);
        } finally {
            setIsUpdateLoading(false);
        }
    }

    useEffect(() => {
        if (!open) return;
        setNewProgress(currentProgress);
    }, [open])

    return (<>
        <DialogComponent
            open={open}
            closeDialogMethod={onClose}
            dialogTitle={"Update Bug Progress"}
            showCancel={true}
            mainActionType={"success"}
            mainActionName={"Update"}
            mainActionLoading={isUpdateLoading}
            mainActionMethod={handleUpdateProgress}
            mainActionDisabled={currentProgress === newProgress}
            dialogContent={
                <FormControl required fullWidth>
                    <Select
                        value={newProgress}
                        onChange={(e) => setNewProgress(e.target.value)}
                    >
                        { Object.keys(ProgressOptions).map((v, i) => (
                            <MenuItem value={v} dense 
                                sx={{display: "flex", gap: "5px", alignItems: "center"}}
                            >
                                <StatusDot colour={ProgressColours[v]} size="10px"/>
                                {ProgressOptions[v]}
                            </MenuItem>
                        ))}
                    </Select>
                    <FormHelperText>{ProgressDescription[newProgress]}</FormHelperText>
                </FormControl>
            }
        />
    </>)

}