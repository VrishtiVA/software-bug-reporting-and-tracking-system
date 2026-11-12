import { alpha, Chip, styled } from "@mui/material";

/**
 * Styled chip for workspace, so that 
 * on hover it can have the background colour of the workspace too.
 */
export const WorkspaceChip = styled(Chip)( ({bgcolor}) => ({
    backgroundColor: bgcolor,
    "&:hover": {
        backgroundColor: alpha(bgcolor, 0.75)
    }
}))