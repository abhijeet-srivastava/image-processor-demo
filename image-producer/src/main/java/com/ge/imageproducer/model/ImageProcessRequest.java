package com.ge.imageproducer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageProcessRequest {
    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_format")
    private String fileFormat;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
}
