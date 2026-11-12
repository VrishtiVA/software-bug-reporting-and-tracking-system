import { PieChart } from "@mui/x-charts";
import React from "react";
import { ProgressOptions } from "../../../api/options";
import { ProgressColours } from "../../../api/colours";
import { useMediaQuery } from "@mui/material";
import theme from "../../../styles/theme";

function WorkspaceProgressDistribution({
    progressDistribution = {},
    pieChartSeriesSettings,
    legendSettings
}) {
    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))

    return (<>
        <PieChart
            series={[
                {
                    ...pieChartSeriesSettings,
                    data: Object.entries(progressDistribution).map(([progress, count]) => ({
                        id: progress,
                        value: count,
                        label: ProgressOptions[progress],
                        color: ProgressColours[progress],
                        labelMarkType: "square",
                    })),
                },
            ]}
            width={Object.keys(progressDistribution).length == 0 ? 150 : 100}
            height={90}
            slotProps={
                legendSettings
            }
            hideLegend={isSmallScreen}
        />
    </>);
}

export default React.memo(WorkspaceProgressDistribution);