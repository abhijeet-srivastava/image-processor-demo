package com.ge.imageprocessorconsumer.reader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Qualifier("local-file-storage")
public class LocalFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorage.class);


    @Override
    public BufferedImage readImage(String inputFilePath, String inputFileName) throws IOException {
        if (inputFilePath == null || inputFileName == null) {
            LOGGER.error("Invalid input: inputFilePath={}  or inputFileName: {} is null", inputFilePath, inputFileName);
            throw new IllegalArgumentException("Invalid input file");
        }
        File imageFile = new File(Paths.get(inputFilePath, inputFileName).toString());

        if (!imageFile.exists() || !imageFile.isFile()) {
            LOGGER.error("Image file not found or invalid: path={}", imageFile.getName());
            throw new IOException("Image file not found: " + imageFile.getName());
        }

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                LOGGER.error("Failed to read image: path={}", imageFile.getName());
                throw new IOException("Invalid image format: " + imageFile.getName());
            }
            LOGGER.info("Successfully read image: path={}", imageFile.getName());
            return image;
        } catch (IOException e) {
            LOGGER.error("Error reading image: path={}, error={}", imageFile.getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void writeImage(BufferedImage image, String outputFilePath, String outputFileName, String fileFormat) throws IOException {
        try {
            File file = new File(Paths.get(outputFilePath, outputFileName).toString());
            file.getParentFile().mkdirs();
            ImageIO.write(image, fileFormat, file);
        } catch (IOException e) {
            LOGGER.error("Error writing image: path={}, error={}", outputFilePath, e.getMessage());
            throw e;
        }
    }

    @Override
    public void write(Path path, byte[] data) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }

}
