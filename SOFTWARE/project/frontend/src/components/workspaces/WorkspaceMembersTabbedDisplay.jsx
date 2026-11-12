import { Box, Card, Collapse, Tab, Tabs } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import TabLabelWithChip from "../common/TabLabelWithChip";
import WorkspaceMembersContent from "./WorkspaceMembersContent";

/**
 *  @returns Workspace members viewing component. 
 * Note: The display card becomes scrollable if it is too long.
 */
export default function WorkspaceMembersTabbedDisplay({
    workspaceMembers = [],
    setWorkspaceMembers,
    userRole,
    showConfirmationDialogs = false,
    removable = false,
    removeMethod = undefined,
    editable = false,
}) {

    //useMemo for efficiency, deriving counts.
    const workspaceMembersCount = useMemo(() => {
        return workspaceMembers.reduce((counts, member) => {
            counts[member.role] ++;
            return counts;
        }, { 
            //Initial counts
            "ADMIN": 0,
            "DEVELOPER": 0,
            "REPORTER": 0
        });
    }, [workspaceMembers]);

    const [workspaceMembersTab, setWorkspaceMembersTab] = useState("");
    useEffect(() => {
        //Ensure tab with 0 members is not selected when displaying workspace members.
        if ((userRole ? workspaceMembers.length -1 : workspaceMembers.length) === 0) {
            setWorkspaceMembersTab("");
            return;
        }
        if (!workspaceMembersTab) {
            setWorkspaceMembersTab(
                workspaceMembersCount.ADMIN !== 0 ? "Admins" :
                workspaceMembersCount.DEVELOPER !== 0 ? "Developers" :
                workspaceMembersCount.REPORTER !== 0 ? "Reporters" : "Admins"
            );
        }
    }, [workspaceMembers, workspaceMembersCount, workspaceMembersTab, userRole]);

    return(<>
        <Collapse in={!removable || (userRole ? workspaceMembers.length -1 : workspaceMembers.length) > 0}>
            <Box>
                <Tabs 
                    sx={{pl: 1}}
                    value={workspaceMembersTab || false} 
                    variant="scrollable"
                    scrollButtons={false}
                    onChange={(event, newValue) => {setWorkspaceMembersTab(newValue)}}
                >
                    <Tab 
                        value={"Admins"} 
                        disabled={workspaceMembersCount.ADMIN === 0}
                        label={<TabLabelWithChip 
                            tabLabel="Admins" 
                            hideChip={workspaceMembersCount.ADMIN === 0}
                            chipLabel={workspaceMembersCount.ADMIN}
                        />}
                    />
                    <Tab 
                        value={"Developers"} 
                        disabled={workspaceMembersCount.DEVELOPER === 0}
                        label={
                            <TabLabelWithChip tabLabel="Developers" 
                            hideChip={workspaceMembersCount.DEVELOPER === 0}
                            chipLabel={workspaceMembersCount.DEVELOPER}
                        />}
                    />
                    <Tab 
                        value={"Reporters"} 
                        disabled={workspaceMembersCount.REPORTER === 0}
                        label={<TabLabelWithChip 
                            tabLabel="Reporters" 
                            hideChip={workspaceMembersCount.REPORTER === 0}
                            chipLabel={workspaceMembersCount.REPORTER}
                        />}
                    />
                </Tabs>
                <Card variant="outlined" sx={{
                    overflowY: "auto", 
                    maxHeight:"50vh", 
                }}>
                    <Box padding="10px" display="flex" gap="5px" flexWrap="wrap">
                        <WorkspaceMembersContent 
                            roleFilter={
                                workspaceMembersTab === "Admins" ? "ADMIN" : 
                                workspaceMembersTab === "Developers" ? "DEVELOPER" :
                                workspaceMembersTab === "Reporters" ? "REPORTER" : "ADMIN"
                            }
                            showConfirmationDialogs={showConfirmationDialogs}
                            workspaceMembers={workspaceMembers}
                            setWorkspaceMembers={setWorkspaceMembers}
                            userRole={userRole}
                            {...(removable && removeMethod && {
                                removable: removable,
                                removeMethod: removeMethod
                            })}
                            editable={editable}
                        />
                    </Box>
                </Card>
            </Box>
        </Collapse>
    </>);
    
}