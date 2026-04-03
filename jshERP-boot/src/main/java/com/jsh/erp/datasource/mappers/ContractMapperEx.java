package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.ContractEx;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ContractMapperEx {

    List<ContractEx> selectByCondition(
            @Param("contractNo") String contractNo,
            @Param("contractName") String contractName,
            @Param("organId") Long organId,
            @Param("status") String status,
            @Param("tenantId") Long tenantId);
}
