package com.ge.imageprocessorconsumer.consumer;


import com.ge.imageprocessorconsumer.exception.ErrorCode;
import com.ge.imageprocessorconsumer.exception.ImageProcessingException;
import com.ge.imageprocessorconsumer.processor.ImageProcessingPipeline;
import com.ge.imageprocessorconsumer.processor.ImageProcessor;
import com.ge.imageprocessorconsumer.reader.FileStorage;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ImageProcessingConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProcessingConsumer.class);
    private static final String SCHEDULED_RETRY_TOPIC = "retry-input-topic";
    private static final String DLQ_PROCESSING_TOPIC = "dlq-input-topic";

    private static final int MAX_RETRIES = 3;

    private static final long RETRY_DELAY_MS = 500L;

    private final KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate;

    private final ImageProcessingPipeline pipeline;

    private final FileStorage fileStorage;

    private final RetryTemplate retryTemplate;
    private final ScheduledExecutorService scheduler;

    private final String baseFilePath;


    @Autowired
    public ImageProcessingConsumer(
            KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate,
            ImageProcessingPipeline pipeline,
            @Qualifier("local-file-storage") FileStorage fileStorage,
            RetryTemplate retryTemplate,
            @Value("${local.baseFilePath}") String rawDirPath
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipeline = pipeline;
        this.retryTemplate = retryTemplate;
        this.fileStorage = fileStorage;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.baseFilePath = rawDirPath;
    }

    /**
     * Processes the given ImageProcessingMessage by invoking the image processing chain and handling any exceptions.
     *
     * @param message The ImageProcessingMessage to be processed.
     */
    @KafkaListener(topics = {"${kafka.topic.name}", "${kafka.retry.topic.name}"}, groupId = "${spring.kafka.consumer.group-id}")
    public void processMessage(ImageProcessingMessage message) {
        try {
            LOGGER.info("Received request to process: {}", message.getImageId());
            processMessageWithChain(message);
            // Execute with retry
            /*retryTemplate.execute(context -> {
                processMessageWithChain(message);
                return null;
            });*/
        } catch (Exception e) {
            LOGGER.error("Error processing message: {}", message.getImageId(), e);
            handleFailure(message, e);
        }
    }

    /**
     * Processes the given ImageProcessingMessage by invoking the image processing chain.
     * It sets the status to START if not provided, checks for image data existence, downloads the image,
     * finds the appropriate processor based on the status, and processes the image.
     *
     * @param message The ImageProcessingMessage to be processed.
     * @throws ImageProcessingException if image data is missing or any processing error occurs.
     * @throws Exception if an unexpected error occurs during processing.
     */
    private void processMessageWithChain(ImageProcessingMessage message) throws Exception {
        ProcessStatus status = Objects.isNull(message.getStatus()) ? ProcessStatus.START: message.getStatus();
        if (message.getImageData() == null || message.getImageData().length == 0) {
            throw new ImageProcessingException(
                    ErrorCode.INVALID_FILE,
                    "No image data in message: " + message.getImageId()
            );
        }
        if (message.getImageFormat() == null) {
            throw new ImageProcessingException(
                    ErrorCode.INVALID_FILE,
                    "No image format in message: " + message.getImageId()
            );
        }
        downloadImageToOutputFolder(status, message.getImageData(), fileStorage.fileName(message.getImageId(), message.getImageFormat()));
        ImageProcessor processor = pipeline.findProcessor(status);
        processor.process(message);
    }

    private void downloadImageToOutputFolder(ProcessStatus status, byte[] imageData, String fileName) {
        if(status != ProcessStatus.START) {
            LOGGER.info("File is in intermediate state of processing, so already downloaded, no need to copy again");
            return;
        }
        Path outputFilePath = Paths.get(baseFilePath, status.getFilePath(), fileName);
        try {
            fileStorage.write(outputFilePath, imageData);
            LOGGER.info("Image downloaded to output folder: {}", outputFilePath);
        } catch (IOException e) {
            throw new ImageProcessingException(
                    ErrorCode.IO_ERROR,
                    "Error downloading image to output folder: " + outputFilePath,
                    e
            );
        }
    }


    private void handleFailure(ImageProcessingMessage message, Exception e) {
        if (shouldRetry(message, e)) {
            message.incrementRetryCount();
            scheduler.schedule(() -> {
                kafkaTemplate.send(SCHEDULED_RETRY_TOPIC, message.getImageId(), message);
                LOGGER.info("Sent to retry topic after {}ms delay: {}", RETRY_DELAY_MS, SCHEDULED_RETRY_TOPIC);
            }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        } else {
            kafkaTemplate.send(DLQ_PROCESSING_TOPIC, message.getImageId(), message);
            LOGGER.info("Sent to DLQ topic: {}", DLQ_PROCESSING_TOPIC);
        }
    }

    private boolean shouldRetry(ImageProcessingMessage message, Exception e) {
        if (message.getRetryCount() >= MAX_RETRIES) {
            return false;
        }
        // Optionally, skip retries for certain errors
        if (e instanceof ImageProcessingException ipe) {
            return ipe.getErrorCode() == ErrorCode.PROCESSING_FAILURE ||
                    ipe.getErrorCode() == ErrorCode.IO_ERROR;
        }
        return true;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
