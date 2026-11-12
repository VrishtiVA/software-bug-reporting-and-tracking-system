import { AppBar, Box, Button, IconButton, Toolbar, Typography } from '@mui/material';
import MenuIcon from '@mui/icons-material/MenuRounded'
import { useNavigate } from 'react-router-dom';
import NotificationsShortcutDisplay from './NotificationsShortcutDisplay';

export default function AppBarComponent({
    toggleNavigation, 
    toggleNavigationMethod = null, 
    navigationOpen = null,
    showNotifications = false
}) {

    const navigate = useNavigate();

    return(<>
        <AppBar
            elevation={0}
            color="transparent"
            position="fixed"
            sx={{
                borderBottom: 1,
                borderColor: "divider",
                backgroundColor: "white"
            }}
        >
            <Toolbar sx={{display: "flex", alignItems:"center", justifyContent:"space-between", gap: "5px"}}>
                <Box sx={{display: "flex", alignItems:"center", gap: 0.5}}>
                    {toggleNavigation && 
                        <IconButton
                            color="inherit"
                            edge="start"
                            aria-label="Open navigation drawer"
                            onClick={toggleNavigationMethod}
                            sx={{ display: navigationOpen ? "none" : "inline-flex" }}
                        >
                            <MenuIcon />
                        </IconButton>
                    }
                    <Typography 
                        variant="overline" color="primary" fontWeight="bold"
                        component={Button} size="small" onClick={() => navigate("/workspaces")} 
                    > 
                        MY BUGSPACE 
                    </Typography>
                </Box>
                {showNotifications == true &&
                    <NotificationsShortcutDisplay/>
                }
            </Toolbar>
        </AppBar>
    </>);
}