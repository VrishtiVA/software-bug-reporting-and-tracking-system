import { Button, Chip, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography } from "@mui/material";

/**
 * Displays list of bug reports and load more button if haven't fetched the last page.
 */
export default function BugReportsList({
    bugReports,
    selectedBugReport,
    handleSelectBugReport,
    moreBugReportPages,
    moreBugReportsLoading,
    handleFetchMoreReports
}) {
    return (<>
        <List disablePadding>
            { bugReports.map((v, _) => (
                <ListItem disablePadding key={v.id}>
                    <ListItemButton
                        selected={selectedBugReport.id === v.id}
                        onClick={() => handleSelectBugReport(v)}
                        sx={{display: "flex", gap: 0.25}}
                    >
                        <ListItemIcon>
                            <Chip size="small" variant="soft-square" strength="weak" label={"ID: " + v.id}/>
                        </ListItemIcon>
                        <ListItemText primary={
                            <Typography variant="body2" noWrap>
                                {v.summary}
                            </Typography>
                        }/>
                    </ListItemButton>
                </ListItem>
            ))}
            {/* Load more bug reports button if available */}
            { moreBugReportPages && <>
                <Button 
                    fullWidth 
                    variant="text"
                    sx={{borderRadius: 0}}
                    loading={moreBugReportsLoading}
                    onClick={handleFetchMoreReports}
                >
                    Show More
                </Button>
            </>}
        </List>
    </>);
}