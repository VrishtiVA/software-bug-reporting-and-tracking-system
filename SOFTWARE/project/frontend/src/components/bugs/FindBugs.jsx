import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { useParams } from "react-router-dom";
import dayjs from "dayjs";
import relativeTime from 'dayjs/plugin/relativeTime'
import { Alert, Box, Button, IconButton, InputAdornment, MenuItem, Pagination, Select, Skeleton, styled, TextField, Typography, useMediaQuery } from "@mui/material";
import { BugSortOptions, ProgressOptions, StatusOptions } from "../../api/options";
import SearchIcon from '@mui/icons-material/SearchRounded';
import ContainedIconButton from "../common/ContainedIconButton";
import TuneIcon from '@mui/icons-material/TuneRounded';
import theme from "../../styles/theme";
import BugFilterMenu from "./BugFilterMenu";
import { debounce } from "../../utilities/functionHandlers";
import BugsTable from "./BugsTable";
import { SortAscendingIcon, SortDescendingIcon } from "../common/SortIcons";
import { DefaultButton } from "../common/DefaultButton";

function FindBugs({
    workspaceDetails
}) {

    const pathParams = useParams();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));
    dayjs.extend(relativeTime);

    const [bugs, setBugs] = useState({});
    const [tableLoading, setTableLoading] = useState(true);

    const filterGroups = [
        ["Progress", ProgressOptions],
        ["Status", StatusOptions],
        ["Labels", workspaceDetails.custom_workspace_labels]
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
        status: new Set([]),
        labels: new Set([])
    });
    const [search, setSearch] = useState("");
    const [sort, setSort] = useState("priority");
    const [isDescending, setIsDescending] = useState(true);

    const [bugsLoading, setBugsLoading] = useState(true);
    const [pageSize, setPageSize] = useState(15);
    const [totalResults, setTotalResults] = useState(3); //3 Initially for skeletons
    const [isLastPage, setIsLastPage] = useState(false);
    const [currentPage, setCurrentPage] = useState(false);
    const [totalPages, setTotalPages] = useState(0);

    /** Function to clear search filters. */
    function handleClearSearch() {
        setSearch("");
        setSelectedFilters({
            progress: new Set([]),
            status: new Set([]),
            labels: new Set([])
        })
        debouncedHandleSearch(true);
    }

    useEffect(() => {
        //Initial load of bugs.
        handleSearch();
    }, []);

    /** Function to update sort */
    function handleUpdateSort(v) {
        setSort(() => {
            //If sort is relevance, keep direction descending.
            let direction = v === "relevance";
            setIsDescending(direction);
            debouncedHandleSearch(false, v, direction);
            return v;
        });
    }

    /** Function to update sort direction itself. */
    function toggleSortDirection() {
        setIsDescending(prev => {
            //If sort is relevance, keep direction descending, otherwise allow toggle. 
            let direction = sort === "relevance" || !prev;
            if (sort != "relevance") debouncedHandleSearch(false, sort, direction);
            return direction;
        });
    }

    /**
     * Function to handle sending a search request.
     * Parameters are provided to allow sending conditions directly, since states may not update immediately.
     * @param {Boolean} clean if search filters were reset just now.
     * @param {String} sortBy the sort direction.
     * @param {Boolean} sortDirection is descending (true) or not (false).
     */
    function handleSearch(clean=false, sortBy=sort, sortDirection=isDescending) {
        setBugsLoading(true);
        let startTime = performance.now();
        api.post(`/workspaces/${pathParams.id}/bugs`,
            {
                progress: clean ? [] : [...selectedFilters.progress],
                status: clean ? [] : [...selectedFilters.status],
                labels: clean ? [] : [...selectedFilters.labels]
            }, 
            {
                params: {
                    query: clean ? "" : search,
                    sort: sortBy,
                    descending: sortDirection,
                    size: pageSize
                }
            }
        ).then(({data}) => {
            let endTime = performance.now();
            console.log(`Fetching bugs took: ${endTime - startTime} ms.`);
            setTableLoading(false);
            setBugs(data.content);
            setTotalResults(data.total_elements);
            setIsLastPage(!data.is_last);
            setCurrentPage(data.page_number);
            setTotalPages(data.total_pages);
            setBugsLoading(false);
        }).catch(error => console.log(error));
    }

    /**
     * Function to handle sending a search request for additional pages.
     * Parameters are provided to allow sending conditions directly, since states may not update immediately.
     */
    function handleSearchForPages(page, size=pageSize) {
        if (page == currentPage) return;
        setBugsLoading(true);
        api.post(`/workspaces/${pathParams.id}/bugs`,
            {
                progress: [...selectedFilters.progress],
                status: [...selectedFilters.status],
                labels: [...selectedFilters.labels]
            }, 
            {
                params: {
                    query: search,
                    sort: sort,
                    descending: isDescending,
                    size: size,
                    page: page
                }
            }
        ).then(({data}) => {
            console.log(data);
            setBugs(data.content);
            setIsLastPage(!data.is_last);
            setCurrentPage(data.page_number);
            setTotalPages(data.total_pages);
            setBugsLoading(false);
        }).catch(error => console.log(error));
    }

    /**
     * Function to handle keyboard support for search.
     * Search on enter key press.
     */
    function handleSearchOnEnterKey(e) {
        if (e.key === "Enter") debouncedHandleSearch();
    }

    /**
     * Debounce searches by 200ms, to prevent overwhelming the backend with requests.
     */
    const debouncedHandleSearch = debounce((clean=false, sortBy=sort, sortDirection=isDescending) => handleSearch(clean, sortBy, sortDirection), 200);
    const debouncedHandleSearchForPages = debounce((pageRequest, pageSize) => handleSearchForPages(pageRequest, pageSize), 200);

    return (<>
        {tableLoading ? 
            <Skeleton/>
        : <>
            {/* Searching, sorting and filtering */}
            <Box mb={1.5} display="flex" alignItems="center" gap={1} flexWrap={isSmallScreen ? "wrap" : "nowrap"}>
                <TextField
                    fullWidth
                    placeholder="Search..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    onKeyDown={handleSearchOnEnterKey}
                    slotProps={{input: {endAdornment: 
                        <InputAdornment>
                            <IconButton 
                                edge="end"
                                onClick={debouncedHandleSearch}
                            >
                                <SearchIcon/>
                            </IconButton>
                        </InputAdornment>
                    }}}
                    sx={{m: 0}}
                />
                <Box display="flex">
                    <Select 
                        value={sort}
                        onChange={e => handleUpdateSort(e.target.value)}
                        sx={{
                            borderTopRightRadius: "0",
                            borderBottomRightRadius: "0",
                        }}
                    >
                        {Object.entries(BugSortOptions).map(([k, v]) => (
                            <MenuItem value={k} dense>
                                {v}
                            </MenuItem>
                        ))}
                    </Select>
                    <ContainedIconButton 
                        noBorderLeft
                        icon={isDescending ? <SortDescendingIcon/> : <SortAscendingIcon/>}
                        onClick={toggleSortDirection}
                        colourGrey={true}
                    />
                </Box>
                <DefaultButton 
                    variant="outlined" 
                    endIcon={<TuneIcon/>} 
                    onClick={openFilterMenu}
                >
                    Filter
                </DefaultButton>
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
                    {...(
                        !(search === "" && 
                        selectedFilters.progress.size === 0 && 
                        selectedFilters.status.size === 0 && 
                        selectedFilters.labels.size === 0) ? {
                            action: 
                                <Button 
                                    size="small" 
                                    color="primary" 
                                    variant="contained"
                                    onClick={handleClearSearch}
                                >
                                    Clear Search
                                </Button>
                        } 
                    : {})}
                >
                    No bug results were found.
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

export default React.memo(FindBugs);