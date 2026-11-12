import { useNavigate, useParams } from "react-router-dom";
import PageHeader from "../../components/common/PageHeader.jsx"
import { useEffect, useState } from "react";
import { AvatarGroup, Box, Button, Chip, Divider, IconButton, Skeleton, Stack, Typography, useMediaQuery } from "@mui/material";
import api from "../../api/axios.js";
import BugTrackingOverview from "../../components/bugs/BugTrackingOverview.jsx";
import BugReportViewer from "../../components/bugs/BugReportViewer.jsx";
import UserAvatar from "../../components/users/UserAvatar.jsx";
import PopoverHoverComponent from "../../components/common/PopoverHoverComponent.jsx";
import BugAssigneesList from "../../components/bugs/BugAssigneesList.jsx";
import { getBugEditAssigneesPermittedRoles } from "../../api/rules.js";
import { useAuthentication } from "../../authentication/AuthenticationContext.jsx";
import { useSnackbar } from "../../components/contexts/SnackbarContext.jsx";
import EditAssigneesDialog from "../../components/bugs/EditAssigneesDialog.jsx";
import PeopleIcon from '@mui/icons-material/PeopleRounded';
import EditIcon from '@mui/icons-material/EditRounded';
import GroupAddIcon from '@mui/icons-material/GroupAddRounded';
import WorkspaceCustomLabelDisplay from "../../components/workspaces/WorkspaceCustomLabelDisplay.jsx";
import EditBugSummaryDialog from "../../components/bugs/EditBugSummaryDialog.jsx";
import EditBugStatusDialog from "../../components/bugs/EditBugStatusDialog.jsx";
import EditBugProgressDialog from "../../components/bugs/EditBugProgressDialog.jsx";
import EditBugLabelsDialog from "../../components/bugs/EditBugLabelsDialog.jsx";
import LabelsIcon from '@mui/icons-material/StyleRounded';
import TimelineIcon from '@mui/icons-material/TimelineRounded';
import FactCheckIcon from '@mui/icons-material/FactCheckRounded';
import TitleIcon from '@mui/icons-material/TitleRounded';
import HistoryIcon from '@mui/icons-material/HistoryRounded';
import theme from "../../styles/theme.js";

export default function BugPage({}) {

    const pathParams = useParams();
    const navigate = useNavigate();
    const {currentUser} = useAuthentication();
    const {displaySnackbar} = useSnackbar();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));

    const [bugDetails, setBugDetails] = useState(null);
    const [bugAssignees, setBugAssignees] = useState(null);
    const [bugSummary, setBugSummary] = useState(null);
    const [bugStatus, setBugStatus] = useState(null);
    const [bugProgress, setBugProgress] = useState(null);
    const [bugLabels, setBugLabels] = useState(null);

    const [workspaceCustomLabels, setWorkspaceCustomLabels] = useState([]);
    const [collapsedActionsCollection, setCollapsedActionsCollection] = useState(undefined);
    const [bugDetailsLoading, setBugDetailsLoading] = useState(true);
    const [editAssigneesPermittedRoles, setEditAssigneesPermittedRoles] = useState(null);
    const [isUserAssignee, setIsUserAssignee] = useState(null);
    const [canEdit, setCanEdit] = useState(false);

    const [isAssignSelfLoading, setIsAssignSelfLoading] = useState(false);

    const [bugAssigneesAnchorEl, setBugAssigneesAnchorEl] = useState(null);
    const handleShowBugAssigneesDetails = (target) => setBugAssigneesAnchorEl(target); 
    const handleHideBugAssigneesDetails = () => setBugAssigneesAnchorEl(null);

    const [displayEditAssigneesDialog, setDisplayEditAssigneesDialog] = useState(false);
    const openEditAssigneesDialog = () => {
        handleHideBugAssigneesDetails();
        setDisplayEditAssigneesDialog(true);
    }
    const closeEditAssigneesDialog = () => setDisplayEditAssigneesDialog(false);

    const [displayEditSummaryDialog, setDisplayEditSummaryDialog] = useState(false);
    const openEditSummaryDialog = () => setDisplayEditSummaryDialog(true);
    const closeEditSummaryDialog = () => setDisplayEditSummaryDialog(false);

    const [displayEditStatusDialog, setDisplayEditStatusDialog] = useState(false);
    const openEditStatusDialog = () => setDisplayEditStatusDialog(true);
    const closeEditStatusDialog = () => setDisplayEditStatusDialog(false);

    const [displayEditProgressDialog, setDisplayEditProgressDialog] = useState(false);
    const openEditProgressDialog = () => setDisplayEditProgressDialog(true);
    const closeEditProgressDialog = () => setDisplayEditProgressDialog(false);

    const [displayEditLabelDialog, setDisplayEditLabelDialog] = useState(false);
    const openEditLabelDialog = () => setDisplayEditLabelDialog(true);
    const closeEditLabelDialog = () => setDisplayEditLabelDialog(false);

    //Check if user is assigned to the bug on this page.
    function updateIfUserIsAssignee(permittedRoles, assignees) {
        if (permittedRoles != null && permittedRoles.length > 0) {
            if (assignees.filter(assignee => assignee.user.username === currentUser).length > 0) {
                setIsUserAssignee(true);
            } else {
                setIsUserAssignee(false);
            }
        }
    }

    useEffect(() => {
        if (bugDetailsLoading) {
            api.get(`/workspaces/${pathParams.id}/labels`)
            .then(({data}) => {
                setWorkspaceCustomLabels(data);
                let wcl = data;
                //Fetch bug report container
                api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}`)
                .then(({data}) => {
                    console.log(data);
                    setBugDetails(data);
                    setBugAssignees(data.assignees);
                    setBugSummary(data.summary);
                    setBugStatus(data.status);
                    setBugProgress(data.progress);
                    setBugLabels(data.labels);
                    let permittedRoles = getBugEditAssigneesPermittedRoles(data.user_workspace_role);
                    setCanEdit(permittedRoles.length > 0);
                    setEditAssigneesPermittedRoles(permittedRoles);
                    updateIfUserIsAssignee(permittedRoles, data.assignees);
                    setCollapsedActionsCollection([
                        {
                            name: "View Audit Log",
                            disabled: false,
                            icon: <HistoryIcon fontSize="small"/>,
                            method: () => navigate(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/audit-log`)
                        },
                        ...(permittedRoles != null && permittedRoles.length > 0 ? [
                            {
                                name: "Edit Progress",
                                disabled: false,
                                icon: <TimelineIcon fontSize="small"/>,
                                method: openEditProgressDialog
                            },
                            {
                                name: "Edit Status",
                                disabled: false,
                                icon: <FactCheckIcon fontSize="small"/>,
                                method: openEditStatusDialog
                            },
                            {
                                name: "Edit Assignees",
                                disabled: false,
                                icon: <PeopleIcon fontSize="small"/>,
                                method: openEditAssigneesDialog
                            },
                            {
                                name: "Edit Summary",
                                disabled: false,
                                icon: <TitleIcon fontSize="small"/>,
                                method: openEditSummaryDialog
                            },
                            ...(wcl.length > 0 ? [{
                                name: "Edit Labels",
                                disabled: false,
                                icon: <LabelsIcon fontSize="small"/>,
                                method: openEditLabelDialog
                            }] : [])
                        ] : [])
                    ])
                    setBugDetailsLoading(false);
                }).catch(error => console.log(error));
            }).catch(error => console.log(error));
        }
    }, []);

    /** Function to handle allowing the user to assign themselves to a bug. */
    function handleAssignSelf() {
        if (isAssignSelfLoading) return;
        setIsAssignSelfLoading(true);
        api.post(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/assignees/join`)
        .then(({data}) => {
            console.log(data);
            setBugAssignees(data);
            displaySnackbar("success", "Successfully Assigned Yourself", "You are now an assignee of this bug.");
            updateIfUserIsAssignee(editAssigneesPermittedRoles, data);
        }).catch(error => {
            console.log(error);
        }).finally(() => {
            setIsAssignSelfLoading(false);
        })
    }

    return (<>
        {bugDetailsLoading ? <>
            <PageHeader
                previous={`/workspaces/${pathParams.id}`}
                title={<Skeleton variant="rounded" width="200px"/>}
            />
        </> : <>
            <PageHeader
                previous={`/workspaces/${pathParams.id}`}
                title={
                    <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
                        <Chip
                            variant="soft-square"
                            strength="weak"
                            size="small"
                            label={"BUG ID: " + bugDetails.id}
                        />
                        <Typography 
                            variant="h5"
                            component={"span"}
                            sx={{
                                whiteSpace: "normal", 
                                wordBreak: "break-word"
                            }}>
                            {bugSummary}
                        </Typography>
                    </Box>
                }
                collapsedActions={collapsedActionsCollection}
            />
            
            <Box display="flex" gap={1.5} justifyContent="space-between" flexWrap="wrap">
                
                {/* Labels */}
                {workspaceCustomLabels.length > 0 && (bugLabels.length > 0 ? <>
                    <Box display="flex" gap={0.5} flexWrap="wrap" alignContent={"start"}>
                        {bugLabels.map((v, i) => (
                            <WorkspaceCustomLabelDisplay label={v} key={i}/>
                        ))}
                        {canEdit &&
                            <IconButton size="small" onClick={openEditLabelDialog} sx={{mt: "-2px"}}>
                                <EditIcon fontSize="inherit"/>
                            </IconButton>
                        }
                    </Box>
                </> : (canEdit &&
                    <Button
                        variant="outlined"
                        startIcon={<EditIcon />}
                        onClick={openEditLabelDialog}
                        fullWidth={isSmallScreen}
                    >
                        Add Labels
                    </Button>
                ))}

                {/* Assignees */}
                {(bugAssignees.length > 0 || canEdit) && 
                    <Button
                        fullWidth={isSmallScreen}
                        sx={{display:"flex", gap: 1, alignSelf: "end"}}
                        variant="outlined"
                        startIcon={<PeopleIcon sx={{mr: "-4px"}}/>}
                        onClick={e => (bugAssignees.length > 0 || canEdit) && handleShowBugAssigneesDetails(e.currentTarget)}
                    >
                        {(bugAssignees == null || bugAssignees.length === 0) ?
                            <Typography variant="subtitle">No Assignees</Typography>
                            : <>
                            <Typography variant="subtitle">Assignees</Typography>
                            <AvatarGroup 
                                spacing={"medium"} 
                                max={3}
                                slotProps={{surplus: {
                                    sx: {height: "30px", width: "30px"}
                                }}}
                            >
                                {bugAssignees.map((v, i) => (
                                    <UserAvatar
                                        size="26px"
                                        username={v.user.username}
                                        avatarColour={v.user.user_colour}
                                    />
                                ))}
                            </AvatarGroup>
                        </>}
                    </Button>
                }
            </Box>

            <Stack spacing={2} mt={bugLabels.length > 0 ? 0 : 1}>
                <div>
                    <Typography 
                        mb={0.5} 
                        mt={bugAssignees.length == 0 && !canEdit ? 1.5 : 0}
                        variant="h6"
                    >
                        Tracking
                    </Typography>
                    <BugTrackingOverview 
                        bugDetails={bugDetails} 
                        bugStatus={bugStatus} 
                        bugProgress={bugProgress} 
                        openEditBugStatus={openEditStatusDialog}
                        openEditBugProgress={openEditProgressDialog}
                        canEdit={canEdit}
                    />
                </div>

                <BugReportViewer/>
                
            </Stack>

            {/* Assignees Hover Card */}
            <PopoverHoverComponent
                anchorEl={bugAssigneesAnchorEl}
                open={Boolean(bugAssigneesAnchorEl)}
                onClose={handleHideBugAssigneesDetails}
                //Anchor menu to centre-right of element
                anchorOrigin={{horizontal: 'right', vertical: 'center'}}
                //Menu top-right meets anchor
                transformOrigin={{horizontal: 'right', vertical: 'top'}}
                popoverContent={<>
                    <BugAssigneesList bugAssignees={bugAssignees}/>
                    {canEdit && <>
                        <Divider sx={{my: 0.75}}/>
                        <Box display="flex" gap={0.25}>
                            {editAssigneesPermittedRoles != null && editAssigneesPermittedRoles.length > 0 && <>
                                <Button
                                    size="small"
                                    onClick={openEditAssigneesDialog}
                                    startIcon={<EditIcon/>}
                                >
                                    Edit Assignees
                                </Button>
                                {/* Show assign self if not already assigned. */}
                                {isUserAssignee != null && isUserAssignee == false &&
                                    <Button
                                        size="small"
                                        startIcon={<GroupAddIcon/>}
                                        onClick={handleAssignSelf} 
                                        loading={isAssignSelfLoading}
                                    >
                                        Assign Yourself
                                    </Button>
                                }
                            </>}
                        </Box>
                    </>}
                </>}
            />

            {/* Edit Assignees */}
            <EditAssigneesDialog 
                open={displayEditAssigneesDialog}
                onClose={closeEditAssigneesDialog}
                permittedRoles={editAssigneesPermittedRoles}
                setBugAssignees={setBugAssignees}
                updateIfUserIsAssignee={updateIfUserIsAssignee}
            />

            {/* Update Summary Dialog */}
            <EditBugSummaryDialog
                open={displayEditSummaryDialog}
                onClose={closeEditSummaryDialog}
                currentSummary={bugSummary}
                setBugSummary={setBugSummary}
            />

            {/* Update Status Dialog */}
            <EditBugStatusDialog
                open={displayEditStatusDialog}
                onClose={closeEditStatusDialog}
                currentStatus={bugStatus}
                setBugStatus={setBugStatus}
            />

            {/* Update Progress Dialog */}
            <EditBugProgressDialog
                open={displayEditProgressDialog}
                onClose={closeEditProgressDialog}
                currentProgress={bugProgress}
                setBugProgress={setBugProgress}
            />

            {/* Update Labels Dialog */}
            <EditBugLabelsDialog
                open={displayEditLabelDialog}
                onClose={closeEditLabelDialog}
                currentLabels={bugLabels}
                setBugLabels={setBugLabels}
                workspaceCustomLabels={workspaceCustomLabels}
            />

        </>}
    </>);
}