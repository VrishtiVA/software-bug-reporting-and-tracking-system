import { PieChart } from "@mui/x-charts";
import React from "react";
import { BugPriorityLabelColours } from "../../../api/colours";
import { useMediaQuery } from "@mui/material";
import theme from "../../../styles/theme";

function WorkspacePriorityCategoryDistribution({
    priorityDistribution = {},
    pieChartSeriesSettings,
    legendSettings
}) {
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"));

    return (<>
        <PieChart
            series={[
                {
                    ...pieChartSeriesSettings,
                    data: Object.entries(priorityDistribution).map(([label, value]) => ({
                        id: label,
                        value: value,
                        label: label,
                        color: BugPriorityLabelColours[label],
                        labelMarkType: "square"
                    })),
                },
            ]}
            width={Object.keys(priorityDistribution).length == 0 ? 150 : 100}
            height={90}
            slotProps={{
                ...legendSettings,
            }}
            hideLegend={isSmallScreen}
        />
    </>);
}

export default React.memo(WorkspacePriorityCategoryDistribution);