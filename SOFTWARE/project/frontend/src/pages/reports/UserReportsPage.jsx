import { useEffect, useState } from "react";
import api from "../../api/axios";
import PageHeader from "../../components/common/PageHeader";
import ReportsTable from "../../components/reports/ReportsTable";
import { debounce } from "../../utilities/functionHandlers";
import { Alert, Box, Chip, MenuItem, Pagination, Select, Typography } from "@mui/material";

export default function UserReportsPage({}) {

    const [tableLoading, setTableLoading] = useState(true);
    const [pageSize, setPageSize] = useState(15);
    const [isCreationTimestampSortDescending, setIsCreationTimestampSortDescending] = useState(true);
    const [reportsLoading, setReportsLoading] = useState(true);
    const [reports, setReports] = useState(null);
    const [totalResults, setTotalResults] = useState(3); //3 initially for skeletons.
    const [isLastPage, setIsLastPage] = useState(false);
    const [currentPage, setCurrentPage] = useState(false);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        handleSearch();
    }, []);

    /** Function used to handle initial fetch for reports. */
    function handleSearch(descending=isCreationTimestampSortDescending) {
        setReportsLoading(true);
        api.get("/reports", {params: {
            size: pageSize,
            creation_timestamp_descending: descending
        }}).then(({data}) => {
            console.log(data);
            setTableLoading(false);
            setReports(data.content);
            setTotalResults(data.total_elements);
            setIsLastPage(!data.is_last);
            setCurrentPage(data.page_number);
            setTotalPages(data.total_pages);
            setReportsLoading(false);
        }).catch(error => console.log(error));
    }

    /** Function used to handle fetch for more report pages. */
    function handleSearchForPages(page, size=pageSize) {
        if (page == currentPage) return;
        setReportsLoading(true);
        api.get("/reports", {params: {
            size: size,
            page: page,
            creation_timestamp_descending: isCreationTimestampSortDescending
        }}).then(({data}) => {
            console.log(data);
            setReports(data.content);
            setIsLastPage(!data.is_last);
            setCurrentPage(data.page_number);
            setTotalPages(data.total_pages);
            setReportsLoading(false);
        }).catch(error => console.log(error));
    }
    const debouncedHandleSearch = debounce((descending=true) => handleSearch(descending), 200);
    const debouncedHandleSearchForPages = debounce((pageRequest, pageSize) => handleSearchForPages(pageRequest, pageSize), 200);

    function handleToggleCreationTimestampSortDirection() {
        setIsCreationTimestampSortDescending(prev => {
            debouncedHandleSearch(!prev);
            return !prev;
        });
    }

    return (<>
        <PageHeader title={"My Reports"}
            showDataInfo={true}
            dataInfo={
                <Typography variant="subtitle2" display="flex" alignItems="center" gap={0.5} flexWrap="no-wrap">
                    Total
                    <Chip size="small" variant="id" label={totalResults}/>
                    Reports
                </Typography>
            }
        />

        <ReportsTable
            totalResults={totalResults}
            reportsLoading={reportsLoading}
            reports={reports}
            isCreationTimestampSortDescending={isCreationTimestampSortDescending}
            handleToggleCreationTimestampSortDirection={handleToggleCreationTimestampSortDirection}
        />
        
        {/* No results then notice and reset search options option. */}
        {!tableLoading && !reportsLoading && totalResults === 0 && 
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
            >
                You haven't reported any bugs yet.
            </Alert>
        }

        {/* Pagination toolbar */}
        <Box mt={1.5} display="flex" justifyContent="space-between" alignItems="center">
            
            {/* Adjust reports per page */}
            {(totalResults > 15) && 
                <Box display="flex" alignItems="center" gap={1}>
                    <Typography variant="body2">Reports Per Page</Typography>
                    <Select
                        value={pageSize}
                        onChange={(e) => {
                            const newSize = parseInt(e.target.value);
                            setPageSize(newSize);
                            debouncedHandleSearchForPages(0, newSize); //Reset to page 0 on size change.
                        }}
                    >
                        {[15, 30, 50, 75, 100].map((v, i) => (
                            <MenuItem dense value={v} key={i}>{v}</MenuItem>
                        ))}
                    </Select>
                </Box>
            }
            
            {/* Select report page */}
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
    </>);
}