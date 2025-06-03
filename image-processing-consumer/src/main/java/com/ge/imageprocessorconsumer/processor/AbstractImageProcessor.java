package com.ge.imageprocessorconsumer.processor;

import com.ge.model.ImageProcessingMessage;

import java.awt.image.BufferedImage;

public abstract class AbstractImageProcessor implements ImageProcessor {
    protected ImageProcessor next;

    @Override
    public void setNext(ImageProcessor next) {
        this.next = next;
    }

    protected ImageProcessingMessage processNext(ImageProcessingMessage input) throws Exception {
        return next != null ? next.process(input) : input;
    }
}
