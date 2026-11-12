import { Badge, Box, Chip, Tooltip, Typography, useMediaQuery } from "@mui/material";
import Timeline from "@mui/lab/Timeline"
import TimelineItem from "@mui/lab/TimelineItem"
import TimelineConnector from "@mui/lab/TimelineConnector"
import TimelineDot from "@mui/lab/TimelineDot"
import TimelineContent from "@mui/lab/TimelineContent"
import TimelineSeparator from "@mui/lab/TimelineSeparator"
import { timelineItemClasses } from "@mui/lab/TimelineItem"
import NewIcon from '@mui/icons-material/NewReleasesRounded';
import BrushIcon from '@mui/icons-material/BrushRounded';
import UpdateIcon from '@mui/icons-material/UpdateRounded';
import AssignmentIcon from '@mui/icons-material/AssignmentIndRounded';
import MergeIcon from '@mui/icons-material/CallMergeRounded';
import SmartIcon from '@mui/icons-material/AutoAwesomeRounded';
import { toCamelCase } from "../../utilities/stringHandlers";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import { BugAuditLogTypeColours } from "../../api/colours";
import UserAvatar from "../users/UserAvatar";
import { ProgressOptions, StatusOptions } from "../../api/options";
import theme from "../../styles/theme";

export default function BugAuditLogTimeline({
    auditLogsData,
    totalAuditLogs
}) {
    dayjs.extend(relativeTime);
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));

    /** Function to find right icon for log type. */
    function auditLogTypeIcon(logType) {
        switch (logType) {
            case "CREATION": return <NewIcon fontSize="small"/>;
            case "MODIFICATION": return <BrushIcon fontSize="small"/>;
            case "PROGRESSION": return <UpdateIcon fontSize="small"/>;
            case "ASSIGNMENT": return <AssignmentIcon fontSize="small"/>;
            case "DUPLICATE_RESOLVED": return <MergeIcon fontSize="small"/>;
            case "DUPLICATE_AUTO_RESOLVED": return <SmartIcon fontSize="small"/>;
        }
    }

    /** Function to convert audit log token into a formatted component. */
    function auditLogTokenHandler(token, userRole, field, key) {
        switch (token.type) {
            case "TEXT": 
                return (
                    <Box key={key} component="span">
                        {token.value}
                    </Box>
                );
            case "USERNAME":
                return (
                    <Box key={key} component={"span"}>
                        <Box component="span" sx={{mr: 0.5}}>
                            @{token.value}
                        </Box>
                        <Chip 
                            component="span" 
                            size="small" 
                            variant="role" 
                            label={toCamelCase(userRole)}
                        />
                    </Box>
                );
            case "ID":
                return (
                    <Chip 
                        key={key} 
                        size="small"
                        variant="id"
                        component="span"
                        label={"ID: " + token.value}
                    />
                );
            case "OLD_VALUE":
                return (
                    <Box 
                        key={key} 
                        component="span"
                        sx={{
                            bgcolor: "old.main",
                            p: "1px 3px",
                            borderRadius: 0.5
                        }}
                    >
                        { field === "PROGRESS" ? ProgressOptions[token.value] 
                        : field === "STATUS" ? StatusOptions[token.value]
                        : token.value
                        }
                    </Box>
                );
            case "NEW_VALUE":
                return (
                    <Box 
                        key={key} 
                        component="span"
                        sx={{
                            bgcolor: "new.main",
                            p: "1px 3px",
                            borderRadius: 0.5
                        }}
                    >
                        { field === "PROGRESS" ? ProgressOptions[token.value] 
                        : field === "STATUS" ? StatusOptions[token.value]
                        : token.value
                        }
                    </Box>
                )
            default: return <></>;
        }
    }

    /** Function to parse content (all audit log tokens for a single message) into formatted components. */
    function auditLogMessageParser(content, userRole, field) {
        return content.map((v, i) => 
            auditLogTokenHandler(v, userRole, field, i)
        );
    }

    return (<>
        <Timeline sx={{
            ...(isSmallScreen ? {p: 0} : {}),
            m: 0,
            [`& .${timelineItemClasses.root}:before`]: {
                flex: 0,
                padding: 0,
            },
        }}>
            {auditLogsData.map((v, i) => (
                <TimelineItem key={v.id}>
                    <TimelineSeparator>
                        <TimelineDot
                            sx={{
                                p: 0.5,
                                boxShadow: "none",
                                bgcolor: BugAuditLogTypeColours[v.log_type]
                            }}
                        > 
                            {v.performer == null ?
                                auditLogTypeIcon(v.log_type)
                            :
                                <Badge
                                    anchorOrigin={{vertical: 'top', horizontal: 'right'}}
                                    badgeContent={
                                        <UserAvatar 
                                            size="16px" 
                                            username={v.performer.username} 
                                            avatarColour={v.performer.user_colour}
                                            border={true}
                                        />
                                    }
                                >
                                    {auditLogTypeIcon(v.log_type)}
                                </Badge>
                            }
                        </TimelineDot>
                        {i !== totalAuditLogs-1 && <TimelineConnector />}
                    </TimelineSeparator>
                    <TimelineContent>
                        <Tooltip title={"This happened on " + dayjs(v.timestamp).format("MMM D, YYYY h:mm A")}>
                            <Typography variant="caption" color="text.secondary">{dayjs(v.timestamp).fromNow()}</Typography>
                        </Tooltip>
                        <Typography variant="body2" sx={{
                            "& > *": {mr: 0.5}
                        }}>
                            {auditLogMessageParser(v.content, v.performer_role, v.field)}
                        </Typography>
                    </TimelineContent>
                </TimelineItem>
            ))}
        </Timeline>
    </>);
}