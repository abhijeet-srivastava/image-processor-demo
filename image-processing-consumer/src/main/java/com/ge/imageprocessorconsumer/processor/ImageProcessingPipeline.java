package com.ge.imageprocessorconsumer.processor;


import com.ge.model.ProcessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;


@Component
public class ImageProcessingPipeline {
    private EnumMap<ProcessStatus, ImageProcessor> processorChain;


    @Autowired
    public ImageProcessingPipeline(ResizeProcessor resizeProcessor,
                                   GrayscaleProcessor grayscaleProcessor) {
        this.processorChain = new EnumMap<>(ProcessStatus.class);
        processorChain.put(ProcessStatus.START, resizeProcessor);
        processorChain.put(ProcessStatus.RESIZED, grayscaleProcessor);
        resizeProcessor.setNext(grayscaleProcessor);
    }

    public ImageProcessor findProcessor(ProcessStatus currStatus) {
        return processorChain.getOrDefault(currStatus, processorChain.get(ProcessStatus.START));
    }
}
