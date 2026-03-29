package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightHeadVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运费单主表扩展Mapper接口
 */
public interface FreightHeadMapperEx {

    /**
     * 条件查询运费单列表（关联结算方名称和创建人名称）
     */
    List<FreightHeadVo> selectByConditionFreightHead(
            @Param("billNo") String billNo,
            @Param("carrierId") Long carrierId,
            @Param("status") String status,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);

    /**
     * 批量逻辑删除运费单主表
     */
    int batchDeleteFreightHeadByIds(@Param("ids") String[] ids);

    /**
     * 批量更新状态
     */
    int batchSetStatus(@Param("status") String status, @Param("ids") String[] ids);

    /**
     * 运费对账聚合查询（按物流方汇总已审核物流单）
     */
    List<java.util.Map<String, Object>> selectReconciliation(
            @Param("carrierId") Long carrierId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);
}
