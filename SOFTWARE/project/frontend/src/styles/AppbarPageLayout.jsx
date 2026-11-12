
import {Box, Toolbar } from '@mui/material';
import AppBarComponent from '../components/users/AppBarComponent';

export default function AppbarPageLayout({children}) {

    return (<>
        <Box>
            {/* App Bar */}
            <AppBarComponent toggleNavigation={false} />
            <Box component="main">
                <Toolbar/>
                {/* Main content container */}
                <Box padding="18px">
                    {children}
                </Box>
            </Box>
        </Box>
    </>);
}