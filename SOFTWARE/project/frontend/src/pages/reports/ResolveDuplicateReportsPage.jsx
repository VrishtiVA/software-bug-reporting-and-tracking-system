import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import PageHeader from "../../components/common/PageHeader";
import { useSnackbar } from "../../components/contexts/SnackbarContext";
import { useEffect, useState } from "react";
import api from "../../api/axios";
import { useError } from "../../components/contexts/ErrorContext";
import { Box, Button, ButtonGroup, Chip, DialogContentText, IconButton, Skeleton, Tooltip, Typography } from "@mui/material";
import { to2dpPercentage } from "../../utilities/stringHandlers";
import DialogComponent from "../../components/common/DialogComponent";
import DoubleArrowRightIcon from '@mui/icons-material/KeyboardDoubleArrowRightRounded';
import SlickCarousel from "../../components/common/SlickCarousel";
import InfoOutlineIcon from '@mui/icons-material/InfoOutlineRounded';
import ReportCard from "../../components/reports/ReportCard";
import theme from "../../styles/theme";

export default function ResolveDuplicateReportsPage({}) {

    const pathParams = useParams();

    const navigate = useNavigate();
    const { displaySnackbar } = useSnackbar();
    const { setError } = useError();

    const REPORT_HEIGHT = "63vh";
    
    const [suggestionsLoading, setSuggestionsLoading] = useState(true);
    const [fromReport, setFromReport] = useState({});
    const [allSuggestions, setAllSuggestions] = useState([]); //Suggestions grouped by container
    const [currentToReports, setCurrentToReports] = useState([]); //{similarity_score_avg: float, to_reports: []}
    const [currentToReportsPointer, setCurrentToReportsPointer] = useState(0);

    const [excludeReportIds, setExcludeReportIds] = useState([]);

    const [nextCandidateLoading, setNextCandidateLoading] = useState(true);
    const [acceptLoading, setAcceptLoading] = useState(false);
    const [acceptDisabled, setAcceptDisabled] = useState(false);
    const [notSureLoading, setNotSureLoading] = useState(false);
    const [notSureDisabled, setNotSureDisabled] = useState(false);
    const [declineLoading, setDeclineLoading] = useState(false);
    const [declineDisabled, setDeclineDisabled] = useState(false);

    const [displayApproveDuplicatesDialog, setDisplayApproveDuplicatesDialog] = useState(false);
    const openApproveDuplicatesDialog = () => setDisplayApproveDuplicatesDialog(true);
    const closeApproveDuplicatesDialog = () => setDisplayApproveDuplicatesDialog(false);

    const [displayRejectDuplicatesDialog, setDisplayRejectDuplicatesDialog] = useState(false);
    const openRejectDuplicatesDialog = () => setDisplayRejectDuplicatesDialog(true);
    const closeRejectDuplicatesDialog = () => setDisplayRejectDuplicatesDialog(false);

    const [displayNotSureDuplicatesDialog, setDisplayNotSureDuplicatesDialog] = useState(false);
    const openNotSureDuplicatesDialog = () => setDisplayNotSureDuplicatesDialog(true);
    const closeNotSureDuplicatesDialog = () => setDisplayNotSureDuplicatesDialog(false);

    const [displaySkipCandidateDialog, setDisplaySkipCandidateDialog] = useState(false);
    const openSkipCandidateDialog = () => setDisplaySkipCandidateDialog(true);
    const closeSkipCandidateDialog = () => setDisplaySkipCandidateDialog(false);

    /**
     * Function to handle accepting a duplicate suggestion.
     */
    function acceptDuplicateSuggestion() {
        setAcceptLoading(true);
        let startTime = performance.now();
        api.post(`/workspaces/${pathParams.id}/duplicates/resolve`, {
            from_report_id: fromReport.id,
            to_report_id: currentToReports.to_reports[0].id,
            is_accepted: true
        }).then( ({data}) => {
            let endTime = performance.now();
            console.log(`Sending accept duplicate suggestion took: ${endTime - startTime} ms.`);

            //Show success
            displaySnackbar("success", "Successfully Approved Duplicate Suggestion");

            //Close dialog and keep states safe until new load
            closeApproveDuplicatesDialog();
            setAcceptDisabled(true);
            setAcceptLoading(false);
            //Try to auto-claim another.
            claimNextReportToResolve(fromReport.id).then((didClaim) => { if (didClaim) {
                //Try fetch claim.
                findSuggestionsClaimToResolve(true);
            }});

        }).catch(error => {
            console.log(error);
            setAcceptLoading(false);
        });
    }
    
    /**
     * Function to handle rejecting a duplicate suggestion, 
     * including rejecting all suggestions to reports in the aggregated group.
     */
    async function rejectDuplicateSuggestion() {
        setDeclineLoading();
        try {
            let startTime = performance.now();
            //Reject each in the group, wait for this before proceed.
            await Promise.all(currentToReports.to_reports.map((report) => {
                let data = api.post(`/workspaces/${pathParams.id}/duplicates/resolve`, {
                    from_report_id: fromReport.id,
                    to_report_id: report.id,
                    is_accepted: false
                })
                console.log(data);
            }))
            let endTime = performance.now();
            console.log(`Rejecting duplicate suggestion(s) took: ${endTime - startTime} ms.`);

            //Show success
            displaySnackbar("success", "Successfully Rejected Duplicate Suggestion")

            //Close dialog and keep states safe until new load.
            closeRejectDuplicatesDialog();
            setDeclineDisabled(true);
            setDeclineLoading(false);

            //Check if more suggestions, and flip to next.
            if (allSuggestions.length > currentToReportsPointer + 1) {
                setSuggestionsLoading(true);
                console.log("More suggestions available");
                let newPointer = currentToReportsPointer + 1
                setCurrentToReportsPointer(newPointer);
                setCurrentToReports(allSuggestions[newPointer])
                setDeclineDisabled(false);
                setSuggestionsLoading(false);
            } else {
                //If no more suggestions (e.g. to other reports/reports of other containers) for current report, handle next claim.
                console.log("Exhausted suggestions");
                setSuggestionsLoading(true);
                //Try to auto-claim another.
                claimNextReportToResolve(fromReport.id).then((didClaim) => { if (didClaim) {
                    //Try fetch claim.
                    findSuggestionsClaimToResolve(true);
                }});
            }
        } catch(error) {
            console.log(error);
            setDeclineLoading(false);
        }
    }

    /**
     * Function for handling not sure click in resolving actions.
     */
    function notSureAboutDuplicateSuggestion() {
        setAcceptDisabled(true);
        setDeclineDisabled(true);
        setNotSureLoading(true);

        //Check if more suggestions, and flip to next.
        if (allSuggestions.length > currentToReportsPointer + 1) {
            setSuggestionsLoading(true);
            console.log("More suggestions available");
            let newPointer = currentToReportsPointer + 1
            setCurrentToReportsPointer(newPointer);
            setCurrentToReports(allSuggestions[newPointer])
            closeNotSureDuplicatesDialog();
            setAcceptDisabled(false);
            setDeclineDisabled(false);
            setNotSureLoading(false);
            setSuggestionsLoading(false);
        } else {
            //If no more suggestions for current report, handle claiming another.
            setSuggestionsLoading(true);
            closeNotSureDuplicatesDialog();
            //Unclaim first
            unclaimReportToResolve(fromReport.id);
            //Try to auto-claim another.
            claimNextReportToResolve(fromReport.id).then((didClaim) => { if (didClaim) {
                //Try fetch claim.
                findSuggestionsClaimToResolve(true);
            }});
        }
    }

    /** 
     * Function to make a claim to resolve
     * @returns true or false depending on if carried out claim or not respectively.
     */
    async function claimReportToResolve() {
        try {
            let startTime = performance.now();
            const data = await api.post(`/workspaces/${pathParams.id}/duplicates/resolve/claim`, {
                "exclude_report_ids": [],
                "report_container_id": pathParams.bug_id ?? null
            })
            setCurrentToReportsPointer(0);
            let endTime = performance.now();
            console.log(`Claiming report to resolve duplicates for successfully took: ${endTime - startTime} ms.`);
            return true;
        } catch (error) {
            console.log(error);
            if (error.response?.status == 409 && error.response?.data) {
                displaySnackbar("warning", error.response.data.message, error.response.data.details)
            } else if (error.response?.status == 404 && error.response.data) {
                displaySnackbar("error", error.response.data.message, error.response.data.details)
            }
            //Navigate back.
            navigate(pathParams.bug_id ? `/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}` : `/workspaces/${pathParams.id}`);
            return false;
        }
    }

    /**
     * Variant function to claim a report to resolve, but 
     * also passed an ID to exclude, to prevent repicking it out again.
     * @param {*} excludeNextId The report ID to exclude.
     * @returns boolean representing success.
     */
    async function claimNextReportToResolve(excludeNextId) {
        try {
            console.log("Trying to claim next");
            let startTime = performance.now();
            const data = await api.post(`/workspaces/${pathParams.id}/duplicates/resolve/claim`, {
                exclude_report_ids: [...excludeReportIds, excludeNextId],
                report_container_id: pathParams.bug_id ?? null
            })
            setExcludeReportIds(prev => [...prev, excludeNextId]);
            setCurrentToReportsPointer(0);
            let endTime = performance.now();
            console.log(`Claiming report to resolve duplicates for (excluding report ID: ${excludeNextId}) successfully took: ${endTime - startTime} ms.`);
            return true;
        } catch (error) {
            console.log("Failed to claim next");
            console.log(error);
            displaySnackbar("info", "Finished Resolving Suggestions", "No more duplicate suggestions available to resolve at the moment.")
            //Navigate.
            navigate(pathParams.bug_id ? `/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}` : `/workspaces/${pathParams.id}`);
            return false;
        }
    }

    /**
     * Function to aggregate suggestions by container.
     * @returns `[ [similarity_score_avg: float, to_reports: {}], ... ]`
     */
    function groupSuggestionsByContainer(data) {
        try {
            //In the reduce function, the accumulated is returned and sent back in.
            let toReports = data.duplicate_suggestions.reduce((accumulator, object) => {
                let container_id = object.to_report.report_container_id;
                
                //If not in a group currently, keep solo
                if (container_id === null) {
                    accumulator.push({
                        container_id: null, 
                        similarity_score_avg: object.similarity_score,
                        to_reports: [object.to_report]
                    });
                    return accumulator;
                } 
                
                //Has a container, check if already have reports from them
                let group = accumulator.find( g => g.container_id === container_id );
                
                //If not already in a group, initialise.
                if (!group) {
                    group = {
                        container_id: container_id,
                        similarity_score_sum: 0,
                        count: 0,
                        to_reports: []
                    };
                    accumulator.push(group);
                } 

                //Add item to collection
                group.similarity_score_sum += object.similarity_score
                group.count ++;
                group.to_reports.push(object.to_report);
                group.similarity_score_avg = group.similarity_score_sum/group.count;

                return accumulator;
            }, []);

            //Sort by descending similarity scores
            //a and b are represent pairs to compare, and a swap would be made if condition is true, to achieve a sorted outcome.
            //Safe as doesn't run if the list only has 0 or 1 items.
            //Descending order is b - a: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/sort
            //Full browser compatibility.
            toReports.sort((a, b) => b.similarity_score_avg - a.similarity_score_avg );

            return toReports;
        } catch (error) {
            console.log(error);
        }
    }

    /**
     * Returns a promise of true or false.
     */
    async function findSuggestionsClaimToResolve(redirctOnError) {
        try {
            setSuggestionsLoading(true)
            let startTime = performance.now();
            const response = await api.get(`/workspaces/${pathParams.id}/duplicates/resolve`)
            let endTime = performance.now();
            console.log(`Fetching duplicate suggestions for report took: ${endTime - startTime} ms.`);
            let data = response.data;
            
            //Set fromReport
            setFromReport(data.from_report);

            //Set suggestions, organised by containers
            let toReports = groupSuggestionsByContainer(data);
            setAllSuggestions(toReports); 

            //Set current to reports as the first item in suggestions collection.
            setCurrentToReports(toReports[0]);

            //Reset states ready to go
            setSuggestionsLoading(false);
            setAcceptLoading(false);
            setAcceptDisabled(false);
            setNotSureLoading(false);
            setNotSureDisabled(false);
            setDeclineLoading(false);
            setDeclineDisabled(false);
            setNextCandidateLoading(false);
            return true;
        } catch (error) {
            console.log(error)
            if (redirctOnError) {
                //Unable to find the claimed report to resolve, redirect to error page.
                setError(404, error.response.data?.message ?? null, error.response.data?.details ?? null);
                navigate("/error");
            }
            return false;
        }
    }

    /**
     * On load, try to fetch the claim,
     * Otherwise try to make a claim, if fail show notice from backend,
     * Otherwise attempt fetch again, if fail and redirect is false, redirect to error page.
     */
    useEffect(() => {
        if (suggestionsLoading) {
            //Fetch claim
            findSuggestionsClaimToResolve(false).then((success) => { if (!success) {
                //If not success, try once to make a claim.
                claimReportToResolve().then((didClaim) => { if (didClaim) {
                    //Try fetch claim again.
                    findSuggestionsClaimToResolve(true);
                }})
            }})
        }
    }, []);

    /** 
     * Fire and forget unclaim request, 
     * to prevent waiting and supporting faster user experience.
     */
    async function unclaimReportToResolve(fromReportId) {
        api.post(`/workspaces/${pathParams.id}/duplicates/resolve/${fromReportId}/cancel`)
            .catch(error => console.log(error));
        console.log("Unclaimed report to resolve");
    }

    /**
     * Function to call on back navigation to handle unclaiming a report to resolve.
     */
    function toPrevious() {
        //Unclaim
        unclaimReportToResolve(fromReport.id);
        //Navigate back.
        navigate(pathParams.bug_id ? `/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}` : `/workspaces/${pathParams.id}`);
    }

    /**
     * Function to fetch the next candidate report on skip.
     * Involves making a new claim.
     */
    function getNextCandidate() {
        closeSkipCandidateDialog();
        setNextCandidateLoading(true);
        setSuggestionsLoading(true);
        //Ensure unclaim first
        unclaimReportToResolve(fromReport.id);
        //Try to auto-claim another.
        claimNextReportToResolve(fromReport.id).then((didClaim) => { if (didClaim) {
            //Try fetch claim.
            findSuggestionsClaimToResolve(true);
        }});
    }

    return (<>
    
        <PageHeader
            title={"Confirm Duplicates"}
            previous={toPrevious}
            previousIsLink={false}
            mainActionName={"Skip"}
            mainActionLoadingState={nextCandidateLoading}
            mainActionMethod={openSkipCandidateDialog}
            mainActionIcon={<DoubleArrowRightIcon/>}
        />

        <Box 
            mb={1.5} 
            display="flex" 
            alignItems="center"
            gap={1} 
            flexWrap="wrap"
        >
            <Typography variant="h6">Are these reports about the same bug?</Typography>
            <Chip 
                size="small"
                variant="outlined"
                color="success"
                label={ 
                    to2dpPercentage(currentToReports.similarity_score_avg) + 
                    "% Similarity Detected"
                }/>
        </Box>
        
        {suggestionsLoading ? <>
            <Box mb={1.5} display="flex" gap={1.5} flexDirection={{xs: "column", md: "row"}}>
                <Box flex={1}>
                    <Skeleton variant="rounded" width="150px" sx={{mb: 1}}/>
                    <Skeleton variant="rounded" height={REPORT_HEIGHT}/>
                </Box>
                <Box flex={1}>
                    <Skeleton variant="rounded" width="150px" sx={{mb: 1}}/>
                    <Skeleton variant="rounded" height={REPORT_HEIGHT}/>
                </Box>
            </Box>
            <Skeleton variant="rounded" width="250px" sx={{mb: 1}}/>
        </>:<>
            <Box display="flex" gap={1.5} flexDirection={{xs: "column", md: "row"}}>
                <Box flex={1}>
                    <Typography mb={0.5} variant="body2" color="text.secondary">
                        Potential Duplicate Report
                    </Typography>
                    <ReportCard 
                        report={fromReport} 
                        height={REPORT_HEIGHT} 
                        isResolvePage={true}
                    />
                </Box>
                <Box flex={1} minWidth={0}>
                    <Typography mb={0.5} variant="body2" display="flex" alignItems="center" gap={0.15} color="text.secondary">
                        Matching Report{currentToReports.to_reports.length === 1 ? "" : "s"} 
                        {currentToReports.to_reports.length !== 1 &&
                            <Tooltip title={"These reports are confirmed to be duplicates of each other. You could swipe to see the others in this group."}>
                                <IconButton component="span" size="small" sx={{mt: -1, mb: -1}}>
                                    <InfoOutlineIcon fontSize="inherit" />
                                </IconButton>
                            </Tooltip>
                        }
                    </Typography>
                    {/* Slidable if multiple */}
                    {currentToReports.to_reports.length > 1 ? <>
                        <SlickCarousel>
                            {currentToReports.to_reports.map((report, i) => (
                                <ReportCard 
                                    key={i + "" + report.id} 
                                    report={report} 
                                    height={REPORT_HEIGHT}
                                    isResolvePage={true}
                                />
                            ))}
                        </SlickCarousel>
                    </>:
                        <ReportCard 
                            report={currentToReports.to_reports[0]}
                            height={REPORT_HEIGHT}
                            isResolvePage={true}
                        />
                    }
                </Box>
            </Box>

            <ButtonGroup sx={{mt: 2}}>
                <Button
                    color="error"
                    variant="contained"
                    onClick={openRejectDuplicatesDialog}
                >
                    No, they're different
                </Button>
                <Button
                    sx={{borderLeft: "none"}}
                    onClick={openNotSureDuplicatesDialog}
                >
                    Not sure
                </Button>
                <Button
                    color="success"
                    variant="contained"
                    onClick={openApproveDuplicatesDialog}
                >
                    Yes, they're duplicates
                </Button>
            </ButtonGroup>
        </>}

        <DialogComponent 
            open={displayApproveDuplicatesDialog}
            closeDialogMethod={closeApproveDuplicatesDialog}
            dialogTitle={"Accept Duplicate Suggestion"}
            dialogContent={
                <DialogContentText>
                    Are you sure you wish to accept this duplicate suggestion to group these reports together?
                </DialogContentText>
            }
            showCancel
            mainActionName={"Yes, they're duplicates"}
            mainActionType={"success"}
            mainActionMethod={acceptDuplicateSuggestion}
            mainActionLoading={acceptLoading}
            mainActionDisabled={acceptDisabled}
        />

        <DialogComponent 
            open={displayRejectDuplicatesDialog}
            closeDialogMethod={closeRejectDuplicatesDialog}
            dialogTitle={"Reject Duplicate Suggestion"}
            dialogContent={
                <DialogContentText>
                    Are you sure you wish to reject this duplicate suggestion?
                </DialogContentText>
            }
            showCancel
            mainActionName={"Yes, they're different"}
            mainActionType={"error"}
            mainActionMethod={rejectDuplicateSuggestion}
            mainActionLoading={declineLoading}
            mainActionDisabled={declineDisabled}
        />

        <DialogComponent 
            open={displayNotSureDuplicatesDialog}
            closeDialogMethod={closeNotSureDuplicatesDialog}
            dialogTitle={"Not Sure"}
            dialogContent={
                <DialogContentText>
                    Do you wish to skip this duplicate suggestion and let it be resolved later or by someone else?
                </DialogContentText>
            }
            showCancel
            mainActionName={"Yes, skip"}
            mainActionMethod={notSureAboutDuplicateSuggestion}
            mainActionLoading={notSureLoading}
            mainActionDisabled={notSureDisabled}
        />

        <DialogComponent 
            open={displaySkipCandidateDialog}
            closeDialogMethod={closeSkipCandidateDialog}
            dialogTitle={"Skip Suggestion"}
            dialogContent={
                <DialogContentText>
                    Do you wish to resolve a different report instead, and leave this report to be resolved later or by someone else.
                </DialogContentText>
            }
            showCancel
            mainActionName={"Yes, skip"}
            mainActionMethod={getNextCandidate}
            mainActionLoading={nextCandidateLoading}
        />

    </>);
}