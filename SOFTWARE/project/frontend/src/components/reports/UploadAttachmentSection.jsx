
import { useCallback, useState } from "react";
import FormLabelComponent from "../common/FormLabelComponent";
import { Box, Button, ButtonBase, Collapse, OutlinedInput, Stack, useMediaQuery } from "@mui/material";
import UploadFileIcon from "@mui/icons-material/UploadFileRounded"
import ContainedIconButton from "../common/ContainedIconButton";
import DeleteIcon from '@mui/icons-material/DeleteOutlineRounded';
import DialogComponent from "../common/DialogComponent";
import { checkCaptionValidity } from "../../utilities/reports/validateReports";
import { debounce } from "../../utilities/functionHandlers";
import { useSnackbar } from "../contexts/SnackbarContext";
import MiniFilePreviewBox from "../common/MiniFilePreviewBox";
import AttachmentSubformSection from "./AttachmentSubformSection";
import theme from "../../styles/theme";

/**
 * Note: 
 * reportAttachments is an array of attachments like this:
 * [
 *      file,
 *      preview,
 *      type,
 *      size
 * ]
 */
export default function UploadAttachmentSection({
    userRole,
    tooltip = "",
    question = "",
    reportAttachments, //array of attachments 
    setReportAttachments,
    reportCaptions,
    setReportCaptions
}) {

    const { displaySnackbar } = useSnackbar();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"))

    const MAX_ATTACHMENTS = userRole === null ? 1 : 3;
    const MAX_IMAGE_SIZE_MB = 10;
    const MAX_VIDEO_SIZE_MB = 50;
    const MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024;
    const MAX_VIDEO_SIZE_BYTES = MAX_VIDEO_SIZE_MB * 1024 * 1024;
    const VALID_ATTACHMENT_TYPES = [
        "image/png", "image/jpeg", "image/jpg",
        "video/mp4", "video/webm"
    ];

    const [currentAttachment, setCurrentAttachment] = useState(null);
    const [currentCaption, setCurrentCaption] = useState("");
    const [currentFileType, setCurrentFileType] = useState(null);
    const [currentFileSize, setCurrentFileSize] = useState(null);
    const [attachmentPreview, setAttachmentPreview] = useState("");
    const [isAttachmentPreviewDialogOpen, setIsAttachmentPreviewDialogOpen] = useState(false);
    const [isUploadProcessing, setIsUploadProcessing] = useState(false);

    const dismissAttachmentPreviewDialog = () => {
        setIsAttachmentPreviewDialogOpen(false);
        setIsUploadProcessing(false);
        setCurrentAttachment(null);
        setCurrentFileType(null);
        setAttachmentPreview("");
        setCurrentCaption("");
        setIsAttachmentCaptionValid(true);
        setAttachmentCaptionHelperText("");
    }

    const [currentModifying, setCurrentModifying] = useState(0);
    const [isAttachmentEditDialogOpen, setIsAttachmentEditDialogOpen] = useState(false);
    const openAttachmentEditDialog = (iView) => {
        setCurrentModifying(iView);
        setAttachmentPreview(reportAttachments[iView].preview);
        setCurrentFileType(reportAttachments[iView].type);
        setCurrentCaption(reportCaptions[iView]);
        setIsAttachmentEditDialogOpen(true);
    }
    const dismissAttachmentEditDialog = () => {
        setIsAttachmentEditDialogOpen(false);
        setAttachmentPreview("");
        setCurrentCaption("");
        setCurrentFileType(null);
        setIsAttachmentCaptionValid(true);
        setAttachmentCaptionHelperText("");
    }

    //Validation
    const [isAttachmentCaptionValid, setIsAttachmentCaptionValid] = useState(true);
    const [attachmentCaptionHelperText, setAttachmentCaptionHelperText] = useState("");

    function checkAttachmentCaption(caption, focussed) {
        let [validityState, helperMessage] = checkCaptionValidity(caption, focussed);
        setIsAttachmentCaptionValid(validityState);
        setAttachmentCaptionHelperText(helperMessage);
        return validityState;
    }

    //Checks while typing, debounced for optimisation.
    const debounceCheckCaption = useCallback(debounce((value) => {
        checkAttachmentCaption(value, true);
    }, 250), []);

    const handleSetCurrentCaption = useCallback((e) => {
        setCurrentCaption(e.target.value);
        debounceCheckCaption(e.target.value);
    }, []);

    /** 
     * Function to handle uploading attachment for preview.
     */
    const handleAttachmentUpload = (e) => {
        setIsUploadProcessing(true);

        //Ensure not max.
        if (reportAttachments.length >= MAX_ATTACHMENTS) {
            displaySnackbar("error", "Unable to upload attachment", "You have already reached the maximum number of attachments.");
            setIsUploadProcessing(false);
            return;
        }
        
        //Presence check.
        let fileUpload = e.target.files[0];
        e.target.value = "";
        if (fileUpload == null) {
            setIsUploadProcessing(false);
            return;
        };

        //Validate supported file type.
        if (!VALID_ATTACHMENT_TYPES.includes(fileUpload.type)) {
            console.log("Unable to upload attachment with invalid type: " + fileUpload.type);
            displaySnackbar("error", "Unable to upload attachment", "Attachment is of an unsupported file type.");
            setIsUploadProcessing(false);
            return;
        }
        let fileType = fileUpload.type.startsWith("video") ? "video" : "image";
        let fileSize = fileUpload.size;

        //If file incorrect type or large file size, show error snackbar.
        if (fileType === "image" && fileSize > MAX_IMAGE_SIZE_BYTES) {
            console.log("Unable to upload image attachment, has size: " + MAX_IMAGE_SIZE_MB);
            displaySnackbar("error", "Unable to upload attachment", "Image attachment should be under 10MB.");
            setIsUploadProcessing(false);
            return;
        } else if (fileType === "video" && fileSize > MAX_VIDEO_SIZE_BYTES) {
            console.log("Unable to upload video attachment, has size " + MAX_VIDEO_SIZE_MB);
            displaySnackbar("error", "Unable to upload attachment", "Video attachment should be under 50MB.");
            setIsUploadProcessing(false);
            return;
        }

        //Check not duplicate file upload.
        let fileIdentifier = fileUpload.name + "-" + fileUpload.size;
        if (reportAttachments.some(attachment => (attachment.file.name + "-" + attachment.size) === fileIdentifier)) {
            console.log("Duplicate file upload detected and prevented.");
            displaySnackbar("error", "Unable to upload attachment", "This attachment has already been added to the report.");
            setIsUploadProcessing(false);
            return;
        }

        //Display preview
        setCurrentAttachment(fileUpload);
        setCurrentFileType(fileType);
        setCurrentFileSize(fileSize);
        setAttachmentPreview(URL.createObjectURL(fileUpload));
        setIsAttachmentPreviewDialogOpen(true);
    };

    /**
     * Function for adding attachment to report attachments.
     */
    const handleAddAttachment = () => {
        //Ensure not max.
        if (reportAttachments.length >= MAX_ATTACHMENTS) {
            displaySnackbar("error", "Unable to upload attachment", "You have already reached the maximum number of attachments.");
            setIsUploadProcessing(false);
            dismissAttachmentPreviewDialog();
            return;
        }

        //Validate caption, if invalid, show error and helper message, keep dialog open.
        if (!checkAttachmentCaption(currentCaption, false)) { 
            return;
        }

        //Add attachment
        setReportAttachments((prev) => 
            [...prev, {
                file: currentAttachment,
                preview: attachmentPreview,
                type: currentFileType,
                size: currentFileSize
            }]
        );
        setReportCaptions((prev) => [...prev, currentCaption]);

        //Close preview
        setIsUploadProcessing(false);
        dismissAttachmentPreviewDialog();
    }

    /**
     * Function to handle removing attachments from report attachments.
     */
    const handleRemoveAttachment = (iRemove) => {
        setReportAttachments((prev) => prev.filter((_, i) => i !== iRemove));
        setReportCaptions((prev) => prev.filter((_, i) => i !== iRemove));
    }

    /**
     * Function to handle updating caption of an already added report attachment.
     */
    const handleEditAttachment = () => {
        //Validate caption, if invalid, show error and helper message, keep dialog open.
        if (!checkAttachmentCaption(currentCaption, false)) { 
            return;
        }
        //Update caption
        setReportCaptions((prev) => {
            let updatedArray = [...prev];
            updatedArray[currentModifying] = currentCaption;
            return updatedArray
        });
        //Close preview
        dismissAttachmentEditDialog();
    }

    return (<>
        <Box 
            display="flex" 
            justifyContent="space-between"
            gap={0.5}
            alignItems={isSmallScreen || reportAttachments.length == 0 ? "start" : "center"}
            flexDirection={isSmallScreen || reportAttachments.length == 0 ? "column" : "row"}
            mb={reportAttachments.length == 0 ? 0 : 1}
        >
            <FormLabelComponent
                label={question}
                tooltipText={tooltip}
            />

            {/* Hide upload button if at max. */}
            {reportAttachments.length < MAX_ATTACHMENTS &&
                <Button 
                    fullWidth={reportAttachments.length == 0}
                    variant="contained"
                    component="label"
                    disabled={isUploadProcessing}
                    startIcon={<UploadFileIcon/>}
                    sx={{
                        mt: isSmallScreen || reportAttachments.length == 0 ? 0 : -1,
                    }}
                >
                    Upload Attachment
                    <input
                        type="file"
                        accept=".png,.jpg,.jpeg,.mp4,.webm"
                        hidden
                        onChange={handleAttachmentUpload}
                    >
                    </input>
                </Button>
            }
        </Box>
        
        {/* Show list */}
        <Collapse in={reportAttachments.length > 0} sx={{width: "100%"}}>
            <Box 
                display="flex" 
                flexDirection="column" 
                gap={1}
            >
                {reportAttachments && reportAttachments.map((v, i) => (
                    <Box 
                        display="flex"
                        alignItems="center"
                        gap={1}
                        flexWrap="wrap"
                        key={i}
                    >
                        {/* Expand to preview/modify on click */}
                        <ButtonBase
                            onClick={() => openAttachmentEditDialog(i)}
                            sx={{
                                overflow: "hidden", 
                                borderRadius: "4px",
                                flex: 1
                            }}
                        >
                            {/* Mini preview box */}
                            <MiniFilePreviewBox
                                preview={v.preview}
                                type={v.type}
                                borderTopRightRadius="0"
                                borderBottomRightRadius="0"
                            />
                            {/* Caption preview */}
                            <OutlinedInput
                                readOnly
                                placeholder="No caption provided"
                                value={reportCaptions[i]}
                                sx={{
                                    "& .MuiInputBase-input": {
                                        cursor: "default"
                                    },
                                    flex: 1,
                                    borderTopLeftRadius: "0",
                                    borderBottomLeftRadius: "0",
                                    "& .MuiOutlinedInput-notchedOutline": {
                                        borderColor: "divider",
                                    },
                                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                                        borderColor: "divider",
                                        borderWidth: "1px"
                                    },
                                }}
                            />
                        </ButtonBase>
                        
                        {/* Remove button */}
                        <ContainedIconButton
                            onClick={() => handleRemoveAttachment(i)}
                            icon={<DeleteIcon/>}
                            tooltip="Remove attachment"
                            variant="danger"
                        />
                    </Box>
                ))}
            </Box>
        </Collapse>

        {/* Add caption through popup */}
        <DialogComponent 
            open={isAttachmentPreviewDialogOpen}
            closeDialogMethod={dismissAttachmentPreviewDialog}
            dialogTitle={"Add Attachment"}
            showCancel={true}
            mainActionName={"Add"}
            mainActionType={"success"}
            mainActionMethod={handleAddAttachment}
            dialogContent={<AttachmentSubformSection
                isAttachmentCaptionValid={isAttachmentCaptionValid}
                currentCaption={currentCaption}
                handleSetCurrentCaption={handleSetCurrentCaption}
                checkAttachmentCaption={checkAttachmentCaption}
                attachmentCaptionHelperText={attachmentCaptionHelperText}
                attachmentPreview={attachmentPreview}
                currentFileType={currentFileType}
            />}
        />

        {/* Preview attachment and edit caption view */}
        <DialogComponent 
            open={isAttachmentEditDialogOpen}
            closeDialogMethod={dismissAttachmentEditDialog}
            dialogTitle={"Edit Caption"}
            showCancel={true}
            mainActionName={"Save"}
            mainActionType={"success"}
            mainActionMethod={handleEditAttachment}
            mainActionDisabled={reportCaptions[currentModifying] === currentCaption}
            dialogContent={<AttachmentSubformSection
                isAttachmentCaptionValid={isAttachmentCaptionValid}
                currentCaption={currentCaption}
                handleSetCurrentCaption={handleSetCurrentCaption}
                checkAttachmentCaption={checkAttachmentCaption}
                attachmentCaptionHelperText={attachmentCaptionHelperText}
                attachmentPreview={attachmentPreview}
                currentFileType={currentFileType}
            />}
        />
    </>);
}