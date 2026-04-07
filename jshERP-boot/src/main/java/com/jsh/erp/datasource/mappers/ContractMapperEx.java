package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.ContractEx;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ContractMapperEx {

    List<ContractEx> selectByCondition(
            @Param("contractNo") String contractNo,
            @Param("contractName") String contractName,
            @Param("organId") Long organId,
            @Param("auditStatus") String auditStatus,
            @Param("signStatus") String signStatus);

    /**
     * 按客户汇总合同余额（多份合同累加）
     * @return { totalAmount, totalTonnage, deliveredAmount, deliveredTonnage, remainAmount, remainTonnage }
     */
    Map<String, Object> getContractBalance(
            @Param("organId") Long organId);
}
