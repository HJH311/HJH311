package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@Slf4j
//@ComponentScan(basePackages = {"com.sky.server", "com.sky.common", "com.sky.pojo"})
public class SkyApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx =
                SpringApplication.run(SkyApplication.class, args);
        // 只打印我们关心的两个
        log.info(">>>> MinioAutoConfig: {}", ctx.containsBean("minioAutoConfiguration"));
        log.info(">>>> MinioUtil: {}", ctx.containsBean("minioUtil"));
        log.info("server started");
    }
}
