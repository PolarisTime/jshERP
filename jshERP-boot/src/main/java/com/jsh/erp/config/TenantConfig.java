package com.jsh.erp.config;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.ISqlParserFilter;
import com.baomidou.mybatisplus.core.parser.SqlParserHelper;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PerformanceInterceptor;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantHandler;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantSqlParser;
import com.jsh.erp.utils.Tools;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TenantConfig {

    private static final Set<String> TENANT_SQL_PARSER_FILTER_IDS = new HashSet<>(Arrays.asList(
            "com.jsh.erp.datasource.mappers.UserMapperEx.getUserByWeixinOpenId",
            "com.jsh.erp.datasource.mappers.UserMapperEx.updateUserWithWeixinOpenId",
            "com.jsh.erp.datasource.mappers.UserMapperEx.getUserListByUserNameOrLoginName",
            "com.jsh.erp.datasource.mappers.UserMapperEx.disableUserByLimit",
            "com.jsh.erp.datasource.mappers.RoleMapperEx.getRoleWithoutTenant",
            "com.jsh.erp.datasource.mappers.LogMapperEx.insertLogWithUserId",
            "com.jsh.erp.datasource.mappers.UserBusinessMapperEx.getBasicDataByKeyIdAndType",
            "com.jsh.erp.datasource.mappers.AccountMapperEx.findAccountInOutList",
            "com.jsh.erp.datasource.mappers.AccountMapperEx.findAccountInOutListCount",
            "com.jsh.erp.datasource.mappers.AccountHeadMapperEx.getDetailByNumber",
            "com.jsh.erp.datasource.mappers.AccountHeadMapperEx.getFinancialBillNoByBillId",
            "com.jsh.erp.datasource.mappers.DepotItemMapperEx.findDetailByDepotIdsAndMaterialIdList",
            "com.jsh.erp.datasource.mappers.DepotItemMapperEx.findDetailByDepotIdsAndMaterialIdCount",
            "com.jsh.erp.datasource.mappers.DepotItemMapperEx.getListWithBuyOrSale",
            "com.jsh.erp.datasource.mappers.DepotItemMapperEx.getListWithBuyOrSaleCount",
            "com.jsh.erp.datasource.mappers.DepotItemMapperEx.buyOrSalePriceTotal",
            "com.jsh.erp.datasource.mappers.UserMapperEx.getNextNodeTree",
            "com.jsh.erp.datasource.mappers.DepotHeadMapperEx.findMaterialsListMapByHeaderIdList",
            "com.jsh.erp.datasource.mappers.DepotHeadMapperEx.getTotalWeightListByHeaderIdList",
            "com.jsh.erp.datasource.mappers.DepotHeadMapperEx.getDetailByNumber",
            "com.jsh.erp.datasource.mappers.DepotHeadMapperEx.getReferencedByNumbersMap"
    ));

    @Bean
    public PaginationInterceptor paginationInterceptor(HttpServletRequest request) {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        List<ISqlParser> sqlParserList = new ArrayList<>();
        TenantSqlParser tenantSqlParser = new TenantSqlParser();
        tenantSqlParser.setTenantHandler(new TenantHandler() {
            @Override
            public Expression getTenantId() {
                String token = request.getHeader("X-Access-Token");
                Long tenantId = Tools.getTenantIdByToken(token);
                if (tenantId!=0L) {
                    return new LongValue(tenantId);
                } else {
                    //超管
                    return null;
                }
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean doTableFilter(String tableName) {
                //获取开启状态
                Boolean res = true;
                String token = request.getHeader("X-Access-Token");
                Long tenantId = Tools.getTenantIdByToken(token);
                if (tenantId!=0L) {
                    // 这里可以判断是否过滤表
                    if ("jsh_sequence".equals(tableName) || "jsh_function".equals(tableName)
                            || "jsh_platform_config".equals(tableName) || "jsh_tenant".equals(tableName)
                            || "jsh_contract_person".equals(tableName)) {
                        res = true;
                    } else {
                        res = false;
                    }
                }
                return res;
            }
        });

        sqlParserList.add(tenantSqlParser);
        paginationInterceptor.setSqlParserList(sqlParserList);
        paginationInterceptor.setSqlParserFilter(new ISqlParserFilter() {
            @Override
            public boolean doFilter(MetaObject metaObject) {
                MappedStatement ms = SqlParserHelper.getMappedStatement(metaObject);
                // 旧版租户 SQL 解析器无法处理 UNION ALL / 动态内联子查询，这里按语句级别绕过。
                return TENANT_SQL_PARSER_FILTER_IDS.contains(ms.getId());
            }
        });
        return paginationInterceptor;
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

    /**
     * 性能分析拦截器，不建议生产使用
     */
//    @Bean
//    public PerformanceInterceptor performanceInterceptor(){
//        return new PerformanceInterceptor();
//    }


}
