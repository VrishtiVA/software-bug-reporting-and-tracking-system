import { Chip } from "@mui/material";
import { CustomLabelColours } from "../../api/colours";

export default function WorkspaceCustomLabelDisplay({label}) {
    return (<>
        <Chip 
            label={label.name} 
            variant="soft-square"
            size="small"
            sx={{
                bgcolor: CustomLabelColours[label.colour],
                color: "white"
            }}
        />
    </>);
}