package com.jsh.erp.config;

import com.jsh.erp.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * JWT 配置：启动时将 jwt.secret 注入 JwtUtil
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        JwtUtil.setSecret(jwtSecret);
    }
}
