import { useEffect, useState } from "react";
import api from "../../api/axios";
import PageHeader from "../../components/common/PageHeader";
import { Alert, Box, Button, Chip, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip, Typography, useMediaQuery } from "@mui/material";
import { findNotificationIcon } from "../../utilities/IconUtilities";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import theme from "../../styles/theme";
import { useNavigate } from "react-router-dom";

export default function CatchUpPage({}) {

    const navigate = useNavigate();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));
    dayjs.extend(relativeTime);

    const [size, setSize] = useState(15);
    const [currentPage, setCurrentPage] = useState(0);
    const [userWorkspaces, setUserWorkspaces] = useState(new Set());
    const [notificationsLoading, setNotificationsLoading] = useState(true);
    const [moreNotificationsLoading, setMoreNotificationsLoading] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [totalElements, setTotalElements] = useState(0);
    const [totalElementsDisplayed, setTotalElementsDisplayed] = useState(0);
    const [moreNotifications, setMoreNotifications] = useState(false);
    const [noNotifications, setNoNotifications] = useState(false);

    useEffect(() => {
        setNotificationsLoading(true);
        //Fetch users workspaces for checks.
        fetchUserWorkspaces();
        fetchNotifications(0, size);
        setNotificationsLoading(false);
    }, []);

    /** Fetch workspaces and hold an array of user workspaces. */
    function fetchUserWorkspaces() {
        api.get("/workspaces")
        .then( ({data}) => {
            console.log(data);
            setUserWorkspaces(new Set(data.workspaces.map(w => w.id)))
        }).catch((error) => console.log(error));
    }

    /** Function to fetch initial notification pages. */
    function fetchNotifications() {
        setMoreNotificationsLoading(true);
        api.get("/notifications", { params: {
            "size": size
        }})
        .then( ({data}) => {
            console.log(data);
            setNotifications(data.content);
            setTotalElements(data.total_elements);
            setTotalElementsDisplayed(data.content.length)
            setMoreNotifications(!data.is_last);
            setCurrentPage(data.page_number);
            setNoNotifications(data.content.length === 0 && data.is_last);
            //Mark loaded notifications that are not yet read as read.
            let loadedUnreadNotificationIds = data.content.length > 0 ? data.content.filter(n => !n.is_read).map(n => n.id) : [];
            if (loadedUnreadNotificationIds.length > 0) api.post("/notifications/read", {
                "notification_ids": loadedUnreadNotificationIds
            });
        })
        .catch((error) => console.log(error))
        .finally(() => setMoreNotificationsLoading(false));
    }

    /** Function to fetch more notification pages. */
    function fetchMoreNotifications() {
        setMoreNotificationsLoading(true);
        api.get(`/notifications`, { params: {
            "size": size,
            "page": currentPage + 1
        }})
        .then( ({data}) => {
            console.log(data);
            let loadedUnreadNotificationIds = [];
            setNotifications(prev => {
                let currentNotificationIds = new Set(prev.map(n => n.id));
                let loadedNotifications = (data.content).filter(newNotification => !currentNotificationIds.has(newNotification.id));
                setTotalElementsDisplayed(prev.length + loadedNotifications.length);
                
                //Mark loaded notifications that are not yet read as read.
                loadedUnreadNotificationIds = loadedNotifications.length > 0 ? loadedNotifications.filter(n => !n.is_read).map(n => n.id) : [];
                if (loadedUnreadNotificationIds.length > 0) api.post("/notifications/read", {
                    "notification_ids": loadedUnreadNotificationIds
                });

                return [...prev, ...loadedNotifications];
            });
            setMoreNotifications(!data.is_last);
            setCurrentPage(data.page_number);
            setMoreNotificationsLoading(false);
        })
        .catch((error) => console.log(error))
        .finally(() => setMoreNotificationsLoading(false))
    }

    /** Function to navigate the user to the right plsce on clicking "checkout" on a notification. */
    const notificationNavigationLink = (resource, resourceId, workspaceId) => {
        console.log(resource, resourceId, workspaceId);
        if (resource == "BUG") return `/workspaces/${workspaceId}/bugs/${resourceId}`;
        else if (resource == "WORKSPACE") return `/workspaces/${workspaceId}`;
        else return "/error";
    }

    return (<>
    
        <PageHeader
            title={"Catch Up"}
            showDataInfo={true}
            dataInfo={
                <Typography variant="subtitle2" display="flex" alignItems="center" gap={0.5} flexWrap="no-wrap">
                    Displaying
                    <Chip size="small" variant="id" label={totalElementsDisplayed+" of "+totalElements}/>
                    Notifications
                </Typography>
            }
        />
        
        {!notificationsLoading && 
            noNotifications ?
                <Alert color="primary">
                    There is nothing to catch-up with yet
                </Alert>
            : <>
            <Box
                borderRadius={2}
                border={`1px solid ${theme.palette.divider}`}
                overflow="hidden"
            >
                <List 
                    dense 
                    disablePadding 
                    sx={{ "& > :last-child": { borderBlock: "none" }}}
                >
                    {notifications != null && notifications.length > 0 && notifications.map((n, i) => (
                        <ListItem 
                            divider 
                            key={n.id} 
                            sx={{
                                bgcolor: n.is_read ? theme.palette.neutral.main : theme.palette.background.paper,
                                display: "flex",
                                alignItems: isSmallScreen ? "start" : "center",
                                justifyContent: isSmallScreen ? "start" : "space-between",
                                flexDirection: isSmallScreen ? "column" : "row"
                            }}
                        >   
                            <Box display="flex" alignItems="center">
                                <ListItemIcon sx={{ minWidth: "40px" }}>
                                    {findNotificationIcon(n.type)}
                                </ListItemIcon>
                                <ListItemText
                                    primary = {
                                        <Box display="flex" flexWrap="wrap" alignItems="center">
                                            <Typography pr={1}>{n.type_string}</Typography>
                                            <Tooltip title={dayjs(n.timestamp).format("MMM D, YYYY h:mm A")}>
                                                <Typography variant="caption" color="text.secondary">{dayjs(n.timestamp).fromNow()}</Typography>
                                            </Tooltip>
                                        </Box>
                                    }
                                    secondary = {
                                        <Typography 
                                            color="text.secondary"
                                            variant="body2" 
                                            sx={{
                                                "& > *": {mr: 0.5}
                                            }
                                        }>
                                            {
                                                n.message != null && n.message.length > 0 && n.message.map((mt, j) => (
                                                mt.highlight ? 
                                                    <Chip 
                                                        component="span"
                                                        key={j}
                                                        variant="id"
                                                        size="small"
                                                        strength="weak"
                                                        label={mt.value}
                                                    /> 
                                                :
                                                    <Box key={j} component="span">
                                                        {mt.value}
                                                    </Box>
                                                ))
                                            }
                                        </Typography>
                                    }
                                />
                            </Box>
                            {n.workspace_id != null && n.resource_id != null && userWorkspaces.has(n.workspace_id) &&
                                <Button
                                    onClick={() => navigate(notificationNavigationLink(n.resource, n.resource_id, n.workspace_id))}
                                    variant="contained"
                                    size={isSmallScreen ? "small" : "medium"}
                                    sx={{
                                        justifySelf: "start",
                                        alignSelf: isSmallScreen ? "start" : "center",
                                        ml: isSmallScreen ? "40px" : 0,
                                        mb: isSmallScreen ? 0.5 : 0,
                                    }}
                                    
                                >
                                    Check Out
                                </Button>
                            }
                        </ListItem>
                    ))}
                </List>
            </Box>
            {moreNotifications && 
                <Button
                    onClick={fetchMoreNotifications}
                    loading={moreNotificationsLoading}
                    variant="outlined"
                    fullWidth={isSmallScreen}
                    sx={{mt: 1.5}}
                    size="small"
                >
                    Show More
                </Button>
            }
        </>}

    </>);
}