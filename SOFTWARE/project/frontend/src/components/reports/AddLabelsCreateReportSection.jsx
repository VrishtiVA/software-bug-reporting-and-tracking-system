import { Box, Card, Chip, Collapse, FormControl, FormHelperText, InputLabel, MenuItem, Select, Typography } from "@mui/material";
import { useCallback, useState } from "react";
import FormLabelComponent from "../common/FormLabelComponent";
import ContainedIconButton from "../common/ContainedIconButton";
import WorkspaceCustomLabelsContent from "../workspaces/WorkspaceCustomLabelsContent";
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import { CustomLabelColours } from "../../api/colours";
import WorkspaceCustomLabelDisplay from "../workspaces/WorkspaceCustomLabelDisplay";

export default function AddLabelsCreateReportSection({
    reportLabels,
    setReportLabels,
    workspaceCustomLabels,
    tooltip = "",
    question = ""
}) {

    const [currentLabel, setCurrentLabel] = useState("None");
    const [isAddLabelLoading, setIsAddLabelLoading] = useState(false);
    const [currentLabelHelperText, setCurrentLabelHelperText] = useState("");
    
    const handleSetCurrentLabel = useCallback((e) => {
        setCurrentLabel(e.target.value);
        setCurrentLabelHelperText("");
    })

    async function handleAddLabel() {
        setIsAddLabelLoading(true);
        //If current label, add.

        //Validate presence, otherwise show helper text.
        if (currentLabel == "None") {
            setCurrentLabelHelperText("Select from labels here");
            setIsAddLabelLoading(false);
            return;
        }

        //Validate unique label, otherwise show helper text.
        if (reportLabels.some(label => (label.name === currentLabel.name))) {
            setCurrentLabelHelperText("This label has already been added");
            setIsAddLabelLoading(false);
            return;
        }

        //Add label
        setReportLabels((prev) => [...prev, currentLabel]);
        setCurrentLabel("None");
        setCurrentLabelHelperText("");
        setIsAddLabelLoading(false);
    }

    /**
     * Function to handle removing a label by filtering it out.
     * @param {Number} iRemove index to remove from list.
     */
    async function handleRemoveLabel(iRemove) {
        setReportLabels((prev) => prev.filter((_, i) => i !== iRemove));
    }

    return (<>
        <Box display="flex" flexDirection="column" gap="8px">
            <FormControl>
                <FormLabelComponent
                    label={question}
                    tooltipText={tooltip}
                />
                <Box display="flex" gap="8px" flexWrap="wrap">
                    <Select
                        sx={{width: "215px"}}
                        value={currentLabel}
                        onChange={handleSetCurrentLabel}
                    >
                        <MenuItem value="None" disabled dense>
                            <Typography>Select Label</Typography>
                        </MenuItem>
                        {workspaceCustomLabels.map((v, i) => (
                            <MenuItem value={v} dense key={i}>
                                <WorkspaceCustomLabelDisplay label={v}/>
                            </MenuItem>
                        ))}
                    </Select>
                    {/* Add label button */}
                    <ContainedIconButton
                        onClick={handleAddLabel}
                        loading={isAddLabelLoading}
                        icon={<AddRoundedIcon/>}
                        variant={"success"}
                        tooltip="Add label to bug report"
                        disabled={currentLabel === "None"}
                        disabledTooltip={"Select a label to add the label to this report."}
                    />
                </Box>
                <FormHelperText sx={{mr: 0}}>{currentLabelHelperText}</FormHelperText>
            </FormControl>
            {/* Preview display */}
            <Collapse in={reportLabels.length > 0}>
                <Card variant="outlined">
                    <Box padding="10px" display="flex" gap="5px" flexWrap="wrap">
                        <WorkspaceCustomLabelsContent
                            workspaceCustomLabels={reportLabels}
                            removable={true}
                            removeMethod={handleRemoveLabel}
                        />
                    </Box>
                </Card>
            </Collapse>
        </Box>
    </>);
}