import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../../api/axios";
import { Box, Button, Chip, Divider, Skeleton, Stack, Tooltip, Typography, useMediaQuery } from "@mui/material";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import PageHeader from "../../components/common/PageHeader";

import BuildRoundedIcon from '@mui/icons-material/BuildRounded';
import PeopleIcon from '@mui/icons-material/PeopleRounded';
import PrivateIcon from '@mui/icons-material/LockRounded';
import PublicIcon from '@mui/icons-material/PublicRounded'
import BugReportIcon from '@mui/icons-material/BugReportRounded';

import { useAuthentication } from "../../authentication/AuthenticationContext";
import { findArticle } from "../../utilities/stringHandlers";
import WorkspaceMembersDialog from "../../components/workspaces/WorkspaceMembersDialog";
import FindBugs from "../../components/bugs/FindBugs";
import WorkspaceDashboard from "../../components/workspaces/WorkspaceDashboard";
import theme from "../../styles/theme";

export default function WorkspacePage() {

    dayjs.extend(relativeTime);
    const pathParams = useParams(); 
    const navigate = useNavigate();
    const { currentUser } = useAuthentication();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))

    const [workspaceLoading, setWorkspaceLoading] = useState(true);
    const [workspace, setWorkspace] = useState(undefined);

    /*
    The workspace object would look like:
    {
        id: long,
        name: string,
        description: string,
        colour: string, 
        is_public: bool,
        owner: string,
        user_role: ADMIN | DEVELOPER | REPORTER | null
    }
    */

    const [descriptionExpanded, setDescriptionExpanded] = useState(false);
    const [collapsedActionsCollection, setCollapsedActionsCollection] = useState(undefined);

    const [isWorkspaceMembersDialogOpen, setIsWorkspaceMembersDialogOpen] = useState(false);
    const [workspaceMembers, setWorkspaceMembers] = useState(undefined);
    const [workspaceMembersLoading, setWorkspaceMembersLoading] = useState(true);

    /** Method to open workspace members dialogue */
    const openWorkspaceMembersDialog = () => {
        setWorkspaceMembersLoading(true);
        setIsWorkspaceMembersDialogOpen(true);
        
        let startTime = performance.now();
        api.get("/workspaces/"+pathParams.id+"/members")
            .then(({data}) => {
                let endTime = performance.now();
                console.log(`Fetching workspace members took: ${endTime - startTime} ms.`);
                setWorkspaceMembers(data);
                setWorkspaceMembersLoading(false);
            })
            .catch(error => console.log(error));
    }
    const closeWorkspaceMembersDialog = () => {
        setIsWorkspaceMembersDialogOpen(false);
    }

    //Runs once after component mounts only.
    useEffect(() => {    
        let startTime = performance.now();
        setWorkspaceLoading(true);
        api.get("/workspaces/" + pathParams.id)
            .then( ({data}) => {
                let endTime = performance.now();
                console.log(`Fetching workspace details took: ${endTime - startTime} ms.`);
                setWorkspace(data);
                setCollapsedActionsCollection([
                    ...((data.user_role == "OWNER" || data.user_role === "ADMIN") ? [{
                        name: "Configure Workspace",
                        disabled: false,
                        icon: <BuildRoundedIcon fontSize="small"/>,
                        method: () => navigate(`/workspaces/${pathParams.id}/configure`)
                    }] : []),
                    {
                        name: "View Members",
                        disabled: false,
                        icon: <PeopleIcon fontSize="small"/>,
                        method: openWorkspaceMembersDialog
                    },
                    {
                        name: "New Bug Report",
                        disabled: false,
                        icon: <BugReportIcon fontSize="small"/>,
                        method: () => navigate(`/workspaces/${pathParams.id}/bugs/report`)
                    }
                ]);
                setWorkspaceLoading(false);                
            })
            .catch(error => console.log(error));
    }, []);


    return (<>

        {/* Workspace Page Heading */}
        { workspaceLoading ? 
            <PageHeader
                title={<Skeleton variant="rounded" width="200px"/>}
                previous="/workspaces"
            />
        : <>
            <PageHeader 
                title={
                    <span style={{
                        whiteSpace: "normal",
                        overflowWrap: "anywhere",
                    }}>
                        {workspace.name}
                    </span>
                }
                previous="/workspaces"
                collapsedActions={collapsedActionsCollection}
            />
            {/* Dialogs */}
            <WorkspaceMembersDialog 
                openState={isWorkspaceMembersDialogOpen}
                closeMethod={closeWorkspaceMembersDialog}
                workspaceMembers={workspaceMembers}
                workspaceMembersLoading={workspaceMembersLoading}
            />
        </>}
        
        {workspaceLoading ? 
            <Skeleton variant="text"/>
        : <>
            <Stack spacing={1.5}>
                <Box position="relative">
                    <Box sx={{
                        width: descriptionExpanded ? "100%" : "calc(100% - 85px)"
                    }}>
                        {workspace.description && <>
                            <Typography 
                                variant="body2"
                                noWrap={!descriptionExpanded}
                            >
                                {workspace.description}
                            </Typography>
                        </>}
                        
                        {(!workspace.description || descriptionExpanded) &&
                            <Box 
                                mt={!workspace.description ? 0 : 1} 
                                display="flex" 
                                gap={0.75} 
                                alignItems={isSmallScreen ? "start" : "center"} 
                                flexDirection={isSmallScreen ? "column" : "row"}
                            >
                                <Chip 
                                    size="small"
                                    variant="soft-square"
                                    strength="weak"
                                    icon={ 
                                        <Tooltip title={"This workspace is "+(workspace.is_public ? "public" : "private")}>
                                            {workspace.is_public ? <PublicIcon fontSize="small"/>: <PrivateIcon fontSize="small"/>}
                                        </Tooltip>
                                    }
                                    label={"Owned by " + workspace.owner}
                                    sx={{alignSelf: "start"}} 
                                />
                                <Tooltip title={dayjs(workspace.creation_timestamp).fromNow()}>
                                    <Typography variant="caption">
                                        {"Created On " + dayjs(workspace.creation_timestamp).format("MMM D, YYYY")}
                                    </Typography>
                                </Tooltip>
                            </Box>
                        }
                    </Box>

                    {workspace.description &&
                        <Button
                            onClick={() => setDescriptionExpanded(currentState => !currentState)}
                            size="small" 
                            sx={{
                                position: "absolute",
                                right: 0,
                                bottom: "-5px"
                            }}
                        >
                            {descriptionExpanded ? "Show Less" : "Show More"}
                        </Button>
                    }
                    
                </Box>
                
                <Divider/>

                {/* Dashboard display */}
                {workspace.user_role != null && <>
                    <Box display="flex" justifyContent="space-between" gap="10px" flexWrap="wrap">
                        { workspace.user_role != null && <>
                            <Box display="flex" alignItems="center" gap="10px" flexWrap="wrap">
                                <Chip 
                                    variant="soft-square"
                                    strength="weak"
                                    label={`You're ${workspace.user_role === "OWNER" ? "the" : findArticle(workspace.user_role)} ${workspace.user_role.toLowerCase()} here`} 
                                />
                            </Box>
                        </>}
                        <Button
                            variant="contained"
                            onClick={() => {navigate(`/workspaces/${pathParams.id}/bugs/assignments`)}}
                        >
                            View Assignments
                        </Button>
                    </Box>
                    <WorkspaceDashboard userRole={workspace.user_role} />
                    <Divider/>
                </>}

                {/* Bug Reports Header */}
                <Box display="flex" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap="5px">
                    <Typography variant="h6">Bugs</Typography>
                    <Button 
                        variant="contained"
                        component={Link} 
                        to={`/workspaces/${pathParams.id}/bugs/report`}
                    >
                        New Bug Report
                    </Button>
                </Box>
                <Divider/>
                <div>
                    <FindBugs workspaceDetails={workspace}/>
                </div>
                
            </Stack>
        </>}
    </>);

}
