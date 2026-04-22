package com.jsh.erp.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.jsh.erp.utils.Tools;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class TenantConfig {

    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
            "jsh_sequence",
            "jsh_function",
            "jsh_platform_config",
            "jsh_tenant",
            "jsh_contract_person"
    ));

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@org.springframework.beans.factory.annotation.Value("${tenant.enabled:true}") boolean tenantEnabled) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (!tenantEnabled) {
            return interceptor;
        }
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = getCurrentTenantId(tenantEnabled);
                return tenantId == 0L ? null : new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return getCurrentTenantId(tenantEnabled) == 0L || IGNORE_TABLES.contains(tableName);
            }
        }));
        return interceptor;
    }

    /**
     * 相当于顶部的：
     * {@code @MapperScan("com.jsh.erp.datasource.mappers*")}
     * 这里可以扩展，比如使用配置文件来配置扫描Mapper的路径
     */
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setBasePackage("com.jsh.erp.datasource.mappers*");
        return scannerConfigurer;
    }

    private Long getCurrentTenantId(boolean tenantEnabled) {
        if (!tenantEnabled) {
            return 0L;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return 0L;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return 0L;
        }
        Long tenantId = Tools.getTenantIdByToken(request.getHeader("X-Access-Token"));
        return tenantId == null ? 0L : tenantId;
    }
}
