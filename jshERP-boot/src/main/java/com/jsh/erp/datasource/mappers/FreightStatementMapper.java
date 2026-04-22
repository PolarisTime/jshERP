package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.FreightStatement;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FreightStatementMapper {

    int insert(FreightStatement record);

    int updateByPrimaryKey(FreightStatement record);

    FreightStatement selectByPrimaryKey(@Param("id") Long id);

    int countTodayStatement(@Param("datePrefix") String datePrefix);

    /** 未对账的物流单列表 */
    List<Map<String, Object>> listUnreconciledItems(
            @Param("carrierId") Long carrierId,
            @Param("beginTime") String beginTime, @Param("endTime") String endTime,
            @Param("offset") Integer offset, @Param("rows") Integer rows);

    int countUnreconciledItems(
            @Param("carrierId") Long carrierId,
            @Param("beginTime") String beginTime, @Param("endTime") String endTime);

    /** 对账单列表 */
    List<Map<String, Object>> listStatements(
            @Param("carrierId") Long carrierId, @Param("status") String status,
            @Param("signStatus") String signStatus,
            @Param("beginTime") String beginTime, @Param("endTime") String endTime,
            @Param("offset") Integer offset, @Param("rows") Integer rows);

    int countStatements(
            @Param("carrierId") Long carrierId, @Param("status") String status,
            @Param("signStatus") String signStatus,
            @Param("beginTime") String beginTime, @Param("endTime") String endTime);

    /** 对账单详情（关联的物流单列表） */
    List<Map<String, Object>> getStatementDetail(@Param("statementId") Long statementId);

    /** 插入关联明细 */
    int insertStatementItems(@Param("statementId") Long statementId, @Param("freightHeadIds") List<Long> freightHeadIds);

    /** 删除关联明细 */
    int deleteStatementItems(@Param("statementId") Long statementId);

    /** 累加已付金额 */
    int addPaidAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 物流单关联的出库明细（用于含明细打印） */
    List<Map<String, Object>> getFreightSubItems(@Param("freightHeadId") Long freightHeadId);
}
