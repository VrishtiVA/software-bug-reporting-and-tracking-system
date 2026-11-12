import { useParams } from "react-router-dom";
import PageHeader from "../../components/common/PageHeader";
import { useEffect, useState } from "react";
import api from "../../api/axios";
import { Button, Chip, Skeleton, Typography, useMediaQuery } from "@mui/material";
import BugAuditLogTimeline from "../../components/bugs/BugAuditLogTimeline";
import theme from "../../styles/theme";

export default function BugAuditLogPage({}) {

    const pathParams = useParams();
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));

    const [pageSize, setPageSize] = useState(15);
    const [auditLogsLoading, setAuditLogsLoading] = useState(true);
    const [moreAuditLogsLoading, setMoreAuditLogsLoading] = useState(false);
    const [auditLogsData, setAuditLogsData] = useState(null);
    const [totalAuditLogs, setTotalAuditLogs] = useState(0);
    const [totalAuditLogsDisplayed, setTotalAuditLogsDisplayed] = useState(0);
    const [moreAuditLogs, setMoreAuditLogs] = useState(false);
    const [currentAuditLogsPage, setCurrentAuditLogsPage] = useState(0);

    useEffect(() => {
        if (auditLogsLoading) {
            //Load initial page first.
            api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/audit-log`, {params: {
                size: pageSize
            }})
            .then(({data}) => {
                console.log(data);
                setAuditLogsData(data.content);
                setTotalAuditLogs(data.total_elements);
                setMoreAuditLogs(!data.is_last);
                setTotalAuditLogsDisplayed(data.content.length);
                setCurrentAuditLogsPage(data.page_number);
            })
            .catch(error => console.log(error))
            .finally(() => setAuditLogsLoading(false));
        }
    }, [])

    /**
     * Function to load more audit log pages.
     */
    async function loadMoreAuditLogs() {
        if (auditLogsLoading || !moreAuditLogs) return;
        setMoreAuditLogsLoading(true);
        api.get(`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}/audit-log`, {params: {
            size: pageSize,
            page: currentAuditLogsPage + 1
        }})
        .then(({data}) => {
            console.log(data);
            setMoreAuditLogs(!data.is_last);
            setCurrentAuditLogsPage(data.page_number);
            //Add audit logs to list, ensuring id is unique by filtering out those already present.
            //This adds extra protection to not multi-add the same log to the view.
            setAuditLogsData(prev => {
                let currentAuditLogIds = new Set(prev.map(log => log.id));
                let loadedAuditLogs = (data.content).filter(newLog => !currentAuditLogIds.has(newLog.id));
                setTotalAuditLogsDisplayed(prev.length + loadedAuditLogs.length);
                return [...prev, ...loadedAuditLogs];
            });
        })
        .catch(error => console.log(error))
        .finally(() => setMoreAuditLogsLoading(false));
    }
    
    return (<>
        <PageHeader
            title={"Bug Audit Log"}
            previous={`/workspaces/${pathParams.id}/bugs/${pathParams.bug_id}`}
            showDataInfo={true}
            dataInfo={
                <Typography variant="subtitle2" display="flex" alignItems="center" gap={0.5} flexWrap="no-wrap">
                    Displaying
                    <Chip size="small" variant="id" label={totalAuditLogsDisplayed+" of "+totalAuditLogs}/>
                    Logs
                </Typography>
            }
        />
        {auditLogsLoading ?
            <Skeleton/>
        : (totalAuditLogs === 0 || auditLogsData === null || auditLogsData.length <= 0) ? 
            "There are no audit logs to see yet"
        : <>
            <BugAuditLogTimeline 
                auditLogsData={auditLogsData}
                totalAuditLogs={totalAuditLogsDisplayed}
            />
            {moreAuditLogs && 
                <Button
                    onClick={loadMoreAuditLogs}
                    loading={moreAuditLogsLoading}
                    variant="outlined"
                    fullWidth={isSmallScreen}
                    sx={{mt: isSmallScreen ? 1.5 : 0}}
                    size="small"
                >
                    Show More
                </Button>
            }
        </>}
    </>)

}