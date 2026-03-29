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
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                                       String beginTime, String endTime) throws Exception {
        List<FreightHeadVo> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo.getTenantId();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = freightHeadMapperEx.selectByConditionFreightHead(billNo, carrierId, status,
                    beginTime, endTime, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 新增运费单（主表+明细）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addFreightBill(String beanJson, String itemsJson, HttpServletRequest request) throws Exception {
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
        //保存主表
        try {
            freightHeadMapper.insertSelective(freightHead);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        Long headId = freightHead.getId();
        //保存明细
        freightItemService.saveItems(headId, itemsJson, tenantId);
        //重新计算并更新主表的总重量和总运费
        recalcHeadTotal(headId, freightHead.getUnitPrice());
        logService.insertLog("运费单",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(freightHead.getBillNo()).toString(), request);
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
            //查询明细列表，并关联出库单信息
            List<FreightItem> items = freightItemMapperEx.selectByHeaderId(id);
            List<Map<String, Object>> detailList = new ArrayList<>();
            if (items != null) {
                for (FreightItem item : items) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", item.getId());
                    row.put("depotHeadId", item.getDepotHeadId());
                    row.put("billNo", item.getDepotNumber());
                    row.put("totalWeight", item.getWeight());
                    row.put("remark", item.getRemark());
                    //查询出库单的客户名称和日期
                    if (item.getDepotHeadId() != null) {
                        Map<String, Object> depotInfo = freightItemMapperEx.selectDepotHeadInfo(item.getDepotHeadId());
                        if (depotInfo != null) {
                            row.put("customerName", depotInfo.get("customerName"));
                            row.put("billTimeStr", depotInfo.get("billTimeStr"));
                            row.put("totalAmount", depotInfo.get("totalAmount"));
                            row.put("materialNames", depotInfo.get("materialNames"));
                            row.put("depotName", depotInfo.get("depotName"));
                            row.put("salesMan", depotInfo.get("salesMan"));
                            row.put("remark", depotInfo.get("remark"));
                        }
                    }
                    detailList.add(row);
                }
            }
            data.put("detailList", detailList);
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
     * 重新计算并更新主表的总重量和总运费
     */
    private void recalcHeadTotal(Long headId, BigDecimal unitPrice) throws Exception {
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
}
