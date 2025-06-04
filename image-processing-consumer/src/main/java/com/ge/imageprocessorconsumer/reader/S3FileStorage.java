package com.ge.imageprocessorconsumer.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Qualifier("s3-file-storage")
public class S3FileStorage implements FileStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileStorage.class);

    private S3Client s3Client;

    private final String bucketName;

    @Autowired
    public S3FileStorage(S3Client s3Client, @Value("${amazon.bucketName}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }


    @Override
    public BufferedImage readImage(String inputFilePath, String inputFileName) throws IOException {
        String key = Paths.get(inputFilePath, inputFileName).toString(); // S3 key
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (ResponseInputStream<?> objectData = s3Client.getObject(getObjectRequest)) {
            return ImageIO.read(objectData);
        } catch (S3Exception e) {
            LOGGER.error("Error reading image file from S3: {}", e.awsErrorDetails().errorMessage());
            throw new IOException("Failed to read image from S3: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error("IO error while reading image: {}", e.getMessage());
            System.err.println("IO error while reading image: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void writeImage(BufferedImage image, String outputFilePath, String outputFileName, String fileFormat) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, fileFormat, baos);
            byte[] imageBytes = baos.toByteArray();
            String outputFileKey = Paths.get(outputFilePath, outputFileName).toString();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(outputFileKey)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentType(getImageMimeType(fileFormat))
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
            LOGGER.info("Image written to S3 successfully: {}", outputFileKey);

        } catch (S3Exception e) {
            LOGGER.error("S3 error writing image: path={}, error={}", outputFilePath, e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Failed to write image to S3: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error("IO error writing image: path={}, error={}", outputFilePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error writing image: path={}, error={}", outputFilePath, e.getMessage(), e);
            throw new IOException("An unexpected error occurred: " + e.getMessage(), e);
        }
    }

    @Override
    public void write(Path path, byte[] data) throws IOException {
        //TODO: Add S3 writing implementations
    }

    private static String getImageMimeType(String fileFormat) {
        return "image/" + fileFormat;
    }
}
