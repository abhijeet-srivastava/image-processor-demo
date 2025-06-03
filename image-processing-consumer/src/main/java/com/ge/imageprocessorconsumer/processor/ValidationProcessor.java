package com.ge.imageprocessorconsumer.processor;

import com.ge.imageprocessorconsumer.exception.ErrorCode;
import com.ge.imageprocessorconsumer.exception.ImageProcessingException;
import com.ge.imageprocessorconsumer.reader.FileStorage;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

@Component
public class ValidationProcessor extends AbstractImageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProcessor.class);

    private static final String INPUT_FOLDER_NAME = "raw";

    private final FileStorage fileStorage;

    @Autowired
    public ValidationProcessor(@Qualifier("local-file-storage")  FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public ImageProcessingMessage process(ImageProcessingMessage input) throws Exception {
        LOGGER.info("Validating input image file and metadata: {}", input.getImageId());
        if(!validFormat(input.getImageFormat())) {
            throw new ImageProcessingException(ErrorCode.INVALID_FILE, "Invalid image format. Supported formats are PNG and JPG");
        }
        String inputFilePath = this.getInPutFilePath(input.getS3Path());
        try {
            BufferedImage inputImage
                    = fileStorage.readImage(inputFilePath, fileStorage.fileName(input.getImageId(), input.getImageFormat()));
            if (inputImage.getWidth() > 2000 || inputImage.getHeight() > 2000
                    || inputImage.getWidth() < 10 || inputImage.getHeight() < 10) {
                LOGGER.error("Invalid image dimensions: Height {}, Width : {}", inputImage.getHeight(), inputImage.getWidth());
                throw new ImageProcessingException(ErrorCode.PROCESSING_FAILURE, "Image dimensions are too large or too small for resizing. Width should be 10 - 5000 and Height should be 10 - 5000");
            }
        } catch (IOException e) {
            LOGGER.error("Image format not supported or failed to load/write image {}", e.getMessage(), e);
            throw new ImageProcessingException(ErrorCode.IO_ERROR, "Invalid file format");
        }
        input.setStatus(ProcessStatus.VALIDATED);
        return processNext(input);
    }

    private boolean validFormat(String imageFormat) {
        return "png".equals(imageFormat) || "jpg".equals(imageFormat);
    }

    @Override
    public String getInPutFilePath(String filePath) {
        return Paths.get(filePath, INPUT_FOLDER_NAME).toString();
    }

    @Override
    public String getOutPutFilePath(String filePath) {
        return null;
    }
}
