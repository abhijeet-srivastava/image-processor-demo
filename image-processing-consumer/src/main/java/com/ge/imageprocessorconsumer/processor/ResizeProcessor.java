package com.ge.imageprocessorconsumer.processor;

import com.ge.imageprocessorconsumer.reader.FileStorage;
import com.ge.model.ImageProcessingMessage;
import com.ge.model.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;


@Component
public class ResizeProcessor extends AbstractImageProcessor {

    private static final String INPUT_FOLDER_NAME = "raw";
    private static final String OUTPUT_FOLDER_NAME = "resized";

    private static final Integer SCALED_WIDTH = 1000;
    private static final Integer SCALED_HEIGHT = 1000;


    private static final Logger LOGGER = LoggerFactory.getLogger(ResizeProcessor.class);

    private final FileStorage fileStorage;

    @Autowired
    public ResizeProcessor(@Qualifier("local-file-storage") FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public ImageProcessingMessage process(ImageProcessingMessage input) throws Exception {
        LOGGER.info("Processing input file for resize: {}", input.getImageId());
        String inputFilePath = this.getInPutFilePath(input.getS3Path());
        BufferedImage inputImage
                = fileStorage.readImage(inputFilePath, fileStorage.fileName(input.getImageId(), input.getImageFormat()));
        BufferedImage resized = resize(inputImage);
        String outputFilePath = getOutPutFilePath(input.getS3Path());
        fileStorage.writeImage(resized, outputFilePath,
                fileStorage.fileName(input.getImageId(), input.getImageFormat()), input.getImageFormat());
        input.setStatus(ProcessStatus.RESIZED);
        return processNext(input);
    }

    private BufferedImage resize(BufferedImage img) {
        BufferedImage resizedImage = new BufferedImage(SCALED_WIDTH, SCALED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(resizedImage, 0, 0, null);
        g2d.dispose();
        return img;
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
