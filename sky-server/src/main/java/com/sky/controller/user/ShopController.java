package com.sky.controller.user;


import com.sky.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "店铺状态相关接口")
@Slf4j
public class ShopController {
    private static final String KEY = "SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;
    @GetMapping("/status")
    @Operation(summary = "获取店铺状态")
    public Result<Integer> getStatus() {
        log.info("获取店铺状态中！！！");
        ValueOperations ops = redisTemplate.opsForValue();
        Integer status = (Integer) ops.get(KEY);
        log.info("获取店铺状态为：{}", status == null ? "未设置(默认打烊)" : (status == 1 ? "营业中" : "打烊中"));
        return Result.success(status);
    }
}
