import { useState } from "react";
import { Box, Button, Divider, Stack, DialogContentText, Alert, Typography } from "@mui/material";
import { Link, useNavigate } from "react-router-dom";

import PageHeader from "../../components/common/PageHeader";
import { getRandomWorkspaceColour } from "../../api/colours";
import { checkWorkspaceDescriptionValidity, checkWorkspaceNameValidity } from "../../utilities/workspaces/validateWorkspace";
import api from "../../api/axios";
import { useSnackbar } from "../../components/contexts/SnackbarContext";
import ErrorAlertComponent from "../../components/common/ErrorAlertComponent";
import WorkspaceDetailsFormSection from "../../components/workspaces/WorkspaceDetailsFormSection";
import WorkspaceCustomLabelsFormSection from "../../components/workspaces/WorkspaceCustomLabelsFormSection";
import WorkspaceMembersFormSection from "../../components/workspaces/WorkspaceMembersFormSection";
import DialogComponent from "../../components/common/DialogComponent";

export default function CreateWorkspacePage({}) {

    const { displaySnackbar } = useSnackbar();
    const navigate = useNavigate();

    //Input fields
    const [workspaceName, setWorkspaceName] = useState("");
    const [workspaceIsPublic, setWorkspaceIsPublic] = useState(false);
    const [workspaceDescription, setWorkspaceDescription] = useState("");
    const [workspaceColour, setWorkspaceColour] = useState(getRandomWorkspaceColour);
    const [workspaceCustomLabels, setWorkspaceCustomLabels] = useState([]);
    const [workspaceMembers, setWorkspaceMembers] = useState([]);
    
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSubmitEnabled, setSubmitEnabled] = useState(true);

    //Validation display states
    const [workspaceNameHelperText, setWorkspaceNameHelperText] = useState("");
    const [isWorkspaceNameValid, setIsWorkspaceNameValid] = useState(true);
    const [workspaceDescriptionHelperText, setWorkspaceDescriptionHelperText] = useState("");
    const [isWorkspaceDescriptionValid, setIsWorkspaceDescriptionValid] = useState(true);

    //Error Object
    const [errorObject, setErrorObject] = useState({show: false, message: "", details: ""});
    const resetErrorObject = () => {setErrorObject({show: false, message: "", details: ""});}
    const hideErrorObject = (prev) => {setErrorObject({...prev, show: false})}

    /* ---------- Confirmation Dialogs ---------- */
    const [displayCreateWorkspaceDialog, setDisplayCreateWorkspaceDialog] = useState(false);
    const openCreateWorkspaceDialog = (event) => {
        event.preventDefault();
        hideErrorObject();

        //Validate first
        let valid = true
        valid = valid && checkWorkspaceName(workspaceName, false);
        valid = valid && checkWorkspaceDescription(workspaceDescription, false);

        //If not valid, re-enable button
        if (!valid) {
            setTimeout(() => scrollToError(), 50);
            setSubmitEnabled(true);
            return;
        }

        //Otherwise if valid, show dialog.
        setDisplayCreateWorkspaceDialog(true)
    };
    const closeCreateWorkspaceDialog = () => setDisplayCreateWorkspaceDialog(false);

    /* ---------- Validity Checks ---------- */

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

    /** Function to scroll to the location of the first error if found. */
    function scrollToError() {
        //MUI gives the input with this error this attribute automatically.
        let firstError = document.querySelector('[aria-invalid="true"]');
        if (firstError) {
            firstError.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }
    }

    //Handle form submission
    async function handleSubmit() {
        //Disable submit and close dialog, ready for whatever is to come.
        setSubmitEnabled(false);
        closeCreateWorkspaceDialog();

        //Handle submit
        try {
            setIsSubmitting(true);
            let createWorkspaceResponse = await api.post("/workspaces", {
                name: workspaceName.trim(),
                visibility: workspaceIsPublic,
                description: workspaceDescription.trim(),
                colour: workspaceColour.trim(),
                labels: workspaceCustomLabels,
                members: workspaceMembers
            });

            //Successful if reached here, show success and go to workspace page.
            console.log(createWorkspaceResponse);
            resetErrorObject();
            displaySnackbar("success", "Successfully Created Workspace");
            console.log(createWorkspaceResponse.headers["location"]);
            navigate(createWorkspaceResponse.headers["location"], {replace: true});
            setIsSubmitting(false);

        } catch (err) {
            console.log(err);
            //Could change this to redirect to error page instead and remove error object depending on wanted experience.

            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Workspace Creation Failed.",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Workspace Creation Failed.",
                    details: "Unable to reach server."
                });
            }

            setIsSubmitting(false);
            setSubmitEnabled(true);
        }
    }

    return (<>
        <PageHeader 
            title = "Create New Workspace"
            previous = "/workspaces"
        />

        {/* Error alert appears here */}
        <ErrorAlertComponent
            errorObject={errorObject}
            marginBottom={"10px"}
            closeMethod={hideErrorObject}
        />

        <Alert 
            severity="info"
            color="primary"
            // variant="outlined"
            sx={{
                mb: 1.5,
                py: 0.25,
                px: 1.75,
                borderRadius: 2
            }} 
        >
            <Typography variant="body2">
                A workspace is a dedicated environment for collecting, organising, and managing bug reports.
                They could be used e.g. for your project, product, or organisation, to provide services including
                report collection, report duplicate detection, report organisation into bugs, and automated bug priority calculation.
            </Typography>
        </Alert>
        <Divider sx={{mb: 1.5}}/>

        <Stack spacing={2}>
            {/* Workspace Details Section */}
            <WorkspaceDetailsFormSection 
                workspaceName={workspaceName}
                setWorkspaceName={setWorkspaceName}
                workspaceNameTooltipText={`
                    You can include up to 30 characters, which are letters, numbers, spaces, - or _. 
                    You should provide a minimum of 3 characters, with at least 1 letter.
                `}
                checkWorkspaceName={checkWorkspaceName}
                isWorkspaceNameValid={isWorkspaceNameValid}
                workspaceNameHelperText={workspaceNameHelperText}
                workspaceDescription={workspaceDescription}
                setWorkspaceDescription={setWorkspaceDescription}
                workspaceDescriptionTooltipText={`
                    You can optionally provide a description for your workspace, with up to a maximum of 500 characters.
                `}
                checkWorkspaceDescription={checkWorkspaceDescription}
                isWorkspaceDescriptionValid={isWorkspaceDescriptionValid}
                workspaceDescriptionHelperText={workspaceDescriptionHelperText}
                workspaceIsPublic={workspaceIsPublic}
                setWorkspaceIsPublic={setWorkspaceIsPublic}
                workspaceVisibilityTooltipText={`
                    If your workspace is private, only users who are a member will be able to access and interact with your workspace.
                `}
                workspaceColour={workspaceColour}
                setWorkspaceColour={setWorkspaceColour}
                workspaceColourTooltipText={`
                    Select a colour that can be used to represent your workspace.
                `}
            />

            {/* Workspace Custom Labels Section */}
            <Box display="flex" flexDirection="column" gap="10px">
                <Divider/>
                <WorkspaceCustomLabelsFormSection
                    showSubheading={true}
                    showConfirmationDialogs={false}
                    workspaceCustomLabels={workspaceCustomLabels}
                    setWorkspaceCustomLabels={setWorkspaceCustomLabels}
                    removable={true}
                    workspaceCustomLabelTooltipText={`
                        You are allowed to add up to 15 labels.
                        Each label name should have at least 1 character, and a maximum of 20 of characters that are letters, numbers, spaces, _ or -.
                    `}
                />
            </Box>

            {/* Workspace Members Section */}
            <Box display="flex" flexDirection="column" gap="10px">
                <Divider/>
                <WorkspaceMembersFormSection 
                    showSubheading={true}
                    showConfirmationDialogs={false}
                    workspaceMembers={workspaceMembers}
                    includesOwner={false}
                    setWorkspaceMembers={setWorkspaceMembers}
                    removable={true}
                    workspaceMembersTooltipText={`
                        A workspace members role dictates their permissions within the workspace.
                        A workspace can have up to a maximum of 50 members.
                        You will automatically be added as the owner of your workspace.
                    `}
                />
            </Box>

            <Divider/>  
            {/* Form submission */}
            <Box display="flex" justifyContent="end" gap="10px">
                <Button component={Link} to="/workspaces" variant="outlined"> 
                    Cancel 
                </Button>
                <Button
                    type="submit"
                    variant="contained"
                    disabled={!isSubmitEnabled}
                    loading={isSubmitting}
                    onClick={openCreateWorkspaceDialog}
                > 
                    Create
                </Button>
            </Box>
        </Stack>

        <DialogComponent
            open={displayCreateWorkspaceDialog}
            closeDialogMethod={closeCreateWorkspaceDialog}
            dialogTitle={"Create Workspace"}
            dialogContent={
                <DialogContentText>
                    Are you sure you are ready to create your new workspace? 
                    Don't worry, you can further configure your workspace details later too.
                </DialogContentText>
            }
            showCancel={true}
            mainActionName={"Yes, Create"}
            mainActionMethod={handleSubmit}
            mainActionType={"success"}
        />
    </>);

}