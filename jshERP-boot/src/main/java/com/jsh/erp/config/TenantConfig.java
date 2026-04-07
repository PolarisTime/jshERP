package com.jsh.erp.config;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.jsh.erp.utils.JwtUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * MyBatis-Plus 3.5.x 配置
 * - TenantLineInnerInterceptor  替代旧 TenantSqlParser
 * - PaginationInnerInterceptor  替代旧 PaginationInterceptor
 * - 方法级别租户忽略改用 @InterceptorIgnore(tenantLine = "true") 注解
 */
@Service
public class TenantConfig {

    /**
     * MyBatis-Plus 插件主入口
     * 注意：TenantLine 必须放在 Pagination 之前
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(HttpServletRequest request) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // ── 1. 多租户行级隔离 ──────────────────────────────────────
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            @Override
            public Expression getTenantId() {
                Long tenantId = JwtUtil.getTenantIdFromRequest(request);
                if (tenantId == null || tenantId == 0L) {
                    return new NullValue();
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                Long tenantId = JwtUtil.getTenantIdFromRequest(request);
                if (tenantId == null || tenantId == 0L) {
                    return true;
                }
                return "jsh_sequence".equals(tableName)
                    || "jsh_function".equals(tableName)
                    || "jsh_platform_config".equals(tableName)
                    || "jsh_tenant".equals(tableName)
                    || "jsh_customer_statement_item".equals(tableName)
                    || "jsh_sys_dict_data".equals(tableName)
                    || "jsh_sys_dict_type".equals(tableName);
            }
        }));

        // ── 2. 分页插件 ───────────────────────────────────────────
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        return interceptor;
    }

    /**
     * Mapper 包扫描（等同于 @MapperScan）
     */
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setBasePackage("com.jsh.erp.datasource.mappers*");
        return scannerConfigurer;
    }
}
