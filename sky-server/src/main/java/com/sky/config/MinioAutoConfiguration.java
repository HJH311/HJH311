package com.sky.config;

import com.sky.properties.MinioProperties;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioAutoConfiguration {
    private final MinioProperties prop;
    public MinioAutoConfiguration(MinioProperties prop) { this.prop = prop; }

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient() {
        log.info(">>>> endpoint={}, bucketName={}", prop.getEndpoint(), prop.getBucketName());
        return MinioClient.builder()
                .endpoint(prop.getEndpoint())
                .credentials(prop.getAccessKey(), prop.getSecretKey())
                .build();
    }
}