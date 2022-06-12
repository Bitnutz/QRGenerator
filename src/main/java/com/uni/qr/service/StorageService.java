package com.uni.qr.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.uni.qr.model.URL;
import com.uni.qr.repository.UrlRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.apache.http.entity.ContentType.*;

@Service
public class StorageService {
    @Value("${s3.bucket.name}")
    private String bucketName;

    private static final Logger logger = LogManager.getLogger(StorageService.class);

    @Autowired
    private AmazonS3 client;

    @Autowired
    private UrlRepository urlRepository;

    public void upload(String path,String fileName,
                       Optional<Map<String, String>> optionalMetaData,
                       InputStream inputStream) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        optionalMetaData.ifPresent(map -> {
            if (!map.isEmpty()) {
                map.forEach(objectMetadata::addUserMetadata);
            }
        });
        try {
            client.putObject(path, fileName, inputStream, objectMetadata);
        } catch (AmazonServiceException e) {
            throw new IllegalStateException("Failed to upload the file", e);
        }
    }


    public void uploadFile(MultipartFile file) {
        File singleFile = convertMultipartFile(file);
        String fileName = "temp_"+System.currentTimeMillis();
        client.putObject(new PutObjectRequest(bucketName, fileName, singleFile));

        logger.info("File "+fileName+" was uploaded successfully!");
        singleFile.delete();
        logger.info("Deletion of the temp file was successful!");
    }

    public byte[] downloadFile(String fileName) {
        S3Object s3Object = client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();

        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            logger.fatal("Couldn't convert input stream to byte array: ", e);
        }
        return null;
    }

    public void deleteFile(String fileName) {
        client.deleteObject(bucketName, fileName);
        logger.info("The file + "+ fileName+ " was successfully deleted from the S3 bucket!");
    }

    private File convertMultipartFile(MultipartFile file) {
        File result = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(result)) {
            fileOutputStream.write(file.getBytes());
        } catch (IOException e) {
            logger.error("Failed to convert Multipart file to file: ", e);
        }
        return result;
    }
}
