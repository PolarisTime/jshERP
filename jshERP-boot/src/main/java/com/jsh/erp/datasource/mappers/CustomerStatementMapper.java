package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.CustomerStatement;
import com.jsh.erp.datasource.vo.CustomerStatementItemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户对账单 Mapper
 */
public interface CustomerStatementMapper {

    // ─── 主表 CRUD ────────────────────────────────────────────────

    int insert(CustomerStatement record);

    int updateByPrimaryKey(CustomerStatement record);

    CustomerStatement selectByPrimaryKey(Long id);

    /**
     * 今日已生成的对账单数量（用于生成序号）
     */
    int countTodayStatement(@Param("datePrefix") String datePrefix);

    // ─── 未对账明细（depot_item 行） ─────────────────────────────

    List<CustomerStatementItemVo> listUnreconciledItems(
            @Param("organId") Long organId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    int countUnreconciledItems(
            @Param("organId") Long organId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime);

    // ─── 对账单列表 ──────────────────────────────────────────────

    List<Map<String, Object>> listStatements(
            @Param("organId") Long organId,
            @Param("statementNo") String statementNo,
            @Param("status") String status,
            @Param("signStatus") String signStatus,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    int countStatements(
            @Param("organId") Long organId,
            @Param("statementNo") String statementNo,
            @Param("status") String status,
            @Param("signStatus") String signStatus,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime);

    // ─── 对账单明细 ──────────────────────────────────────────────

    List<CustomerStatementItemVo> getStatementDetail(
            @Param("statementId") Long statementId);

    // ─── 关联 item 表 ────────────────────────────────────────────

    int insertStatementItems(
            @Param("statementId") Long statementId,
            @Param("itemIds") List<Long> itemIds);

    /**
     * 软删关联 item（用于删除对账单时恢复明细为未对账状态）
     */
    int deleteStatementItems(@Param("statementId") Long statementId);

    List<Map<String, Object>> listUnpaidStatements(@Param("organId") Long organId);

    int addReceivedAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
}
