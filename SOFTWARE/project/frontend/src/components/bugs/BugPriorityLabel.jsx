import { Chip, Tooltip } from "@mui/material";
import { BugPriorityLabelColours } from "../../api/colours";
import { BugPriorityLabelDescriptions } from "../../api/options";

export default function BugPriorityLabel({priorityLabel}) {
    return (<>
        <Tooltip title={BugPriorityLabelDescriptions[priorityLabel]}>
            <Chip
                label={priorityLabel}
                size="small"
                variant="soft-square"
                sx={{
                    bgcolor: BugPriorityLabelColours[priorityLabel],
                    color: "white"
                }}
            />
        </Tooltip>
    </>)
}