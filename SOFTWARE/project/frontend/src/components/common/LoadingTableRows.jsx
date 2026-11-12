import { Skeleton, TableCell, TableRow } from "@mui/material";
import React from "react";

function LoadingTableRows({
    rows, 
    columns
}) {

    const tableRows = [];
    for (let i = 0; i < rows; i++) {
        let tableRow = [];
        for (let j = 0; j < columns; j++) {
            tableRow.push(
                <TableCell key={"column"+j}>
                    <Skeleton/>
                </TableCell>
            );
        }
        tableRows.push(tableRow);
    }

    return (<>
        {tableRows.map((rowCells, i) => (
            <TableRow key={"row"+i} sx={{
                '&:last-child td, &:last-child th': {
                    borderBottom: 0,
                }
            }}>
                {rowCells}
            </TableRow>
        ))}
    </>);
}

export default React.memo(LoadingTableRows);