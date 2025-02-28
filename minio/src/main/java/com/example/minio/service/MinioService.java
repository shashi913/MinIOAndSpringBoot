package com.example.minio.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Scheduled(cron = "0 45 14 28 * ?")
    public void removeObjectsMonthly() {
        System.out.println("Monthly task started at " + LocalDateTime.now());
        try {

            if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                removeAllObjects(minioClient);
                System.out.println("Objects in bucket '" + bucketName + "' removed (monthly).");
            } else {
                System.out.println("Bucket '" + bucketName + "' does not exist (monthly).");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeAllObjects(MinioClient minioClient) throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
        List<String> objectNames = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            objectNames.add(item.objectName());
        }

        for (String objectName : objectNames) {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            InputStream fileStream = file.getInputStream();

            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(fileStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return "File uploaded successfully: " + fileName;

        } catch (IOException e) {
            return "Error: Issue with reading the file stream.";
        } catch (MinioException e) {
            return "Error: MinIO Exception - " + e.getMessage();
        } catch (Exception e) {
            return "Error: Unexpected error - " + e.getMessage();
        }
    }

    public InputStream downloadFile(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    public List<String> listFiles() {
        List<String> fileNames = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).build()
            );

            for (Result<Item> result : results) {
                fileNames.add(result.get().objectName());
            }

        } catch (Exception e) {
            fileNames.add("Error: " + e.getMessage());
        }
        return fileNames;
    }

    public String moveFile(String fileName, String targetBucket) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(targetBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
            }

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder().bucket(bucketName).object(fileName).build())
                            .bucket(targetBucket)
                            .object(fileName)
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(fileName).build()
            );

            return "File moved successfully: " + fileName + " → " + targetBucket;

        } catch (Exception e) {
            return "Error moving file: " + e.getMessage();
        }
    }
}
