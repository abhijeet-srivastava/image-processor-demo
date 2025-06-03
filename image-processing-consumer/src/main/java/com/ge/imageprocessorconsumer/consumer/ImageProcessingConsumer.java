package com.ge.imageprocessorconsumer.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.imageprocessorconsumer.processor.ImageProcessingPipeline;
import com.ge.imageprocessorconsumer.processor.ImageProcessor;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ImageProcessingConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProcessingConsumer.class);
    private static final String SCHEDULED_RETRY_TOPIC = "image.retry.scheduled";
    private static final String DLT_TOPIC = "image.dlt";

    @Autowired
    private final KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageProcessingPipeline pipeline;


    public ImageProcessingConsumer(KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = {"${kafka.topic.name}", "${kafka.retry.topic.name}"}, groupId = "${spring.kafka.consumer.group-id}")
    public void processMessage(ImageProcessingMessage message) {
        try {
            processMessageWithChain(message);
        } catch (Exception e) {
            LOGGER.error("Error deserializing message: {}", e.getMessage(), e);
        }
    }

    private void processMessageWithChain(ImageProcessingMessage message) throws Exception {
        ProcessStatus status = message.getStatus();
        ImageProcessor processor = pipeline.findProcessor(status);
        processor.process(message);
    }

}
