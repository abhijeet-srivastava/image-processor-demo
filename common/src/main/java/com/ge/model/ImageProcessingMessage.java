package com.ge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

public class ImageProcessingMessage implements Serializable {
    @JsonProperty("image_id")
    private String imageId;

    @JsonProperty("image_format")
    private String imageFormat;

    @JsonProperty("s3_path")
    private String s3Path;
    @JsonProperty("retry_count")
    private int retryCount;

    @JsonProperty("process_status")
    @JsonIgnore
    private ProcessStatus status;

    @JsonProperty("delivery_timestamp")
    private String deliveryTimestamp;

    @JsonProperty("last_process_timestamp")
    private String lastProcessTimestamp;
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getS3Path() {
        return s3Path;
    }

    public void setS3Path(String s3Path) {
        this.s3Path = s3Path;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public String getDeliveryTimestamp() {
        return deliveryTimestamp;
    }

    public void setDeliveryTimestamp(String deliveryTimestamp) {
        this.deliveryTimestamp = deliveryTimestamp;
    }

    public String getLastProcessTimestamp() {
        return lastProcessTimestamp;
    }

    public void setLastProcessTimestamp(String lastProcessTimestamp) {
        this.lastProcessTimestamp = lastProcessTimestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
