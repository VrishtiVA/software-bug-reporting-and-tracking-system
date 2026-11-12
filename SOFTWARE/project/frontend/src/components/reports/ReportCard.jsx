import { Box, Card } from "@mui/material";
import ReportView from "./ReportView";
import theme from "../../styles/theme";

export default function ReportCard({
    report, 
    height,
    isResolvePage = false
}) {
    return(<>
        <Box 
            variant="outlined" 
            sx={{
                overflow: "hidden",
                bgcolor: "neutral.main",
                borderRadius: 2,
                border: `1px solid ${theme.palette.divider}`
            }}
        >
            <Box sx={{
                p: 1.25,
                maxHeight: {xs: height},
                height: {md: height},
                overflowY: "auto",
            }}
            >
                <ReportView 
                    report={report} 
                    height={height} 
                    isResolvePage={isResolvePage}
                />
            </Box>
        </Box>
    </>);
}