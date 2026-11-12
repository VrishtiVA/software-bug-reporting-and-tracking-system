import { alpha, colors, Typography } from "@mui/material";
import { ScatterChart } from "@mui/x-charts";
import React from "react";
import { BugPriorityLabelColours } from "../../../api/colours";

function WorkspacePriorityFrequencyBugCounts({
    priorityFrequencyBugCounts
}) {

    const PRIORITY_BUCKET_SIZE = 5;
    const FREQUENCY_BUCKET_SIZE = 5;

    const data = priorityFrequencyBugCounts.map((v) => ({
        x: Number(v.priority_bucket_lb) + PRIORITY_BUCKET_SIZE/2,
        y: Number(v.frequency_bucket_lb) + FREQUENCY_BUCKET_SIZE/2,
        size: Number(v.bug_count)
    }))

    const yAxisMax = Math.max(...data.map(v => v.y)) + FREQUENCY_BUCKET_SIZE/2 + 12.5;
    const showData = priorityFrequencyBugCounts.length > 0 && data != null

    return (<>
        <ScatterChart
            dataset={data}
            xAxis={[{
                dataKey: "x", 
                min: 0, 
                max: 100, 
                label: "Priority",
                colorMap: {
                    type: 'continuous',
                    min: 10,
                    max: 90,
                    color: ['gold', 'maroon']
                }
            }]}
            yAxis={[{
                dataKey: "y",
                label: "Frequency", 
                width: 50,
                min: 0,
                max: yAxisMax,
                tickMinStep: 5
            }]}
            zAxis={[{
                dataKey: "size"
            }]}
            margin={0}
            height={250}
            grid={{
                horizontal: showData, 
                vertical: showData
            }}
            series={data.map((point) => ({
                data: [{ 
                    x: point.x,
                    y: point.y,
                    z: point.x,
                    size: point.size
                }],
                highlightScope: { highlight: 'series', fade: 'global' },
                markerSize: Math.sqrt(point.size * 25),
                valueFormatter: (point) => (<>
                    <Typography variant="body2">Bugs: {point.size}</Typography>
                    <Typography variant="body2">{`Priority: ${point.x - PRIORITY_BUCKET_SIZE/2} to ${point.x + PRIORITY_BUCKET_SIZE/2}`}</Typography>
                    <Typography variant="body2">{`Frequency: ${point.y - FREQUENCY_BUCKET_SIZE/2} to ${point.y + FREQUENCY_BUCKET_SIZE/2}`}</Typography>
                </>),
            }))}
            sx={{
                '& circle': {
                    opacity: 0.5,
                    transition: "opacity 0.3s ease-in-out",
                    pointerEvents: "all"
                },
                '& [data-highlighted=true]': { 
                    opacity: 1
                }
            }}
                slotProps={{
                tooltip: {
                    sx: {
                        '& .MuiChartsTooltip-labelCell': {
                            display: "flex",
                            alignItems: "center",
                            mt: 1
                        },
                        '& .MuiChartsTooltip-row': {
                            display: "flex",
                            flexDirection: "column"
                        },
                    }
                }
            }}
        />
    </>);
}

export default React.memo(WorkspacePriorityFrequencyBugCounts);