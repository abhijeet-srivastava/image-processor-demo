package com.ge.imageprocessorconsumer.consumer;

import com.ge.imageprocessorconsumer.processor.ImageProcessingPipeline;
import com.ge.imageprocessorconsumer.processor.ImageProcessor;
import com.ge.imageprocessorconsumer.reader.FileStorage;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageProcessingConsumerTest {

    @InjectMocks
    private ImageProcessingConsumer consumer;

    @Mock
    private KafkaTemplate<String, ImageProcessingMessage> kafkaTemplate;

    @Mock
    private ImageProcessingPipeline pipeline;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private ImageProcessor imageProcessor;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private FileStorage fileWriter;

    @TempDir
    File tempDir; // Temporary directory for baseFilePath

    private ImageProcessingMessage message;

    @BeforeEach
    void setUp() {
        // Initialize test data
        message = new ImageProcessingMessage();
        message.setImageId("TestImageId");
        message.setImageData(new byte[]{0x12, 0x13, 0x14});
        message.setImageFormat("jpg");
        message.setStatus(ProcessStatus.START);

        // Initialize consumer with tempDir as baseFilePath
        consumer = new ImageProcessingConsumer(kafkaTemplate, pipeline, fileWriter, retryTemplate, tempDir.getAbsolutePath());
    }

    @Test
    void testProcessMessageIntermediateStatusSkipsDownload() throws Exception {
        // Arrange
        message.setStatus(ProcessStatus.VALIDATED);
        when(pipeline.findProcessor(eq(ProcessStatus.VALIDATED))).thenReturn(imageProcessor);
        when(imageProcessor.process(any())).thenReturn(message);

        // Act
        consumer.processMessage(message);

        // Assert
        verify(imageProcessor, times(1)).process(message);
        verify(fileWriter, never()).write(any(), any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}