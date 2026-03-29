package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightCarrier;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运费结算方扩展Mapper接口
 */
public interface FreightCarrierMapperEx {

    List<FreightCarrier> selectByCondition(
            @Param("name") String name,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows,
            @Param("tenantId") Long tenantId);

    int countByCondition(
            @Param("name") String name,
            @Param("tenantId") Long tenantId);

    List<FreightCarrier> selectAll(@Param("tenantId") Long tenantId);
}
