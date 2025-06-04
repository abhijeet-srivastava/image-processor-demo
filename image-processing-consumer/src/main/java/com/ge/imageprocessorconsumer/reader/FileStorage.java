package com.ge.imageprocessorconsumer.reader;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public interface FileStorage {

    BufferedImage readImage(String inputFilePath, String inputFileName) throws IOException;
    void writeImage(BufferedImage image, String outputFilePath, String outputFileName, String fileFormat) throws IOException;

    void write(Path path, byte[] data) throws IOException;

    default String fileName(String fileName, String fileFormat) {
        return fileName + "." + fileFormat;
    }
}
