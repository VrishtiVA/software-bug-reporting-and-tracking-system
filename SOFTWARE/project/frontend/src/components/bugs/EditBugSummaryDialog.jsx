import { useCallback, useEffect, useState } from "react";
import DialogComponent from "../common/DialogComponent";
import { checkReportSummaryValidity } from "../../utilities/reports/validateReports";
import { debounce } from "../../utilities/functionHandlers";
import api from "../../api/axios";
import { FormControl, FormHelperText, OutlinedInput } from "@mui/material";
import FormLabelComponent from "../common/FormLabelComponent";
import { useParams } from "react-router-dom";
import { useSnackbar } from "../contexts/SnackbarContext";

export default function EditBugSummaryDialog({
    open,
    onClose,
    currentSummary,
    setBugSummary
}) {
    
    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();

    const [isUpdateLoading, setIsUpdateLoading] = useState(false);
    const [newSummary, setNewSummary] = useState(currentSummary);
    const [isBugSummaryValid, setIsBugSummaryValid] = useState(true);
    const [bugSummaryHelperText, setBugSummaryHelperText] = useState("");

    /** Function to check validity of bug summary */
    function checkBugSummary(inputText, focussed) {
        let [validityState, helperMessage] = checkReportSummaryValidity(inputText, focussed);
        setIsBugSummaryValid(validityState);
        setBugSummaryHelperText(helperMessage);
        return validityState;
    }
    const debounceCheckBugSummary = useCallback(debounce((value) => {checkBugSummary(value, true)}, 250), []);
    const handleSetNewBugSummary = useCallback((e) => {
        setNewSummary(e.target.value);
        debounceCheckBugSummary(e.target.value);
    }, []);

    async function handleUpdateSummary() {
        try {
            setIsUpdateLoading(true);
            //Validate
            if (!checkBugSummary(newSummary, false)) {
                return;
            }
            //Try update
            let response = await api.patch(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/summary`, null, {
                params: {
                    summary: newSummary
                }
            });
            //Here if success
            console.log(response);
            setBugSummary(response?.data);
            displaySnackbar("success", "Successfully Updated Bug Summary");
            onClose();
        } catch (error) {
            console.log(error);
        } finally {
            setIsUpdateLoading(false);
        }
    }

    useEffect(() => {
        if (!open) return;
        setNewSummary(currentSummary);
    }, [open])

    return (<>
        <DialogComponent
            open={open}
            closeDialogMethod={onClose}
            dialogTitle={"Edit Bug Summary"}
            showCancel={true}
            mainActionName={"Update"}
            mainActionType={"success"}
            mainActionLoading={isUpdateLoading}
            mainActionMethod={handleUpdateSummary}
            mainActionDisabled={!isBugSummaryValid || (currentSummary === newSummary)}
            dialogContent={
                <FormControl required error={!isBugSummaryValid} fullWidth>
                    <FormLabelComponent label={"Bug Summary"} />
                    <OutlinedInput
                        value={newSummary}
                        onChange={handleSetNewBugSummary}
                        onBlur={(e) => checkBugSummary(e.target.value, false)}
                    />
                    <FormHelperText>{bugSummaryHelperText}</FormHelperText>
                </FormControl>
            }
        />
    </>);
}