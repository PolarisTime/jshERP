package com.jsh.erp.service;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.FreightStatement;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.FreightStatementMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class FreightStatementService {
    private Logger logger = LoggerFactory.getLogger(FreightStatementService.class);

    @Resource
    private FreightStatementMapper freightStatementMapper;
    @Resource
    private UserService userService;

    public List<Map<String, Object>> listUnreconciledItems(Long carrierId, String beginTime, String endTime,
                                                           Integer offset, Integer rows) throws Exception {
        return freightStatementMapper.listUnreconciledItems(carrierId, beginTime, endTime, offset, rows);
    }

    public int countUnreconciledItems(Long carrierId, String beginTime, String endTime) throws Exception {
        return freightStatementMapper.countUnreconciledItems(carrierId, beginTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long generateStatement(Long carrierId, List<Long> itemIds, String remark,
                                  String beginTime, String endTime) throws Exception {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "请至少选择一条物流单");
        }
        User user = userService.getCurrentUser();
        // 生成单号 WDZ+yyyyMMdd+4位序号
        String datePrefix = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int todayCount = freightStatementMapper.countTodayStatement(datePrefix);
        String statementNo = "WDZ" + datePrefix + String.format("%04d", todayCount + 1);

        FreightStatement fs = new FreightStatement();
        fs.setStatementNo(statementNo);
        fs.setCarrierId(carrierId);
        fs.setBeginTime(parseDate(beginTime));
        fs.setEndTime(parseDate(endTime));
        fs.setTotalWeight(BigDecimal.ZERO);
        fs.setTotalFreight(BigDecimal.ZERO);
        fs.setPaidAmount(BigDecimal.ZERO);
        fs.setStatus("0");
        fs.setSignStatus("0");
        fs.setRemark(remark);
        fs.setDeleteFlag("0");
        fs.setCreateTime(new Date());
        fs.setCreator(user == null ? null : user.getId());
        freightStatementMapper.insert(fs);
        freightStatementMapper.insertStatementItems(fs.getId(), itemIds);
        // 汇总
        List<Map<String, Object>> details = freightStatementMapper.getStatementDetail(fs.getId());
        BigDecimal sumWeight = BigDecimal.ZERO, sumFreight = BigDecimal.ZERO;
        for (Map<String, Object> d : details) {
            sumWeight = sumWeight.add(new BigDecimal(String.valueOf(d.getOrDefault("totalWeight", "0"))));
            sumFreight = sumFreight.add(new BigDecimal(String.valueOf(d.getOrDefault("totalFreight", "0"))));
        }
        fs.setTotalWeight(sumWeight);
        fs.setTotalFreight(sumFreight);
        freightStatementMapper.updateByPrimaryKey(fs);
        return fs.getId();
    }

    public List<Map<String, Object>> listStatements(Long carrierId, String status, String signStatus,
                                                    String beginTime, String endTime,
                                                    Integer offset, Integer rows) throws Exception {
        return freightStatementMapper.listStatements(carrierId, status, signStatus, beginTime, endTime, offset, rows);
    }

    public int countStatements(Long carrierId, String status, String signStatus,
                               String beginTime, String endTime) throws Exception {
        return freightStatementMapper.countStatements(carrierId, status, signStatus, beginTime, endTime);
    }

    public Map<String, Object> getDetail(Long id) throws Exception {
        FreightStatement fs = freightStatementMapper.selectByPrimaryKey(id);
        List<Map<String, Object>> items = freightStatementMapper.getStatementDetail(id);
        // 为每个物流单加载出库明细（subItems）
        for (Map<String, Object> item : items) {
            Long freightHeadId = item.get("id") != null ? Long.valueOf(item.get("id").toString()) : null;
            if (freightHeadId != null) {
                List<Map<String, Object>> subItems = freightStatementMapper.getFreightSubItems(freightHeadId);
                item.put("subItems", subItems);
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("header", fs);
        result.put("items", items);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String status) throws Exception {
        FreightStatement fs = freightStatementMapper.selectByPrimaryKey(id);
        if (fs == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "对账单不存在");
        if ("0".equals(status) && "1".equals(fs.getSignStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已签署的对账单不能反审核");
        }
        fs.setStatus(status);
        freightStatementMapper.updateByPrimaryKey(fs);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sign(Long id, String signStatus) throws Exception {
        FreightStatement fs = freightStatementMapper.selectByPrimaryKey(id);
        if (fs == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "对账单不存在");
        if ("1".equals(signStatus) && !"1".equals(fs.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "请先审核后再签署");
        }
        fs.setSignStatus(signStatus);
        freightStatementMapper.updateByPrimaryKey(fs);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAttachment(Long id, String attachment) {
        FreightStatement fs = freightStatementMapper.selectByPrimaryKey(id);
        if (fs != null) {
            fs.setAttachment(attachment);
            freightStatementMapper.updateByPrimaryKey(fs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStatement(Long id) throws Exception {
        FreightStatement fs = freightStatementMapper.selectByPrimaryKey(id);
        if (fs == null) return;
        if ("1".equals(fs.getSignStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已签署的对账单不能删除");
        }
        freightStatementMapper.deleteStatementItems(id);
        fs.setDeleteFlag("1");
        freightStatementMapper.updateByPrimaryKey(fs);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addPaidAmount(Long id, BigDecimal amount) {
        freightStatementMapper.addPaidAmount(id, amount);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            if (dateStr.length() == 10) return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (Exception e) { return null; }
    }
}
