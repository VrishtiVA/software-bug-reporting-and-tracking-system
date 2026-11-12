import { Box, Chip, Divider, IconButton, ImageList, ImageListItem, ImageListItemBar, Skeleton, Stack, Typography, useMediaQuery } from "@mui/material";
import React, { useEffect, useState } from "react";
import { CustomLabelColours, RatingColour } from "../../api/colours";
import api from "../../api/axios";
import { useParams } from "react-router-dom";
import theme from "../../styles/theme";
import MenuOpenIcon from '@mui/icons-material/MenuOpenRounded';
import ReportMetadataLine from "./ReportMetadataLine";
import unavailableImage from "../../assets/attachment_unavailable.png";
import { showFancybox } from "../../utilities/showFancybox";

function ReportView({
    report,
    showToggle = false,
    handleToggle = null,
    isResolvePage = false
}) {
    const pathParams = useParams();

    //Responsive attachment columns
    const isScreenSmall = useMediaQuery(theme.breakpoints.up("sm"));
    const isScreenMedium = useMediaQuery(theme.breakpoints.up("md"));
    const isScreenLarge = useMediaQuery(theme.breakpoints.up("lg"));
    const attachmentColumns = isResolvePage 
        ? (isScreenLarge ? 2 : isScreenMedium ? 1 : isScreenSmall ? 2 : 1) 
        : (isScreenMedium ? 3 : isScreenSmall ? 2 : 1);

    const [attachments, setAttachments] = useState([]);
    const [attachmentsLoading, setAttachmentsLoading] = useState(false);

    useEffect(() => {
        let attachments = [];
        if (report.attachment_metadata.length === 0) {
            setAttachments(attachments);
            return;
        } else if (report.attachment_metadata.length > 0) {
            setAttachmentsLoading(true);

            //Fetch attachments in parallel, wait for completition, obtains array of responses.
            Promise.all(report.attachment_metadata.map( async (v) => {
                //Fetch resource
                let available = true;
                let resource = await api
                    .get(`/workspaces/${pathParams.id}/bugs/reports/attachments/${v.id}`, {
                        params: {for_masonry: true}, //to fetch the thumbnail version (quicker).
                        responseType: "blob"
                    }).then(({data}) => {
                        return URL.createObjectURL(data);
                    }).catch(error => {
                        console.log(error);
                        //Handle if could not load resource by allowing display of placeholder image.
                        available = false;
                        return unavailableImage;
                    });
                return {
                    "caption": v.caption,
                    "id": v.id,
                    "size": v.size,
                    "type": v.type,
                    "resource": resource,
                    "available": available
                };
            }))
            .then(setAttachments) //Set attachments in state for view.
            .finally(() => setAttachmentsLoading(false));
            
        } else {
            console.log("No report to check for attachments");
        }
    }, [report?.attachment_metadata?.map(a => a.id).join(",")]); //Reload only for different attachments.

    /**
     * Method to fetch full attachment from thumbnail and present Fancybox lightbox view.
     */
    function viewAttachmentInFancybox(thumbnail) {
        api.get(`/workspaces/${pathParams.id}/bugs/reports/attachments/${thumbnail.id}`, {
            responseType: "blob"
        }).then(({data}) => {
            console.log(data);
            let resourceURL = URL.createObjectURL(data);
            showFancybox(
                resourceURL,
                thumbnail.caption,
                (thumbnail.type).toLowerCase() === "video" ? "html5video" : "image",
                thumbnail.reference
            );
        }).catch(error => {
            console.log(error);
        });
    }

    useEffect(() => {
        //Runs on unmount for cleanup.
        return () => {
            //Revoke thumbnail image URLs.
            attachments.forEach(thumb => {
                if (thumb.available) {
                    URL.revokeObjectURL(thumb.resource);
                }
            });
        }
    }, [])

    return (<>
        <div>
            <Box display="flex" alignItems="center" gap={1} mb={1} flexWrap="wrap">
                {showToggle &&
                    <IconButton size="small" color="inherit" onClick={handleToggle}>
                        <MenuOpenIcon/>
                    </IconButton>
                }
                <Box display="flex" alignItems="center" gap={1} flexWrap="wrap" mb={!showToggle ? 0.25 : 0} mt={!showToggle ? 0.2 : 0}>
                    <Chip size="small" variant="soft-square" strength="weak" label={"REPORT ID: " + report.id}/>
                    <Typography variant="h5" sx={{title: report.summary}}>{report.summary}</Typography>
                </Box>
            </Box>
            <Divider/>
            <Box display="flex" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={1} mt={1}>
                <ReportMetadataLine
                    user={report.user}
                    userRole={report.user_role}
                    creationTimestamp={report.creation_timestamp}
                />
                <Box display="flex" gap={0.5}>
                    <Chip size="small" variant="rating" label={"User Impact: " + report.user_impact_rating} sx={{bgcolor: RatingColour[report.user_impact_rating], color: "white" }}/>
                    <Chip size="small" variant="rating" label={"Severity: " + report.severity_rating} sx={{bgcolor: RatingColour[report.severity_rating], color: "white" }}/>
                </Box>
            </Box>
            <Stack spacing={1} mt={1}>
                {report.environment !== null && report.environment !== "" && <div>
                    <Typography variant="body1">Environment</Typography>
                    <Typography variant="body2">{report.environment}</Typography>
                </div>}
                {report.steps !== null && report.steps !== "" && <div>
                    <Typography variant="body1">Steps To Reproduce</Typography>
                    <Typography variant="body2">{report.steps}</Typography>
                </div>}
                {report.expected !== null && report.expected !== "" && <div>
                    <Typography variant="body1">Expected Results</Typography>
                    <Typography variant="body2">{report.expected}</Typography>
                </div>}
                {report.actual !== null && report.actual !== "" && <div>
                    <Typography variant="body1">Actual Results</Typography>
                    <Typography variant="body2">{report.actual}</Typography>
                </div>}
                {report.fix_suggestions !== null && report.fix_suggestions !== "" && <div>
                    <Typography variant="body1">Fix Suggestions</Typography>
                    <Typography variant="body2">{report.fix_suggestions}</Typography>
                </div>}
                {report.labels !== null && report.labels.length > 0 && <div>
                    <Typography variant="body1">Labels</Typography>
                    <Box display="flex" mt={0.25} gap={0.5} flexWrap="wrap">
                        {report.labels.map((v, _) => (
                            <Chip key={v.name} label={v.name} variant="soft-square" size="small" sx={{bgcolor: CustomLabelColours[v.colour], color: "white"}}/>
                        ))}
                    </Box>
                </div>}
                {report.additional_comments !== null && report.additional_comments !== "" && <div>
                    <Typography variant="body1">Additional Comments</Typography>
                    <Typography variant="body2">{report.additional_comments}</Typography>
                </div>}
                {report.attachment_metadata !== null && report.attachment_metadata.length > 0 && <div>
                    <Typography mb={0.5} variant="body1">Attachments</Typography>
                    {attachmentsLoading ? <>
                        <Skeleton variant="rounded" height="175px"/>
                        <Skeleton width="150px"/>
                    </>:<>
                        <ImageList variant="masonry" gap={12} cols={attachmentColumns} sx={{mt: 1}}>
                            {attachments.map((thumb) => (
                                <ImageListItem
                                    key={thumb.id}
                                    onClick={() => viewAttachmentInFancybox(thumb)}
                                    sx={{
                                        border: `1px solid ${theme.palette.divider}`,
                                        borderRadius: "4px",
                                        overflow: "hidden",
                                        cursor: "pointer"
                                    }}
                                >
                                    <img
                                        src={thumb.resource}
                                        alt={thumb.caption}
                                        onError={(e) => e.currentTarget.src = unavailableImage}
                                    />
                                    {thumb.caption !== null && thumb.caption !== "" && 
                                        <ImageListItemBar
                                            title={thumb.caption}
                                            position="bottom"
                                        /> 
                                    }
                                </ImageListItem>
                            ))}
                        </ImageList>
                    </>}
                </div>}
            </Stack>
        </div>
    </>);
};

export default React.memo(ReportView);