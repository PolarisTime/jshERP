package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.ContractPerson;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ContractPersonMapper {
    List<ContractPerson> selectByContractId(@Param("contractId") Long contractId);
    int insertBatch(@Param("contractId") Long contractId, @Param("persons") List<ContractPerson> persons);
    int deleteByContractId(@Param("contractId") Long contractId);
}
