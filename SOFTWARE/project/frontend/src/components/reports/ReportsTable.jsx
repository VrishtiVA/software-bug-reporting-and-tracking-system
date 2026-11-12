import { Box, Button, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TableSortLabel, Tooltip, Typography } from "@mui/material";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import LoadingTableRows from "../common/LoadingTableRows";
import StatusDot from "../common/StatusDot";
import { ProgressColours, StatusColours, WorkspaceColours } from "../../api/colours";
import { ProgressOptions, StatusOptions } from "../../api/options";
import { WorkspaceChip } from "../workspaces/WorkspaceChip";
import theme from "../../styles/theme";

function ReportsTable({
    totalResults,
    reportsLoading,
    reports,
    isCreationTimestampSortDescending,
    handleToggleCreationTimestampSortDirection
}) {
    const navigate = useNavigate();
    dayjs.extend(relativeTime);

    function handleBugClick(bugId, workspaceId) {
        navigate(`/workspaces/${workspaceId}/bugs/${bugId}`);
    }

    function handleWorkspaceClick(workspaceId) {
        navigate(`/workspaces/${workspaceId}`);
    }

    return (<>
        <Box
            border={totalResults != 0 ? `1px solid ${theme.palette.divider}` : "transparent"}
            borderRadius={2}
            overflow={"hidden"}
        >
            <TableContainer>
                <Table size="small" aria-label="Reports table">
                    {/* Table Header */}
                    { totalResults !== 0 &&
                        <TableHead>
                            <TableRow sx={{
                                bgcolor: theme.palette.neutral.main
                            }}>
                                <TableCell>Workspace</TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        Report ID
                                    </Typography>
                                </TableCell>
                                <TableCell>Summary</TableCell>
                                <TableCell>
                                    <TableSortLabel
                                        direction={isCreationTimestampSortDescending ? "desc": "asc"}
                                        onClick={handleToggleCreationTimestampSortDirection}
                                    >
                                        Submitted
                                    </TableSortLabel>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        Bug Progress
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        Bug Status
                                    </Typography>
                                </TableCell>
                                <TableCell>Bug</TableCell>
                            </TableRow>
                        </TableHead>
                    }
                    <TableBody>
                        { reportsLoading ? <>
                            <LoadingTableRows rows={totalResults <= 1 ? 1 : totalResults <= 2 ? 2 : totalResults < 5 ? 3 : 5} columns={7}/>
                        </>: reports.map((r, _) => (
                            // Table Row
                            <TableRow
                                key={r.report_id}
                                sx={{
                                    '&:last-child td, &:last-child th': {
                                        borderBottom: 0,
                                    }
                                }}
                            >
                                <TableCell>
                                    { r.is_workspace_visitable ?
                                        <WorkspaceChip
                                            label={
                                                <Typography variant="body2" noWrap sx={{maxWidth: "100px"}} fontSize={"12px"}>
                                                    {r.workspace_name}
                                                </Typography>
                                            }
                                            size="small"
                                            variant="soft-square"
                                            strength="weak"
                                            bgcolor={WorkspaceColours[r.workspace_colour]}
                                            {...(r.is_workspace_visitable && {
                                                onClick: () => handleWorkspaceClick(r.workspace_id)
                                            })}
                                        />
                                        :
                                        <Tooltip title="This workspace is currently private">
                                            <Chip
                                                label={
                                                    <Typography variant="body2" noWrap sx={{maxWidth: "100px"}} fontSize={"12px"}>
                                                        {r.workspace_name}
                                                    </Typography>
                                                }
                                                size="small"
                                                variant="soft-square"
                                                strength="weak"
                                                sx={{bgcolor: WorkspaceColours[r.workspace_colour]}}
                                            />
                                        </Tooltip>
                                    }
                                </TableCell>
                                <TableCell>
                                    <Chip variant="id" label={"ID: " + r.report_id}/>
                                </TableCell>
                                <TableCell>
                                    <Box display="flex" alignItems="center" minHeight={"30px"}>
                                        <Typography 
                                            variant="body2"
                                            maxWidth="300px"
                                            noWrap
                                        >
                                            {r.report_summary}
                                        </Typography>
                                    </Box>
                                    
                                </TableCell>
                                <TableCell>
                                    <Tooltip title={dayjs(r.creation_timestamp).format("MMM D, YYYY h:mm A")}>
                                        <Typography variant="body2" noWrap>
                                            {dayjs(r.creation_timestamp).fromNow()}
                                        </Typography>
                                    </Tooltip>
                                </TableCell>
                                <TableCell>
                                    {r.is_contained === true ? <>
                                        <Box display="flex" alignItems="center" flexWrap="nowrap">
                                            <StatusDot colour={ProgressColours[r.bug_progress]}/>
                                            <Typography noWrap variant="body2">{ProgressOptions[r.bug_progress]}</Typography>
                                        </Box>
                                    </> : 
                                        <Typography variant="body2" color="textDisabled">Pending</Typography>
                                    }
                                </TableCell>
                                <TableCell>
                                    {r.is_contained === true ? <>
                                        <Box display="flex" alignItems="center" flexWrap="nowrap">
                                            <StatusDot colour={StatusColours[r.bug_status]}/>
                                            <Typography noWrap variant="body2">{StatusOptions[r.bug_status]}</Typography>
                                        </Box>
                                    </> : 
                                        <Typography variant="body2" color="textDisabled">Pending</Typography>
                                    }
                                </TableCell>
                                <TableCell>
                                    {r.is_contained === true && r.is_workspace_visitable === true ? <>
                                        <Button
                                            variant="contained"
                                            size="small"
                                            onClick={() => handleBugClick(r.bug_id, r.workspace_id)}
                                        >
                                            Visit
                                        </Button>
                                    </> : r.is_contained === true && r.is_workspace_visitable === false ?
                                        <Tooltip title="This workspace is private">
                                            <Typography variant="body2" color="textDisabled">Private</Typography>
                                        </Tooltip>
                                    :
                                        <Typography variant="body2" color="textDisabled">Pending</Typography>
                                    }
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    </>);
}

export default React.memo(ReportsTable);