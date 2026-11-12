import { ListItemIcon, Menu, MenuItem } from "@mui/material";

/**
 * Actions should be passed as an array of objects e.g. 
 * [
 *      {
 *          action.name,
 *          action.icon,
 *          action.method,
 *          action.disabled
 *      }, ...
 * ]
 */
export default function MenuComponent({
    anchorEl,
    open,
    onClose,
    anchorOrigin,
    transformOrigin,
    actions,
    adjustMarginTop = 0,
    adjustMarginLeft = 0,
}) {

    return (<>
        <Menu
            anchorEl={anchorEl}
            open={open}
            onClose={onClose}
            anchorOrigin={anchorOrigin}
            transformOrigin={transformOrigin}
            slotProps={{
                paper: {
                    sx: {
                        mt: adjustMarginTop,
                        ml: adjustMarginLeft
                    }
                }
            }}
        >
            {actions.map((action, i) => (
                <MenuItem
                    dense 
                    key={i} 
                    disabled={action.disabled} 
                    onClick={() => {
                        onClose?.();
                        action.method?.();
                    }} 
                    aria-label={action.name}
                >
                    {action.icon &&
                        <ListItemIcon>
                            {action.icon}
                        </ListItemIcon>
                    }
                    {action.name}
                </MenuItem>
            ))}
        </Menu>
    </>)

}