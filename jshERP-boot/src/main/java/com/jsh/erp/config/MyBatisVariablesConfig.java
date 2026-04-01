package com.jsh.erp.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 将业务配置注入 MyBatis 全局变量，供 mapper XML 中通过 ${variableName} 引用。
 * 避免在多个 SQL 文件中硬编码业务常量。
 */
@Configuration
public class MyBatisVariablesConfig {

    /**
     * 重量可编辑的商品类别ID列表（逗号分隔），如盘螺(210)、线材(220)。
     * 这些类别在出入库时允许手动输入实际过磅重量，优先于理论重量。
     */
    @Value("${erp.weight-editable-category-ids:210,220}")
    private String weightEditableCategoryIds;

    @Bean
    public ConfigurationCustomizer mybatisVariablesCustomizer() {
        return configuration -> {
            Properties vars = configuration.getVariables();
            if (vars == null) {
                vars = new Properties();
            }
            vars.setProperty("weightEditableCategoryIds", weightEditableCategoryIds);
            configuration.setVariables(vars);
        };
    }
}
