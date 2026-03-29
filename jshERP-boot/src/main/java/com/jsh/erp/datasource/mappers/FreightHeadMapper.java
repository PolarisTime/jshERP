package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightHead;

/**
 * 运费单主表基础Mapper接口
 */
public interface FreightHeadMapper {

    int deleteByPrimaryKey(Long id);

    int insert(FreightHead record);

    int insertSelective(FreightHead record);

    FreightHead selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FreightHead record);

    int updateByPrimaryKey(FreightHead record);
}
