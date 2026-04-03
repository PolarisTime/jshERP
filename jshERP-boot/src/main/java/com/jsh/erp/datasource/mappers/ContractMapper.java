package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.Contract;

public interface ContractMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Contract record);

    int insertSelective(Contract record);

    Contract selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Contract record);

    int updateByPrimaryKey(Contract record);
}
