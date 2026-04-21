package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.PriceApproval;
import com.jsh.erp.datasource.entities.PriceApprovalItem;
import com.jsh.erp.datasource.vo.PriceApprovalItemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 价格核准 Mapper
 */
public interface PriceApprovalMapper {

    // ─── 主表 CRUD ────────────────────────────────────────────────

    int insert(PriceApproval record);

    int updateByPrimaryKey(PriceApproval record);

    PriceApproval selectByPrimaryKey(Long id);

    /** 按出库单ID查找核准记录（排除已删除） */
    PriceApproval selectByDepotHeadId(@Param("depotHeadId") Long depotHeadId,
                                      @Param("tenantId") Long tenantId);

    /** 今日已生成的核准单数量（用于生成序号） */
    int countTodayApproval(@Param("datePrefix") String datePrefix,
                           @Param("tenantId") Long tenantId);

    // ─── 核准单列表 ──────────────────────────────────────────────

    List<Map<String, Object>> listApprovals(
            @Param("organId") Long organId,
            @Param("status") String status,
            @Param("billNo") String billNo,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    int countApprovals(
            @Param("organId") Long organId,
            @Param("status") String status,
            @Param("billNo") String billNo,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);

    // ─── 核准单明细 ──────────────────────────────────────────────

    List<PriceApprovalItemVo> getApprovalItems(@Param("approvalId") Long approvalId);

    PriceApprovalItem selectItemById(@Param("id") Long id);

    int batchInsertItems(@Param("items") List<PriceApprovalItem> items);

    int updateItemByPrimaryKeySelective(PriceApprovalItem record);

    int deleteItemById(@Param("id") Long id);

    int deleteItemsByApprovalId(@Param("approvalId") Long approvalId);

    // ─── 可导入的出库单 ──────────────────────────────────────────

    List<Map<String, Object>> listAvailableSaleOut(
            @Param("organId") Long organId,
            @Param("billNo") String billNo,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    int countAvailableSaleOut(
            @Param("organId") Long organId,
            @Param("billNo") String billNo,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("tenantId") Long tenantId);

    // ─── 校验：核准明细是否被对账单引用 ──────────────────────────

    int countStatementItemsByApprovalId(@Param("approvalId") Long approvalId);
}
