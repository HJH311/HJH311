package com.sky.utils;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioUtil {
    private final MinioClient minioClient;
    /**
     * 创建桶
     */
    @SneakyThrows//编译时忽略异常
    public void  createBucket(String bucketName){
        boolean exists=minioClient.bucketExists(BucketExistsArgs
                .builder()
                .bucket(bucketName)
                .build());
        if (!exists){
            minioClient.makeBucket(MakeBucketArgs
                    .builder()
                    .bucket(bucketName)
                    .build());
        }
    }
    /**
     * 删除空桶
     */
    @SneakyThrows
    public void deleteBucket(String bucketName){
        boolean exists=minioClient.bucketExists(BucketExistsArgs
                .builder()
                .bucket(bucketName)
                .build());
        if (exists) minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }
    /**
     * 上传文件
     * 接收 MultipartFile
     */
    @SneakyThrows
    public String upload(String bucketName, String objectName, MultipartFile file){
        try( InputStream in=file.getInputStream()){
            PutObjectArgs args= PutObjectArgs
                    .builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(args);
        }
        String URL = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .method(io.minio.http.Method.GET)//指定用get方法访问
                        .expiry(7, TimeUnit.DAYS)  // 7天
                        .build());
        return URL;
    }
    /**
     * 判断文件是否存在
     */
    @SneakyThrows
    public boolean exist(String bucketName, String objectName){
        minioClient.statObject(
                //不抛异常，返回True
                StatObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()

        );
        return true;
    }
    /**
     * 删除文件
     */
    @SneakyThrows
    public void delete(String bucketName, String objectName){
        log.info("删除文件：{}",objectName);
        if (exist(bucketName,objectName)) {
            minioClient.removeObject(
                    RemoveObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        }

    }
}
