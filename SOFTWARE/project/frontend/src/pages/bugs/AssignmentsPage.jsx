import { useParams } from "react-router-dom";
import PageHeader from "../../components/common/PageHeader";
import { useEffect, useState } from "react";
import { ProgressOptions } from "../../api/options";
import api from "../../api/axios";
import { Alert, Box, Button, Chip, MenuItem, Pagination, Select, Skeleton, Tooltip, Typography, useMediaQuery } from "@mui/material";
import TuneIcon from '@mui/icons-material/TuneRounded';
import BugFilterMenu from "../../components/bugs/BugFilterMenu";
import BugsTable from "../../components/bugs/BugsTable";
import theme from "../../styles/theme";
import { debounce } from "../../utilities/functionHandlers";
import UserAvatar from "../../components/users/UserAvatar";
import { toCamelCase } from "../../utilities/stringHandlers";
import { DefaultButton } from "../../components/common/DefaultButton";

export default function AssignmentsPage({}) {

    const pathParams = useParams();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));

    const [bugs, setBugs] = useState({});
    const [tableLoading, setTableLoading] = useState(true);

    const [bugsLoading, setBugsLoading] = useState(true);
    const [pageSize, setPageSize] = useState(15);
    const [totalResults, setTotalResults] = useState(3); //3 initially for skeletons.
    const [isLastPage, setIsLastPage] = useState(false);
    const [currentPage, setCurrentPage] = useState(false);
    const [totalPages, setTotalPages] = useState(0);

    const [userId, setUserId] = useState(null);
    const [workspaceOptions, setWorkspaceOptions] = useState([]);
    const [selectedWorkspace, setSelectedWorkspace] = useState(pathParams.id ? pathParams.id : "All");
    const [memberOptions, setMemberOptions] = useState([]);
    const [selectedMember, setSelectedMember] = useState(undefined);

    const filterGroups = [
        ["Progress", ProgressOptions]
    ];
    const [filterMenuAnchorEl, setFilterMenuAnchorEl] = useState(false); 
    const [filterDirtyMarker, setFilterDirtyMarker] = useState(false);
    const openFilterMenu = (event) => setFilterMenuAnchorEl(event.currentTarget);
    const closeFilterMenu = () => {
        setFilterMenuAnchorEl(null);
        if (filterDirtyMarker) debouncedHandleSearch();
    };
    const [selectedFilters, setSelectedFilters] = useState({
        //Hiding closed bugs by default.
        progress: new Set(["OPEN", "IN_PROGRESS", "IN_TESTING", "IN_REVIEW"]),
    });

    useEffect(() => {
        //Get workspace options for current user.
        api.get(`/workspaces`)
        .then(({data}) => {
            setWorkspaceOptions(data.workspaces ?? []);
            setUserId(data.user_id);
            setSelectedMember(data.user_id);
            handleSearch();
        }).catch(error => console.log(error));
    }, []);

    /**
     * Function to handle searching for first page of assignments.
     * Variables allowed to allow search with up to date items.
     * @param {*} clean If search filters were recently cleared.
     * @param {*} workspace_id The workspace id.
     * @param {*} user_id The member to find assignments for.
     */
    function handleSearch(clean = false, workspace_id=null, user_id=null) {
        setBugsLoading(true);
        api.post(`/bugs/assignments`, 
            { progress: clean ? [] : [...selectedFilters.progress] },
            { params: {
                size: pageSize,
                workspace_id: workspace_id ? (workspace_id === "All" ? null : workspace_id) : selectedWorkspace === "All" ? null : selectedWorkspace,
                user_id: user_id ? user_id : selectedMember
            }}
        ).then(({data}) => {
            setTableLoading(false);
            setBugs(data.assigned_bugs.content);
            setTotalResults(data.assigned_bugs.total_elements);
            setIsLastPage(!data.assigned_bugs.is_last);
            setCurrentPage(data.assigned_bugs.page_number);
            setTotalPages(data.assigned_bugs.total_pages);
            setMemberOptions(data.workspace_assignable_members);
            setBugsLoading(false);
        }).catch(error => console.log(error));
    }

    /**
     * Function to handle searching for more assignment pages.
     * @param {*} page The page number.
     * @param {*} size The page size.
     */
    function handleSearchForPages(page, size=pageSize) {
        if (page == currentPage) return;
        setBugsLoading(true);
        api.post(`/bugs/assignments`, 
            { progress: [...selectedFilters.progress] },
            { params: {
                size: size,
                page: page,
                workspace_id: selectedWorkspace === "All" ? null : selectedWorkspace,
                user_id: selectedMember
            }}
        ).then(({data}) => {
            console.log(data);
            setBugs(data.assigned_bugs.content);
            setIsLastPage(!data.assigned_bugs.is_last);
            setCurrentPage(data.assigned_bugs.page_number);
            setTotalPages(data.assigned_bugs.total_pages);
            setBugsLoading(false);
        }).catch(error => console.log(error));
    }

    //Debouncing search requests to protect the backend.
    const debouncedHandleSearch = debounce((clean=false, workspace_id=null, user_id=null) => handleSearch(clean, workspace_id, user_id), 200);
    const debouncedHandleSearchForPages = debounce((pageRequest, pageSize) => handleSearchForPages(pageRequest, pageSize), 200);

    //Function to clear search filters, triggering search again.
    function handleClearSearch() {
        setSelectedFilters({
            progress: new Set([]),
        });
        debouncedHandleSearch(true);
    }

    //Function to select a workspace filter.
    function handleSelectWorkspace(selectedId) {
        setSelectedWorkspace(selectedId);
        setSelectedMember(userId);
        debouncedHandleSearch(false, selectedId, userId);
    }

    //Function to select member to find assignments for.
    function handleSelectMember(selectedId) {
        setSelectedMember(selectedId);
        debouncedHandleSearch(false, null, selectedId);
    }

    return (<>

        {/* 
        - if came from a workspace (seen through link if pathparams.id), allow go back, otherwise no previous
        - view allocations via workspace will auto-select workspace ();
        */}
        <PageHeader
            previous={pathParams.id ? `/workspaces/${pathParams.id}` : null}
            title={"Bug Assignments"}
        />

        {tableLoading ? 
            <Skeleton/>
        : <>
            {/* Sorting and filtering */}
            <Box mb={1.5} display="flex" alignItems="center" justifyContent={"space-between"} gap={1} flexWrap={isSmallScreen ? "wrap" : "nowrap"}>
                <Box display="flex" alignItems="center" gap={1.5}>
                    <Select
                        value={selectedWorkspace}
                        onChange={e => handleSelectWorkspace(e.target.value)}
                    >
                        <MenuItem value={"All"} dense>
                            All Workspaces
                        </MenuItem>
                        {workspaceOptions.map((v, _) => (
                            <MenuItem value={v.id} dense>
                                {v.name}
                            </MenuItem>
                        ))}
                    </Select>
                    {totalResults > 0 && 
                        <Box display="flex" alignItems="center" gap={0.75}>
                            <Typography variant="body2">Total Results </Typography>
                            <Tooltip title={`${totalResults} bug ${totalResults === 1 ? "assignment was" : "assignments were"} found`}>
                                <Chip 
                                    size="small"
                                    label={totalResults}
                                />
                            </Tooltip>
                        </Box>
                    }
                </Box>
                <Box display="flex" alignItems="center" gap={1} flexWrap={"wrap"}>
                    {selectedWorkspace != "All" && memberOptions?.length > 0 &&
                        <Box display="flex">
                            <Select
                                value={selectedMember}
                                onChange={e => handleSelectMember(e.target.value)}
                                sx={{
                                    "& .MuiSelect-select .hide-on-selection": {
                                        display: "none"
                                    }
                                }}
                            >
                                {memberOptions?.map((v, _) => (
                                    <MenuItem value={v.user_id} dense>
                                        <Box display="flex" width="100%" justifyContent="space-between" gap={1} alignItems="center">
                                            <Box display="flex" alignItems="center" gap={1.25}>
                                                <UserAvatar username={v.username} avatarColour={v.user_colour} size="24px" />
                                                <Box display="flex" gap={0.75} alignItems={"center"}>
                                                    <Typography variant="body2">{v.username}</Typography>
                                                    <Chip size="small" variant="role" label={toCamelCase(v.role)} />
                                                </Box>
                                            </Box>
                                            <Chip
                                                label={v.assignment_count}
                                                className="hide-on-selection"
                                                variant="id"
                                                size="small"
                                                color="primary"
                                            />
                                        </Box>
                                    </MenuItem>
                                ))}
                            </Select>
                        </Box>
                    }
                    <DefaultButton
                        variant="outlined" 
                        endIcon={<TuneIcon/>} 
                        onClick={openFilterMenu}
                    >
                        Filter
                    </DefaultButton>
                </Box>
            </Box>

            {/* Filter Menu */}
            <BugFilterMenu
                filterMenuAnchorEl={filterMenuAnchorEl}
                openFilterMenu={openFilterMenu}
                closeFilterMenu={closeFilterMenu}
                filterGroups={filterGroups}
                selectedFilters={selectedFilters}
                setSelectedFilters={setSelectedFilters}
                filterDirtyMarker={filterDirtyMarker}
                setFilterDirtyMarker={setFilterDirtyMarker}
            />

            <BugsTable
                totalResults={totalResults} 
                bugsLoading={bugsLoading} 
                bugs={bugs}
                showWorkspaces={selectedWorkspace === "All"}
            />

            {/* No results then notice and reset search options option. */}
            {!tableLoading && !bugsLoading && totalResults === 0 && 
                <Alert
                    color="primary"
                    severity="info"
                    sx={{
                        mt: 0.5,
                        display: "flex",
                        alignItems: "center",
                        "& .MuiAlert-action": {
                            pt: 0
                        }
                    }}
                    {...(!(selectedFilters.progress.size === 0) ? {
                        action: 
                            <Button 
                                size="small" 
                                color="primary" 
                                variant="contained"
                                onClick={handleClearSearch}
                            >
                                Clear Filters
                            </Button>
                    } : {})}
                >
                    No assigned bugs were found.
                </Alert>
            }

            {/* Pagination toolbar */}
            <Box mt={1.5} display="flex" justifyContent="space-between" alignItems="center">
                
                {/* Adjust bugs per page */}
                {(totalResults > 15) && 
                    <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="body2">Bugs Per Page</Typography>
                        <Select
                            value={pageSize}
                            onChange={(e) => {
                                const newSize = parseInt(e.target.value);
                                setPageSize(newSize);
                                debouncedHandleSearchForPages(0, newSize);
                            }}
                        >
                            {[15, 30, 50, 75, 100].map((v, i) => (
                                <MenuItem dense value={v} key={i}>{v}</MenuItem>
                            ))}
                        </Select>
                    </Box>
                }
                
                {/* Select bug page */}
                {totalResults > pageSize && 
                    <Pagination
                        color="primary"
                        count={totalPages}
                        siblingCount={0}
                        boundaryCount={0}
                        page={currentPage + 1} //To handle indexing +1, since backend pages are 0-indexed.
                        onChange={(e, newPage) => {
                            debouncedHandleSearchForPages(newPage - 1);
                        }}
                        showFirstButton 
                        showLastButton
                    />
                }
            </Box>
        </>}

    </>);
}