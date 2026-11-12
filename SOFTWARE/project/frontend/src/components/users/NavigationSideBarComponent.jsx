import { useState } from 'react'
import { Link, useNavigate } from "react-router-dom";
import { 
    Typography, 
    Drawer,
    Toolbar, 
    Divider, 
    ListItemText, 
    List, 
    ListItemButton,
    ListItem,
    ListItemIcon,
    Card,
    CardHeader,
    IconButton,
    Box,
    CardActionArea,
    DialogContentText,
    Button
} from "@mui/material";
import SignOutIcon from '@mui/icons-material/LogoutRounded';
import MenuIcon from '@mui/icons-material/MenuRounded';
import ManageAccountsIcon from '@mui/icons-material/ManageAccountsRounded';
import WorkspacesIcon from '@mui/icons-material/WorkspacesRounded';
import AssignmentIcon from '@mui/icons-material/AssignmentRounded';
import SendIcon from '@mui/icons-material/SendRounded';
import NewspaperIcon from '@mui/icons-material/NewspaperRounded';

import { useAuthentication } from '../../authentication/AuthenticationContext';

import DialogComponent from '../common/DialogComponent';
import MenuComponent from '../common/MenuComponent';
import UserAvatar from './UserAvatar';

const navigationLinks = [
    {name: "Workspaces", path: "/workspaces", icon: <WorkspacesIcon fontSize="small"/>},
    {name: "Assignments", path: "/bugs/assignments", icon: <AssignmentIcon fontSize="small"/>},
    {name: "Sent Reports", path: "/reports", icon: <SendIcon fontSize="small"/>},
    {name: "Catch Up", path: "/notifications", icon: <NewspaperIcon fontSize="small"/>}
]

export default function NavigationSideBarComponent({variant, navigationWidth, open, onClose}) {

    const navigate = useNavigate();
    const {currentUser, avatarColour, setSignedOut} = useAuthentication();

    //Handle opening and closing user settings menu
    const [userSettingsAnchorEl, setUserSettingsAnchorEl] = useState(null);
    const handleUserSettingsClick = (event) => setUserSettingsAnchorEl(event.currentTarget);
    const handleUserSettingsClose = () => setUserSettingsAnchorEl(null);

    //Handle opening and closing sign out dialog
    const [isSignoutDialogOpen, setSignoutDialogOpen] = useState(false);
    const closeSignoutDialog = () => { setSignoutDialogOpen(false); }
    const openSignoutDialog = () => { setSignoutDialogOpen(true); }

    function signOut() {
        setSignedOut();
        navigate("/signin", { replace: true });
    }

    const navigationSidebarContent = (<>
        <Box sx={{display: "flex", flexDirection: "column", height:"100%"}}>
            <Toolbar sx={{justifyContent: "space-between", alignItems:"center"}}>
                <Typography 
                    variant="overline" color="primary" fontWeight="bold"
                    component={Button} size="small" onClick={() => navigate("/workspaces")} 
                > 
                    MY BUGSPACE 
                </Typography>
                <IconButton color="inherit" edge="end"
                    aria-label="Open navigation drawer"
                    onClick={onClose}
                >
                    <MenuIcon />
                </IconButton>
            </Toolbar>
            <Divider />

            {/* Links takes up majority of space */}
            <Box sx={{flex: 1}}>
                <List dense>
                    {/* Show selected for active list */}
                    { navigationLinks.map((link) => (
                        <ListItem key={link.name} sx={{
                            p: 0.75,
                            pb: 0
                        }}>
                            <ListItemButton 
                                component={Link} 
                                to={link.path} 
                                selected={location.pathname === link.path} 
                                aria-label={link.name}
                                sx={{
                                    borderRadius: 1
                                }}
                            >
                                <ListItemIcon>
                                    {link.icon}
                                </ListItemIcon>
                                <ListItemText primary={link.name}/>
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
            </Box>

            <Box>
                <Divider />

                <Card 
                    variant="outlined" 
                    sx={{margin: "7px"}}
                >
                    <CardActionArea onClick={handleUserSettingsClick} aria-label="User settings">
                        <CardHeader
                            avatar={<UserAvatar size="36px" username={currentUser} avatarColour={avatarColour}/>}
                            title={currentUser ? currentUser : "UserName"}
                            sx={{
                                padding: "7px"
                            }}
                        />
                    </CardActionArea>
                </Card>
            </Box>
        </Box>

        {/* User Settings Menu */}
        <MenuComponent
            anchorEl={userSettingsAnchorEl}
            open={Boolean(userSettingsAnchorEl)}
            onClose={handleUserSettingsClose}
            //Anchor menu to top-left of element
            anchorOrigin={{horizontal: 'left', vertical: 'top'}}
            //Menu bottom-left meets anchor
            transformOrigin={{horizontal: 'left', vertical: 'bottom'}}
            adjustMarginLeft={-1}
            adjustMarginTop={-1}
            actions={[
                {
                    name: "Manage Profile",
                    disabled: true,
                    icon: <ManageAccountsIcon fontSize="small" />
                },
                {
                    name: "Sign Out",
                    icon: <SignOutIcon fontSize="small" />,
                    method: openSignoutDialog
                }
            ]}
        />

        {/* Signout Confirmation Dialog */}
        <DialogComponent 
            open={isSignoutDialogOpen}
            closeDialogMethod={closeSignoutDialog}
            dialogTitle="Sign Out?"
            dialogContent={
                <DialogContentText>
                    Are you sure you wish to sign out?
                </DialogContentText>
            }
            showCancel={true}
            mainActionName="Sign Out"
            mainActionMethod={signOut}
            mainActionType="error"
        />
    </>);

    return (<>
        {/* Temporary vs persistant variant with media query depending on size. */}
        <Drawer 
            variant={variant}
            anchor="left" 
            open={open} 
            onClose={onClose}
            ModalProps={{
                keepMounted: false, //destroy navigation when closed
                disablePortal: true, //prevent orphaned DOM that could block clicks.
            }}
            sx={{
                width: navigationWidth,
                //Its paper width
                "& .MuiDrawer-paper": {
                    width: navigationWidth,
                    boxSizing: "border-box",
                }
            }}>
            {navigationSidebarContent}
        </Drawer>
    </>);   
}