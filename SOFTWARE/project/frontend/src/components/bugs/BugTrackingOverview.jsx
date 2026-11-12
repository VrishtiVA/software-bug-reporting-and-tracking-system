import { Badge, Box, Button, Card, Chip, IconButton, styled, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from "@mui/material";
import StatusDot from "../../components/common/StatusDot.jsx";
import { ProgressColours, StatusColours } from "../../api/colours.js";
import React, { useState } from "react";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import { BugFieldDescriptions, ProgressDescription, ProgressOptions, StatusDescriptions, StatusOptions } from "../../api/options.js";
import EditIcon from '@mui/icons-material/EditRounded';
import BugPriorityLabel from "./BugPriorityLabel.jsx";
import DoubleArrowRightIcon from '@mui/icons-material/KeyboardDoubleArrowRight';
import { useNavigate, useParams } from "react-router-dom";
import api from "../../api/axios.js";
import { useSnackbar } from "../contexts/SnackbarContext.jsx";
import InfoTooltip from "../common/InfoTooltip.jsx";
import theme from "../../styles/theme.js";

function BugTrackingOverview({
    bugDetails,
    bugStatus,
    bugProgress,
    openEditBugStatus,
    openEditBugProgress,
    canEdit = false
}) {

    dayjs.extend(relativeTime);
    const pathParams = useParams();
    const { displaySnackbar } = useSnackbar();
    const navigate = useNavigate();

    const [isResolveDuplicateSuggestionsLoading, setIsResolveDuplicateSuggestionsLoading] = useState(false);
    const [suggestionsCount, setSuggestionsCount] = useState(bugDetails.relevant_duplicate_suggestions);

    /**
     * Function to request to try claim, go to resolve page if accepted, otherwise show some alert for user feedback.
     */
    function claimReportForResolve() {
        //Handle loading state.
        setIsResolveDuplicateSuggestionsLoading(true);
        
        let startTime = performance.now();
        api.post(`workspaces/${pathParams.id}/duplicates/resolve/claim`, {
            "exclude_report_ids": [],
            "report_container_id": pathParams.bug_id
        }).then( ({data}) => {
            let endTime = performance.now();
            console.log(`Claiming report to resolve duplicates for in bug successfully took: ${endTime - startTime} ms.`);
            console.log(data);
            //Redirect to resolve page.
            navigate(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/reports/duplicates/resolve`)

        }).catch(error => {
            console.log(error);
            if (error.response?.status == 409 && error.response?.data) {
                displaySnackbar("warning", error.response.data.message, error.response.data.details)
            } else if (error.response?.status == 404 && error.response.data) {
                displaySnackbar("error", error.response.data.message, error.response.data.details)
                setSuggestionsCount(0);
            }
            setIsResolveDuplicateSuggestionsLoading(false);
        })
    }

    return (<>
        <TableContainer component={Card} variant="outlined" sx={{borderRadius: 2}}>
            <Table size="small" aria-label="Bug tracking overview">
                {/* Table Header */}
                <TableHead>
                    <TableRow sx={{
                        bgcolor: theme.palette.neutral.main
                    }}>
                        <TableCell>Progress</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>
                            <Typography variant="body2" noWrap>
                                First Reported
                            </Typography>
                        </TableCell>
                        <TableCell>
                            <Typography variant="body2" noWrap>
                                Last Updated
                            </Typography>
                        </TableCell>
                        <TableCell>
                            <Box display="flex" alignItems="center" gap={0.2}>
                                Priority
                                <InfoTooltip title={BugFieldDescriptions["PRIORITY"]}/>
                            </Box>
                        </TableCell>
                        <TableCell>
                            <Box display="flex" alignItems="center" gap={0.2}>
                                Frequency
                                <InfoTooltip title={BugFieldDescriptions["FREQUENCY"]}/>
                            </Box>
                        </TableCell>
                        <TableCell>
                            <Box display="flex" alignItems="center" gap={0.2}>
                                <Typography variant="body2" noWrap>Similar Suggestions</Typography>
                                <InfoTooltip title={BugFieldDescriptions["SUGGESTIONS"]}/>
                            </Box>
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    <TableRow sx={{"& td, & th": {borderBottom: "none"}}}>
                        <TableCell>
                            <Box display="flex" alignItems="center" gap={0.4}>
                                <Tooltip title={ProgressDescription[bugProgress]}>
                                    <Box display="flex" alignItems="center" flexWrap="nowrap">
                                        <StatusDot colour={ProgressColours[bugProgress]}/>
                                        <Typography noWrap variant="body2">{ProgressOptions[bugProgress]}</Typography>
                                    </Box>
                                </Tooltip>
                                {canEdit &&
                                    <IconButton size="small" onClick={openEditBugProgress}>
                                        <EditIcon fontSize="inherit"/>
                                    </IconButton>
                                }
                            </Box>
                        </TableCell>
                        <TableCell>
                            <Box display={"flex"} alignItems={"center"} gap={0.4}>
                                <Tooltip title={StatusDescriptions[bugStatus]}>
                                    <Box display="flex" alignItems="center" flexWrap="nowrap">
                                        <StatusDot colour={StatusColours[bugStatus]}/>
                                        <Typography noWrap variant="body2">{StatusOptions[bugStatus]}</Typography>
                                    </Box>
                                </Tooltip>
                                {canEdit &&
                                    <IconButton size="small" onClick={openEditBugStatus}>
                                        <EditIcon fontSize="inherit"/>
                                    </IconButton>
                                }
                            </Box>
                        </TableCell>
                        <TableCell>
                            <Tooltip title={dayjs(bugDetails.first_reported_timestamp).format("MMM D, YYYY h:mm A")}>
                                <Typography variant="body2" noWrap>{dayjs(bugDetails.first_reported_timestamp).fromNow()}</Typography>
                            </Tooltip>
                        </TableCell>
                        <TableCell>
                            <Tooltip title={dayjs(bugDetails.last_updated_timestamp).format("MMM D, YYYY h:mm A")}>
                                <Typography variant="body2" noWrap>{dayjs(bugDetails.last_updated_timestamp).fromNow()}</Typography>
                            </Tooltip>
                        </TableCell>
                        <TableCell>
                            <BugPriorityLabel priorityLabel={bugDetails.priority_label}/>
                        </TableCell>
                        <TableCell>{bugDetails.frequency}</TableCell>
                        <TableCell>
                            {suggestionsCount == 0 || !canEdit ? 
                                suggestionsCount
                                :
                                <Button
                                    size="small"
                                    variant="outlined"
                                    color="error"
                                    loading={isResolveDuplicateSuggestionsLoading}
                                    onClick={claimReportForResolve}
                                    sx={{ display: "flex", gap: 1 }}
                                    endIcon={<DoubleArrowRightIcon/>}
                                >
                                    <Chip 
                                        label={bugDetails.relevant_duplicate_suggestions} 
                                        size="small"
                                        variant="id"
                                        color="error"
                                    />
                                    Resolve
                                </Button>
                            }
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        </TableContainer>
    </>);
}

export default React.memo(BugTrackingOverview)