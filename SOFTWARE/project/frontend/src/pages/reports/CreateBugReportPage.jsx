import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import PageHeader from "../../components/common/PageHeader";
import api from "../../api/axios";
import React, { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Checkbox, Chip, debounce, DialogContentText, Divider, FormControl, FormControlLabel, FormHelperText, LinearProgress, OutlinedInput, Radio, RadioGroup, Skeleton, Stack, styled, Typography, useMediaQuery } from "@mui/material";
import { useSnackbar } from "../../components/contexts/SnackbarContext";
import ErrorAlertComponent from '../../components/common/ErrorAlertComponent';
import FormLabelComponent from "../../components/common/FormLabelComponent";

import questions from "../../api/bug_report_questions.json"
import UploadAttachmentSection from "../../components/reports/UploadAttachmentSection";
import { checkReportLongTextValidity, checkReportSummaryValidity } from "../../utilities/reports/validateReports";
import AddLabelsCreateReportSection from "../../components/reports/AddLabelsCreateReportSection";
import DialogComponent from "../../components/common/DialogComponent";
import theme from "../../styles/theme";
import { WorkspaceColours } from "../../api/colours";

const StyledRadioGroup = styled(RadioGroup, {shouldForwardProp: (prop) => prop !== "isSmallScreen"})( ({theme, isSmallScreen}) => ({
    borderRadius: "4px",
    backgroundColor: "#fcfcfc",
    border: `1px solid ${theme.palette.divider}`,
    marginTop: "3px",
    ".MuiFormControlLabel-root": {
        ...(!isSmallScreen && {height: "42.5px"}),
        ".MuiTypography-root": {
            fontSize: "15px",
            lineHeight: "1.35"
        }
    }
}));

/**
 * This form is adaptive in the following way:
 * - Non members and reporters see a simpler version of the form by default.
 * - Developers and above see an advanced version of the form by default, but can also toggle it to a simple view.
 * - Non members can only add 1 attachment, while workspace members can add up to 3 attachments.
 * - Workspace members see the form with more precise wording compared to outsiders.
 * - Only developers and above can attach labels, and labels are not shown if there aren't any available.
 * - Only developers and above should provide fix suggestions and reliable workarounds.
 */
export default function CreateBugReportPage() {

    const pathParams = useParams();
    const [queryParams, setQueryParams] = useSearchParams();
    const { displaySnackbar } = useSnackbar();
    const navigate = useNavigate();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"));

    //Toggle
    const [formToggle, setFormToggle] = useState(null);

    //Fields
    const [reportSummary, setReportSummary] = useState("");
    const [reportSteps, setReportSteps] = useState("");
    const [reportEnvironment, setReportEnvironment] = useState(queryParams.get("environment") ?? "");
    const [reportExpected, setReportExpected] = useState("");
    const [reportActual, setReportActual] = useState("");
    const [reportUserImpact, setReportUserImpact] = useState(null);
    const [reportSeverity, setReportSeverity] = useState(null);
    const [reportFixSuggestions, setReportFixSuggestions] = useState("");
    const [reportAdditionalComments, setReportAdditionalComments] = useState("");
    const [reportConsent, setReportConsent] = useState(false);
    const [reportLabels, setReportLabels] = useState([]);
    const [reportAttachments, setReportAttachments] = useState([]);
    const [reportCaptions, setReportCaptions] = useState([]);

    const [reportSummaryComplete, setReportSummaryComplete] = useState(false);
    const [reportStepsComplete, setReportStepsComplete] = useState(false);
    const [reportEnvironmentComplete, setReportEnvironmentComplete] = useState(false);
    const [reportExpectedComplete, setReportExpectedComplete] = useState(false);
    const [reportActualComplete, setReportActualComplete] = useState(false);
    const [reportSeverityComplete, setReportSeverityComplete] = useState(false);
    const [reportUserImpactComplete, setReportUserImpactComplete] = useState(false);
    
    //Form state
    const [reportProgress, setFormProgress] = useState(0);
    const [userRole, setUserRole] = useState(null);
    const [isFormLoading, setIsFormLoading] = useState(true);
    const [isSubmitEnabled, setIsSubmitEnabled] = useState(true);
    const [isSubmitLoading, setIsSubmitLoading] = useState(false);
    const [workspaceCustomLabels, setWorkspaceCustomLabels] = useState([]);
    const [workspaceDetails, setWorkspaceDetails] = useState({
        id: null,
        name: null,
        colour: null
    });

    /** Function to scroll to the location of the first error if found. */
    function scrollToError() {
        //MUI gives the input with an error this attribute automatically.
        let firstError = document.querySelector('[aria-invalid="true"]');
        if (firstError) {
            firstError.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }
    }

    const [displaySubmitReportDialog, setDisplaySubmitReportDialog] = useState(false);
    const openSubmitReportDialog = () => {
        resetErrorObject();

        //Pre-submit validation first
        let valid = checkReportSummary(reportSummary, false);
        valid = checkReportSteps(reportSteps, false) && valid;
        valid = checkReportEnvironment(reportEnvironment, false) && valid;
        valid = checkReportExpected(reportExpected, false) && valid;
        valid = checkReportActual(reportActual, false) && valid;
        valid = checkUserImpactRatingSelected() && valid;
        valid = checkSeverityRatingSelected() && valid;
        valid = checkReportFixSuggestions(reportFixSuggestions, false) && valid;
        valid = checkReportAdditionalComments(reportAdditionalComments, false) && valid;
        if (!valid) {
            setTimeout(() => scrollToError(), 50);
            setIsSubmitEnabled(true);
            return;
        }

        //Only show dialog if valid and submittable.
        setDisplaySubmitReportDialog(true);
    };
    const closeSubmitReportDialog = () => setDisplaySubmitReportDialog(false);

    //Error Object
    const [errorObject, setErrorObject] = useState({show: false, message: "", details: ""});
    const resetErrorObject = () => {setErrorObject({show: false, message: "", details: ""});}
    const hideErrorObject = (prev) => {setErrorObject({...prev, show: false})}

    //Runs once after component mounts only.
    useEffect(() => { 
        if (isFormLoading) {
            let startTime = performance.now();
            api.get(`/workspaces/${pathParams.id}/bugs/report`)
                .then( ({data}) => {
                    let endTime = performance.now();
                    console.log(`Fetching report a bug permission took: ${endTime - startTime} ms.`);
                    console.log(data);
                    setWorkspaceCustomLabels(data.custom_labels);
                    setWorkspaceDetails({
                        id: data.workspace_id,
                        name: data.workspace_name,
                        colour: data.workspace_colour
                    })
                    setUserRole(data.user_role);
                    //Set form view depending on incoming user.
                    //Developers and above see the advanced version by default, while reporters and users see the simple version only.
                    setFormToggle(data.userRole === null || data.userRole === "REPORTER" ? "Simple" : "Advanced");
                    setIsFormLoading(false);  
                })
                .catch(error => console.log(error));
        }
    }, []);

    /* ---------- Validation ---------- */

    const [isReportSummaryValid, setIsReportSummaryValid] = useState(true);
    const [reportSummaryHelperText, setReportSummaryHelperText] = useState("");
    const [isReportStepsValid, setIsReportStepsValid] = useState(true);
    const [reportStepsHelperText, setReportStepsHelperText] = useState("");
    const [isReportEnvironmentValid, setIsReportEnvironmentValid] = useState(true);
    const [reportEnvironmentHelperText, setReportEnvironmentHelperText] = useState("");
    const [isReportExpectedValid, setIsReportExpectedValid] = useState(true);
    const [reportExpectedHelperText, setReportExpectedHelperText] = useState("");
    const [isReportActualValid, setIsReportActualValid] = useState(true);
    const [reportActualHelperText, setReportActualHelperText] = useState("");
    const [isReportFixSuggestionsValid, setIsReportFixSuggestionsValid] = useState(true);
    const [reportFixSuggestionsHelperText, setReportFixSuggestionsHelperText] = useState("");
    const [isReportAdditionalCommentsValid, setIsReportAdditionalCommentsValid] = useState(true);
    const [reportAdditionalCommentsHelperText, setReportAdditionalCommentsHelperText] = useState("");

    const [isReportUserImpactRatingValid, setIsReportUserImpactRatingValid] = useState(true);
    const [reportUserImpactRatingHelperText, setReportUserImpactRatingHelperText] = useState("");
    const [isReportSeverityRatingValid, setIsReportSeverityRatingValid] = useState(true);
    const [reportSeverityRatingHelperText, setReportSeverityRatingHelperText] = useState("");

    /** Function to check validity of report summary */
    function checkReportSummary(inputText, focussed) {
        let [validityState, helperMessage] = checkReportSummaryValidity(inputText, focussed);
        setIsReportSummaryValid(validityState);
        setReportSummaryComplete(validityState);
        setReportSummaryHelperText(helperMessage);
        return validityState;
    }

    /* Functions to check validity of required report long input fields */
    function checkReportSteps(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, false, focussed);
        setIsReportStepsValid(validityState);
        setReportStepsComplete(validityState);
        setReportStepsHelperText(helperMessage);
        return validityState;
    }
    function checkReportEnvironment(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, false, focussed);
        setIsReportEnvironmentValid(validityState);
        setReportEnvironmentComplete(validityState);
        setReportEnvironmentHelperText(helperMessage);
        return validityState;
    }
    function checkReportExpected(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, false, focussed);
        setIsReportExpectedValid(validityState);
        setReportExpectedComplete(validityState);
        setReportExpectedHelperText(helperMessage);
        return validityState;
    }
    function checkReportActual(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, false, focussed);
        setIsReportActualValid(validityState);
        setReportActualComplete(validityState);
        setReportActualHelperText(helperMessage);
        return validityState;
    }

    /* Functions to check validity of optional report long input fields */
    function checkReportFixSuggestions(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, true, focussed);
        setIsReportFixSuggestionsValid(validityState);
        setReportFixSuggestionsHelperText(helperMessage);
        return validityState;
    }
    function checkReportAdditionalComments(inputText, focussed) {
        let [validityState, helperMessage] = checkReportLongTextValidity(inputText, true, focussed);
        setIsReportAdditionalCommentsValid(validityState);
        setReportAdditionalCommentsHelperText(helperMessage);
        return validityState;
    }

    /* Functions to check validity of required number rating input fields */
    function checkUserImpactRatingSelected() {
        if (reportUserImpact === null) {
            setIsReportUserImpactRatingValid(false);
            setReportUserImpactRatingHelperText("Please select a rating");
            return false;
        }
        return true;
    }
    function checkSeverityRatingSelected() {
        if (reportSeverity === null) {
            setIsReportSeverityRatingValid(false);
            setReportSeverityRatingHelperText("Please select a rating");
            return false;
        }
        return true;
    }

    /* Update progress bar */
    useEffect(() => {
        let total = 0;
        if (reportSummaryComplete) total += 1;
        if (reportStepsComplete) total += 1;
        if (reportEnvironmentComplete) total += 1;
        if (reportExpectedComplete) total += 1;
        if (reportActualComplete) total += 1;
        if (reportSeverityComplete) total += 1;
        if (reportUserImpactComplete) total += 1;
        if (reportConsent) total += 1;
        setFormProgress((total/8) * 100);
    }, [
        reportSummaryComplete, 
        reportStepsComplete,
        reportEnvironmentComplete,
        reportExpectedComplete,
        reportActualComplete,
        reportSeverityComplete,
        reportUserImpactComplete,
        reportConsent
    ])

    /* ----- Optimised debounce checks while typing ----- */

    const debounceCheckReportSummary = useCallback(debounce((value) => {checkReportSummary(value, true)}, 250), []);
    const debounceCheckReportSteps = useCallback(debounce((value) => {checkReportSteps(value, true)}, 250), []);
    const debounceCheckReportEnvironment = useCallback(debounce((value) => {checkReportEnvironment(value, true)}, 250), []);
    const debounceCheckReportExpected = useCallback(debounce((value) => {checkReportExpected(value, true)}, 250), []);
    const debounceCheckReportActual = useCallback(debounce((value) => {checkReportActual(value, true)}, 250), []);
    const debounceCheckReportFixSuggestions = useCallback(debounce((value) => {checkReportFixSuggestions(value, true)}, 250), []);
    const debounceCheckReportAdditionalComments = useCallback(debounce((value) => {checkReportAdditionalComments(value, true)}, 250), []);

    const handleSetReportSummary = useCallback((e) => {
        setReportSummary(e.target.value);
        debounceCheckReportSummary(e.target.value);
    }, []);
    const handleSetReportSteps = useCallback((e) => {
        setReportSteps(e.target.value);
        debounceCheckReportSteps(e.target.value);
    }, []);
    const handleSetReportEnvironment = useCallback((e) => {
        setReportEnvironment(e.target.value);
        debounceCheckReportEnvironment(e.target.value);
    }, []);
    const handleSetReportExpected = useCallback((e) => {
        setReportExpected(e.target.value);
        debounceCheckReportExpected(e.target.value);
    }, []);
    const handleSetReportActual = useCallback((e) => {
        setReportActual(e.target.value);
        debounceCheckReportActual(e.target.value);
    }, []);
    const handleSetReportFixSuggestions = useCallback((e) => {
        setReportFixSuggestions(e.target.value);
        debounceCheckReportFixSuggestions(e.target.value);
    }, []);
    const handleSetReportAdditionalComments = useCallback((e) => {
        setReportAdditionalComments(e.target.value);
        debounceCheckReportAdditionalComments(e.target.value);
    }, []);

    const handleSetReportUserImpactRating = useCallback((e) => {
        setReportUserImpact(Number(e.target.value));
        setIsReportUserImpactRatingValid(true);
        setReportUserImpactComplete(true);
        setReportUserImpactRatingHelperText("");
    })
    const handleSetReportSeverityRating = useCallback((e) => {
        setReportSeverity(Number(e.target.value));
        setIsReportSeverityRatingValid(true);
        setReportSeverityComplete(true);
        setReportSeverityRatingHelperText("");
    })

    /** Function to handle submitting of the report */
    async function handleSubmit() {
        setIsSubmitEnabled(false);
        closeSubmitReportDialog();
        
        //Build up form data using FormData, allows to build up key value pairs.
        try {
            //Set submit loading
            setIsSubmitLoading(true);

            //Append data to form data.
            let formData = new FormData();
            formData.append("reportData", new Blob([JSON.stringify({
                summary: reportSummary.trim(),
                steps: reportSteps.trim(),
                environment: reportEnvironment.trim(),
                expected: reportExpected.trim(),
                actual: reportActual.trim(),
                user_impact: reportUserImpact,
                severity: reportSeverity,
                fix_suggestions: formToggle === "Advanced" ? reportFixSuggestions.trim() : "",
                additional_comments: reportAdditionalComments.trim(),
                labels: formToggle === "Advanced" ? reportLabels.map(label => label.name) : [],
                captions: reportCaptions
            })], {type: "application/json"}))

            //Add attachments
            for (let i = 0; i < reportAttachments.length; i++) {
                formData.append("attachments", reportAttachments[i].file);
            }

            //Sent request
            let response = await api.post(`/workspaces/${pathParams.id}/bugs/report`, formData);

            //Successful if reached here.
            resetErrorObject();
            console.log(response);
            displaySnackbar("success", "Successfully Submit Report", "Your report will soon be delivered to the workspace.");
            navigate(`/workspaces/${pathParams.id}`)

            //Incase still here, but won't renable since already sent report.
            setIsSubmitLoading(false);

        } catch (err) {
            console.log(err);
            if (err.response?.data) {
                setErrorObject({
                    show: true,
                    message: err.response.data.message || "Submit Report Failed",
                    details: err.response.data.details
                });
            } else {
                setErrorObject({
                    show: true,
                    message: "Submit Report Failed",
                    details: "Unable to reach server."
                });
            }

            //Reset submit state
            setIsSubmitLoading(false);
            setIsSubmitEnabled(true);
        }
    }

    return (<>
        <PageHeader
            previous={`/workspaces/${pathParams.id}`}
            title="Create New Bug Report"
            showToggleGroup={userRole !== null && userRole !== "REPORTER"}
            selectedToggle={formToggle}
            setSelectedToggle={setFormToggle}
            toggleOption1Name="Simple"
            toggleOption1Tooltip="Show fields provided to all users only."
            toggleOption2Name="Advanced"
            toggleOption2Tooltip="Show additional fields for developers and above."
        />

        {/* Error alert appears here */}
        <ErrorAlertComponent
            errorObject={errorObject}
            marginBottom={"10px"}
            closeMethod={hideErrorObject}
        />

        {isFormLoading ?
            <Skeleton />
        : <>
            {/* Create Report Form */}
            
            <Box
                mb={1.5}
                mt={-1.5}
                py={1}
                sx={{
                    position: "sticky",
                    top: theme.mixins.toolbar.minHeight + (isSmallScreen ? 0 : 8),
                    zIndex: "15",
                    bgcolor: theme.palette.background.paper,
                    borderBottom: `1px solid ${theme.palette.divider}`
                }}
            >
                <LinearProgress 
                    variant="determinate" 
                    value={reportProgress}
                    sx={{
                        height: "7px",
                        borderRadius: "4px",  
                    }}
                />
            </Box>

            <Alert 
                severity="info" 
                color="primary"
                sx={{
                    mb: 1.5,
                    py: 0.25,
                    px: 1.75,
                    borderRadius: 2
                }} 
            >
                <Typography variant="body2">
                    It seems like you've discovered an issue.
                    Let's break it down into a bug report that 
                    supplies all the necessary details to 
                    help developers recognise, reproduce, and resolve the problem.
                </Typography>
            </Alert>

            <Divider sx={{mb: 1.5}}/>

            <Typography
                display="flex"
                variant="subtitle2"
                justifySelf={isSmallScreen ? "start" : "end"}
                gap={0.5}
                position={isSmallScreen ? "relative" : "absolute" }
                maxWidth={"300px"}
                flexWrap="wrap"
                mb={isSmallScreen ? 1.5 : 0}
                justifyContent={isSmallScreen ? "start" : "end"}
            >
                <span>You're writing a bug report for</span>
                <Chip
                    label={workspaceDetails.name}
                    variant="soft-square"
                    size="small"
                    sx={{
                        bgcolor: workspaceDetails.colour ? WorkspaceColours[workspaceDetails.colour] : "lightgrey"
                    }}
                />
            </Typography>
            
            <Stack spacing={2}>
                {/* Summary 
                    - This field is common for everyone.
                */}
                <ReportSummaryField 
                    reportSummary={reportSummary} 
                    isReportSummaryValid={isReportSummaryValid}
                    handleSetReportSummary={handleSetReportSummary}
                    checkReportSummary={checkReportSummary} 
                    reportSummaryHelperText={reportSummaryHelperText}
                />

                {/* Row */}
                <Box display="flex" gap="16px" sx={{flexDirection: {xs: "column", sm: "row"}}}>
                    {/* Steps 
                        This field appears differently for members vs non-members.
                    */}
                    <Box flex={1}>
                        <ReportStepsField
                            reportSteps={reportSteps}
                            userRole={userRole}
                            isReportStepsValid={isReportStepsValid}
                            handleSetReportSteps={handleSetReportSteps}
                            checkReportSteps={checkReportSteps}
                            reportStepsHelperText={reportStepsHelperText}
                        />
                    </Box>
                    
                    {/* 
                        Environment 
                        This field appears differently for members vs non-members.
                    */}
                    <Box flex={1}>
                        <ReportEnvironmentField
                            reportEnvironment={reportEnvironment}
                            userRole={userRole}
                            isReportEnvironmentValid={isReportEnvironmentValid}
                            handleSetReportEnvironment={handleSetReportEnvironment}
                            checkReportEnvironment={checkReportEnvironment}
                            reportEnvironmentHelperText={reportEnvironmentHelperText}
                        />
                    </Box>
                    
                </Box>

                {/* Row */}
                <Box display="flex" gap="16px" sx={{flexDirection: {xs: "column", sm: "row"}}}>
                    {/* Expected 
                        This field is common for all users.
                    */}
                    <Box flex={1}>
                        <ReportExpectedField
                            reportExpected={reportExpected}
                            isReportExpectedValid={isReportExpectedValid}
                            handleSetReportExpected={handleSetReportExpected}
                            checkReportExpected={checkReportExpected}
                            reportExpectedHelperText={reportExpectedHelperText}
                        />
                    </Box>
                    {/* Actual 
                        This field is common for all users.
                    */}
                    <Box flex={1}>
                        <ReportActualField
                            reportActual={reportActual}
                            isReportActualValid={isReportActualValid}
                            handleSetReportActual={handleSetReportActual}
                            checkReportActual={checkReportActual}
                            reportActualHelperText={reportActualHelperText}
                        />
                    </Box>
                </Box>

                <Divider/>

                {/* 
                    User Impact Rating
                    This field is worded differently for members vs non-members 
                */}
                <ReportUserImpactField 
                    reportUserImpact={reportUserImpact}
                    userRole={userRole}
                    isReportUserImpactRatingValid={isReportUserImpactRatingValid}
                    handleSetReportUserImpactRating={handleSetReportUserImpactRating}
                    reportUserImpactRatingHelperText={reportUserImpactRatingHelperText}
                />

                {/* 
                    Severity Rating 
                    This field is common for all users.
                */}
                <ReportSeverityField
                    reportSeverity={reportSeverity}
                    isReportSeverityRatingValid={isReportSeverityRatingValid}
                    handleSetReportSeverityRating={handleSetReportSeverityRating}
                    reportSeverityRatingHelperText={reportSeverityRatingHelperText}
                />

                <Divider/>

                {/* 
                    Custom Labels 
                    - is exclusively for developers, admins and owners.
                    - is not shown if there are no workspace custom labels available.
                    - not essential so can be toggle hidden.
                */}
                {workspaceCustomLabels.length > 0 && userRole !== null && userRole !== "REPORTER" && formToggle === "Advanced" &&
                    <AddLabelsCreateReportSection
                        reportLabels={reportLabels}
                        setReportLabels={setReportLabels}
                        workspaceCustomLabels={workspaceCustomLabels}
                        tooltip={questions.labels.tooltip}
                        question={questions.labels.question}
                    />
                }

                {/* 
                    Image Attachments 
                    - Non members are only allowed to attach one attachment, while 
                    - members can attach up to 3 attachments.
                */}
                <Box>
                    <UploadAttachmentSection
                        userRole={userRole}
                        tooltip={questions.attachments[userRole === null ? "simple" : "advanced"].tooltip}
                        question={questions.attachments[userRole === null ? "simple" : "advanced"].question}
                        reportAttachments={reportAttachments}
                        setReportAttachments={setReportAttachments}
                        reportCaptions={reportCaptions}
                        setReportCaptions={setReportCaptions}
                    />
                </Box>

                {/* Fix Suggestions 
                    - is exclusively for developers, admins and owners.
                    - not essential so can be toggle hidden.
                */}
                {userRole !== null && userRole !== "REPORTER" && formToggle === "Advanced" &&
                    <ReportFixSuggestionsField
                        reportFixSuggestion={reportFixSuggestions}
                        isReportFixSuggestionsValid={isReportFixSuggestionsValid}
                        handleSetReportFixSuggestions={handleSetReportFixSuggestions}
                        checkReportFixSuggestions={checkReportFixSuggestions}
                        reportFixSuggestionsHelperText={reportFixSuggestionsHelperText}
                    />
                }
                <Divider/>

                {/* 
                    Additional Comments 
                    This field is common for all users.
                */}
                <ReportAdditionalCommentsField
                    reportAdditionalComments={reportAdditionalComments}
                    isReportAdditionalCommentsValid={isReportAdditionalCommentsValid}
                    handleSetReportAdditionalComments={handleSetReportAdditionalComments}
                    checkReportAdditionalComments={checkReportAdditionalComments}
                    reportAdditionalCommentsHelperText={reportAdditionalCommentsHelperText}
                />

                {/* Consent */}
                <ReportConsentField
                    reportConsent={reportConsent}
                    setReportConsent={setReportConsent}
                />
                
                <Divider/>
                <Box display="flex" alignItems="center" justifyContent="end" flexWrap="wrap" gap="10px">
                    <Button 
                        variant="outlined"
                        onClick={() => navigate(`/workspaces/${pathParams.id}`)}
                    >
                        Cancel
                    </Button>
                    <Button 
                        variant="contained"
                        onClick={openSubmitReportDialog}
                        disabled={!isSubmitEnabled || !reportConsent} 
                        loading={isSubmitLoading}
                    >
                        Submit
                    </Button>
                </Box>
            
            </Stack>

            <DialogComponent
                open={displaySubmitReportDialog}
                closeDialogMethod={closeSubmitReportDialog}
                dialogTitle={"Send Report"}
                dialogContent={
                    <DialogContentText>
                        Are you sure you are ready to submit your bug report?
                    </DialogContentText>
                }
                showCancel={true}
                mainActionName={"Yes, Send"}
                mainActionMethod={handleSubmit}
                mainActionType={"success"}
            />
        </>}

    </>);
}

const ReportSummaryField = React.memo(({
    isReportSummaryValid,
    reportSummary,
    handleSetReportSummary,
    checkReportSummary,
    reportSummaryHelperText
}) => {
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))
    return(
        <FormControl required error={!isReportSummaryValid}>
            <FormLabelComponent
                label={questions.summary.question}
                keyword={questions.summary.keyword}
                tooltipText={questions.summary.tooltip}
            />
            <OutlinedInput
                fullWidth={isSmallScreen}
                sx={{...!isSmallScreen && {width: "350px"}}}
                placeholder={questions.summary.placeholder}
                value={reportSummary}
                onChange={handleSetReportSummary}
                onBlur={(e) => checkReportSummary(e.target.value, false)}
            />
            <FormHelperText>{reportSummaryHelperText}</FormHelperText>
        </FormControl>
    );
})

const ReportStepsField = React.memo(({
    reportSteps,
    userRole,
    isReportStepsValid,
    handleSetReportSteps,
    checkReportSteps,
    reportStepsHelperText
}) => {
    return (
        <FormControl required fullWidth error={!isReportStepsValid}>
            <FormLabelComponent
                label={userRole === null ? questions.steps.simple.question : questions.steps.advanced.question}
                keyword={userRole === null ? questions.steps.simple.keyword : questions.steps.advanced.keyword}
                tooltipText={questions.steps.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                placeholder={questions.summary.placeholder}
                value={reportSteps}
                onChange={handleSetReportSteps}
                onBlur={(e) => checkReportSteps(e.target.value, false)}
            />
            <FormHelperText>{reportStepsHelperText}</FormHelperText>
        </FormControl>
    );
})

const ReportEnvironmentField = React.memo(({
    reportEnvironment,
    userRole,
    isReportEnvironmentValid,
    handleSetReportEnvironment,
    checkReportEnvironment,
    reportEnvironmentHelperText
}) => {
    return (
        <FormControl required fullWidth error={!isReportEnvironmentValid}>
            <FormLabelComponent
                label={userRole === null ? questions.environment.simple.question : questions.environment.advanced.question}
                keyword={userRole === null ? questions.environment.simple.keyword : questions.environment.advanced.keyword}
                tooltipText={questions.environment.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                placeholder={questions.environment.placeholder}
                value={reportEnvironment}
                onChange={handleSetReportEnvironment}
                onBlur={(e) => checkReportEnvironment(e.target.value, false)}
            />
            <FormHelperText>{reportEnvironmentHelperText}</FormHelperText>
        </FormControl>
    );
});

const ReportExpectedField = React.memo(({
    reportExpected,
    isReportExpectedValid,
    handleSetReportExpected,
    checkReportExpected,
    reportExpectedHelperText
}) => {
    return (
        <FormControl required fullWidth error={!isReportExpectedValid}>
            <FormLabelComponent
                label={questions.expected.question}
                keyword={questions.expected.keyword}
                tooltipText={questions.expected.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                placeholder={questions.expected.placeholder}
                value={reportExpected}
                onChange={handleSetReportExpected}
                onBlur={(e) => checkReportExpected(e.target.value, false)}
            />
            <FormHelperText>{reportExpectedHelperText}</FormHelperText>
        </FormControl>
    );
})

const ReportActualField = React.memo(({
    reportActual,
    isReportActualValid,
    handleSetReportActual,
    checkReportActual,
    reportActualHelperText
}) => {
    return (
        <FormControl required fullWidth error={!isReportActualValid}>
            <FormLabelComponent
                label={questions.actual.question}
                keyword={questions.actual.keyword}
                tooltipText={questions.actual.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                placeholder={questions.actual.placeholder}
                value={reportActual}
                onChange={handleSetReportActual}
                onBlur={(e) => checkReportActual(e.target.value, false)}
            />
            <FormHelperText>{reportActualHelperText}</FormHelperText>
        </FormControl>
    );
})

const ReportUserImpactField = React.memo(({
    reportUserImpact,
    userRole,
    isReportUserImpactRatingValid,
    handleSetReportUserImpactRating,
    reportUserImpactRatingHelperText
}) => {
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"))
    return(
        <FormControl required error={!isReportUserImpactRatingValid} aria-invalid={!isReportUserImpactRatingValid}>
            <FormLabelComponent
                id="report-user-impact-rating"
                label={questions.user_impact_rating[userRole === null ? "simple" : "advanced"].question}
                tooltipText={questions.user_impact_rating.tooltip}
            />
            <StyledRadioGroup 
                isSmallScreen={isSmallScreen}
                aria-labelledby="report-user-impact-rating"
                name="user_impact_rating"
                value={reportUserImpact}
                onChange={handleSetReportUserImpactRating}
            >
                {questions.user_impact_rating[userRole === null ? "simple" : "advanced"].ratings.map((v, i) => (
                    <FormControlLabel 
                        key={i}
                        value={i}
                        label={v}
                        control={<Radio/>}
                        variant="hoverable"
                    />
                ))}
            </StyledRadioGroup>
            <FormHelperText>{reportUserImpactRatingHelperText}</FormHelperText>
        </FormControl>
    );
})

const ReportSeverityField = React.memo(({
    reportSeverity,
    isReportSeverityRatingValid,
    handleSetReportSeverityRating,
    reportSeverityRatingHelperText
}) => {
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"))
    return (
        <FormControl required error={!isReportSeverityRatingValid} aria-invalid={!isReportSeverityRatingValid}>
            <FormLabelComponent
                id="report-severity-rating"
                label={questions.severity_rating.question}
                tooltipText={questions.severity_rating.tooltip}
            />
            <StyledRadioGroup 
                isSmallScreen={isSmallScreen}
                aria-labelledby="report-severity-rating"
                name="user_severity_rating"
                value={reportSeverity}
                onChange={handleSetReportSeverityRating}
            >
                {questions.severity_rating.ratings.map((v, i) => (
                    <FormControlLabel key={i} value={i} label={v} control={<Radio/>} variant="hoverable" />
                ))}
            </StyledRadioGroup>
            <FormHelperText>{reportSeverityRatingHelperText}</FormHelperText>
        </FormControl>
    );
})


const ReportFixSuggestionsField = React.memo(({
    reportFixSuggestions,
    isReportFixSuggestionsValid,
    handleSetReportFixSuggestions,
    checkReportFixSuggestions,
    reportFixSuggestionsHelperText
}) => {
    return (
        <FormControl error={!isReportFixSuggestionsValid}>
            <FormLabelComponent
                label={questions.fix_suggestions.question}
                tooltipText={questions.fix_suggestions.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                value={reportFixSuggestions}
                onChange={handleSetReportFixSuggestions}
                onBlur={(e) => checkReportFixSuggestions(e.target.value, false)}
            />
            <FormHelperText>{reportFixSuggestionsHelperText}</FormHelperText>
        </FormControl>
    )
})

const ReportAdditionalCommentsField = React.memo(({
    reportAdditionalComments,
    isReportAdditionalCommentsValid,
    handleSetReportAdditionalComments,
    checkReportAdditionalComments,
    reportAdditionalCommentsHelperText
}) => {
    return (
        <FormControl error={!isReportAdditionalCommentsValid}>
            <FormLabelComponent
                label={questions.additional_comments.question}
                tooltipText={questions.additional_comments.tooltip}
            />
            <OutlinedInput multiline rows={4} fullWidth
                placeholder={questions.additional_comments.placeholder}
                value={reportAdditionalComments}
                onChange={handleSetReportAdditionalComments}
                onBlur={(e) => checkReportAdditionalComments(e.target.value, false)}
            />
            <FormHelperText>{reportAdditionalCommentsHelperText}</FormHelperText>
        </FormControl>
    )
})

const ReportConsentField = React.memo(({
    reportConsent,
    setReportConsent
}) => {
    return (
        <FormControl required>
            <FormControlLabel 
                control={<Checkbox
                    checked={reportConsent}
                    onChange={(e) => setReportConsent(e.target.checked)}
                />}
                label={
                    <FormLabelComponent
                        margins={false}
                        label={questions.consent.question}
                        tooltipText={questions.consent.tooltip}
                    />
                } 
            />
        </FormControl>
    );
});