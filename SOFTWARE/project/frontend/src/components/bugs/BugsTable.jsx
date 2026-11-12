import { AvatarGroup, Box, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from "@mui/material";
import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import LoadingTableRows from "../common/LoadingTableRows";
import StatusDot from "../common/StatusDot";
import { CustomLabelColours, ProgressColours, StatusColours, WorkspaceColours } from "../../api/colours";
import { BugFieldDescriptions, ProgressOptions, StatusOptions } from "../../api/options";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import UserAvatar from "../users/UserAvatar";
import BugPriorityLabel from "./BugPriorityLabel";
import TrendingUpIcon from '@mui/icons-material/TrendingUpRounded';
import InfoTooltip from "../common/InfoTooltip"
import theme from "../../styles/theme";

function BugsTable({
    totalResults,
    bugsLoading,
    bugs,
    showWorkspaces=false
}) {

    const pathParams = useParams();
    const navigate = useNavigate();
    dayjs.extend(relativeTime);

    const SHOW_TRENDING_THRESHOLD = 1.5

    function handleBugClick(bugId, workspaceId) {
        navigate(`/workspaces/${workspaceId ?? pathParams.id}/bugs/${bugId}`);
    }

    return (<>
        <Box 
            border={totalResults != 0 ? `1px solid ${theme.palette.divider}` : "transparent"}
            borderRadius={2}
            overflow={"hidden"}
        >
            <TableContainer>
                <Table size="small" aria-label="Bugs table">
                    {/* Table Header */}
                    { totalResults !== 0 &&
                        <TableHead>
                            <TableRow sx={{
                                bgcolor: theme.palette.neutral.main
                            }}>
                                {showWorkspaces === true && 
                                    <TableCell>Workspace</TableCell>
                                }
                                <TableCell>Progress</TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        Bug ID
                                    </Typography>
                                </TableCell>
                                <TableCell>Summary</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        First Reported
                                    </Typography>
                                </TableCell>
                                <TableCell>Assignees</TableCell>
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
                                        <Typography variant="body2">
                                            Suggestions
                                        </Typography>
                                        <InfoTooltip title={BugFieldDescriptions["SUGGESTIONS"]}/>
                                    </Box>
                                </TableCell>
                            </TableRow>
                        </TableHead>
                    }
                    <TableBody>
                        {bugsLoading ? <>
                            {/* Results loading view */}
                            <LoadingTableRows rows={totalResults <= 1 ? 1 : totalResults <= 2 ? 2 : totalResults < 5 ? 3 : 5} columns={showWorkspaces === true ? 10 : 9}/>
                        </> : bugs.map((v, _) => ( 
                            // Table Row
                            <TableRow
                                key={v.id} 
                                onClick={() => handleBugClick(v.id, v.workspace_id ? v.workspace_id : null)}
                                sx={{
                                    cursor: "pointer",
                                    transition: "background-color 0.25s ease-in-out",
                                    "&:hover": {backgroundColor: "action.hover"},
                                    "&:active": {backgroundColor: "action.selected"},
                                    '&:last-child td, &:last-child th': {
                                        borderBottom: 0,
                                    }
                                }}
                            >
                                {showWorkspaces === true &&
                                    <TableCell>
                                        <Chip
                                            label={
                                                <Typography
                                                    variant="body2"
                                                    noWrap
                                                    fontSize={"12px"}
                                                    sx={{maxWidth: "100px"}}
                                                >
                                                    {v.workspace_name}
                                                </Typography>
                                            }
                                            size="small"
                                            variant="soft-square"
                                            strength="weak"
                                            sx={{bgcolor: WorkspaceColours[v.workspace_colour]}}
                                        />
                                    </TableCell>
                                }
                                <TableCell>
                                    <Box display="flex" alignItems="center" flexWrap="nowrap" minHeight={"30px"}>
                                        <StatusDot colour={ProgressColours[v.progress]}/>
                                        <Typography noWrap variant="body2">{ProgressOptions[v.progress]}</Typography>
                                    </Box>
                                </TableCell>
                                <TableCell>
                                    <Chip size="small" variant="id" label={"ID: " + v.id}/>
                                </TableCell>
                                <TableCell>
                                    <Box>
                                        <Typography 
                                            variant="body2"
                                            maxWidth="172px"
                                            noWrap
                                        >
                                            {v.summary}
                                        </Typography>
                                        {v.labels?.length > 0 &&
                                            <Box display="flex" gap={0.25} mt={0.25}>
                                                {v.labels?.slice(0, 7).map((label, i) => (
                                                    <Tooltip title={label.name} key={i}>
                                                        <Box 
                                                            width="20px" 
                                                            height="8px" 
                                                            borderRadius={"2px"}
                                                            bgcolor={CustomLabelColours[label.colour]}
                                                        />
                                                    </Tooltip>
                                                ))}
                                            </Box>
                                        }
                                    </Box>
                                </TableCell>
                                <TableCell>
                                    <Box display="flex" alignItems="center" flexWrap="nowrap">
                                        <StatusDot colour={StatusColours[v.status]}/>
                                        <Typography noWrap variant="body2">{StatusOptions[v.status]}</Typography>
                                    </Box>
                                </TableCell>
                                <TableCell>
                                    <Tooltip title={dayjs(v.first_reported_timestamp).format("MMM D, YYYY h:mm A")}>
                                        <Typography variant="body2" noWrap>
                                            {dayjs(v.first_reported_timestamp).fromNow()}
                                        </Typography>
                                    </Tooltip>
                                </TableCell>
                                <TableCell align="left">
                                    {v.assignees.total_assignees == 0 ?
                                        <Typography variant="body2">None</Typography>
                                    :
                                        <AvatarGroup 
                                            spacing={"medium"} 
                                            max={3}
                                            total={v.assignees.total_assignees}
                                            slotProps={{surplus: {
                                                sx: {height: "28px", width: "28px"}
                                            }}}
                                            sx={{justifyContent: "flex-end"}}
                                        >
                                            {v.assignees.limited_assignees.map((a, _) => (
                                                <UserAvatar 
                                                    size="28px" 
                                                    username={a.user.username}
                                                    avatarColour={a.user.user_colour}
                                                />
                                            ))}
                                        </AvatarGroup>
                                    }
                                </TableCell>
                                <TableCell>
                                    <BugPriorityLabel priorityLabel={v.priority_label}/>
                                </TableCell>
                                <TableCell>
                                    <Box display="flex" alignItems="center" gap={0.5}>
                                        <Typography variant="body2">{v.frequency}</Typography>
                                        {v.hotness > SHOW_TRENDING_THRESHOLD && 
                                            <Tooltip title="This bug has a high rate of report">
                                                <TrendingUpIcon fontSize="small" color="error"/>
                                            </Tooltip>
                                        }
                                    </Box>
                                </TableCell>
                                <TableCell>{v.relevant_duplicate_suggestions}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    </>);
}

export default React.memo(BugsTable);