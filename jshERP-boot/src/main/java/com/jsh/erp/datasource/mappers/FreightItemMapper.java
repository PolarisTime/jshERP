package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightItem;
import org.apache.ibatis.annotations.Param;

/**
 * 运费单明细基础Mapper接口
 */
public interface FreightItemMapper {

    int deleteByPrimaryKey(Long id);

    int insert(FreightItem record);

    int insertSelective(FreightItem record);

    FreightItem selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FreightItem record);

    int updateByPrimaryKey(FreightItem record);
}
