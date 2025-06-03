package com.ge.imageproducer.controller;


import com.ge.imageproducer.model.ImageProcessRequest;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/images")
public class ImageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageController.class);


    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    @Autowired
    private KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topic;

    @Value("${local.baseFilePath}")
    private String basePath;

    @PostMapping
    public ResponseEntity<String> submitImage(@RequestBody ImageProcessRequest msg) {
        LOGGER.info("Received message: {} and FileFormat: {}", msg.getFileName(), msg.getFileFormat());
        String imageId = UUID.randomUUID().toString();
        String newFileName = imageId + "." + msg.getFileFormat();

        try {
            copyImage("/input", msg.getFileName(), "/raw", newFileName);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Invalid Input file");
        }
        ImageProcessingMessage inputMsg = new ImageProcessingMessage();
        inputMsg.setImageId(imageId);
        inputMsg.setS3Path(basePath);
        inputMsg.setImageFormat(msg.getFileFormat());
        inputMsg.setStatus(ProcessStatus.START);
        inputMsg.setRetryCount(0);
        String timestamp = currentTimeStamp();
        inputMsg.setDeliveryTimestamp(timestamp);
        kafkaTemplate.send(topic, inputMsg.getImageId(), inputMsg);
        return ResponseEntity.ok("{\"message\":\"Message Sent\"}");
    }

    private String currentTimeStamp() {
        ZonedDateTime nowUtcZoned = ZonedDateTime.now(ZoneOffset.UTC);
        return nowUtcZoned.format(formatter);
    }

    /**
     * Copies an image file from a source location to a destination location,
     * optionally changing its name.
     *
     * @param sourceFolderPath     The path to the folder containing the source image.
     * @param sourceFileName       The name of the source image file.
     * @param destinationFolderPath The path to the folder where the image will be copied.
     * @param newFileName          The new name for the copied image file.
     * @throws IOException If an I/O error occurs during the copy operation.
     */
    public void copyImage(String sourceFolderPath, String sourceFileName,
                          String destinationFolderPath, String newFileName) throws IOException {

        // Construct the full path for the source file
        Path sourcePath = Paths.get(sourceFolderPath, sourceFileName);

        // Construct the full path for the destination file
        Path destinationPath = Paths.get(destinationFolderPath, newFileName);

        // Ensure the source file exists
        if (!Files.exists(sourcePath)) {
            LOGGER.error("Source file does not exist: {}", sourcePath.toAbsolutePath());
            throw new IOException("Source file does not exist: " + sourcePath.toAbsolutePath());
        }

        // Ensure the destination directory exists, create if not
        if (!Files.exists(destinationPath.getParent())) {
            LOGGER.info("Destination directory does not exist. Creating: {}", destinationPath.getParent().toAbsolutePath());
            Files.createDirectories(destinationPath.getParent());
        }

        try {
            // Copy the file from source to destination
            // StandardCopyOption.REPLACE_EXISTING will overwrite the destination file if it already exists.
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Image copied successfully from {}, to: {}, in {} ",
                    sourcePath.getFileName(), destinationPath.getFileName(),destinationPath.getParent().toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error copying image: {}", e.getMessage(), e);
            throw e;
        }
    }
}
