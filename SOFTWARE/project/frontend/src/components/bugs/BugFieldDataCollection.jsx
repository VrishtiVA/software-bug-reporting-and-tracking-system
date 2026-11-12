import { Button, List, ListItem } from "@mui/material";
import React from "react";
import ReportMetadataLine from "../reports/ReportMetadataLine";

function BugFieldDataCollection({
    fieldData,
    moreFieldDataPages,
    fieldDataLoading,
    handleFetchMoreFieldData
}) {
    
    return (<>
        <List>
            {fieldData.map((v, _) => (<>
                <ListItem key={v.id} disableGutters divider sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "start"
                }}>
                    <ReportMetadataLine
                        user={v.user}
                        userRole={v.user_role}
                        creationTimestamp={v.creationTimestamp}
                    />
                    {v.field_data}
                </ListItem>
            </>))}
        </List>
        {moreFieldDataPages &&
            <Button
                fullWidth
                loading={fieldDataLoading}
                onClick={handleFetchMoreFieldData}
            >
                Show More
            </Button>
        }
    </>)
}

export default React.memo(BugFieldDataCollection);