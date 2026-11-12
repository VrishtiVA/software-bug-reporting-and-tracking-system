import { Alert, Box, Chip, Collapse, Divider, FormControl, Icon, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText, MenuItem, Select, Tooltip, Typography, useMediaQuery } from "@mui/material";
import React, { useEffect, useState } from "react";
import theme from "../../styles/theme";
import { useParams } from "react-router-dom";
import ReportView from "../reports/ReportView";
import api from "../../api/axios";
import BugReportsList from "./BugReportsList";
import MenuOpenIcon from '@mui/icons-material/MenuOpenRounded';
import BugAttachmentsMasonry from "./BugAttachmentsMasonry";
import BugFieldDataCollection from "./BugFieldDataCollection";
import OpenInFullIcon from '@mui/icons-material/OpenInFullRounded';
import CloseFullscreenIcon from '@mui/icons-material/CloseFullscreenRounded';

/** Bug Explorer Component */
function BugReportViewer() {
    
    const pathParams = useParams();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"));

    //Bug Explorer View
    const [isViewSmall, setIsViewSmall] = useState(true);
    const [viewMenuOpen, setViewMenuOpen] = useState(true);
    const [selectedView, setSelectedView] = useState("all_reports")

    //Reports Pagination States
    const [moreBugReportPages, setMoreBugReportPages] = useState(false);
    const [moreBugReportsLoading, setMoreBugReportsLoading] = useState(false);
    const [bugReports, setBugReports] = useState([]);
    const [bugReportsCount, setBugReportsCount] = useState(0);
    const [currentBugReportPage, setCurrentBugReportPage] = useState(0);
    const [selectedBugReport, setSelectedBugReport] = useState();

    //Focussed View Pagination States
    const [selectedFieldView, setSelectedFieldView] = useState("images");
    const [moreAttachmentsLoading, setMoreAttachmentsLoading] = useState(false);
    const [moreFieldDataLoading, setMoreFieldDataLoading] = useState(false);

    const [imageAttachmentsMetadata, setImageAttachmentsMetadata] = useState(null);
    const [moreImageAttachmentPages, setMoreImageAttachmentPages] = useState(false);
    const [imageAttachmentsCount, setImageAttachmentsCount] = useState(0)
    const [currentImageAttachmentsPage, setCurrentImageAttachmentsPage] = useState(0);

    const [videoAttachmentsMetadata, setVideoAttachmentsMetadata] = useState(null);
    const [moreVideoAttachmentPages, setMoreVideoAttachmentPages] = useState(false);
    const [videoAttachmentsCount, setVideoAttachmentsCount] = useState(0)
    const [currentVideoAttachmentsPage, setCurrentVideoAttachmentsPage] = useState(0);

    const [fieldData, setFieldData] = useState()
    const [moreFieldDataPages, setMoreFieldDataPages] = useState(false);
    const [fieldDataCount, setFieldDataCount] = useState(0);
    const [currentFieldDataPage, setCurrentFieldDataPage] = useState(0);

    const DataFields = {
        "steps": "Steps To Reproduce",
        "environment": "Environments",
        "expected": "Expected Results",
        "actual": "Actual Results",
        "fix-suggestions": "Fix Suggestions",
        "additional-comments": "Additional Comments"
    }

    //Toggle auto bug explorer sidebar depending on screen size.
    useEffect(() => {
        setViewMenuOpen(!isSmallScreen && bugReportsCount > 1);
    }, [isSmallScreen, bugReportsCount])

    useEffect(() => {
        //Fetch first page of bug reports
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/reports`)
            .then(({data}) => {
                setMoreBugReportPages(!data.is_last);
                setBugReports(data.content);
                setBugReportsCount(data.total_elements);
                setCurrentBugReportPage(data.page_number);
                setSelectedBugReport(data.content[0]);
            }).catch(error => console.log(error));

        //Also fetch first pages of fields metadata, lightweight data.
        if (imageAttachmentsMetadata == null) loadImageAttachmentsFirstPage();
        if (videoAttachmentsMetadata == null) loadVideoAttachmentsFirstPage();
    }, []);

    /**
     * Function to fetch first page of a specific field data page.
     * @param {String} field 
     */
    function fetchFieldDataFirstPage(field) {
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/fields/${field}`)
        .then(({data}) => {
            setFieldData(data.content);
            setMoreFieldDataPages(!data.is_last);
            setFieldDataCount(data.total_elements);
            setCurrentFieldDataPage(data.page_number);
        }).catch(error => console.log(error));
    }

    /**
     * Function to handle toggling the view height of the bug explorer box.
     */
    function handleToggleViewHeight() {
        (isViewSmall) ? setIsViewSmall(false) : setIsViewSmall(true);
    }

    /**
     * Function to handle fetching more reports.
     */
    async function handleFetchMoreReports() {
        //Don't fetch more if last time was last page, or if currently fetching.
        if (moreBugReportsLoading || !moreBugReportPages) return;
        setMoreBugReportsLoading(true);

        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/reports`, {
            params: {
                page: currentBugReportPage + 1
            }
        })
        .then(({data}) => {
            setMoreBugReportPages(!data.is_last);
            setCurrentBugReportPage(data.page_number);
            //Add bugs reports to list, ensuring id is unique by filtering out reports which are visible reports.
            //This adds extra protection to not multi-add the same report to the view.
            setBugReports(prev => {
                let currentReportIds = new Set(prev.map(r => r.id));
                let loadedReports = (data.content).filter(r => !currentReportIds.has(r.id));
                return [...prev, ...loadedReports];
            });
            setMoreBugReportsLoading(false);
        }).catch(error => {
            console.log(error);
            setMoreBugReportsLoading(false);
        })
    }

    /** Function to load first page of image attachments metadata. */
    function loadImageAttachmentsFirstPage(firstTime = true) {
        //Load first images page.
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/attachments/images`)
        .then(({data}) => {
            setMoreImageAttachmentPages(!data.is_last);
            setImageAttachmentsMetadata(data.content); //this is attachment metadata
            setCurrentImageAttachmentsPage(data.page_number);
            if (firstTime) setImageAttachmentsCount(data.total_elements);
        }).catch(error => console.log(error));
    }

    /** Function to load first page of video attachments metadata. */
    function loadVideoAttachmentsFirstPage(firstTime = true) {
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/attachments/videos`)
        .then(({data}) => {
            setMoreVideoAttachmentPages(!data.is_last);
            setVideoAttachmentsMetadata(data.content); //this is attachment metadata
            setVideoAttachmentsCount(data.total_elements);
            if (firstTime) setCurrentVideoAttachmentsPage(data.page_number);
        }).catch(error => console.log(error));
    }

    /** Function to handle change view of bug explorer. */
    function handleSetView(e) {
        let newView = e.target.value
        setSelectedView(newView);
    }

    /** Function to reset image attachments page. */
    function resetImageAttachmentPages() {
        loadImageAttachmentsFirstPage(false);
    }

    /** Function to reset video attachments page. */
    function resetVideoAttachmentPages() {
        loadVideoAttachmentsFirstPage(false);
    }

    /** Function to fetch more image attachment metadata pages. */
    async function handleFetchMoreImageAttachments() {
        if (moreAttachmentsLoading || !moreImageAttachmentPages) return;
        setMoreAttachmentsLoading(true);
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/attachments/images`, {
            params: {
                page: currentImageAttachmentsPage + 1
            }
        })
        .then(({data}) => {
            setMoreImageAttachmentPages(!data.is_last);
            setCurrentImageAttachmentsPage(data.page_number);
            //Add attachments to list, ensuring id is unique by filtering out those already present.
            //This adds extra protection to not multi-add the same attachment to the list.
            setImageAttachmentsMetadata(prev => {
                let currentAttachmentIds = new Set(prev.map(a => a.id));
                let loadedAttachmentMetadata = (data.content).filter(a => !currentAttachmentIds.has(a.id));
                return [...prev, ...loadedAttachmentMetadata];
            });
            setMoreAttachmentsLoading(false);
        }).catch(error => {
            console.log(error);
            setMoreAttachmentsLoading(false);
        })
    }

    /**
     * Function to fetch more video attachments metadata pages.
     */
    async function handleFetchMoreVideoAttachments() {
        if (moreAttachmentsLoading || !moreVideoAttachmentPages) return;
        setMoreAttachmentsLoading(true);
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/attachments/videos`, {
            params: {
                page: currentVideoAttachmentsPage + 1
            }
        })
        .then(({data}) => {
            setMoreVideoAttachmentPages(!data.is_last);
            setCurrentVideoAttachmentsPage(data.page_number);
            //Add attachments to list, ensuring id is unique by filtering out those already present.
            //This adds extra protection to not multi-add the same attachment to the list.
            setVideoAttachmentsMetadata(prev => {
                let currentAttachmentIds = new Set(prev.map(a => a.id));
                let loadedAttachmentMetadata = (data.content).filter(a => !currentAttachmentIds.has(a.id));
                return [...prev, ...loadedAttachmentMetadata];
            });
            setMoreAttachmentsLoading(false);
        }).catch(error => {
            console.log(error);
            setMoreAttachmentsLoading(false);
        });
    }

    /**
     * Function to fetch more fields data pages.
     */
    async function handleFetchMoreFieldData(field) {
        if (moreFieldDataLoading || !moreFieldDataPages) return;
        setMoreFieldDataLoading(true);
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/fields/${field}`, {params: {
            page: currentFieldDataPage + 1,
        }})
        .then(({data}) => {
            setMoreFieldDataPages(!data.is_last);
            setCurrentFieldDataPage(data.page_number);
            setFieldData(prev => {
                let currentDataIds = new Set(prev.map(r => r.id));
                let loadedGieldData = (data.content).filter(r => !currentDataIds.has(r.id));
                return [...prev, ...loadedGieldData];
            });
            setMoreFieldDataLoading(false);
        }).catch(error => {
            console.log(error);
            setMoreFieldDataLoading(false);
        });
    }

    /** Function to select bug report to view. */
    function handleSelectBugReport(bugReport) {
        setSelectedBugReport(bugReport);
    }

    /** Function to handle selecting a field to view. */
    function handleSelectFieldView(view) {
        setSelectedFieldView(view);
        if (
            view == "steps" ||
            view == "environment" ||
            view == "expected" ||
            view == "actual" ||
            view == "fix-suggestions" ||
            view == "additional-comments"
        ) {
            fetchFieldDataFirstPage(view);
        }
    }

    /** Toggle collapse sidebar */
    const handleToggleViewMenuCollapse = () => setViewMenuOpen(prev => !prev);

    return (<>
        <Box 
            border={"1px solid " + theme.palette.divider} 
            borderRadius={2} 
            overflow={"hidden"}
        >
            <Box display={"flex"} alignItems={"center"} justifyContent={"space-between"} pr={1}>
                <Typography variant="h6" p={1.25} pl={1.75} display="flex" alignItems="center" gap={1}>
                    Bug Explorer
                    <Tooltip title={`${bugReportsCount} bug report${bugReportsCount == 1 ? " was" : "s were"} found related to this bug`}>
                        <Chip size="small" label={bugReportsCount}/>
                    </Tooltip>
                </Typography>
                <IconButton onClick={handleToggleViewHeight}>
                    {isViewSmall ? <OpenInFullIcon/> : <CloseFullscreenIcon/>}
                </IconButton>
            </Box>
            <Divider/>
            <Box display={"flex"} height={isViewSmall ? "45vh" : "80vh"} minHeight={"50vh"} position={"relative"} sx={{transition: "height 0.3s ease-in-out"}}>
                <Collapse 
                    orientation="horizontal" 
                    in={viewMenuOpen}
                    sx={{
                        position: {xs: "absolute", md: "relative"},
                        height: "100%",
                        zIndex: 2
                    }}
                >
                    <Box 
                        width={"250px"}
                        borderRight={`1px solid ${theme.palette.divider}`}
                        height="100%"
                        overflow={"auto"}
                        bgcolor={"white"}
                    >
                        <Box display="flex" p={1} gap={0.5} mr={1}>
                            <FormControl fullWidth>
                                <Select value={selectedView} onChange={handleSetView}>
                                    <MenuItem dense value="all_reports">All Reports</MenuItem>
                                    <MenuItem dense value="all_fields">Focussed View</MenuItem>
                                </Select>
                            </FormControl>
                            <IconButton color="inherit" edge="end"
                                onClick={handleToggleViewMenuCollapse}
                            >
                                <MenuOpenIcon />
                            </IconButton>
                        </Box>
                        <Divider/>

                        {selectedView === "all_reports" ?
                            <BugReportsList
                                bugReports={bugReports}
                                selectedBugReport={selectedBugReport}
                                handleSelectBugReport={handleSelectBugReport}
                                moreBugReportPages={moreBugReportPages}
                                moreBugReportsLoading={moreBugReportsLoading}
                                handleFetchMoreReports={handleFetchMoreReports}
                            /> 
                        : selectedView === "all_fields" ?
                            <List dense disablePadding>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "images"}
                                        onClick={() => handleSelectFieldView("images")}
                                    >
                                        <ListItemText primary="Related Images"/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "videos"}
                                        onClick={() => handleSelectFieldView("videos")}
                                    >
                                        <ListItemText primary="Related Videos"/>
                                    </ListItemButton>
                                </ListItem>

                                {/* Other fields */}
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "steps"}
                                        onClick={() => handleSelectFieldView("steps")}
                                    >
                                        <ListItemText primary={DataFields["steps"]}/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "environment"}
                                        onClick={() => handleSelectFieldView("environment")}
                                    >
                                        <ListItemText primary={DataFields["environment"]}/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "expected"}
                                        onClick={() => handleSelectFieldView("expected")}
                                    >
                                        <ListItemText primary={DataFields["expected"]}/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "actual"}
                                        onClick={() => handleSelectFieldView("actual")}
                                    >
                                        <ListItemText primary={DataFields["actual"]}/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "fix-suggestions"}
                                        onClick={() => handleSelectFieldView("fix-suggestions")}
                                    >
                                        <ListItemText primary={DataFields["fix-suggestions"]}/>
                                    </ListItemButton>
                                </ListItem>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        selected={selectedFieldView === "additional-comments"}
                                        onClick={() => handleSelectFieldView("additional-comments")}
                                    >
                                        <ListItemText primary={DataFields["additional-comments"]}/>
                                    </ListItemButton>
                                </ListItem>
                            </List>
                        : <></>
                        }
                    </Box>
                </Collapse>
                <Box 
                    p={1.5} 
                    sx={{flex: 1}}
                    overflow={"auto"}
                >
                    {selectedBugReport != null && selectedView === "all_reports" &&
                        <ReportView 
                            report={selectedBugReport} 
                            height={"100%"}
                            displayAsCard="false"
                            showToggle={!viewMenuOpen}
                            handleToggle={handleToggleViewMenuCollapse}
                        />
                    }
                    {selectedView === "all_fields" && <>
                        <Box display="flex" alignItems="center" gap={1} mb={1} flexWrap="wrap">
                            {!viewMenuOpen &&
                                <IconButton size="small" color="inherit" onClick={handleToggleViewMenuCollapse}>
                                    <MenuOpenIcon/>
                                </IconButton>
                            }
                            <Box display="flex" alignItems="center" gap={1} flexWrap="wrap" mb={viewMenuOpen ? 0.25 : 0} mt={viewMenuOpen ? 0.2 : 0}>
                                <Typography variant="h6" ml={viewMenuOpen ? 1 : 0} display="flex" alignItems="center" gap={1}> 
                                    { selectedFieldView == null ?  
                                        "Field View"
                                    : (selectedFieldView === "images" || selectedFieldView === "videos") ? <>
                                        Bug {
                                            selectedFieldView === "images" ? "Images" : 
                                            selectedFieldView === "videos" ? "Videos" : 
                                            "Attachments"
                                        }
                                        <Tooltip title={
                                            `${selectedFieldView === "images" ? 
                                                `${imageAttachmentsCount} image attachment` + (imageAttachmentsCount == 1 ? " was" : "s were") 
                                                : 
                                                `${videoAttachmentsCount} video attachment` + (videoAttachmentsCount == 1 ? " was" : "s were")
                                            } found related to this bug`
                                        }>
                                            <Chip size="small" label={selectedFieldView === "images" ? imageAttachmentsCount : videoAttachmentsCount}/>
                                        </Tooltip>
                                    </> : <>
                                        All {DataFields[selectedFieldView]}
                                        <Tooltip title={`${fieldDataCount} ${fieldDataCount == 1 ? "entry was" : "entries were"} found for this field`}>
                                            <Chip size="small" label={fieldDataCount}/>
                                        </Tooltip>
                                    </>}
                                </Typography>
                            </Box>
                        </Box>
                        <Divider/>
                        {selectedFieldView === "images" && imageAttachmentsMetadata !== null && (
                            imageAttachmentsMetadata.length > 0 ?
                                <BugAttachmentsMasonry
                                    attachmentsMetadata={imageAttachmentsMetadata}
                                    moreAttachmentPages={moreImageAttachmentPages}
                                    moreAttachmentsLoading={moreAttachmentsLoading}
                                    handleFetchMoreAttachments={handleFetchMoreImageAttachments}
                                    resetAttachmentPages={resetImageAttachmentPages}
                                />
                            :   <Alert severity="info" color="primary" sx={{mt: 1.5}}>
                                    There are no images related to this bug yet
                                </Alert>
                        )}
                        {selectedFieldView === "videos" && videoAttachmentsMetadata !== null && (
                            videoAttachmentsMetadata.length > 0 ?
                                <BugAttachmentsMasonry
                                    attachmentsMetadata={videoAttachmentsMetadata}
                                    moreAttachmentPages={moreVideoAttachmentPages}
                                    moreAttachmentsLoading={moreAttachmentsLoading}
                                    handleFetchMoreAttachments={handleFetchMoreVideoAttachments}
                                    resetAttachmentPages={resetVideoAttachmentPages}
                                /> 
                            :   <Alert severity="info" color="primary" sx={{mt: 1.5}}>
                                    There are no video clips related to this bug yet
                                </Alert>
                        )}
                        {(selectedFieldView === "steps" || 
                            selectedFieldView === "environment" || 
                            selectedFieldView === "expected" || 
                            selectedFieldView === "actual" ||
                            selectedFieldView === "fix-suggestions" ||
                            selectedFieldView === "additional-comments"
                        ) && fieldData != null && (
                            fieldData.length > 0 ?
                                <BugFieldDataCollection
                                    fieldData={fieldData}
                                    moreFieldDataPages={moreFieldDataPages}
                                    fieldDataLoading={moreFieldDataLoading}
                                    handleFetchMoreFieldData={() => handleFetchMoreFieldData(selectedFieldView)}
                                />
                            : <Alert severity="info" color="primary" sx={{mt: 1.5}}>
                                There is no data for this field yet
                            </Alert>
                        )}
                    </>}
                </Box>
            </Box>
        </Box>
    </>);
}

export default React.memo(BugReportViewer);