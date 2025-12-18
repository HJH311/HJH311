package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    // 使用足够长的密钥（至少32个字符/256位）
    private static final String SECRET_KEY = "sky-take-out-project-2025-jwt-secret-key-for-sky-takeout";

    // 生成安全的密钥
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            "bXktc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmcta2V5LTIwMjU=" // 或者直接用字符串
    ));

    // 或者使用更简单的方式
    private static final SecretKey SIMPLE_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成JWT令牌
     */
    public static String createJWT(String subject, Long ttlMillis, Map<String, Object> claims) {
        // jjwt 0.12.5+ 的新API
        var builder = Jwts.builder()
                // 添加自定义Claims,id
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .signWith(SIMPLE_KEY, Jwts.SIG.HS256);

        if (ttlMillis != null && ttlMillis > 0) {
            Date exp = new Date(System.currentTimeMillis() + ttlMillis);
            builder.expiration(exp);
        }

        return builder.compact();
    }

    /**
     * 解析JWT令牌
     */
    public static Claims parseJWT(String jwt) {
        return Jwts.parser()
                .verifyWith(SIMPLE_KEY)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /**
     * 验证JWT令牌
     */
    public static boolean validateToken(String token) {
        try {
            parseJWT(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}