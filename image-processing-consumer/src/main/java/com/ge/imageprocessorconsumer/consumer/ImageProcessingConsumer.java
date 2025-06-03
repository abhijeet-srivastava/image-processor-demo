package com.ge.imageprocessorconsumer.consumer;


import com.ge.imageprocessorconsumer.exception.ErrorCode;
import com.ge.imageprocessorconsumer.exception.ImageProcessingException;
import com.ge.imageprocessorconsumer.processor.ImageProcessingPipeline;
import com.ge.imageprocessorconsumer.processor.ImageProcessor;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

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

    private final RetryTemplate retryTemplate;
    private final ScheduledExecutorService scheduler;


    @Autowired
    public ImageProcessingConsumer(
            KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate,
            ImageProcessingPipeline pipeline,
            RetryTemplate retryTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipeline = pipeline;
        this.retryTemplate = retryTemplate;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

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

    private void processMessageWithChain(ImageProcessingMessage message) throws Exception {
        ProcessStatus status = message.getStatus();
        ImageProcessor processor = pipeline.findProcessor(status);
        processor.process(message);
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
