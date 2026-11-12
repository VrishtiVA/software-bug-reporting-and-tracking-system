import { useEffect, useState } from "react";
import api from "../../api/axios";
import { useParams } from "react-router-dom";
import { Button, ImageList, ImageListItem, ImageListItemBar, useMediaQuery } from "@mui/material";
import theme from "../../styles/theme";
import { showFancybox } from "../../utilities/showFancybox";

export default function BugImageAttachmentsMasonry({
    attachmentsMetadata,
    moreAttachmentPages,
    moreAttachmentsLoading,
    handleFetchMoreAttachments,
    resetAttachmentPages
}) {
    const pathParams = useParams();

    //Responsive attachment columns
    const isScreenSmall = useMediaQuery(theme.breakpoints.up("sm"));
    const isScreenMedium = useMediaQuery(theme.breakpoints.up("md"));
    const isScreenLarge = useMediaQuery(theme.breakpoints.up("lg"));
    const attachmentColumns = isScreenLarge ? 4 : isScreenMedium ? 3 : isScreenSmall ? 2 : 1;

    const [thumbnails, setThumbnails] = useState([]);
    const [thumbnailsLoading, setThumbnailsLoading] = useState(true);

    /**
     * Function to fetch first page of attachments using metadata. 
     */
    async function fetchAttachmentsFromMetadata(attachmentsMetadata) {
        try {
            //Fetch all attachments
            attachmentsMetadata.forEach((attachment) => {
                //If attachment already added to thumbnails, skip early if possible.
                api.get(`/workspaces/${pathParams.id}/bugs/reports/attachments/${attachment.id}`, {
                    params: {for_masonry: true}, 
                    responseType: "blob"
                }).then(({data}) => {
                    let resource = null;
                    let available = true;
                    try {
                        resource = URL.createObjectURL(data);
                    } catch (err) {
                        //Handle if could not load resource by allowing display of placeholder image.
                        resource = unavailableImage;
                        available = false;
                    }
                    setThumbnails(prev => {
                        //Enforce attachment only added once.
                        if (data && prev.some(thumbnail => thumbnail.id === attachment.id)) {
                            return prev;
                        }
                        //Otherwise add thumbail
                        return [...prev, {
                            "caption": attachment.caption,
                            "id": attachment.id,
                            "size": attachment.size,
                            "type": attachment.type,
                            "resource": resource,
                            "available": available
                        }]
                    })
                }).catch(error => {
                    console.log(error);
                });
            });
        } catch (error) {
            console.log(error);
        }
    }

    /**
     * Function to orchestrate fetching mmore attachments.
     */
    function fetchMore() {
        setThumbnailsLoading(true);
        handleFetchMoreAttachments();
    }

    /**
     * Function to open the fancybox lightbox view for an attachment,
     * Usually used after clicking on its thumbnail.
     * Handles fetching and displaying the full attachment
     * @param {*} thumbnail The thumbnail to view full attachment of.
     */
    function viewAttachmentInFancybox(thumbnail) {
        api.get(`/workspaces/${pathParams.id}/bugs/reports/attachments/${thumbnail.id}`, {
            responseType: "blob"
        }).then(({data}) => {
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
        try{
            //Try to fetch attachments if any exist.
            if (!(attachmentsMetadata.length > 0)) return;
            fetchAttachmentsFromMetadata(attachmentsMetadata);
        } catch (error) {
            console.log(error);
        } finally {
            setThumbnailsLoading(false);
        }
    }, [attachmentsMetadata]);

    useEffect(() => {
        //Runs on unmount for cleanup.
        return () => {
            //Reset to 1 page
            resetAttachmentPages();
            //Revoke thumbnail image URLs.
            thumbnails.forEach(thumb => {
                if (thumb.available) {
                    URL.revokeObjectURL(thumb.resource);
                }
            });
        }
    }, []);

    return (<>        
        {thumbnails &&
            <ImageList variant="masonry" cols={attachmentColumns} gap={12} sx={{mt: 1.5}}>
                {thumbnails.map(thumb => (
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
                        />
                        {thumb.caption !== null && thumb.caption !== "" && 
                            <ImageListItemBar
                                subtitle={thumb.caption}
                                position="bottom"
                            /> 
                        }
                    </ImageListItem>
                ))}
            </ImageList>
        }

        {moreAttachmentPages &&
            <Button
                fullWidth
                loading={thumbnailsLoading || moreAttachmentsLoading}
                onClick={fetchMore}
            >
                Show More
            </Button>
        }
    </>);
}