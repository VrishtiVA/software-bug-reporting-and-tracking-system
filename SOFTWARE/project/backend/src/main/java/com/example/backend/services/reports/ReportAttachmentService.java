package com.example.backend.services.reports;

import com.example.backend.dto.responses.reports.ReportAttachmentDTO;
import com.example.backend.dto.responses.reports.TemporaryReportAttachments;
import com.example.backend.model.reports.FileType;
import com.example.backend.model.reports.ReportAttachment;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.reports.ReportAttachmentRepository;
import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Parts of this service implementation were influenced by
 * <ul>
 *     <li><a href="https://howtodoinjava.com/spring-boot/spring-boot-file-upload-with-react/">Spring Boot Multiple File Uploads with React</a></li>
 * </ul>
 */
@Service
public class ReportAttachmentService {

    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportAttachmentRepository reportAttachmentRepository;

    private final Path WORKSPACES_UPLOAD_PATH;

    /**
     * Constructor method for ReportAttachmentService.
     * Using absolute version of relative for portability and to avoid errors occurring with if was only relative;
     * when it was relative by itself, it was not able to create some parts.
     * @param BASE_UPLOAD_PATH The base upload path, which is a relative path to the project help in application.properties.
     */
    public ReportAttachmentService(
        @Value("${upload.base-path}") String BASE_UPLOAD_PATH
    ) {
        this.WORKSPACES_UPLOAD_PATH = Paths.get(BASE_UPLOAD_PATH).toAbsolutePath().resolve("workspaces");
        try {
            Files.createDirectories(WORKSPACES_UPLOAD_PATH);
        } catch (Exception e) {
            log.error("Failed to create WORKSPACES_UPLOAD_PATH in ReportAttachmentService initialisation", e);
        }
    }

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;
    private static final int MAX_ATTACHMENTS_MEMBER = 3;
    private static final int MAX_ATTACHMENTS_NON_MEMBER = 1;
    private static final Set<String> ACCEPTED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Set<String> ACCEPTED_VIDEO_EXTENSIONS = Set.of("mp4", "webm");

    private static final Logger log = LoggerFactory.getLogger(ReportAttachmentService.class);

    /* ---------- Attachment Handling ---------- */

    /**
     * Method to validate attachments and their captions, and turn them into a report attachment object if they are valid.
     * @param attachments The list of attachments as MultipartFiles.
     * @param captions The list of captions
     * @param userRole The role if the user in the workspace they sent this for.
     * @return Set of report attachments if the attachment is valid and temporary location path, otherwise null.
     */
    public TemporaryReportAttachments validateAttachmentsAndCaptionsIntoReportAttachments(List<MultipartFile> attachments, List<String> captions, WorkspaceRole userRole, Workspace workspace) {
        Set<ReportAttachment> reportAttachments = new HashSet<>();
        Set<String> seenReportAttachments = new HashSet<>();
        Path tempLocation = null;

        try {
            //If unbalanced attachments and captions, invalid.
            if (attachments.size() != captions.size()) {
                return null;
            }
            //Validate number of attachments
            if (attachments.size() > (userRole == null ? MAX_ATTACHMENTS_NON_MEMBER : MAX_ATTACHMENTS_MEMBER)) {
                log.info("Too many attachments were provided.");
                return null;
            }
            boolean valid;

            //Create temporary storage location
            tempLocation = createTemporaryAttachmentStorageLocation(workspace);

            for (int i = 0; i < attachments.size(); i++) {
                String caption = captions.get(i).replaceAll("\\s+", " ");
                MultipartFile attachment = attachments.get(i);
                //Check not a duplicate attachment in request.
                String attachmentIdentifier = attachment.getOriginalFilename() + "-" + attachment.getSize();
                if (!seenReportAttachments.add(attachmentIdentifier)) {
                    cleanTemporaryLocation(tempLocation);
                    return null;
                }
                //Validate individual attachment and caption.
                valid = isAttachmentValid(attachment) && reportService.isAttachmentCaptionValid(caption);
                if (!valid) {
                    cleanTemporaryLocation(tempLocation);
                    return null;
                } else {
                    //Store temporarily and get reference.
                    String fileNameReference = storeAttachmentTemporarilyAndGetFileName(tempLocation, attachment);
                    if (fileNameReference == null) {
                        log.info("No temporary location and file name reference could be provided.");
                        cleanTemporaryLocation(tempLocation);
                        return null;
                    }
                    //Add as a report attachment to the return collection.
                    reportAttachments.add(new ReportAttachment(
                        caption,
                        fileNameReference,
                        //This was validated to not null previously.
                        (attachment.getContentType().startsWith("video/") ? FileType.VIDEO : FileType.IMAGE),
                        attachment.getSize()
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed in validateAttachmentsAndCaptionsIntoReportAttachment", e);
            cleanTemporaryLocation(tempLocation);
            return null;
        }
        //Valid if got through all that.
        return new TemporaryReportAttachments(tempLocation, reportAttachments);
    }

    /**
     * Method to validate attachment size and type.
     * @param attachment The attachment to validate
     * @return true if valid, otherwise false.
     */
    public boolean isAttachmentValid(MultipartFile attachment) {
        try {
            String attachmentType = attachment.getContentType();
            String originalFileName = attachment.getOriginalFilename();
            if (attachmentType == null || originalFileName == null) {
                log.info("Attachment type or original file name with extension is null.");
                return false;
            }
            //Validating size and type
            int extensionIndex = originalFileName.lastIndexOf(".");
            if (extensionIndex == -1) {
                log.info("Attachment extension is missing.");
                return false;
            }
            String extension = originalFileName.substring(extensionIndex + 1).toLowerCase();
            if (attachmentType.startsWith("image/")) {
                if (!ACCEPTED_IMAGE_EXTENSIONS.contains(extension)) {
                    log.info("Image attachment was found to be of an unsupported type: {}", extension);
                    return false;
                } else if (attachment.getSize() > MAX_IMAGE_SIZE) {
                    log.info("Maximum image size of {} bytes was exceeded.", MAX_IMAGE_SIZE);
                    return false;
                }
            } else if (attachmentType.startsWith("video/")) {
                if (!ACCEPTED_VIDEO_EXTENSIONS.contains(extension)) {
                    log.info("Video attachment was found to be of an unsupported type: {}", extension);
                    return false;
                } else if (attachment.getSize() > MAX_VIDEO_SIZE) {
                    log.info("Maximum video size of {} bytes was exceeded.", MAX_VIDEO_SIZE);
                    return false;
                }
            } else if (!attachmentType.startsWith("image/") && !attachmentType.startsWith("video/")) {
                log.info("Attachment was found to be of an unsupported type: {}", attachmentType);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed in isAttachmentValid", e);
            return false;
        }
        //Valid if got through
        return true;
    }

    /**
     * Method to create a temporary storage location for attachments of a request.
     * @param workspace The workspace it is relative to.
     * @return the Path of the new temporary storage location.
     */
    public Path createTemporaryAttachmentStorageLocation(Workspace workspace) {

        try {
            //If base directory doesn't exist, create.
            if (!Files.exists(WORKSPACES_UPLOAD_PATH)) {
                Files.createDirectories(WORKSPACES_UPLOAD_PATH);
            }

            //Path object for where we will be storing the attachments temporarily, derived from the base path, and ensuring the directory exists.
            Path attachmentStorageDirectory = WORKSPACES_UPLOAD_PATH
                    .resolve(String.valueOf(workspace.getId()))
                    .resolve("attachments")
                    .resolve("temporary");
            if (!Files.exists(attachmentStorageDirectory)){
                Files.createDirectories(attachmentStorageDirectory);
            }

            //Temporary folder for attachments of this request.
            //Using unique id for temporary folder, with uuid where chance of non-unique is incredibly low, additionally folder is temporary.
            Path temporaryAttachmentStorageDirectory = attachmentStorageDirectory.resolve(String.valueOf(UUID.randomUUID()));
            log.info("Creating temporary attachment location at {}", temporaryAttachmentStorageDirectory);
            Files.createDirectories(temporaryAttachmentStorageDirectory);
            return temporaryAttachmentStorageDirectory;

        } catch (Exception e) {
            log.error("Failed in createTemporaryAttachmentStorageLocation", e);
            return null;
        }
    }

    /**
     * Method to store in a temporary folder in the workspace.
     * This is so that if something goes wrong, it can be emptied easily, otherwise moved into the actual location asynchronously.
     * @param temporaryAttachmentStorageDirectory The path of where to store the attachment temporarily, this is a special location for the request.
     * @param attachment The multipart file attachment
     * @return The reference for the report attachment.
     */
    public String storeAttachmentTemporarilyAndGetFileName(Path temporaryAttachmentStorageDirectory, MultipartFile attachment) {
        try {
            //New file name with timestamp and uuid for guaranteed uniqueness.
            String originalFilename = attachment.getOriginalFilename();
            int extIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf(".");
            String extension = (extIndex == -1) ? "" : originalFilename.substring(extIndex).toLowerCase();
            String newFileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;

            //Store file in temporary folder for now.
            Path tempFilePath = temporaryAttachmentStorageDirectory.resolve(newFileName);
            attachment.transferTo(tempFilePath.toFile());

            //Return new file name.
            return newFileName;

        } catch (Exception e) {
            log.error("Failed in storeAttachmentTemporarilyAndGetFileName", e);
            return null;
        }
    }

    /**
     * Method to move the attachments from the temporary folder to the permanent folder
     * This can be done asynchronously once the report is fully created successfully, since
     * the report attachment object will already contain the reference needed to reach the permanent location.
     * @param source The source path which is the temporary location where the attachments are currently held.
     * @param workspace The workspace this is relative to.
     */
    @Async
    @Transactional
    public void moveAttachmentsFromTemporaryToPermanent(Path source, Workspace workspace) {
        try {
            //If base directory doesn't exist, create.
            if (!Files.exists(WORKSPACES_UPLOAD_PATH)) {
                Files.createDirectories(WORKSPACES_UPLOAD_PATH);
            }
            //Path object for where the attachments will be stored, derived from the base path, and ensuring the directory exists.
            Path destination = WORKSPACES_UPLOAD_PATH
                    .resolve(String.valueOf(workspace.getId()))
                    .resolve("attachments")
                    .resolve("permanent");
            if (!Files.exists(destination)){
                Files.createDirectories(destination);
            }
            //Path object for where the thumbnails will be stored.
            Path thumbnailsDestination = WORKSPACES_UPLOAD_PATH
                    .resolve(String.valueOf(workspace.getId()))
                    .resolve("attachments")
                    .resolve("thumbnails");
            if (!Files.exists(thumbnailsDestination)){
                Files.createDirectories(thumbnailsDestination);
            }

            //Move the attachments within temporary folder to permanent folder.
            Files.walk(source).filter(Files::isRegularFile).forEach(file -> {
                try {
                    log.debug("Moving file {} to permanent storage.", file.getFileName());
                    generateAndSaveThumbnail(thumbnailsDestination, file);
                    Files.move(file, destination.resolve(file.getFileName()), StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            log.error("Failed in moveAttachmentsFromTempToPermanent (asynchronous)", e);
        } finally {
            //Clean up temporary
            cleanTemporaryLocation(source);
        }
    }

    /**
     * Using thumbnailator.io
     * @param thumbnailsDestination
     * @param file
     */
    @Transactional
    public void generateAndSaveThumbnail(Path thumbnailsDestination, Path file) {
        try {
            //Check if attachment needs a thumbnail.
            ReportAttachment reportAttachment = reportAttachmentRepository.findByReference(file.getFileName().toString()).orElse(null);
            if (reportAttachment == null) return;

            //Unique thumbnail file name using timestamp and uuid, and get new filepath for thumbnail.
            String newFileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ".jpg";
            Path destinationFilePath = thumbnailsDestination.resolve(newFileName).normalize();

            if (reportAttachment.getType() == FileType.IMAGE) { //If attachment is an image

                //Generate and store image thumbnail
                //Using BufferedImage to improve quality by avoiding extra compression.
                Thumbnails.of(ImageIO.read(file.toFile()))
                    .size(800, 800)
                    .keepAspectRatio(true)
                    .outputQuality(0.9f)
                    .toFile(destinationFilePath.toFile());
                log.debug("Generated and stored image attachment thumbnail for attachment ID:{}", reportAttachment.getId());

                //Update attachment entity to hold reference to thumbnail too.
                reportAttachmentRepository.updateThumbnailReference(reportAttachment.getId(), newFileName);

            } else if (reportAttachment.getType() == FileType.VIDEO) { //If attachment is a video

                //Extract frame from video
                BufferedImage frame = extractVideoFrame(file);
                if (frame == null) throw new Exception("Could not extract frame from video");

                //Generate and store video thumbnail
                Thumbnails.of(frame)
                    .size(800, 800)
                    .keepAspectRatio(true)
                    .outputQuality(0.9f)
                    .toFile(destinationFilePath.toFile());
                log.debug("Generated and stored video attachment thumbnail for attachment ID:{}", reportAttachment.getId());

                //Update attachment entity to hold reference to thumbnail too.
                reportAttachmentRepository.updateThumbnailReference(reportAttachment.getId(), newFileName);
            }
        } catch (Exception e) {
            log.error("Failed in saveThumbnail(Path thumbnailsDestination, Path file)", e);
        }
    }

    /**
     * Method to extract first key-frame from a video.
     * Useful resources for working with humble video library:
     * <a href="https://stackoverflow.com/questions/67656157/humble-video-take-snapshot-of-given-time">humble-video-take-snapshot-of-given-time</a>,
     * <a href="https://github.com/artclarke/humble-video/blob/master/humble-video-demos/src/main/java/io/humble/video/demos/DecodeAndPlayAudio.java">DecodeAndPlayAudio Demo</a>
     * @return BufferedImage frame
     */
    public BufferedImage extractVideoFrame(Path file) {
        Demuxer demuxer = null;
        try {
            //Use Demuxer to open and stream video file.
            demuxer = Demuxer.make();
            demuxer.open(
                file.toString(),
                null,
                false,
                true,
                null,
                null
            );

            //Find first video stream
            int videoStreamIndex = -1;
            Decoder videoDecoder = null;
            for (int i = 0; i < demuxer.getNumStreams(); i++) {
                DemuxerStream stream = demuxer.getStream(i);
                Decoder decoder = stream.getDecoder();
                if (decoder != null && decoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO) {
                    videoDecoder = decoder;
                    videoStreamIndex = i;
                    break;
                }
            }
            if (videoDecoder == null) throw new IOException("Invalid video file");
            videoDecoder.open(
                null,
                null
            );

            MediaPicture picture = MediaPicture.make(
                videoDecoder.getWidth(),
                videoDecoder.getHeight(),
                videoDecoder.getPixelFormat()
            );
            MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(MediaPictureConverterFactory.HUMBLE_BGR_24, picture);
            MediaPacket packet = MediaPacket.make();
            BufferedImage frame = null;

            //Decode packets until found the first key-frame
            while (demuxer.read(packet) >= 0) {
                if (packet.getStreamIndex() == videoStreamIndex) {
                    videoDecoder.decode(picture, packet, 0);
                    if (picture.isComplete()) {
                        //Found, convert to image.
                        frame = converter.toImage(frame, picture);
                        break;
                    }
                }
            }
            //Return frame.
            return frame;

        } catch (Exception e) {
            log.error("Failed in extractVideoFrame", e);
            return null;

        } finally {
            if (demuxer != null) {
                try { demuxer.close(); }
                catch (Exception e) {
                    log.warn("Error occurred in finally of extractVideoFrame, when trying to close Demuxer", e);
                }
            }
        }
    }

    /**
     * Method to remove the temporary location for the request
     * @param temporaryLocation the temporary location path.
     */
    public void cleanTemporaryLocation(Path temporaryLocation) {
        try {
            if (temporaryLocation != null) {
                //Remove the temporary folder
                FileSystemUtils.deleteRecursively(temporaryLocation);
            }
        } catch (Exception e) {
            log.error("Failed in cleanTemporaryLocation (asynchronous)", e);
        }
    }

    /**
     * Method to fetch attachment as a resource, this takes the reference from the database, and
     * then fetches it from where it's stored.
     * @param attachmentId The attachment ID.
     * @param workspaceId The workspace ID.
     * @return The attachment as a Resource, or null.
     */
    public Resource fetchAttachmentAsResource(Long attachmentId, Long workspaceId) {
        try {
            //Get filename
            ReportAttachment attachment = reportAttachmentRepository.findById(attachmentId).orElse(null);
            if (attachment == null) return null;
            String filename = attachment.getReference();

            //Find attachment resource
            Path filePath = WORKSPACES_UPLOAD_PATH
                .resolve(String.valueOf(workspaceId))
                .resolve("attachments")
                .resolve("permanent")
                .resolve(filename)
                .normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("Attachment file not found: " + filePath);
            }
        } catch (Exception e) {
            log.error("Failed in fetchAttachmentAsResource(Long workspaceId, String filename)", e);
            return null;
        }
    }

    /**
     * Method to fetch attachment thumbnail as a resource, this takes the reference from the database, and
     * then fetches it from where it's stored.
     * @param attachmentId The attachment ID.
     * @param workspaceId The workspace ID.
     * @return The attachment thumbnail as a Resource, or null.
     */
    public Resource fetchAttachmentThumbnailAsResource(Long attachmentId, Long workspaceId) {
        try {
            //Get filename
            ReportAttachment attachment = reportAttachmentRepository.findById(attachmentId).orElse(null);
            if (attachment == null) return null;
            String filename = attachment.getThumbnailReference();

            //Find attachment resource
            Path filePath = WORKSPACES_UPLOAD_PATH
                    .resolve(String.valueOf(workspaceId))
                    .resolve("attachments")
                    .resolve("thumbnails")
                    .resolve(filename)
                    .normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("Attachment thumbnail file not found: " + filePath);
            }
        } catch (Exception e) {
            log.error("Failed in fetchAttachmentThumbnailAsResource(Long attachmentId, Long workspaceId)", e);
            return null;
        }
    }

    /**
     * Method to find page of report attachments of a certain type that belong to reports within a certain container.
     * @param reportContainer The report container to look for attachments under.
     * @param fileType The file type of the attachments to find.
     * @param pageable The pagination configuration.
     * @return A page of report attachment metadata, in ReportAttachmentDTO format.
     * @see ReportAttachmentDTO
     */
    public Page<ReportAttachmentDTO> findPaginatedReportAttachmentsMetadata(ReportContainer reportContainer, FileType fileType, Pageable pageable) {
        return reportAttachmentRepository.findByReportContainerAndFileTypeAsPage(reportContainer.getId(), fileType, pageable).map(ReportAttachmentDTO::fromEntity);
    }

}
