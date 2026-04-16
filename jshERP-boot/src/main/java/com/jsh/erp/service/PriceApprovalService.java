package com.jsh.erp.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.mappers.PriceApprovalMapper;
import com.jsh.erp.datasource.vo.PriceApprovalItemVo;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.constants.ExceptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格核准 Service
 */
@Service
public class PriceApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(PriceApprovalService.class);

    @Resource
    private PriceApprovalMapper priceApprovalMapper;

    @Resource
    private DepotHeadMapper depotHeadMapper;

    @Resource
    private DepotItemMapper depotItemMapper;

    @Resource
    private DepotItemMapperEx depotItemMapperEx;

    @Resource
    private UserService userService;

    @Resource
    private MaterialExtendService materialExtendService;

    // ─── 从出库单创建核准记录 ────────────────────────────────────

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Long createFromSaleOut(Long depotHeadId) throws Exception {
        User user = userService.getCurrentUser();
        Long tenantId = user.getTenantId();

        // 校验：出库单存在
        DepotHead dh = depotHeadMapper.selectByPrimaryKey(depotHeadId);
        if (dh == null || "1".equals(dh.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "出库单不存在");
        }
        // 校验：不能重复创建
        PriceApproval existing = priceApprovalMapper.selectByDepotHeadId(depotHeadId, tenantId);
        if (existing != null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "该出库单已存在核准记录：" + existing.getApprovalNo());
        }

        // 生成单号 HZ+yyyyMMdd+4位序
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int todayCount = priceApprovalMapper.countTodayApproval(dateStr, tenantId);
        String approvalNo = "HZ" + dateStr + String.format("%04d", todayCount + 1);

        // 创建主表
        PriceApproval pa = new PriceApproval();
        pa.setApprovalNo(approvalNo);
        pa.setDepotHeadId(depotHeadId);
        pa.setOrganId(dh.getOrganId());
        pa.setDeliveryDate(dh.getOperTime());
        pa.setRemark(dh.getRemark());
        pa.setTotalWeight(BigDecimal.ZERO);
        pa.setTotalAmount(BigDecimal.ZERO);
        pa.setStatus("0");
        // tenant_id 由多租户插件自动注入，不手动设置避免重复
        pa.setCreateTime(new Date());
        pa.setCreator(user.getId());
        pa.setDeleteFlag("0");
        priceApprovalMapper.insert(pa);

        Long approvalId = pa.getId();

        // 查询出库单明细并复制到核准明细
        List<DepotItemVo4WithInfoEx> depotItems = depotItemMapperEx.getDetailList(depotHeadId);
        if (depotItems != null && !depotItems.isEmpty()) {
            List<PriceApprovalItem> items = new ArrayList<>();
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (DepotItemVo4WithInfoEx di : depotItems) {
                PriceApprovalItem item = new PriceApprovalItem();
                item.setApprovalId(approvalId);
                item.setDepotItemId(di.getId());
                item.setMaterialId(di.getMaterialId());
                item.setMaterialExtendId(di.getMaterialExtendId());
                item.setBarCode(di.getBarCode());
                item.setName(di.getMName());
                item.setStandard(di.getMStandard());
                item.setModel(di.getMModel());
                item.setColor(di.getMColor());
                item.setBrand(di.getBrand());
                item.setOperNumber(di.getOperNumber());
                item.setWeight(di.getWeight());
                item.setUnitPrice(di.getUnitPrice());
                item.setAllPrice(di.getAllPrice());
                item.setTaxRate(di.getTaxRate());
                item.setTaxMoney(di.getTaxMoney());
                item.setTaxLastMoney(di.getTaxLastMoney());
                item.setRemark(di.getRemark());
                items.add(item);
                if (di.getWeight() != null) totalWeight = totalWeight.add(di.getWeight());
                if (di.getAllPrice() != null) totalAmount = totalAmount.add(di.getAllPrice());
            }
            priceApprovalMapper.batchInsertItems(items);
            pa.setTotalWeight(totalWeight);
            pa.setTotalAmount(totalAmount);
            priceApprovalMapper.updateByPrimaryKey(pa);
        }
        return approvalId;
    }

    // ─── 保存核准明细（含拆分行） ────────────────────────────────

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveApprovalItems(Long approvalId, String deliveryDate, String remark,
                                  String itemsJson) throws Exception {
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(approvalId);
        if (pa == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "核准单不存在");
        }
        if ("1".equals(pa.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已核准的记录不能编辑");
        }

        User user = userService.getCurrentUser();
        JSONArray itemArr = JSONArray.parseArray(itemsJson);
        List<PriceApprovalItem> items = new ArrayList<>();

        // 解析明细并构建拆分组校验数据
        Map<Long, BigDecimal> groupWeightSum = new HashMap<>();
        Map<Long, BigDecimal> groupOriginalWeight = new HashMap<>();

        for (int i = 0; i < itemArr.size(); i++) {
            JSONObject obj = itemArr.getJSONObject(i);
            PriceApprovalItem item = new PriceApprovalItem();
            item.setApprovalId(approvalId);
            item.setDepotItemId(obj.getLong("depotItemId"));
            item.setMaterialId(obj.getLong("materialId"));
            item.setMaterialExtendId(obj.getLong("materialExtendId"));
            item.setBarCode(obj.getString("barCode"));
            item.setName(obj.getString("name"));
            item.setStandard(obj.getString("standard"));
            item.setModel(obj.getString("model"));
            item.setColor(obj.getString("color"));
            item.setBrand(obj.getString("brand"));
            item.setOperNumber(obj.getBigDecimal("operNumber"));
            item.setWeight(obj.getBigDecimal("weight"));
            item.setUnitPrice(obj.getBigDecimal("unitPrice"));
            item.setAllPrice(obj.getBigDecimal("allPrice"));
            item.setTaxRate(obj.getBigDecimal("taxRate"));
            item.setTaxMoney(obj.getBigDecimal("taxMoney"));
            item.setTaxLastMoney(obj.getBigDecimal("taxLastMoney"));
            item.setRemark(obj.getString("remark"));
            items.add(item);

            // 累计拆分组重量
            Long diId = item.getDepotItemId();
            BigDecimal w = item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO;
            groupWeightSum.merge(diId, w, BigDecimal::add);
            if (obj.getBigDecimal("originalWeight") != null) {
                groupOriginalWeight.putIfAbsent(diId, obj.getBigDecimal("originalWeight"));
            }
        }

        // 拆分组重量校验：同组求和必须等于原始重量
        for (Map.Entry<Long, BigDecimal> entry : groupWeightSum.entrySet()) {
            Long diId = entry.getKey();
            BigDecimal sumWeight = entry.getValue().setScale(6, RoundingMode.HALF_UP);
            BigDecimal origWeight = groupOriginalWeight.get(diId);
            if (origWeight != null) {
                origWeight = origWeight.setScale(6, RoundingMode.HALF_UP);
                if (sumWeight.compareTo(origWeight) != 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                            "拆分组重量校验失败：depot_item_id=" + diId
                                    + "，原始重量=" + origWeight + "，拆分合计=" + sumWeight);
                }
            }
        }

        // 删旧插新
        priceApprovalMapper.deleteItemsByApprovalId(approvalId);
        if (!items.isEmpty()) {
            priceApprovalMapper.batchInsertItems(items);
        }

        // 更新头部
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PriceApprovalItem item : items) {
            if (item.getWeight() != null) totalWeight = totalWeight.add(item.getWeight());
            if (item.getAllPrice() != null) totalAmount = totalAmount.add(item.getAllPrice());
        }
        pa.setTotalWeight(totalWeight);
        pa.setTotalAmount(totalAmount);
        pa.setDeliveryDate(parseDate(deliveryDate));
        pa.setRemark(remark);
        pa.setUpdateTime(new Date());
        pa.setUpdater(user.getId());
        priceApprovalMapper.updateByPrimaryKey(pa);
    }

    // ─── 核准确认（回写depot_head） ─────────────────────────────

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void confirmApproval(Long approvalId) throws Exception {
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(approvalId);
        if (pa == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "核准单不存在");
        }
        if ("1".equals(pa.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "该记录已核准");
        }

        // 检查所有明细是否填写了单价
        List<PriceApprovalItemVo> items = priceApprovalMapper.getApprovalItems(approvalId);
        for (PriceApprovalItemVo item : items) {
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                        "明细行单价不能为空或0：" + item.getName() + " " + item.getStandard());
            }
        }

        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PriceApprovalItemVo item : items) {
            if (item.getAllPrice() != null) {
                totalAmount = totalAmount.add(item.getAllPrice());
            }
        }

        // 核准状态置1
        User user = userService.getCurrentUser();
        pa.setStatus("1");
        pa.setTotalAmount(totalAmount);
        pa.setUpdateTime(new Date());
        pa.setUpdater(user.getId());
        priceApprovalMapper.updateByPrimaryKey(pa);

        // 回写 depot_head：discount_last_money、total_price、price_approved、remark
        DepotHead headUpd = new DepotHead();
        headUpd.setId(pa.getDepotHeadId());
        headUpd.setTotalPrice(totalAmount);
        headUpd.setDiscountLastMoney(totalAmount);
        headUpd.setDiscount(BigDecimal.ZERO);
        headUpd.setDiscountMoney(BigDecimal.ZERO);
        headUpd.setPriceApproved("1");
        headUpd.setRemark(pa.getRemark());
        depotHeadMapper.updateByPrimaryKeySelective(headUpd);

        // 回写 depot_item：按 depotItemId 分组聚合（支持拆分行场景）
        Map<Long, List<PriceApprovalItemVo>> groupByDepotItem = items.stream()
                .collect(Collectors.groupingBy(PriceApprovalItemVo::getDepotItemId));
        for (Map.Entry<Long, List<PriceApprovalItemVo>> entry : groupByDepotItem.entrySet()) {
            Long depotItemId = entry.getKey();
            List<PriceApprovalItemVo> group = entry.getValue();
            BigDecimal sumAllPrice = BigDecimal.ZERO;
            BigDecimal sumTaxMoney = BigDecimal.ZERO;
            BigDecimal sumTaxLastMoney = BigDecimal.ZERO;
            BigDecimal sumWeight = BigDecimal.ZERO;
            StringBuilder remarkBuilder = new StringBuilder();
            for (PriceApprovalItemVo v : group) {
                if (v.getAllPrice() != null) sumAllPrice = sumAllPrice.add(v.getAllPrice());
                if (v.getTaxMoney() != null) sumTaxMoney = sumTaxMoney.add(v.getTaxMoney());
                if (v.getTaxLastMoney() != null) sumTaxLastMoney = sumTaxLastMoney.add(v.getTaxLastMoney());
                if (v.getWeight() != null) sumWeight = sumWeight.add(v.getWeight());
                if (v.getRemark() != null && !v.getRemark().isEmpty()) {
                    if (remarkBuilder.length() > 0) remarkBuilder.append("；");
                    remarkBuilder.append(v.getRemark());
                }
            }
            // 计算回写单价：金额 / 重量（重量为单价计量维度）
            BigDecimal unitPrice = BigDecimal.ZERO;
            if (sumWeight.compareTo(BigDecimal.ZERO) > 0) {
                unitPrice = sumAllPrice.divide(sumWeight, 6, RoundingMode.HALF_UP);
            } else if (group.size() == 1 && group.get(0).getUnitPrice() != null) {
                unitPrice = group.get(0).getUnitPrice();
            }
            DepotItem itemUpd = new DepotItem();
            itemUpd.setId(depotItemId);
            itemUpd.setUnitPrice(unitPrice);
            itemUpd.setAllPrice(sumAllPrice);
            itemUpd.setTaxMoney(sumTaxMoney);
            itemUpd.setTaxLastMoney(sumTaxLastMoney);
            itemUpd.setRemark(remarkBuilder.toString());
            depotItemMapper.updateByPrimaryKeySelective(itemUpd);
        }
    }

    // ─── 取消核准 ────────────────────────────────────────────────

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void cancelApproval(Long approvalId) throws Exception {
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(approvalId);
        if (pa == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "核准单不存在");
        }
        if (!"1".equals(pa.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "该记录未核准，无需取消");
        }
        // 检查是否被对账单引用
        int refCount = priceApprovalMapper.countStatementItemsByApprovalId(approvalId);
        if (refCount > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    "该核准记录已被对账单引用，无法取消核准");
        }

        User user = userService.getCurrentUser();
        pa.setStatus("0");
        pa.setUpdateTime(new Date());
        pa.setUpdater(user.getId());
        priceApprovalMapper.updateByPrimaryKey(pa);

        // 清零 depot_head
        DepotHead headUpd = new DepotHead();
        headUpd.setId(pa.getDepotHeadId());
        headUpd.setTotalPrice(BigDecimal.ZERO);
        headUpd.setDiscountLastMoney(BigDecimal.ZERO);
        headUpd.setPriceApproved("0");
        headUpd.setRemark("");
        depotHeadMapper.updateByPrimaryKeySelective(headUpd);

        // 清零 depot_item 的价格字段
        List<PriceApprovalItemVo> items = priceApprovalMapper.getApprovalItems(approvalId);
        Set<Long> depotItemIds = items.stream()
                .map(PriceApprovalItemVo::getDepotItemId)
                .collect(Collectors.toSet());
        for (Long depotItemId : depotItemIds) {
            DepotItem itemUpd = new DepotItem();
            itemUpd.setId(depotItemId);
            itemUpd.setUnitPrice(BigDecimal.ZERO);
            itemUpd.setAllPrice(BigDecimal.ZERO);
            itemUpd.setTaxMoney(BigDecimal.ZERO);
            itemUpd.setTaxLastMoney(BigDecimal.ZERO);
            itemUpd.setRemark("");
            depotItemMapper.updateByPrimaryKeySelective(itemUpd);
        }
    }

    // ─── 删除（软删） ────────────────────────────────────────────

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteApproval(Long approvalId) throws Exception {
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(approvalId);
        if (pa == null) return;
        if ("1".equals(pa.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已核准的记录不能删除，请先取消核准");
        }
        // 硬删明细
        priceApprovalMapper.deleteItemsByApprovalId(approvalId);
        // 软删头部
        pa.setDeleteFlag("1");
        priceApprovalMapper.updateByPrimaryKey(pa);
    }

    // ─── 查询方法 ────────────────────────────────────────────────

    public List<Map<String, Object>> listApprovals(Long organId, String status, String billNo,
                                                    String beginTime, String endTime,
                                                    Integer offset, Integer rows) throws Exception {
        Long tenantId = getTenantId();
        return priceApprovalMapper.listApprovals(organId, status, billNo, beginTime, endTime, tenantId, offset, rows);
    }

    public int countApprovals(Long organId, String status, String billNo,
                              String beginTime, String endTime) throws Exception {
        Long tenantId = getTenantId();
        return priceApprovalMapper.countApprovals(organId, status, billNo, beginTime, endTime, tenantId);
    }

    public Map<String, Object> getDetail(Long id) throws Exception {
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(id);
        List<PriceApprovalItemVo> items = priceApprovalMapper.getApprovalItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("header", pa);
        result.put("items", items);
        return result;
    }

    public List<Map<String, Object>> listAvailableSaleOut(Long organId, String billNo,
                                                          String beginTime, String endTime,
                                                          Integer offset, Integer rows) throws Exception {
        Long tenantId = getTenantId();
        return priceApprovalMapper.listAvailableSaleOut(organId, billNo, beginTime, endTime, tenantId, offset, rows);
    }

    public int countAvailableSaleOut(Long organId, String billNo,
                                     String beginTime, String endTime) throws Exception {
        Long tenantId = getTenantId();
        return priceApprovalMapper.countAvailableSaleOut(organId, billNo, beginTime, endTime, tenantId);
    }

    // ─── 工具方法 ────────────────────────────────────────────────

    private Long getTenantId() throws Exception {
        User user = userService.getCurrentUser();
        return user == null ? null : user.getTenantId();
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
