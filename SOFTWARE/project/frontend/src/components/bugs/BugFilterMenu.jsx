import { Box, Checkbox, Divider, ListSubheader, Menu, MenuItem } from "@mui/material";
import StatusDot from "../common/StatusDot";
import { ProgressColours, StatusColours } from "../../api/colours";
import WorkspaceCustomLabelDisplay from "../workspaces/WorkspaceCustomLabelDisplay";
import InfoTooltip from "../common/InfoTooltip";
import { FilterOptionTooltips } from "../../api/options";

export default function BugFilterMenu({
    filterMenuAnchorEl,
    openFilterMenu,
    closeFilterMenu,
    filterGroups,
    selectedFilters,
    setSelectedFilters,
    filterDirtyMarker,
    setFilterDirtyMarker
}) {

    /**
     * Method to update selected filters by toggling it in the specific set and then updating.
     * @param {String} group The filter group to update.
     * @param {String} filter The filter to toggle.
     */
    function handleAdjustFilters(group, filter) {
        setSelectedFilters(prev => {
            let updatedGroup = new Set(prev[group]); //Copy set to avoid mutability issues.
            if (updatedGroup.has(filter)) updatedGroup.delete(filter);
            else updatedGroup.add(filter);
            return {
                ...prev,
                [group]: updatedGroup
            }
        })
        //Mark if filters was adjusted, so that parent knows.
        if (!filterDirtyMarker) setFilterDirtyMarker(true);
    }

    return (<>
        <Menu
            anchorEl={filterMenuAnchorEl}
            open={Boolean(filterMenuAnchorEl)}
            onClose={closeFilterMenu}
            //Anchor menu to bottom-right of icon
            anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
            //Menu top-right meets anchor
            transformOrigin={{horizontal: 'right', vertical: 'top'}}
            slotProps={{
                paper: {
                    sx: {
                        mt: 0.5,
                        ml: -0.5
                    }
                }
            }}
            sx={{
                maxHeight: "70vh"
            }}
        >
            {
                //For each group
                filterGroups.map((v, i) => (<div key={i}>
                    <ListSubheader sx={{lineHeight: "25px"}}>
                        <Box display="flex" alignItems="center" gap={0.25}>
                            {v[0]}
                            <InfoTooltip title={FilterOptionTooltips[v[0]]}/>
                        </Box>
                    </ListSubheader>
                    { v[0] == "Progress" ? 
                        Object.entries(v[1]).map(([x, t]) => (
                            <MenuItem 
                                key={x} 
                                dense
                                onClick={() => handleAdjustFilters("progress", String(x))}
                            >
                                <Checkbox 
                                    edge="start" 
                                    value={t} 
                                    size="small"
                                    checked={selectedFilters.progress.has(String(x))}
                                    disableRipple 
                                    sx={{py: 0.5}}
                                />
                                <StatusDot colour={ProgressColours[x]} size="10px"/>
                                {t}
                            </MenuItem>
                        ))
                    : v[0] == "Status" ?
                        Object.entries(v[1]).map(([x, t]) => (
                            <MenuItem 
                                key={x} 
                                dense
                                onClick={() => handleAdjustFilters("status", String(x))}
                            >
                                <Checkbox 
                                    edge="start" 
                                    value={t} 
                                    size="small" 
                                    disableRipple 
                                    checked={selectedFilters.status?.has(String(x))}
                                    sx={{py: 0.5}}
                                />
                                <StatusDot colour={StatusColours[x]} size="10px"/>
                                {t}
                            </MenuItem>
                        ))
                    : v[0] == "Labels" ?
                        v[1].map((label, _) => (
                            <MenuItem 
                                key={label.name}
                                dense
                                onClick={() => handleAdjustFilters("labels", String(label.name))}
                            >
                                <Checkbox 
                                    edge="start" 
                                    value={label.name} 
                                    size="small" 
                                    disableRipple 
                                    sx={{py: 0.5}}
                                    checked={selectedFilters.labels?.has(String(label.name))}
                                />
                                <WorkspaceCustomLabelDisplay label={label}/>
                            </MenuItem>
                        ))
                    : "" } 
                    <Divider/>
                </div>))
            }
        </Menu>
    </>);
}