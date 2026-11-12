import { Alert, AlertTitle, Badge, Box, Button, Chip, Divider, IconButton, ListItem, ListItemButton, ListItemIcon, ListItemText, Snackbar, styled, Typography } from "@mui/material";
import { useAuthentication } from "../../authentication/AuthenticationContext";
import NotificationsIcon from '@mui/icons-material/NotificationsNoneRounded';
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { findNotificationIcon } from "../../utilities/IconUtilities";
import PopoverHoverComponent from "../common/PopoverHoverComponent";

function NotificationsShortcutDisplay({}) {

    const navigate = useNavigate();
    const {unreadNotificationsCount, latestNotification, latestNotificationToast, setLatestNotificationToast} = useAuthentication();
    console.log("Unread count: "+ unreadNotificationsCount);

    const [notificationBellAnchorEl, setNotificationBellAnchorEl] = useState(null);
    const handleShowNotificationPreview = (target) => setNotificationBellAnchorEl(target); 
    const handleHideNotificationPreview = () => setNotificationBellAnchorEl(null);

    const StyledBadge = styled(Badge)(({theme}) => ({
        "& .MuiBadge-badge": {
            right: 2,
            top: 5,
            fontSize: "11px",
            border: `1.5px solid ${theme.palette.background.paper}`,
            padding: "0 4px",
        }
    }));

    function handleNotificationClick() {
        setLatestNotificationToast(null);
        navigate("/notifications")
    }
    
    function handleNotificationClose() {
        setLatestNotificationToast(null);
    }

    function handleGoToNotificationsPage() {
        navigate("/notifications");
        handleHideNotificationPreview();
    }

    return (<>
        <IconButton
            edge="end"
            aria-label="Notifications"
            onClick={(e) => handleShowNotificationPreview(e.currentTarget)}
        >
            <StyledBadge badgeContent={unreadNotificationsCount} color="error">
                <NotificationsIcon/>
            </StyledBadge>
        </IconButton>

        {/* Notifications Preview */}
        <PopoverHoverComponent
            anchorEl={notificationBellAnchorEl}
            open={Boolean(notificationBellAnchorEl)}
            onClose={handleHideNotificationPreview}
            //Anchor menu to bottom-right of element
            anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
            //Menu top-right meets anchor
            transformOrigin={{horizontal: 'right', vertical: 'top'}}
            popoverContent={<>
                {latestNotification == null ?
                    <Typography variant="body2" pl={1} pt={0.25}>
                        No New Notifications
                    </Typography>
                    :
                    <ListItemButton 
                        dense
                        onClick={handleGoToNotificationsPage}
                        sx={{
                            p: 0,
                            pl: 0.5, 
                            alignItems: "start",
                            borderRadius: 1.5
                        }}
                    >
                        <ListItemIcon sx={{minWidth: 0, py: 1}}>
                            {findNotificationIcon(latestNotification.type)}
                        </ListItemIcon>
                        <ListItemText
                            sx={{pl: 1.25}}
                            primary={
                                <Typography variant="body2">{latestNotification.type_string}</Typography>
                            }
                            secondary={
                                <Typography 
                                    variant="body2"
                                    maxWidth={"300px"}
                                    sx={{
                                        "& > *": {mr: 0.2},
                                        fontSize: "12px"
                                    }}
                                >
                                    {latestNotification.message.length > 0 && latestNotification.message.map((v, i) => (
                                        v.highlight ? 
                                            <Chip
                                                component="span"
                                                key={i}
                                                variant="id"
                                                size="small"
                                                strength="weak"
                                                label={v.value}
                                                sx={{fontSize: "11px"}}
                                            /> 
                                        :
                                            <Box 
                                                key={i} 
                                                component="span"
                                            >
                                                {v.value}
                                            </Box>
                                    ))}
                                </Typography>
                            }
                        />
                    </ListItemButton>
                }
                <Divider sx={{my: 0.75}}/>
                <Button
                    size="small"
                    startIcon={<NotificationsIcon/>}
                    onClick={handleGoToNotificationsPage}
                >
                    See All Notifications
                </Button>
            </>}
        />

        {latestNotification != null && 
            <Snackbar
                open={latestNotificationToast != null}
                autoHideDuration={5000}
                anchorOrigin={{vertical: "bottom", horizontal: "right"}}
                onClose={handleNotificationClose}
            >
                <Alert
                    severity="primary"
                    icon={findNotificationIcon(latestNotification.type, false)}
                    onClick={handleNotificationClick}
                    sx={{cursor: "pointer"}}
                    variant="filled"
                    elevation={2}
                >
                    <AlertTitle>{latestNotification.type_string}</AlertTitle>
                    <Typography 
                        variant="body2" 
                        maxWidth={"300px"}
                        sx={{
                            "& > *": {mr: 0.5}
                        }}
                    >
                        {latestNotification.message.length > 0 && latestNotification.message.map((v, i) => (
                            v.highlight ? 
                                <Chip
                                    component="span"
                                    key={i}
                                    variant="id"
                                    size="small"
                                    strength="weak"
                                    color="primary"
                                    label={v.value}
                                /> 
                            :
                                <Box 
                                    key={i} 
                                    component="span"
                                >
                                    {v.value}
                                </Box>
                        ))}
                    </Typography>
                </Alert>
            </Snackbar>
        }
    </>);
}

export default React.memo(NotificationsShortcutDisplay);