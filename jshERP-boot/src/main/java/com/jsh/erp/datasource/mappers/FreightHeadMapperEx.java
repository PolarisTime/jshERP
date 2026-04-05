package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightHeadVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
            @Param("paymentStatus") String paymentStatus,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);

    /**
     * 批量设置送达状态
     */
    int batchSetDeliveryStatus(@Param("deliveryStatus") String deliveryStatus, @Param("ids") String[] ids);

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
    /**
     * 根据单据编号查询运费单ID
     */
    Long selectIdByBillNo(@Param("billNo") String billNo);

    List<Map<String, Object>> selectReconciliation(
            @Param("carrierId") Long carrierId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);

    /**
     * 对账明细查询（带付款状态）
     */
    List<Map<String, Object>> selectReconciliationDetail(
            @Param("carrierId") Long carrierId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("paymentStatus") String paymentStatus,
            @Param("tenantId") Long tenantId);

    /**
     * 批量标记付款状态
     */
    int batchSetPaymentStatus(
            @Param("paymentStatus") String paymentStatus,
            @Param("paidAmount") BigDecimal paidAmount,
            @Param("paymentTime") Date paymentTime,
            @Param("paymentOperator") Long paymentOperator,
            @Param("ids") String[] ids);

    /**
     * 取消付款标记
     */
    int cancelPayment(@Param("ids") String[] ids);

    /**
     * 查询指定前缀的最大单据编号（包含已删除记录，避免单号复用）
     */
    String getMaxBillNoByPrefix(@Param("prefix") String prefix);
}
