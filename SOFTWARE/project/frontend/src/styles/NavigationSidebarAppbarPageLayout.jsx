
import {useEffect, useState} from 'react'
import {Box, Toolbar, useTheme, useMediaQuery } from '@mui/material';
import NavigationSideBarComponent from '../components/users/NavigationSideBarComponent';
import AppBarComponent from '../components/users/AppBarComponent';

export default function NavigationSidebarAppbarPageLayout({children}) {

    const theme = useTheme();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"));
    const [navigationOpen, setNavigationOpen] = useState(!isSmallScreen);
    const NAVIGATION_WIDTH = 245;

    useEffect(() => {
        setNavigationOpen(!isSmallScreen);
    }, [isSmallScreen])

    return (<>
        {/* Navigation Side Bar */}
        <NavigationSideBarComponent 
            variant={isSmallScreen ? "temporary" : "persistent"}
            navigationWidth={NAVIGATION_WIDTH}
            open={navigationOpen} 
            onClose={() => setNavigationOpen(false)}
        />
        <Box>
            {/* App Bar */}
            <AppBarComponent 
                toggleNavigation={true}
                toggleNavigationMethod={() => setNavigationOpen(true)}
                navigationOpen={navigationOpen}
                showNotifications={true}
            />
            <Box component="main"
                sx={{
                    ml: !isSmallScreen && navigationOpen ? `${NAVIGATION_WIDTH}px` : 0,
                    transition: "margin 0.3s ease-in-out",
                }}
            >
                <Toolbar/>
                {/* Main content container */}
                <Box padding="18px">
                    {children}
                </Box>
            </Box>
        </Box>
    </>);
}