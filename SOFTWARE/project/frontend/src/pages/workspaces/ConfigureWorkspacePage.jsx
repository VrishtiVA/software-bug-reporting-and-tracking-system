import { useNavigate, useParams } from "react-router-dom"
import PageHeader from "../../components/common/PageHeader";
import { useEffect, useRef, useState } from "react";
import ErrorAlertComponent from "../../components/common/ErrorAlertComponent";
import api from "../../api/axios";
import { Accordion, AccordionActions, AccordionDetails, AccordionSummary, Box, Button, ButtonGroup, DialogContentText, Skeleton, Typography } from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import theme from "../../styles/theme";
import WorkspaceDetailsFormSection from "../../components/workspaces/WorkspaceDetailsFormSection";
import { checkWorkspaceDescriptionValidity, checkWorkspaceNameValidity } from "../../utilities/workspaces/validateWorkspace";
import { useSnackbar } from "../../components/contexts/SnackbarContext";
import DialogComponent from "../../components/common/DialogComponent";
import WorkspaceCustomLabelsFormSection from "../../components/workspaces/WorkspaceCustomLabelsFormSection";
import FormLabelComponent from "../../components/common/FormLabelComponent";
import WorkspaceMembersFormSection from "../../components/workspaces/WorkspaceMembersFormSection";

export default function ConfigureWorkspacePage({}) {

    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();
    const navigate = useNavigate();

    //Input fields
    const [workspaceName, setWorkspaceName] = useState("");
    const [workspaceIsPublic, setWorkspaceIsPublic] = useState(false);
    const [workspaceDescription, setWorkspaceDescription] = useState("");
    const [workspaceColour, setWorkspaceColour] = useState("blue");

    const [workspaceDetailsChangesDetected, setWorkspaceDetailsChangesDetected] = useState(false);
    const [isWorkspaceDetailsSubmitting, setIsWorkspaceDetailsSubmitting] = useState(false);
    const [isWorkspaceDetailsSubmitEnabled, setIsWorkspaceDetailsSubmitEnabled] = useState(true);
    const [isWorkspaceDetailsResetting, setIsWorkspaceDetailsResetting] = useState(false);
    const [isWorkspaceDetailsResetEnabled, setIsWorkspaceDetailsResetEnabled] = useState(true);
    const [isWorkspaceDeleteEnabled, setIsWorkspaceDeleteEnabled] = useState(true);
    const [isWorkspaceDeleting, setIsWorkspaceDeleting] = useState(false);

    const [workspaceCustomLabels, setWorkspaceCustomLabels] = useState([]);
    const [workspaceMembers, setWorkspaceMembers] = useState([]);

    //Error Object
    const [errorObject, setErrorObject] = useState({show: false, message: "", details: ""});
    const resetErrorObject = () => {setErrorObject({show: false, message: "", details: ""});}
    const hideErrorObject = (prev) => {setErrorObject({...prev, show: false})}

    const [workspaceLoading, setWorkspaceLoading] = useState(true);
    const [workspace, setWorkspace] = useState(undefined);

    //Confirmation dialogs
    const [displayUpdateWorkspaceDetailsConfirmationDialog, setDisplayUpdateWorkspaceDetailsConfirmationDialog] = useState(false);
    const openUpdateWorkspaceDetailsConfirmationDialog = () => {
        hideErrorObject();

        //Validate first
        let valid = true
        valid = valid && checkWorkspaceName(workspaceName, false);
        valid = valid && (workspaceDescription === null || checkWorkspaceDescription(workspaceDescription, false));

        //If not valid, renable submit
        if (!valid) {
            setIsWorkspaceDetailsSubmitEnabled(true);
            setIsWorkspaceDetailsResetEnabled(true);
            return;
        }

        setDisplayUpdateWorkspaceDetailsConfirmationDialog(true);
    };
    const closeUpdateWorkspaceDetailsConfirmationDialog = () => setDisplayUpdateWorkspaceDetailsConfirmationDialog(false);
    const [displayDeleteWorkspaceConfirmationDialog, setDisplayDeleteWorkspaceConfirmationDialog] = useState(false);
    const openDeleteWorkspaceConfirmationDialog = () => setDisplayDeleteWorkspaceConfirmationDialog(true);
    const closeDeleteWorkspaceConfirmationDialog = () => setDisplayDeleteWorkspaceConfirmationDialog(false);

    //Validation display states
    const [workspaceNameHelperText, setWorkspaceNameHelperText] = useState("");
    const [isWorkspaceNameValid, setIsWorkspaceNameValid] = useState(true);
    const [workspaceDescriptionHelperText, setWorkspaceDescriptionHelperText] = useState("");
    const [isWorkspaceDescriptionValid, setIsWorkspaceDescriptionValid] = useState(true);

    /* ----- Validity Checks ----- */

    function checkWorkspaceName(name, focussed) {
        let [validityState, helperMessage] = checkWorkspaceNameValidity(name, focussed);
        setIsWorkspaceNameValid(validityState);
        setWorkspaceNameHelperText(helperMessage);
        return validityState;
    }

    function checkWorkspaceDescription(description, focussed) {
        let [validityState, helperMessage] = checkWorkspaceDescriptionValidity(description, focussed);
        setIsWorkspaceDescriptionValid(validityState);
        setWorkspaceDescriptionHelperText(helperMessage);
        return validityState;
    }


    const initialWorkspaceDetails = useRef({
        name: workspaceName,
        description: workspaceDescription,
        is_public: workspaceIsPublic,
        colour: workspaceColour
    });

    const updateInitialWorkspaceDetails = (name, description, is_public, colour) => {
        initialWorkspaceDetails.current = {
            name: name,
            description: description,
            is_public: is_public,
            colour: colour
        }
    }

    //Run once after component mounts.
    useEffect(() => {    
        let startTime = performance.now();
        setWorkspaceLoading(true);
        api.get(`/workspaces/${pathParams.id}/configure/access`)
            .then( ({data}) => {
                let endTime = performance.now();
                console.log(`Fetching workspace configurable details took: ${endTime - startTime} ms.`);
                console.log(data);
                setWorkspace(data);
                setWorkspaceName(data.name);
                setWorkspaceIsPublic(data.is_public);
                setWorkspaceDescription(data.description);
                setWorkspaceColour(data.colour);
                setWorkspaceMembers(data.members);
                setWorkspaceCustomLabels(data.custom_labels);
                updateInitialWorkspaceDetails(data.name, data.description, data.is_public, data.colour);
                setWorkspaceLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    /**
     * Reset workspace details by
     * fetching the most recent data from the server.
     */
    async function resetWorkspaceDetails() {
        setIsWorkspaceDetailsResetting(true);
        setIsWorkspaceDetailsResetEnabled(false);
        let startTime = performance.now();
        api.get(`/workspaces/${pathParams.id}/configure/access`)
            .then( ({data}) => {
                let endTime = performance.now();
                console.log(`Fetching workspace configurable details for reset took: ${endTime - startTime} ms.`);
                console.log(data);
                setWorkspace(data);
                setWorkspaceName(data.name);
                setWorkspaceIsPublic(data.is_public);
                setWorkspaceDescription(data.description);
                setWorkspaceColour(data.colour);
                updateInitialWorkspaceDetails(data.name, data.description, data.is_public, data.colour);
                
                setWorkspaceDetailsChangesDetected(false);
                setIsWorkspaceDetailsResetting(false);
                setIsWorkspaceDetailsResetEnabled(true);
            })
            .catch(error => {
                console.log(error);
                setIsWorkspaceDetailsResetting(false);
                setIsWorkspaceDetailsResetEnabled(true);
            });
    }

    async function saveWorkspaceDetails() {
        setIsWorkspaceDetailsResetEnabled(false);
        setIsWorkspaceDetailsSubmitEnabled(false);
        closeUpdateWorkspaceDetailsConfirmationDialog();

        //Handle submit
        try {
            setIsWorkspaceDetailsSubmitting(true);
            console.log("Will be setting workspace Desc:" + workspaceDescription);
            let response = await api.patch(`/workspaces/${pathParams.id}/configure/details`, {
                name: workspaceName,
                visibility: workspaceIsPublic,
                description: workspaceDescription,
                colour: workspaceColour
            });

            //Successful if reached here, ensure display with updated resource
            console.log(response);
            resetErrorObject();
            setWorkspace(response.data);
            setWorkspaceName(response.data.name);
            setWorkspaceIsPublic(response.data.is_public);
            setWorkspaceDescription(response.data.description || "");
            setWorkspaceColour(response.data.colour);
            updateInitialWorkspaceDetails(response.data.name, response.data.description, response.data.is_public, response.data.colour);
            displaySnackbar("success", "Successfully Saved Changes");

            setWorkspaceDetailsChangesDetected(false);
            setIsWorkspaceDetailsSubmitting(false);
            setIsWorkspaceDetailsSubmitEnabled(true);
            setIsWorkspaceDetailsResetEnabled(true);
        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Workspace Update Failed",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Workspace Update Failed",
                    details: "Unable to reach server."
                });
            }
            setIsWorkspaceDetailsSubmitting(false);
            setIsWorkspaceDetailsSubmitEnabled(true);
            setIsWorkspaceDetailsResetEnabled(true);
        }
    }

    async function deleteWorkspace() {
        hideErrorObject();
        setIsWorkspaceDeleteEnabled(false);
        closeDeleteWorkspaceConfirmationDialog();
        try {
            setIsWorkspaceDeleting(true);
            let response = await api.delete(`/workspaces/${pathParams.id}`);

            //Successful if reached here, display success and return to workspaces page.
            console.log(response);
            resetErrorObject();
            displaySnackbar("success", "Successfully Deleted Workspace");
            navigate("/workspaces", {replace: true});

            //In case still here.
            setIsWorkspaceDeleteEnabled(true);
            setIsWorkspaceDeleting(false);

        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Workspace Deletion Failed",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Workspace Deletion Failed",
                    details: "Unable to reach server."
                });
            }
            setIsWorkspaceDeleteEnabled(true);
            setIsWorkspaceDeleting(false);
        }
    }

    return (<>
        <PageHeader 
            title="Configure Workspace"
            previous={"/workspaces/"+pathParams.id}
        />

        {/* Error alert appears here */}
        <ErrorAlertComponent
            errorObject={errorObject}
            marginBottom={"10px"}
            closeMethod={hideErrorObject}
        />

        { workspaceLoading ?
            <Skeleton width="250px" />
        :<>
            <div>
                <Accordion defaultExpanded>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography>Workspace Details</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <WorkspaceDetailsFormSection
                            privilegedOnly={true}
                            userRole={workspace.user_role}
                            setWorkspaceDetailsChangesDetected={setWorkspaceDetailsChangesDetected}
                            initialWorkspaceDetails={initialWorkspaceDetails}
                            workspaceName={workspaceName}
                            setWorkspaceName={setWorkspaceName}
                            workspaceNameTooltipText={`
                                The workspace name can include up to 30 characters, which are letters, numbers, spaces, - or _. 
                                There should be a minimum of 3 characters, with at least 1 letter.
                            `}
                            checkWorkspaceName={checkWorkspaceName}
                            isWorkspaceNameValid={isWorkspaceNameValid}
                            workspaceNameHelperText={workspaceNameHelperText}
                            workspaceDescription={workspaceDescription}
                            setWorkspaceDescription={setWorkspaceDescription}
                            workspaceDescriptionTooltipText={`
                                The optional workspace description can have up to a maximum of 500 characters.
                            `}
                            checkWorkspaceDescription={checkWorkspaceDescription}
                            isWorkspaceDescriptionValid={isWorkspaceDescriptionValid}
                            workspaceDescriptionHelperText={workspaceDescriptionHelperText}
                            workspaceIsPublic={workspaceIsPublic}
                            setWorkspaceIsPublic={setWorkspaceIsPublic}
                            workspaceVisibilityTooltipText={`
                                If the workspace is private, only those who are a member will be able to access and interact with the workspace.
                            `}
                            workspaceColour={workspaceColour}
                            setWorkspaceColour={setWorkspaceColour}
                            workspaceColourTooltipText={`
                                The colour that represents this workspace.
                            `}
                        />
                    </AccordionDetails>
                    <AccordionActions>
                        <ButtonGroup>
                            <Button 
                                variant="outlined" 
                                onClick={() => resetWorkspaceDetails()}
                                loading={isWorkspaceDetailsResetting}
                                disabled={!workspaceDetailsChangesDetected || !isWorkspaceDetailsResetEnabled}
                            >
                                Reset
                            </Button>
                            <Button 
                                variant="contained"
                                color="success" 
                                onClick={openUpdateWorkspaceDetailsConfirmationDialog}
                                loading={isWorkspaceDetailsSubmitting}
                                disabled={!workspaceDetailsChangesDetected || !isWorkspaceDetailsSubmitEnabled}
                            >
                                Save Changes
                            </Button>
                        </ButtonGroup>
                    </AccordionActions>
                </Accordion>

                <Accordion>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <FormLabelComponent
                            asLabel={false}
                            label="Custom Workspace Labels" 
                            margins={false}
                            tooltipText={`
                                A workspace can have up to 15 custom labels.
                                Each label name should contain at least 1 character, and a maximum of 20 of characters that are letters, numbers, spaces, _ or -.
                            `}
                        />
                    </AccordionSummary>
                    <AccordionDetails>
                        <Box display="flex" flexDirection="column" gap="10px">
                            <WorkspaceCustomLabelsFormSection
                                workspaceCustomLabels={workspaceCustomLabels}
                                setWorkspaceCustomLabels={setWorkspaceCustomLabels}
                                removable={true}
                                editable={true}
                            />
                        </Box>
                    </AccordionDetails>
                </Accordion>

                <Accordion>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <FormLabelComponent
                            asLabel={false}
                            label="Workspace Members" 
                            margins={false}
                            tooltipText={`
                                A workspace members role dictates their permissions within the workspace.
                                A workspace can have up to a maximum of 50 members.
                            `}
                        />
                    </AccordionSummary>
                    <AccordionDetails>
                        <Box display="flex" flexDirection="column" gap="10px">
                            <WorkspaceMembersFormSection
                                workspaceMembers={workspaceMembers}
                                setWorkspaceMembers={setWorkspaceMembers}
                                removable={true}
                                editable={true}
                                userRole={workspace.user_role}
                            />
                        </Box>
                    </AccordionDetails>
                </Accordion>

                <Accordion 
                    // sx={{borderBottom: "0"}} 
                    disabled={workspace.user_role !== "OWNER"}
                >
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography>Danger Zone (Owner Only)</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <Typography 
                            variant="body2" 
                            color="text.secondary" 
                            mt={-0.5}
                        >
                            This section contains actions that are only visible to you as the workspace owner.
                        </Typography>
                    </AccordionDetails>
                    <AccordionActions>
                        <Button
                            variant="contained"
                            color="error"
                            onClick={openDeleteWorkspaceConfirmationDialog}
                            loading={isWorkspaceDeleting}
                            disabled={!isWorkspaceDeleteEnabled}
                        >
                            Delete Workspace
                        </Button>
                    </AccordionActions>
                </Accordion>
            </div>

            <DialogComponent
                open={displayDeleteWorkspaceConfirmationDialog}
                closeDialogMethod={closeDeleteWorkspaceConfirmationDialog}
                dialogTitle={"Delete Workspace"}
                dialogContent={
                    <DialogContentText>
                        Are you sure you want to delete this workspace? 
                        This action cannot be undone.
                    </DialogContentText>
                }
                showCancel={true}
                mainActionName={"Yes, Delete"}
                mainActionMethod={deleteWorkspace}
                mainActionType={"error"}
            />

            <DialogComponent
                open={displayUpdateWorkspaceDetailsConfirmationDialog}
                closeDialogMethod={closeUpdateWorkspaceDetailsConfirmationDialog}
                dialogTitle={"Save Changes"}
                dialogContent={
                    <DialogContentText>
                        Are you sure you are ready to save your changes and update the workspace details?
                    </DialogContentText>
                }
                showCancel={true}
                mainActionName={"Yes, Save"}
                mainActionMethod={saveWorkspaceDetails}
                mainActionType={"success"}
            />
        </>}

    </>);
}