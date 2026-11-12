import { BarChart, RadarChart } from "@mui/x-charts";
import React from "react";
import { CustomLabelColours } from "../../../api/colours";

function WorkspaceLabelDistributionDisplay({
    workspaceLabelsDistribution
}) {

    const labels = workspaceLabelsDistribution.map(v => v.workspace_custom_label.name);
    const colours = workspaceLabelsDistribution.map(v => CustomLabelColours[v.workspace_custom_label.colour]);

    const data = workspaceLabelsDistribution.map((v, i) => ({
        label: v.workspace_custom_label.name,
        colour: CustomLabelColours[v.workspace_custom_label.colour],
        count: v.label_usage
    }));

    const yAxisMax = Math.max(...data.map(v => v.count)) + 0.25

    return (<>
        <BarChart
            dataset={data}
            xAxis={[{ 
                dataKey: 'label',
                scaleType: "band",
                colorMap: {
                    type: "ordinal",
                    values: labels,
                    colors: colours
                },
                disableTicks: true,
                height: 25,
                label: "Labels"
            }]}
            yAxis={[{
                width: 50,
                label: "Usage",
                tickMinStep: 1,
                max: yAxisMax
            }]}
            series={[{ 
                dataKey: 'count'
            }]}
            height={250}
            borderRadius={8}
            margin={0}
            hideLegend={false}
        />
    </>);
}

export default React.memo(WorkspaceLabelDistributionDisplay)