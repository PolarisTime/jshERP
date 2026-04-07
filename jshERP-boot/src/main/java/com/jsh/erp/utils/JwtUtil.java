package com.jsh.erp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 签发 access_token，包含 userId 和 tenantId
 */
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /** 签名密钥，由 Spring 容器启动时注入 */
    private static String secret;

    /** 签名密钥对象 */
    private static SecretKey secretKey;

    /** access_token 有效期：24小时 */
    private static final long ACCESS_TOKEN_EXPIRE = 24 * 60 * 60 * 1000L;

    /** refresh_token 有效期：7天 */
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    /** Redis 黑名单 key 前缀 */
    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 由 Spring 配置类在启动时调用，注入签名密钥
     */
    public static void setSecret(String secretKey) {
        if (secretKey == null || secretKey.trim().length() < 32) {
            throw new IllegalArgumentException("jwt.secret 长度至少 32 个字符");
        }
        secret = secretKey.trim();
        JwtUtil.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从请求中提取 token，支持 Authorization: Bearer 和 X-Access-Token
     */
    public static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        String xToken = request.getHeader("X-Access-Token");
        if (xToken != null && !xToken.isEmpty()) {
            return xToken;
        }
        return null;
    }

    /**
     * 判断是否是旧格式 token（不包含 "." 的短字符串）
     * 仅作快速过滤，安全判断应使用 parseToken 是否返回 null
     */
    public static boolean isLegacyToken(String token) {
        if (token == null) return true;
        // JWT 格式: header.payload.signature，包含两个 "."
        // 旧格式: UUID_tenantId，不包含 "."
        return !token.contains(".");
    }

    /**
     * 生成 access_token
     */
    public static String generateAccessToken(Long userId, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId != null ? tenantId : 0L);
        claims.put("type", "access");
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 refresh_token
     */
    public static String generateRefreshToken(Long userId, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId != null ? tenantId : 0L);
        claims.put("type", "refresh");
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token，返回 Claims；过期或签名无效返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT已过期: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 token（即使已过期也返回 Claims），仅签名无效返回 null。
     * 用于 logout 等需要从过期 token 中提取信息的场景。
     */
    public static Claims parseTokenAllowExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 过期但签名有效，返回 claims
            return e.getClaims();
        } catch (Exception e) {
            logger.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 token 中获取 userId
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object userId = claims.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /**
     * 从 token 中获取 tenantId
     */
    public static Long getTenantId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return 0L;
        Object tenantId = claims.get("tenantId");
        return tenantId != null ? Long.valueOf(tenantId.toString()) : 0L;
    }

    /**
     * 从 token 中获取过期时间
     */
    public static Date getExpiration(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.getExpiration();
    }

    /**
     * 对 token 做 SHA-256 摘要，用于黑名单 key（减少 Redis 内存占用）
     */
    public static String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 不应发生，回退到原始 token
            logger.warn("SHA-256 不可用，回退使用原始 token 作为 key", e);
            return token;
        }
    }

    /**
     * 获取黑名单 Redis key
     */
    public static String getBlacklistKey(String token) {
        return BLACKLIST_PREFIX + hashToken(token);
    }

    /**
     * 从请求中提取 tenantId，支持 JWT 和旧格式 token
     */
    public static Long getTenantIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) return 0L;
        if (!isLegacyToken(token)) {
            return getTenantId(token);
        }
        return Tools.getTenantIdByToken(token);
    }
}
