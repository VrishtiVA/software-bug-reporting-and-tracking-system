import { Box, FormControl, FormHelperText, OutlinedInput, Skeleton, Stack } from "@mui/material";
import FormLabelComponent from "../common/FormLabelComponent";
import theme from "../../styles/theme";

export default function AttachmentSubformSection({
    isAttachmentCaptionValid,
    currentCaption,
    handleSetCurrentCaption,
    checkAttachmentCaption,
    attachmentCaptionHelperText,
    attachmentPreview,
    currentFileType
}) {

    return(<>
        <Stack spacing={1.5}>

            {/* Caption */}
            <FormControl fullWidth error={!isAttachmentCaptionValid}>
                <FormLabelComponent 
                    label="Caption (Optional)"
                    tooltipText={"Provide a short caption to describe this attachment, e.g. about 5 to 10 words. You are allowed to use a maximum of 100 characters."}
                />
                <OutlinedInput
                    placeholder="Add a caption"
                    value={currentCaption}
                    onChange={handleSetCurrentCaption}
                    onBlur={(e) => checkAttachmentCaption(e.target.value, false)}
                />
                <FormHelperText>{attachmentCaptionHelperText}</FormHelperText>
            </FormControl>

            {/* Attachment Preview */}
            {attachmentPreview !== "" ? <>
                {currentFileType === "image" ?
                    <Box
                        component="img"
                        src={attachmentPreview}
                        alt="Image Upload Preview"
                        sx={{
                            width: "100%",
                            objectFit: "cover",
                            borderRadius: "5px",
                            border: `1px solid ${theme.palette.divider}`
                        }}
                    />
                    :
                    <Box 
                        component="video"
                        src={attachmentPreview}
                        aria-label="Video Upload Preview"
                        controls
                        sx={{
                            width: "100%",
                            borderRadius: "5px",
                            border: `1px solid ${theme.palette.divider}`
                        }}
                    />
                }</>
                :
                <Skeleton variant="rounded" height="35vh"/>
            }
        </Stack>
    </>)
}