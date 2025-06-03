package com.ge.imageprocessorconsumer.accessor;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class S3Accessor {

    @Value("${amazon.accessKey}")
    private String accessKey;

    @Value("${amazon.secretKey}")
    private String secretKey;

    @Bean
    public S3Client amazonS3() {
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().profileName("default").build();
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
    }

}
