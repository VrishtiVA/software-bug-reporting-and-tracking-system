import { Box, Button, Card, Divider, Skeleton, Typography, useMediaQuery } from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../api/axios";
import React, { useEffect, useState } from "react";
import DoubleArrowRightIcon from '@mui/icons-material/KeyboardDoubleArrowRight';
import { useSnackbar } from "../contexts/SnackbarContext";
import TaskAltIcon from '@mui/icons-material/TaskAltRounded';
import { PieChart } from "@mui/x-charts";
import ContentCopyIcon from '@mui/icons-material/ContentCopyRounded';
import InfoTooltip from "../common/InfoTooltip";
import { WorkspaceMetricDescriptions } from "../../api/options";
import theme from "../../styles/theme";

function DuplicateSuggestionsOverview({
    outstandingPotentialDuplicateReportsCount,
    stagedDuplicateReportsCount,
    pieChartSeriesSettings,
    userRole
}) {
    
    const pathParams = useParams(); 
    const navigate = useNavigate();
    const { displaySnackbar } = useSnackbar();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))

    const [isResolveDuplicateSuggestionsLoading, setIsResolveDuplicateSuggestionsLoading] = useState(false);
    const [isResolveDuplicateSuggestionsEnabled, setIsResolveDuplicateSuggestionsEnabled] = useState(outstandingPotentialDuplicateReportsCount > 0);

    //Form visualisation data.
    const duplicatesStateData = [];
    if (outstandingPotentialDuplicateReportsCount > 0) {
        duplicatesStateData.push(
            { 
                id: 0, 
                value: outstandingPotentialDuplicateReportsCount, 
                label: 'Outstanding', 
                labelMarkType: "square", 
                color: "salmon",
                details: 
                    (outstandingPotentialDuplicateReportsCount == 1 ? "report" : "reports") + 
                    (userRole == "REPORTER" ? " waiting to be resolved." : " waiting for your decision.")
            }
        );
    } 
    if (stagedDuplicateReportsCount > 0) {
        duplicatesStateData.push(
            { 
                id: 1, 
                value: stagedDuplicateReportsCount, 
                label: 'Waiting', 
                labelMarkType: "square",
                color: "green",
                details: 
                    (stagedDuplicateReportsCount == 1 ? "report" : "reports") + 
                    (" dependent on the resolution of outstanding duplicate suggestions.")
            }
        )
    }

    /**
     * Function to request to try claim, go to page if accepted, otherwise show some alert for user feedback.
     */
    function claimReportForResolve() {
        //Handle loading state.
        setIsResolveDuplicateSuggestionsLoading(true);
        
        let startTime = performance.now();
        api.post(`workspaces/${pathParams.id}/duplicates/resolve/claim`, {
            "exclude_report_ids": [],
            "report_container_id": null
        }).then( ({data}) => {
            let endTime = performance.now();
            console.log(`Claiming report to resolve duplicates for successfully took: ${endTime - startTime} ms.`);
            console.log(data);
            //Redirect to resolve page.
            navigate(`/workspaces/${pathParams.id}/bugs/reports/duplicates/resolve`)

        }).catch(error => {
            console.log(error);
            if (error.response?.status == 409 && error.response?.data) {
                displaySnackbar("warning", error.response.data.message, error.response.data.details)
            } else if (error.response?.status == 404 && error.response.data) {
                displaySnackbar("error", error.response.data.message, error.response.data.details)
            }
            
            setIsResolveDuplicateSuggestionsLoading(false);
        })
    }

    return (<>
        { isResolveDuplicateSuggestionsEnabled ? <>
            <Box display="flex" gap={0} position="relative">
                <div>
                    <Box display="flex" alignItems="center" gap={0.25}>
                        <Typography variant="body1" color="textSecondary" noWrap>
                            {isSmallScreen ? "Suggestions" : "Duplicate Suggestions"}
                        </Typography>
                        <InfoTooltip title={WorkspaceMetricDescriptions["DUPLICATE_SUGGESTIONS"]}/>
                    </Box>
                    <Box display="flex" alignItems="center">
                        <ContentCopyIcon fontSize="large"/>
                        <Typography 
                            variant="h3"
                            paddingLeft={0.5}
                        >
                            {outstandingPotentialDuplicateReportsCount}
                        </Typography>
                    </Box>
                </div>

                <PieChart
                    series={[
                        {
                            ...pieChartSeriesSettings,
                            data: duplicatesStateData,
                            valueFormatter: (item) => (<>
                                <Divider sx={{mb: 1}}/>
                                <Typography variant="body2" mb={1}>{`${item.value} ${item.details}`}</Typography>
                            </>)
                        },
                    ]}
                    width={50}
                    height={110}
                    hideLegend
                    sx={{
                        position: "absolute",
                        right: 0,
                    }}
                    slotProps={{
                        tooltip: {
                            sx: {
                                '& .MuiChartsTooltip-labelCell': {
                                    display: "flex",
                                    alignItems: "center",
                                    mt: 1
                                },
                                '& .MuiChartsTooltip-row': {
                                    display: "flex",
                                    flexDirection: "column",
                                    gap: 0,
                                    maxWidth: "300px"
                                },
                            }
                        }
                    }}
                />
            </Box>

            {userRole != "REPORTER" &&
                <Button
                    fullWidth
                    variant="contained"
                    size="small"
                    loading={isResolveDuplicateSuggestionsLoading}
                    onClick={claimReportForResolve}
                    endIcon={<DoubleArrowRightIcon/>}
                >
                    {isSmallScreen ? "Resolve" : "Resolve Suggestions"}
                </Button>
            }

        </>:<>
            {/* If none, show tick and none to resolve. */}
            <Box 
                display="flex" 
                flexDirection="column"
            >
                <Box display="flex" alignItems="center" gap={0.25}>
                    <Typography variant="subtitle1" color="textSecondary" noWrap>
                        {isSmallScreen ? "Suggestions" : "Duplicate Suggestions"}
                    </Typography>
                    <InfoTooltip title={WorkspaceMetricDescriptions["DUPLICATE_SUGGESTIONS"]}/>
                </Box>
                <Box display="flex" alignItems="center" gap={1} pl={0.5}>    
                    <Typography variant="h3">
                        0
                    </Typography>
                    <TaskAltIcon 
                        fontSize="large"
                        color="success"
                    />
                </Box>
                
            </Box>
        </>}
    </>);
}

export default React.memo(DuplicateSuggestionsOverview);