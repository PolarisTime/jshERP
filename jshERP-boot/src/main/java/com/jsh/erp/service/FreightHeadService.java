package com.jsh.erp.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.FreightHead;
import com.jsh.erp.datasource.entities.FreightHeadVo;
import com.jsh.erp.datasource.entities.FreightItem;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.FreightHeadMapper;
import com.jsh.erp.datasource.mappers.FreightHeadMapperEx;
import com.jsh.erp.datasource.mappers.FreightItemMapper;
import com.jsh.erp.datasource.mappers.FreightItemMapperEx;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.RedisLockUtil;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 运费单Service
 */
@Service
public class FreightHeadService {
    private Logger logger = LoggerFactory.getLogger(FreightHeadService.class);

    @Resource
    private FreightHeadMapper freightHeadMapper;
    @Resource
    private FreightHeadMapperEx freightHeadMapperEx;
    @Resource
    private FreightItemMapper freightItemMapper;
    @Resource
    private FreightItemMapperEx freightItemMapperEx;
    @Resource
    private com.jsh.erp.datasource.mappers.FreightCarrierMapper freightCarrierMapper;
    @Resource
    private FreightItemService freightItemService;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private RedisLockUtil redisLockUtil;

    private static final String FREIGHT_SEQ_NAME = "freight_bill_no_seq";
    private static final long LOCK_EXPIRE_TIME = 3000;
    private static final long LOCK_WAIT_TIME = 100;

    /**
     * 生成运费单编号，格式: yyyyW0001
     * 基于数据库已有最大单号+1，保证连续不跳号
     * 通过 Redis 分布式锁保证并发安全，每年自动从 0001 开始
     */
    public String buildFreightBillNo() throws Exception {
        String lockKey = "sequence:lock:" + FREIGHT_SEQ_NAME;
        String requestId = UUID.randomUUID().toString();
        boolean locked = false;
        try {
            locked = redisLockUtil.tryLock(lockKey, requestId, LOCK_EXPIRE_TIME, LOCK_WAIT_TIME);
            if (!locked) {
                throw new BusinessRunTimeException(ExceptionConstants.SEQUENCE_ONLY_FAILED_CODE,
                        ExceptionConstants.SEQUENCE_ONLY_FAILED_MSG);
            }
            int currentYear = LocalDate.now().getYear();
            String prefix = currentYear + "W";
            //查询当前年度已有的最大单号（包含已删除记录，避免单号复用）
            String maxBillNo = freightHeadMapperEx.getMaxBillNoByPrefix(prefix);
            long nextSeq = 1;
            if (maxBillNo != null && maxBillNo.length() > prefix.length()) {
                try {
                    nextSeq = Long.parseLong(maxBillNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    logger.warn("无法解析物流单号后缀: {}", maxBillNo);
                }
            }
            return prefix + String.format("%04d", nextSeq);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRunTimeException(ExceptionConstants.SEQUENCE_ONLY_BREAK_CODE,
                    ExceptionConstants.SEQUENCE_ONLY_BREAK_MSG);
        } finally {
            if (locked) {
                redisLockUtil.unlock(lockKey, requestId);
            }
        }
    }

    /**
     * 根据id获取运费单
     */
    public FreightHead getFreightHead(Long id) throws Exception {
        FreightHead result = null;
        try {
            result = freightHeadMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 分页条件查询运费单列表
     */
    public List<FreightHeadVo> select(String billNo, Long carrierId, String status,
                                       String paymentStatus, String deliveryStatus,
                                       String beginTime, String endTime) throws Exception {
        List<FreightHeadVo> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = freightHeadMapperEx.selectByConditionFreightHead(billNo, carrierId, status,
                    paymentStatus, deliveryStatus, beginTime, endTime, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 批量设置送达状态
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetDeliveryStatus(String deliveryStatus, String ids) throws Exception {
        int result = 0;
        try {
            String[] idArray = ids.split(",");
            result = freightHeadMapperEx.batchSetDeliveryStatus(deliveryStatus, idArray);
            String statusStr = "1".equals(deliveryStatus) ? "[标记送达]" : "[取消送达]";
            logService.insertLog("运费单",
                    new StringBuffer(statusStr).append(ids).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 不分页查询运费单列表（用于导出）
     */
    public List<FreightHeadVo> selectForExport(String billNo, Long carrierId, String status,
                                                String paymentStatus, String deliveryStatus,
                                                String beginTime, String endTime) throws Exception {
        List<FreightHeadVo> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            list = freightHeadMapperEx.selectByConditionFreightHead(billNo, carrierId, status,
                    paymentStatus, deliveryStatus, beginTime, endTime, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 新增运费单（主表+明细）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String addFreightBill(String beanJson, String itemsJson, HttpServletRequest request) throws Exception {
        FreightHead freightHead = JSONObject.parseObject(beanJson, FreightHead.class);
        User userInfo = userService.getCurrentUser();
        Long tenantId = userInfo == null ? null : userInfo.getTenantId();
        freightHead.setCreator(userInfo == null ? null : userInfo.getId());
        freightHead.setCreateTime(new Date());
        if (StringUtil.isEmpty(freightHead.getStatus())) {
            freightHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        }
        freightHead.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        //防重复校验：检查明细中的depotHeadId是否已被其他运费单关联
        checkDuplicateDepotHead(itemsJson, null);
        //服务端生成单号并插入，遇到唯一键冲突时自动重试
        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            freightHead.setBillNo(buildFreightBillNo());
            try {
                freightHeadMapper.insertSelective(freightHead);
                break;
            } catch (DuplicateKeyException e) {
                logger.warn("运费单编号冲突: {}，第{}次重试", freightHead.getBillNo(), i + 1);
                if (i == maxRetry - 1) {
                    JshException.writeFail(logger, e);
                }
            } catch (Exception e) {
                JshException.writeFail(logger, e);
            }
        }
        Long headId = freightHead.getId();
        //保存明细
        freightItemService.saveItems(headId, itemsJson, tenantId);
        //重新计算并更新主表的总重量和总运费
        recalcHeadTotal(headId, freightHead.getUnitPrice());
        logService.insertLog("运费单",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(freightHead.getBillNo()).toString(), request);
        return freightHead.getBillNo();
    }

    /**
     * 编辑运费单（主表+明细）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateFreightBill(String beanJson, String itemsJson, HttpServletRequest request) throws Exception {
        FreightHead freightHead = JSONObject.parseObject(beanJson, FreightHead.class);
        //防重复校验：排除当前运费单自身的明细
        checkDuplicateDepotHead(itemsJson, freightHead.getId());
        //更新主表
        try {
            freightHeadMapper.updateByPrimaryKeySelective(freightHead);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        //先删除原明细，再保存新明细
        User userInfo = userService.getCurrentUser();
        Long tenantId = userInfo == null ? null : userInfo.getTenantId();
        freightItemService.saveItems(freightHead.getId(), itemsJson, tenantId);
        //重新计算并更新主表的总重量和总运费
        recalcHeadTotal(freightHead.getId(), freightHead.getUnitPrice());
        logService.insertLog("运费单",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(freightHead.getBillNo()).toString(), request);
    }

    /**
     * 删除运费单（主表和明细逻辑删除）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteFreightBill(Long id, HttpServletRequest request) throws Exception {
        FreightHead freightHead = getFreightHead(id);
        if (freightHead != null && !"0".equals(freightHead.getStatus())) {
            throw new BusinessRunTimeException(0, "已审核的运费单不能删除，请先反审核");
        }
        if (freightHead != null && !"0".equals(freightHead.getPaymentStatus())) {
            throw new BusinessRunTimeException(0, "已付款的运费单不能删除，请先取消付款标记");
        }
        //逻辑删除明细
        freightItemMapperEx.deleteByHeaderId(id);
        //逻辑删除主表
        freightHeadMapperEx.batchDeleteFreightHeadByIds(new String[]{id.toString()});
        logService.insertLog("运费单",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE)
                        .append(freightHead != null ? freightHead.getBillNo() : "").toString(), request);
    }

    /**
     * 批量删除运费单
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteBatchFreightBill(String ids, HttpServletRequest request) throws Exception {
        String[] idArray = ids.split(",");
        for (String idStr : idArray) {
            if (idStr != null && !idStr.trim().isEmpty()) {
                Long id = Long.parseLong(idStr.trim());
                deleteFreightBill(id, request);
            }
        }
    }

    /**
     * 批量审核/反审核
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(String status, String ids) throws Exception {
        int result = 0;
        try {
            String[] idArray = ids.split(",");
            //反审核时校验付款状态
            if ("0".equals(status)) {
                for (String idStr : idArray) {
                    if (idStr != null && !idStr.trim().isEmpty()) {
                        FreightHead head = getFreightHead(Long.parseLong(idStr.trim()));
                        if (head != null && !"0".equals(head.getPaymentStatus())) {
                            throw new BusinessRunTimeException(0,
                                    "单据[" + head.getBillNo() + "]已有付款记录，不能反审核，请先取消付款标记");
                        }
                    }
                }
            }
            result = freightHeadMapperEx.batchSetStatus(status, idArray);
            //记录日志
            String statusStr = "1".equals(status) ? "[审核]" : "[反审核]";
            logService.insertLog("运费单",
                    new StringBuffer(statusStr).append(ids).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 根据单据编号查询运费单详情
     */
    public Map<String, Object> getDetailByBillNo(String billNo) throws Exception {
        Long id = freightHeadMapperEx.selectIdByBillNo(billNo);
        if (id == null) {
            return new HashMap<>();
        }
        return getDetail(id);
    }

    /**
     * 获取运费单详情（主表+明细列表），返回前端可直接使用的扁平结构
     */
    public Map<String, Object> getDetail(Long id) throws Exception {
        Map<String, Object> data = new HashMap<>();
        try {
            //查看详情时自动同步重量和运费（从关联出库单实时计算）
            FreightHead headBefore = freightHeadMapper.selectByPrimaryKey(id);
            if (headBefore != null && "0".equals(headBefore.getDeleteFlag())) {
                recalcItemWeights(id);
                recalcHeadTotal(id, headBefore.getUnitPrice());
            }
            FreightHead head = freightHeadMapper.selectByPrimaryKey(id);
            if (head != null) {
                data.put("id", head.getId());
                data.put("billNo", head.getBillNo());
                data.put("billTime", head.getBillTime());
                data.put("billTimeStr", head.getBillTime() != null
                        ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(head.getBillTime()) : "");
                data.put("carrierId", head.getCarrierId());
                data.put("unitPrice", head.getUnitPrice());
                data.put("totalWeight", head.getTotalWeight());
                data.put("totalFreight", head.getTotalFreight());
                data.put("remark", head.getRemark());
                data.put("status", head.getStatus());
                //查询结算方名称
                if (head.getCarrierId() != null) {
                    com.jsh.erp.datasource.entities.FreightCarrier carrier =
                            freightCarrierMapper.selectByPrimaryKey(head.getCarrierId());
                    data.put("carrierName", carrier != null ? carrier.getName() : "");
                }
            }
            //查询明细列表，按商品明细行展开（每个商品一行）
            List<FreightItem> items = freightItemMapperEx.selectByHeaderId(id);
            List<Map<String, Object>> detailList = new ArrayList<>();
            if (items != null && items.size() > 0) {
                List<Long> headerIds = new ArrayList<>();
                for (FreightItem item : items) {
                    if (item.getDepotHeadId() != null) {
                        headerIds.add(item.getDepotHeadId());
                    }
                }
                if (headerIds.size() > 0) {
                    detailList = freightItemMapperEx.selectDepotItemsByHeaderIds(headerIds);
                }
            }
            data.put("detailList", detailList);
            // 聚合出库单备注（去重）供打印使用
            java.util.LinkedHashSet<String> saleRemarks = new java.util.LinkedHashSet<>();
            for (Map<String, Object> row : detailList) {
                Object r = row.get("remark");
                if (r != null && !r.toString().trim().isEmpty()) {
                    saleRemarks.add(r.toString().trim());
                }
            }
            data.put("saleRemark", String.join("、", saleRemarks));
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return data;
    }

    /**
     * 获取可关联的销售出库单（排除已关联的）
     * @param billNo 出库单编号搜索
     */
    public List<Map<String, Object>> getAvailableSaleOut(String billNo, HttpServletRequest request) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            PageUtils.startPage();
            resultList = freightItemMapperEx.selectAvailableSaleOut(billNo, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return resultList;
    }

    /**
     * 计算单张出库单的总重量
     */
    public BigDecimal calcDepotHeadWeight(Long depotHeadId) throws Exception {
        BigDecimal weight = BigDecimal.ZERO;
        try {
            weight = freightItemMapperEx.calcDepotHeadWeight(depotHeadId);
            if (weight == null) {
                weight = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return weight;
    }

    /**
     * 按出库单ID列表查询商品明细行
     */
    public List<Map<String, Object>> getDepotItemsByHeaderIds(List<Long> headerIds) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            if (headerIds != null && headerIds.size() > 0) {
                list = freightItemMapperEx.selectDepotItemsByHeaderIds(headerIds);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 运费对账聚合查询（按物流方汇总已审核物流单）
     */
    public List<Map<String, Object>> selectReconciliation(Long carrierId, String beginTime, String endTime) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = freightHeadMapperEx.selectReconciliation(carrierId, beginTime, endTime, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 防重复校验：检查明细中的depotHeadId是否已被其他运费单关联
     * @param itemsJson 明细JSON
     * @param excludeHeaderId 排除的运费单id（编辑时排除自身）
     */
    private void checkDuplicateDepotHead(String itemsJson, Long excludeHeaderId) throws Exception {
        JSONArray itemArr = JSONArray.parseArray(itemsJson);
        if (itemArr == null || itemArr.size() == 0) {
            return;
        }
        List<Long> depotHeadIds = new ArrayList<>();
        for (int i = 0; i < itemArr.size(); i++) {
            JSONObject itemObj = itemArr.getJSONObject(i);
            if (itemObj.get("depotHeadId") != null && !"".equals(itemObj.getString("depotHeadId"))) {
                depotHeadIds.add(itemObj.getLong("depotHeadId"));
            }
        }
        if (depotHeadIds.isEmpty()) {
            return;
        }
        //查询这些depotHeadId是否已被关联
        List<Long> linkedIds = freightItemMapperEx.selectLinkedByDepotHeadIds(depotHeadIds);
        if (linkedIds != null && !linkedIds.isEmpty()) {
            //如果是编辑模式，需要排除当前运费单自身的明细
            if (excludeHeaderId != null) {
                List<FreightItem> currentItems = freightItemMapperEx.selectByHeaderId(excludeHeaderId);
                List<Long> currentDepotHeadIds = new ArrayList<>();
                for (FreightItem item : currentItems) {
                    if (item.getDepotHeadId() != null) {
                        currentDepotHeadIds.add(item.getDepotHeadId());
                    }
                }
                linkedIds.removeAll(currentDepotHeadIds);
            }
            if (!linkedIds.isEmpty()) {
                throw new BusinessRunTimeException(0, "出库单[" + linkedIds.toString() + "]已被其他运费单关联，不可重复关联");
            }
        }
    }

    /**
     * 根据出库单ID列表，重新计算关联物流单明细的重量，并更新主表汇总
     * （用于出库单编辑/审核后同步更新物流单）
     */
    public void recalcByDepotHeadIds(List<Long> depotHeadIds) throws Exception {
        if (depotHeadIds == null || depotHeadIds.isEmpty()) {
            return;
        }
        List<Long> freightHeadIds = freightItemMapperEx.selectFreightHeadIdsByDepotHeadIds(depotHeadIds);
        if (freightHeadIds == null || freightHeadIds.isEmpty()) {
            return;
        }
        for (Long headId : freightHeadIds) {
            FreightHead fh = freightHeadMapper.selectByPrimaryKey(headId);
            if (fh != null && "0".equals(fh.getDeleteFlag())) {
                //先重新计算每条明细的重量（从出库单实时取值）
                recalcItemWeights(headId);
                //再汇总更新主表
                recalcHeadTotal(headId, fh.getUnitPrice());
            }
        }
    }

    /**
     * 重新计算运费单下每条明细的重量（根据关联出库单实时计算）
     */
    private void recalcItemWeights(Long headId) {
        List<FreightItem> items = freightItemMapperEx.selectByHeaderId(headId);
        if (items == null) {
            return;
        }
        for (FreightItem item : items) {
            if (item.getDepotHeadId() != null) {
                BigDecimal calcWeight = freightItemMapperEx.calcDepotHeadWeight(item.getDepotHeadId());
                if (calcWeight != null && calcWeight.compareTo(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO) != 0) {
                    FreightItem upd = new FreightItem();
                    upd.setId(item.getId());
                    upd.setWeight(calcWeight);
                    try {
                        freightItemMapper.updateByPrimaryKeySelective(upd);
                    } catch (Exception e) {
                        logger.error("更新运费单明细重量失败, itemId={}", item.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * 重新计算并更新主表的总重量和总运费
     */
    public void recalcHeadTotal(Long headId, BigDecimal unitPrice) throws Exception {
        List<FreightItem> items = freightItemMapperEx.selectByHeaderId(headId);
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (items != null) {
            for (FreightItem item : items) {
                if (item.getWeight() != null) {
                    totalWeight = totalWeight.add(item.getWeight());
                }
            }
        }
        FreightHead update = new FreightHead();
        update.setId(headId);
        update.setTotalWeight(totalWeight);
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            update.setTotalFreight(totalWeight.multiply(unitPrice));
        } else {
            update.setTotalFreight(BigDecimal.ZERO);
        }
        try {
            freightHeadMapper.updateByPrimaryKeySelective(update);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
    }

    /**
     * 对账明细查询（带付款状态筛选）
     */
    public List<Map<String, Object>> selectReconciliationDetail(Long carrierId, String beginTime, String endTime, String paymentStatus) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = freightHeadMapperEx.selectReconciliationDetail(carrierId, beginTime, endTime, paymentStatus, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 批量标记付款状态
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetPaymentStatus(String paymentStatus, BigDecimal paidAmount, String ids) throws Exception {
        int result = 0;
        try {
            User userInfo = userService.getCurrentUser();
            String[] idArray = ids.split(",");
            result = freightHeadMapperEx.batchSetPaymentStatus(paymentStatus, paidAmount, new Date(), userInfo.getId(), idArray);
            String statusStr = "1".equals(paymentStatus) ? "标记已付款" : "2".equals(paymentStatus) ? "标记部分付款" : "标记未付款";
            logService.insertLog("运费单", statusStr + ":" + ids,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 取消付款标记
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int cancelPayment(String ids) throws Exception {
        int result = 0;
        try {
            String[] idArray = ids.split(",");
            result = freightHeadMapperEx.cancelPayment(idArray);
            logService.insertLog("运费单", "取消付款标记:" + ids,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 导出对账明细（不分页，全量查询）
     */
    public List<Map<String, Object>> getReconciliationDetailForExport(Long carrierId, String beginTime, String endTime, String paymentStatus) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            // 不调用 PageUtils.startPage()，全量查询
            list = freightHeadMapperEx.selectReconciliationDetail(carrierId, beginTime, endTime, paymentStatus, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }
}
