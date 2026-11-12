import { useEffect, useState } from 'react'
import { useNavigate } from "react-router-dom";
import { Alert, Grid, Skeleton } from "@mui/material";

import api from '../../api/axios';
import PageHeader from '../../components/common/PageHeader';
import WorkspaceOverview from '../../components/workspaces/WorkspaceOverview';

export default function WorkspacesPage() {

    const navigate = useNavigate();

    const [ownedWorkspaces, setOwnedWorkspaces] = useState([]);
    const [memberWorkspaces, setMemberWorkspaces] = useState([]);
    const [ownedWorkspacesLoading, setOwnedWorkspacesLoading] = useState(true);
    const [memberWorkspacesLoading, setMemberWorkspacesLoading] = useState(true);

    const [canCreateWorkspace, setCanCreateWorkspace] = useState({
        can: false,
        loading: true
    });

    //Runs after component mounts only
    useEffect(() => {
        //Fetching workspace ownerships
        let startTime = performance.now();
        setOwnedWorkspacesLoading(true);
        api.get("/workspaces/owned")
            .then( ({data}) => {
                let endTime = performance.now();
                console.log(`Fetching owned workspaces took: ${endTime - startTime} ms.`);
                setOwnedWorkspaces(data);
                setOwnedWorkspacesLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    useEffect(() => {
        //Fetching workspace memberships
        let startTime = performance.now()
        setMemberWorkspacesLoading(true);
        api.get("/workspaces/member")
        .then( ({data}) => {
            let endTime = performance.now();
            console.log(`Fetch member workspaces took: ${endTime - startTime} ms.`);
            setMemberWorkspaces(data);
            setMemberWorkspacesLoading(false);
        })
        .catch(error => console.log(error));
    }, []);

    useEffect(() => {
        //Fetching can create new workspace permissions.
        let startTime = performance.now()
        setCanCreateWorkspace({
            can: false,
            loading: true
        });
        api.get("/workspaces/create/access")
        .then( ({data}) => {
            let endTime = performance.now();
            console.log(`Fetch can create workspace permission took: ${endTime - startTime} ms.`);
            setCanCreateWorkspace({
                can: data,
                loading: false
            })
        })
        .catch(error => console.log(error));
    }, []);

    return (<>

        {/* Owned Workspaces display */}
        <PageHeader 
            title="Your Workspaces" 
            info="These are the workspaces where you are the owner. You could own up to 5 workspaces if you wanted to."
            mainActionName="New Workspace"
            mainActionDisabledState={!canCreateWorkspace.can}
            mainActionLoadingState={canCreateWorkspace.loading}
            mainActionMethod={() => navigate("/workspaces/create")}
            mainActionDisabledTooltip="You can only own up to 5 workspaces."
        />
        <WorkspacesCollection 
            workspaces={ownedWorkspaces} 
            loading={ownedWorkspacesLoading}
            emptyMessage="You have not created any workspaces yet."
        />
        <br/>

        {/* Member workspaces display */}
        <PageHeader title="Other Workspaces" />
        <WorkspacesCollection 
            workspaces={memberWorkspaces} 
            loading={memberWorkspacesLoading}
            emptyMessage="You have not been added to any workspaces yet."
        />
        <br/>
    </>);
}

function WorkspacesCollection({workspaces = null, emptyMessage, loading}) {
    
    const WORKSPACE_OVERVIEW_HEIGHT = "125px";
    const SKELECTON_COUNT = 1;
    
    return (<>
        { (loading) ?
            <Grid container spacing={2}> {
                Array.from(new Array(SKELECTON_COUNT)).map((_, i) => (
                    <Grid size={{xs: 6, sm: 4, md: 3}} key={i}>
                        <Skeleton variant="rounded" height={WORKSPACE_OVERVIEW_HEIGHT}/>
                    </Grid>
                ))
            } </Grid>
        : (workspaces && workspaces.length > 0) ?
            <Grid container spacing={2}> {
                workspaces.map((w, i) => (
                    <Grid size={{xs: 6, sm: 4, md: 3}} key={w.id}>
                        <WorkspaceOverview 
                            workspace={w} 
                            height={WORKSPACE_OVERVIEW_HEIGHT} 
                        />
                    </Grid>
                ))
            } </Grid>
        :
            <Alert severity="info" color="primary">
                {emptyMessage}
            </Alert>
        }
    </>);
}