import { useState } from "react";
import { Divider, Typography, Box, Tooltip, IconButton, Button, Icon, ToggleButtonGroup, ToggleButton } from "@mui/material";

import InfoOutlineIcon from '@mui/icons-material/InfoOutlineRounded';
import ArrowBackIcon from '@mui/icons-material/ArrowBackRounded';
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded';
import { useNavigate } from "react-router-dom";
import MenuComponent from "./MenuComponent";

/**
 * Note: CollapsedActions takes an array of objects, e.g.:
 * [
 *      {
 *          action.name,
 *          action.icon,
 *          action.method,
 *          action.disabled
 *      }, ...
 * ]
 * @returns Page header component with the necessary bits.
 */
export default function PageHeader({
    title, 
    info = null,
    previous = null,
    previousIsLink = true,
    mainActionName, 
    mainActionMethod, 
    mainActionType = "primary", 
    mainActionIcon = null,
    mainActionLoadingState = false,
    mainActionDisabledState = false,
    mainActionDisabledTooltip = null,
    collapsedActions = null, 

    showToggleGroup = false,
    selectedToggle,
    setSelectedToggle,
    toggleOption1Name = "",
    toggleOption2Name = "",

    showDataInfo = false,
    dataInfo
}) {

    const navigate = useNavigate();
    const [actionsMenuAnchorEl, setActionsMenuAnchorEl] = useState(null);

    //Setting anchor element to where the menu was opened from
    const openActionsMenu = (event) => setActionsMenuAnchorEl(event.currentTarget);
    const closeActionsMenu = () => setActionsMenuAnchorEl(null);

    const handleSetToggle = (event, newValue) => {
        if (newValue != null) {
            setSelectedToggle(newValue);
        }
    }

    return (<>
        <Box mb={1.5}>
            <Box 
                display="flex" 
                alignItems="center" 
                justifyContent="space-between" 
                mb="8px"
                gap="8px"
                flexWrap={ mainActionName || showDataInfo || showToggleGroup ? "wrap" : "nowrap"}
            >
                <Box display="flex" alignItems="center" gap="5px">
                    {(previous) &&
                        <IconButton onClick={() => {
                            if (previousIsLink) {
                                navigate(previous);
                            } else {
                                previous();
                            }
                        }}>
                            <ArrowBackIcon />
                        </IconButton>
                    }
                    <Box display="flex" alignItems="center" gap="2px">
                        <Typography variant="h5" pl={previous ? 0 : 1}>
                            {title}
                        </Typography>
                        {info && 
                            <Tooltip title={info}>
                                <IconButton size="small">
                                    <InfoOutlineIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>
                        }
                    </Box>
                </Box>
                <Box 
                    display="flex" 
                    alignItems="center" 
                    gap="5px" 
                    flexWrap="wrap" 
                    // sx={{ml: "auto"}} //Push to the right by consuming left space.
                >
                    {/* Data info if exists */
                    (showDataInfo == true && dataInfo != null) &&
                        dataInfo
                    }
                    {/* Main action display if exists */}
                    { (mainActionName) &&
                        <Tooltip title={mainActionDisabledState && mainActionDisabledTooltip}>
                            <span><Button 
                                variant="contained" 
                                color={mainActionType}
                                onClick={mainActionMethod}
                                disabled={mainActionDisabledState}
                                loading={mainActionLoadingState}
                                {...(mainActionIcon && { 
                                    endIcon: mainActionIcon
                                })}
                            >
                                {mainActionName}
                            </Button></span>
                        </Tooltip>
                    }
                    {showToggleGroup &&
                        <Box display="flex" alignItems="center" gap="10px" flexWrap="wrap">
                            <Typography>Viewing Mode</Typography>
                            <ToggleButtonGroup
                                value={selectedToggle}
                                color="primary"
                                exclusive
                                onChange={handleSetToggle}
                            >
                                <ToggleButton value={toggleOption1Name} aria-label={toggleOption1Name}>
                                    {toggleOption1Name}
                                </ToggleButton>
                                <ToggleButton value={toggleOption2Name} aria-label={toggleOption2Name}>
                                    {toggleOption2Name}
                                </ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                    }
                    { collapsedActions &&
                        <IconButton 
                            onClick={openActionsMenu}
                            sx={{ml: "auto"}} //Push to the right by consuming left space.
                        >
                            <MoreVertRoundedIcon />
                        </IconButton>
                    }
                </Box>
            </Box>
            <Divider/>
        </Box>

        { collapsedActions &&
            <MenuComponent
                anchorEl={actionsMenuAnchorEl}
                open={Boolean(actionsMenuAnchorEl)}
                onClose={closeActionsMenu}
                //Anchor menu to bottom-right of icon
                anchorOrigin={{horizontal: 'right', vertical: 'bottom'}}
                //Menu top-right meets anchor
                transformOrigin={{horizontal: 'right', vertical: 'top'}}
                actions={collapsedActions}
            />
        }
        
    </>);

}