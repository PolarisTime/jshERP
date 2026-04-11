package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.CustomerStatement;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.CustomerStatementMapper;
import com.jsh.erp.datasource.vo.CustomerStatementItemVo;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.constants.ExceptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户对账单 Service
 */
@Service
public class CustomerStatementService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerStatementService.class);

    @Resource
    private CustomerStatementMapper customerStatementMapper;

    @Resource
    private UserService userService;

    // ─── 未对账明细 ──────────────────────────────────────────────

    public List<CustomerStatementItemVo> listUnreconciledItems(Long organId, String beginTime, String endTime,
                                                               Integer offset, Integer rows) throws Exception {
        Long tenantId = getTenantId();
        return customerStatementMapper.listUnreconciledItems(organId, beginTime, endTime, tenantId, offset, rows);
    }

    public int countUnreconciledItems(Long organId, String beginTime, String endTime) throws Exception {
        Long tenantId = getTenantId();
        return customerStatementMapper.countUnreconciledItems(organId, beginTime, endTime, tenantId);
    }

    // ─── 生成对账单 ──────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public Long generateStatement(Long organId, List<Long> itemIds, String remark,
                                  String beginTime, String endTime) throws Exception {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "请至少选择一条明细");
        }
        User user = userService.getCurrentUser();
        Long tenantId = user.getTenantId();

        // 生成单号 DZ+yyyyMMdd+4位序
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int todayCount = customerStatementMapper.countTodayStatement(dateStr, tenantId);
        String statementNo = "DZ" + dateStr + String.format("%04d", todayCount + 1);

        // 汇总明细数据获取重量/金额（从已勾选的 unreconciledItems 中无法直接拿，需查 depot_item）
        // 直接通过 itemIds 构造临时汇总 —— 用 itemIds 查 detail 数据
        // 先插主表（weight/amount 先 0，后更新）
        CustomerStatement statement = new CustomerStatement();
        statement.setStatementNo(statementNo);
        statement.setOrganId(organId);
        statement.setBeginTime(parseDate(beginTime));
        statement.setEndTime(parseDate(endTime));
        statement.setTotalWeight(BigDecimal.ZERO);
        statement.setTotalAmount(BigDecimal.ZERO);
        statement.setStatus("0");
        statement.setSignStatus("0");
        statement.setRemark(remark);
        statement.setTenantId(tenantId);
        statement.setDeleteFlag("0");
        statement.setCreateTime(new Date());
        statement.setCreator(user.getId());
        customerStatementMapper.insert(statement);

        Long statementId = statement.getId();

        // 插入关联明细
        customerStatementMapper.insertStatementItems(statementId, itemIds);

        // 从 detail 汇总 totalWeight / totalAmount
        List<CustomerStatementItemVo> details = customerStatementMapper.getStatementDetail(statementId);
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CustomerStatementItemVo item : details) {
            if (item.getItemWeight() != null) totalWeight = totalWeight.add(item.getItemWeight());
            if (item.getAllPrice() != null) totalAmount = totalAmount.add(item.getAllPrice());
        }
        statement.setTotalWeight(totalWeight);
        statement.setTotalAmount(totalAmount);
        customerStatementMapper.updateByPrimaryKey(statement);

        return statementId;
    }

    // ─── 对账单列表 ──────────────────────────────────────────────

    public List<Map<String, Object>> listStatements(Long organId, String status, String signStatus,
                                                    String beginTime, String endTime,
                                                    Integer offset, Integer rows) throws Exception {
        Long tenantId = getTenantId();
        return customerStatementMapper.listStatements(organId, status, signStatus,
                beginTime, endTime, tenantId, offset, rows);
    }

    public int countStatements(Long organId, String status, String signStatus,
                               String beginTime, String endTime) throws Exception {
        Long tenantId = getTenantId();
        return customerStatementMapper.countStatements(organId, status, signStatus,
                beginTime, endTime, tenantId);
    }

    // ─── 对账单详情 ──────────────────────────────────────────────

    public Map<String, Object> getStatementDetail(Long id) throws Exception {
        CustomerStatement header = customerStatementMapper.selectByPrimaryKey(id);
        List<CustomerStatementItemVo> items = customerStatementMapper.getStatementDetail(id);
        Map<String, Object> result = new HashMap<>();
        result.put("header", header);
        result.put("items", items);
        return result;
    }

    // ─── 审核 ────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String status) throws Exception {
        CustomerStatement cs = customerStatementMapper.selectByPrimaryKey(id);
        if (cs == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "对账单不存在");
        // 已签署不允许反审核
        if ("0".equals(status) && "1".equals(cs.getSignStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "已签署的对账单不能反审核");
        }
        cs.setStatus(status);
        customerStatementMapper.updateByPrimaryKey(cs);
    }

    // ─── 签署 ────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void sign(Long id, String signStatus) throws Exception {
        CustomerStatement cs = customerStatementMapper.selectByPrimaryKey(id);
        if (cs == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "对账单不存在");
        if ("1".equals(signStatus) && !"1".equals(cs.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "请先审核后再签署");
        }
        cs.setSignStatus(signStatus);
        customerStatementMapper.updateByPrimaryKey(cs);
    }

    // ─── 附件 ────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void updateAttachment(Long id, String attachment) throws Exception {
        CustomerStatement cs = customerStatementMapper.selectByPrimaryKey(id);
        if (cs == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "对账单不存在");
        cs.setAttachment(attachment);
        customerStatementMapper.updateByPrimaryKey(cs);
    }

    // ─── 删除（软删） ────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void deleteStatement(Long id) throws Exception {
        CustomerStatement cs = customerStatementMapper.selectByPrimaryKey(id);
        if (cs == null) return;
        if ("1".equals(cs.getSignStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "已签署的对账单不能删除");
        }
        // 软删关联 item，恢复未对账状态
        customerStatementMapper.deleteStatementItems(id);
        cs.setDeleteFlag("1");
        customerStatementMapper.updateByPrimaryKey(cs);
    }

    // ─── 工具方法 ────────────────────────────────────────────────

    private Long getTenantId() throws Exception {
        User user = userService.getCurrentUser();
        return user == null ? null : user.getTenantId();
    }

    /**
     * 查询未收完的对账单（已审核、待收金额>0）
     */
    public List<Map<String, Object>> listUnpaidStatements(Long organId) throws Exception {
        User userInfo = userService.getCurrentUser();
        Long tenantId = userInfo == null ? null : userInfo.getTenantId();
        return customerStatementMapper.listUnpaidStatements(organId, tenantId);
    }

    /**
     * 累加对账单已收金额
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addReceivedAmount(Long statementId, BigDecimal amount) {
        customerStatementMapper.addReceivedAmount(statementId, amount);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            if (dateStr.length() == 10) {
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
