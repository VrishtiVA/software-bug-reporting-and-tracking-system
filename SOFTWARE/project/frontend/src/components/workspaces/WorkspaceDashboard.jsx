import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../../api/axios'
import { Box, Card, Grid, Skeleton, styled, Typography, useMediaQuery } from '@mui/material';
import DuplicateSuggestionsOverview from '../duplicate_suggestions/DuplicateSuggestionsOverview';
import WorkspaceLabelsDistributionDisplay from './metrics/WorkspaceLabelsDistributionDisplay';
import WorkspacePriorityFrequencyBugCounts from './metrics/WorkspacePriorityFrequencyBugCounts';
import { WorkspaceMetricDescriptions } from '../../api/options';
import WorkspaceProgressDistribution from './metrics/WorkspaceProgressDistribution';
import WorkspacePriorityCategoryDistribution from './metrics/WorkspacePriorityCategoryDistribution';
import BugReportIcon from '@mui/icons-material/BugReportRounded';
import InfoTooltip from '../common/InfoTooltip';
import theme from '../../styles/theme';

function WorkspaceDashboard({
    userRole
}) {

    const pathParams = useParams();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"));

    const [metrics, setMetrics] = useState(null);
    const [metricsLoading, setMetricsLoading] = useState(true);

    const pieChartSeriesSettings = {
        innerRadius: 20, 
        outerRadius: 40,
        paddingAngle: 3,
        cornerRadius: 3,
        highlightScope: { 
            fade: "global",
            highlight: "item" 
        },
        faded: { 
            innerRadius: 20, 
            additionalRadius: -5, 
            color: "gray"
        }
    }

    const pieChartSeriesSettings_DuplicateSuggestions = {
        innerRadius: 10, 
        outerRadius: 20,
        paddingAngle: 3,
        cornerRadius: 3,
        highlightScope: { 
            fade: "global",
            highlight: "item" 
        },
        faded: { 
            innerRadius: 10, 
            additionalRadius: -2, 
            color: "gray"
        }
    }

    const legendSettings = {
        legend: {
            toggleVisibilityOnClick: true,
            sx: {
                gap: "2px",
                "& .MuiChartsLegend-label": {
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    maxWidth: 100
                }
            }
        }
    }

    const DashboardCard = styled(Card)(({theme}) => ({
        padding: theme.spacing(1.5),
        position: "relative",
        minHeight: "145px",
        [theme.breakpoints.up("sm")]: {
            padding: theme.spacing(1.75),
            minHeight: "150px"
        },
        border: "1px solid " + theme.palette.divider,
        boxShadow: "none",
        borderRadius: "6px"
    }));
    
    useEffect(() => {
        api.get(`/workspaces/${pathParams.id}/metrics`).then(({data}) => {
            setMetrics(data);
            setMetricsLoading(false);
        }).catch(error => console.log(error));
    }, []);

    return (<>
        <Grid container spacing={1.5}>
            {metricsLoading === true ? <>
                <Box width={isLargeScreen ? "50%" : "100%"} display="flex" gap={1.5}>
                    <Skeleton variant="rounded" width={"100%"} height={"150px"}/>
                    <Skeleton variant="rounded" width={"100%"} height={"150px"}/>
                </Box>
            </> : <>
                {userRole != null && <>
                    
                    <Grid size={{xs: 6, sm: 6, md: 6, lg: 3}}>
                        <DashboardCard>
                            <Typography variant="body1" color="textSecondary"> Total Bugs </Typography>
                            <Box 
                                display="flex"
                                alignItems="center"
                                gap={0.5}
                            >
                                <BugReportIcon fontSize="large"/>
                                <Typography variant="h3">{metrics.total_bugs}</Typography>
                            </Box>
                            <Box display="flex" alignItems="center" gap={0.75}>
                                <Typography variant="h6">{metrics.total_bugs_resolved}</Typography>
                                <Typography color="textSecondary"> Bug{metrics.total_bugs_resolved == 1 ? "" : "s"} Resolved </Typography>
                            </Box>
                        </DashboardCard>
                    </Grid>

                    {/* Duplicate Suggestions Overview */}
                    <Grid size={{xs: 6, sm: 6, md: 6, lg: 3}}>
                        <DashboardCard sx={{
                            display: "flex",
                            flexDirection: "column",
                            justifyContent: "space-between",
                        }}>
                            <DuplicateSuggestionsOverview 
                                outstandingPotentialDuplicateReportsCount={metrics.total_outstanding_duplicate_report_suggestions}
                                stagedDuplicateReportsCount={metrics.total_staged_duplicate_reports}
                                pieChartSeriesSettings={pieChartSeriesSettings_DuplicateSuggestions}
                                userRole={userRole}
                            />
                        </DashboardCard>
                    </Grid>

                    {(
                        metrics.progress_distribution.length > 0 || 
                        metrics.priority_category_distribution.length > 0 || 
                        metrics.workspace_custom_label_usage_distribution.length > 0 || 
                        metrics.priority_frequency_bug_count_buckets.length > 0
                    ) && <>

                        {/* Progress and their Counts */}
                        <Grid size={{xs: 6, sm: 6, md: 6, lg: 3}}>
                            <DashboardCard>
                                <Box display="flex" alignItems="center" gap={0.25}>
                                    <Typography variant="subtitle1" color="textSecondary" mb={0.25}>
                                        {isSmallScreen ? "Progress" : "Progress Distribution"}
                                    </Typography>
                                    <InfoTooltip title={WorkspaceMetricDescriptions["PROGRESS_DISTRIBUTION"]}/>
                                </Box>
                                <WorkspaceProgressDistribution 
                                    progressDistribution={metrics?.progress_distribution ?? {}} 
                                    pieChartSeriesSettings={pieChartSeriesSettings}
                                    legendSettings={legendSettings}
                                />
                            </DashboardCard>
                        </Grid>

                        {/* Bug Priority Category Distribution */}
                        <Grid size={{xs: 6, sm: 6, md: 6, lg: 3}}>
                            <DashboardCard>
                                <Box display="flex" alignItems="center" gap={0.25}>
                                    <Typography variant="subtitle1" color="textSecondary" mb={0.25}>
                                        {isSmallScreen ? "Priority" : "Priority Distribution"}
                                    </Typography>
                                    <InfoTooltip title={WorkspaceMetricDescriptions["PRIORITY_DISTRIBUTION"]} />
                                </Box>
                                <WorkspacePriorityCategoryDistribution 
                                    priorityDistribution={metrics?.priority_category_distribution ?? {}} 
                                    pieChartSeriesSettings={pieChartSeriesSettings}
                                    legendSettings={legendSettings}
                                />
                            </DashboardCard>
                        </Grid>

                        {/* Labels and their Counts */}
                        <Grid size={{xs: 12, sm: 12, md: 6, lg: 6}}>
                            <DashboardCard sx={{height: "315px"}}>
                                <Box display="flex" alignItems="center" gap={0.25} mb={1}>
                                    <Typography variant="subtitle1" color="textSecondary">Label Usage Distribution</Typography>
                                    <InfoTooltip title={WorkspaceMetricDescriptions["LABEL_USAGE_DISTRIBUTION"]}/>
                                </Box>
                                <WorkspaceLabelsDistributionDisplay workspaceLabelsDistribution={metrics.workspace_custom_label_usage_distribution} />
                            </DashboardCard>
                        </Grid>

                        {/* Priority Frequency Bubbles */}
                        <Grid size={{xs: 12, sm: 12, md: 6, lg: 6}}>
                            <DashboardCard sx={{height: "315px"}}>
                                <Box display="flex" alignItems="center" gap={0.25} mb={1}>
                                    <Typography variant="subtitle1" color="textSecondary">Priority Frequency Spread</Typography>
                                    <InfoTooltip title={WorkspaceMetricDescriptions["PRIORITY_FREQUENCY_SPREAD"]}/>
                                </Box>
                                <WorkspacePriorityFrequencyBugCounts
                                    priorityFrequencyBugCounts={metrics.priority_frequency_bug_count_buckets}
                                />
                            </DashboardCard>
                        </Grid>

                    </>}

                </>}
            </>}
        </Grid>
        
    </>);
}

export default React.memo(WorkspaceDashboard)