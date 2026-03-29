package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightCarrier;

/**
 * 运费结算方基础Mapper接口
 */
public interface FreightCarrierMapper {

    int deleteByPrimaryKey(Long id);

    int insert(FreightCarrier record);

    int insertSelective(FreightCarrier record);

    FreightCarrier selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FreightCarrier record);

    int updateByPrimaryKey(FreightCarrier record);
}
