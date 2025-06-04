package com.ge.model;

public enum ProcessStatus {
    START("raw"),
    VALIDATED("raw"),
    RESIZED("resized"),
    GRAY_SCALED("grayscaled");

    private String filePath;

    ProcessStatus(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
