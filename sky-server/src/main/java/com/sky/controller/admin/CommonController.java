package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


@RestController
@RequestMapping("admin/common")
@Tag(name = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private MinioUtil minioUtil;
    @PostMapping("upload")
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file.getOriginalFilename());
        try {
            //原始文件名
            String filename = file.getOriginalFilename();
            //截取原始文件名的扩展名
            String extension = filename.substring(filename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            String filePath = minioUtil.upload("dish-thumbnail",objectName, file);
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }

    }
}
