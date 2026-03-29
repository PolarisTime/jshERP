package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 运费单明细扩展Mapper接口
 */
public interface FreightItemMapperEx {

    /**
     * 按运费单主表id查询明细列表（关联出库单编号）
     */
    List<FreightItem> selectByHeaderId(@Param("headerId") Long headerId);

    /**
     * 查询所有已关联的出库单id列表（用于防重复）
     */
    List<Long> selectLinkedDepotHeadIds();

    /**
     * 检查指定出库单id是否已被关联
     */
    List<Long> selectLinkedByDepotHeadIds(@Param("ids") List<Long> ids);

    /**
     * 按运费单主表id逻辑删除明细
     */
    int deleteByHeaderId(@Param("headerId") Long headerId);

    /**
     * 计算单张出库单的总重量（数量*物料重量之和）
     */
    BigDecimal calcDepotHeadWeight(@Param("depotHeadId") Long depotHeadId);

    /**
     * 查询出库单的客户名称和日期
     */
    Map<String, Object> selectDepotHeadInfo(@Param("depotHeadId") Long depotHeadId);

    /**
     * 查询可关联的销售出库单（已审核且未被关联）
     */
    List<Map<String, Object>> selectAvailableSaleOut(
            @Param("number") String number,
            @Param("tenantId") Long tenantId);

    /**
     * 根据出库单ID列表批量查询关联的物流单号
     */
    List<Map<String, Object>> selectFreightBillNoByDepotHeadIds(@Param("idList") List<Long> idList);

    /**
     * 按出库单ID列表查询商品明细行（每个商品一行，附带出库单头信息）
     */
    List<Map<String, Object>> selectDepotItemsByHeaderIds(@Param("headerIds") List<Long> headerIds);
}
