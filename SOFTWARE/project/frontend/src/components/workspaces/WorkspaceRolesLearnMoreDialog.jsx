import { Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, useMediaQuery } from "@mui/material";
import React, { useState } from "react";
import DialogComponent from "../common/DialogComponent";
import { PermissionCategories, WorkspaceRolePermissionDetails } from "../../api/options";
import CheckIcon from '@mui/icons-material/CheckRounded';
import CrossIcon from '@mui/icons-material/CloseRounded';
import theme from "../../styles/theme";

function WorkspaceRolesLearnMoreDialog({}) {

    const isSmallScreen = useMediaQuery(theme.breakpoints.only("xs"))

    const [isLearnMoreDialogOpen, setIsLearnMoreDialogOpen] = useState(false);
    const showLearnMoreDialog = () => setIsLearnMoreDialogOpen(true);
    const hideLearnMoreDialog = () => setIsLearnMoreDialogOpen(false);

    return (<>
        <Button 
            size="small"
            onClick={showLearnMoreDialog}
        >
            Learn More About Roles
        </Button>

        <DialogComponent
            open={isLearnMoreDialogOpen}
            closeDialogMethod={hideLearnMoreDialog}
            dialogTitle={"Workspace (WS) Roles and Permissions"}
            dialogContent={
                <Box
                    border={`1px solid ${theme.palette.divider}`}
                    borderRadius={2}
                    overflow={"hidden"}
                >
                    <TableContainer>
                        <Table 
                            size={isSmallScreen ? "small" : "medium"}
                            aria-label="Workspace Roles and Permissions Table"
                        >
                            <TableHead sx={{bgcolor: theme.palette.neutral.main}}>
                                <TableRow>
                                    <TableCell sx={{borderRight: `1px solid ${theme.palette.divider}`}}>
                                        Ability
                                    </TableCell>
                                    <TableCell align="center">Owner</TableCell>
                                    <TableCell align="center">Admin</TableCell>
                                    <TableCell align="center">Developer</TableCell>
                                    <TableCell align="center">Reporter</TableCell>
                                    <TableCell align="center" sx={{whiteSpace: "nowrap"}}>Non Member (Public)</TableCell>
                                    <TableCell align="center" sx={{whiteSpace: "nowrap"}}>Non Member (Private)</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {Object.keys(PermissionCategories).map((category, i) => (
                                    <TableRow key={i}
                                        sx= {{'&:last-child td, &:last-child th': {
                                            borderBottom: 0,
                                        }}}
                                    >
                                        <TableCell sx={{
                                            borderRight: `1px solid ${theme.palette.divider}`,
                                            bgcolor: theme.palette.neutral.main,
                                            // minWidth: "175px"
                                        }}>
                                            <Typography variant="body2">
                                                {PermissionCategories[category]}
                                            </Typography>
                                        </TableCell>
                                        {Object.keys(WorkspaceRolePermissionDetails).map((role, j) => (
                                            <TableCell key={j} align="center">{
                                                WorkspaceRolePermissionDetails[role][category] == 1 ? <CheckIcon fontSize="small" sx={{mb: -0.55}}/> 
                                                : WorkspaceRolePermissionDetails[role][category] == 0 ? <CrossIcon fontSize="small" sx={{mb: -0.55}}/>
                                                : <Typography variant="caption">
                                                    {WorkspaceRolePermissionDetails[role][category]}
                                                </Typography>
                                            }</TableCell>
                                        ))}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            }
            showDismiss={true}
        />
    </>);
}

export default React.memo(WorkspaceRolesLearnMoreDialog);