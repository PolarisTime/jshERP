package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.ColumnConfig;
import org.apache.ibatis.annotations.Param;

/**
 * 列配置Mapper接口
 */
public interface ColumnConfigMapperEx {

    ColumnConfig selectByPageCode(@Param("pageCode") String pageCode);

    int insertColumnConfig(@Param("pageCode") String pageCode, @Param("columnConfig") String columnConfig);

    int updateByPageCode(@Param("pageCode") String pageCode, @Param("columnConfig") String columnConfig);

    int deleteByPageCode(@Param("pageCode") String pageCode);
}
