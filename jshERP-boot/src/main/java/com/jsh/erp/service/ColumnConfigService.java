package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.ColumnConfig;
import com.jsh.erp.datasource.mappers.ColumnConfigMapperEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

/**
 * 列配置Service
 */
@Service
public class ColumnConfigService {
    private Logger logger = LoggerFactory.getLogger(ColumnConfigService.class);

    @Resource
    private ColumnConfigMapperEx columnConfigMapperEx;

    public ColumnConfig getByPageCode(String pageCode) {
        try {
            return columnConfigMapperEx.selectByPageCode(pageCode);
        } catch (Exception e) {
            logger.error("获取列配置异常", e);
            return null;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveColumnConfig(String pageCode, String columnConfig) throws Exception {
        ColumnConfig existing = columnConfigMapperEx.selectByPageCode(pageCode);
        if (existing != null) {
            columnConfigMapperEx.updateByPageCode(pageCode, columnConfig);
        } else {
            columnConfigMapperEx.insertColumnConfig(pageCode, columnConfig);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteByPageCode(String pageCode) throws Exception {
        columnConfigMapperEx.deleteByPageCode(pageCode);
    }
}
