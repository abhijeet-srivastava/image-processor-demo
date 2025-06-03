package com.ge.imageprocessorconsumer.processor;

import com.ge.model.ImageProcessingMessage;

import java.awt.image.BufferedImage;

public interface ImageProcessor {
    void setNext(ImageProcessor next);
    ImageProcessingMessage process(ImageProcessingMessage input) throws Exception;

    String getInPutFilePath(String filePath);
    String getOutPutFilePath(String filePath);
}
