package com.ge.imageprocessorconsumer.processor;

import com.ge.imageprocessorconsumer.reader.FileStorage;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.nio.file.Paths;

@Component
public class GrayscaleProcessor extends AbstractImageProcessor {

    private static final String INPUT_FOLDER_NAME = "resized";
    private static final String OUTPUT_FOLDER_NAME = "grayscaled";

    private static final Logger LOGGER = LoggerFactory.getLogger(GrayscaleProcessor.class);

    private final FileStorage fileStorage;

    @Autowired
    public GrayscaleProcessor(@Qualifier("local-file-storage") FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public ImageProcessingMessage process(ImageProcessingMessage input) throws Exception {
        LOGGER.info("Processing input file for grayscale: {}", input.getImageId());
        String inputFilePath = this.getInPutFilePath(input.getS3Path());
        BufferedImage image
                = fileStorage.readImage(inputFilePath, fileStorage.fileName(input.getImageId(), input.getImageFormat()));
        BufferedImage processedImage = applyGrayscaleFilter(image);
        String outputFile  = getOutPutFilePath(input.getS3Path());
        fileStorage.writeImage(processedImage, outputFile,
                fileStorage.fileName(input.getImageId(), input.getImageFormat()), input.getImageFormat());
        input.setStatus(ProcessStatus.GRAY_SCALED);
        return processNext(input);
    }

    private BufferedImage applyGrayscaleFilter(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(image, grayImage);
        return grayImage;
    }

    @Override
    public String getInPutFilePath(String filePath) {
        return Paths.get(filePath, INPUT_FOLDER_NAME).toString();
    }

    @Override
    public String getOutPutFilePath(String filePath) {
        return Paths.get(filePath, OUTPUT_FOLDER_NAME).toString();
    }
}
