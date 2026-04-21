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

    @Resource
    private SystemConfigService systemConfigService;

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
            if (item.getDepotItemId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "存在未关联原始出库明细的记录");
            }
            if (item.getWeight() == null || item.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                        "明细行重量必须大于0：" + item.getName() + " " + item.getStandard());
            }
            items.add(item);
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

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void splitApprovalItem(Long approvalItemId, List<BigDecimal> splitWeights) throws Exception {
        if (approvalItemId == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "请选择需要拆分的明细");
        }
        if (splitWeights == null || splitWeights.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "请至少拆分为两行");
        }

        PriceApprovalItem source = priceApprovalMapper.selectItemById(approvalItemId);
        if (source == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "待拆分明细不存在");
        }

        PriceApproval approval = priceApprovalMapper.selectByPrimaryKey(source.getApprovalId());
        if (approval == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "核准单不存在");
        }
        User user = userService.getCurrentUser();
        Long tenantId = user == null ? null : user.getTenantId();
        if (tenantId != null && approval.getTenantId() != null && !tenantId.equals(approval.getTenantId())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "无权操作当前明细");
        }
        if ("1".equals(approval.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已核准的明细不能拆分");
        }

        BigDecimal sourceWeight = normalizeWeight(source.getWeight());
        BigDecimal totalWeight = BigDecimal.ZERO;
        boolean materialPriceTaxFlag = systemConfigService.getMaterialPriceTaxFlag();
        List<PriceApprovalItem> newItems = new ArrayList<>();
        for (int i = 0; i < splitWeights.size(); i++) {
            BigDecimal weight = normalizeWeight(splitWeights.get(i));
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "拆分后的重量必须大于0");
            }
            totalWeight = totalWeight.add(weight);

            PriceApprovalItem item = new PriceApprovalItem();
            item.setApprovalId(source.getApprovalId());
            item.setDepotItemId(source.getDepotItemId());
            item.setMaterialId(source.getMaterialId());
            item.setMaterialExtendId(source.getMaterialExtendId());
            item.setBarCode(source.getBarCode());
            item.setName(source.getName());
            item.setStandard(source.getStandard());
            item.setModel(source.getModel());
            item.setColor(source.getColor());
            item.setBrand(source.getBrand());
            item.setOperNumber(source.getOperNumber());
            item.setWeight(weight);
            item.setUnitPrice(source.getUnitPrice());
            item.setTaxRate(source.getTaxRate());
            item.setRemark(i == 0 ? source.getRemark() : "");

            BigDecimal[] moneyValues = calcMoneyValues(weight, source.getUnitPrice(), source.getTaxRate(), materialPriceTaxFlag);
            item.setAllPrice(moneyValues[0]);
            item.setTaxMoney(moneyValues[1]);
            item.setTaxLastMoney(moneyValues[2]);
            newItems.add(item);
        }

        if (totalWeight.compareTo(sourceWeight) != 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "拆分重量合计必须等于原始重量");
        }

        priceApprovalMapper.deleteItemById(source.getId());
        priceApprovalMapper.batchInsertItems(newItems);
        refreshApprovalTotals(approval, user);
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

        // 计算总金额（不含税）和含税合计
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;
        for (PriceApprovalItemVo item : items) {
            if (item.getAllPrice() != null) {
                totalAmount = totalAmount.add(item.getAllPrice());
            }
            if (item.getTaxLastMoney() != null) {
                totalTaxLastMoney = totalTaxLastMoney.add(item.getTaxLastMoney());
            }
        }

        // 核准状态置1
        User user = userService.getCurrentUser();
        pa.setStatus("1");
        pa.setTotalAmount(totalAmount);
        pa.setUpdateTime(new Date());
        pa.setUpdater(user.getId());
        priceApprovalMapper.updateByPrimaryKey(pa);

        // 回写 depot_head：价格核准同时确认重量、价格和备注
        DepotHead headUpd = new DepotHead();
        headUpd.setId(pa.getDepotHeadId());
        headUpd.setTotalPrice(totalAmount);
        headUpd.setDiscountLastMoney(totalTaxLastMoney);
        headUpd.setDiscount(BigDecimal.ZERO);
        headUpd.setDiscountMoney(BigDecimal.ZERO);
        headUpd.setPriceApproved("1");
        headUpd.setWeightApproved("1");
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
            itemUpd.setWeight(sumWeight);
            itemUpd.setUnitPrice(unitPrice);
            itemUpd.setAllPrice(sumAllPrice);
            itemUpd.setTaxMoney(sumTaxMoney);
            itemUpd.setTaxLastMoney(sumTaxLastMoney);
            itemUpd.setRemark(remarkBuilder.toString());
            depotItemMapper.updateByPrimaryKeySelective(itemUpd);
        }
    }

    /**
     * 销售出库修改重量后，同步刷新价格核准单明细/表头，
     * 若该核准单已确认，则同时回写销售出库金额。
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void syncByDepotHeadAfterWeightChange(Long depotHeadId) throws Exception {
        Long tenantId = getTenantId();
        PriceApproval brief = priceApprovalMapper.selectByDepotHeadId(depotHeadId, tenantId);
        if (brief == null) {
            return;
        }
        PriceApproval pa = priceApprovalMapper.selectByPrimaryKey(brief.getId());
        if (pa == null) {
            return;
        }

        List<PriceApprovalItemVo> items = priceApprovalMapper.getApprovalItems(pa.getId());
        if (items == null || items.isEmpty()) {
            return;
        }

        boolean materialPriceTaxFlag = systemConfigService.getMaterialPriceTaxFlag();
        User user = userService.getCurrentUser();
        Date now = new Date();

        Map<Long, List<PriceApprovalItemVo>> groupByDepotItem = items.stream()
                .collect(Collectors.groupingBy(PriceApprovalItemVo::getDepotItemId, LinkedHashMap::new, Collectors.toList()));

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;

        for (Map.Entry<Long, List<PriceApprovalItemVo>> entry : groupByDepotItem.entrySet()) {
            List<PriceApprovalItemVo> group = entry.getValue();
            BigDecimal currentGroupWeight = normalizeWeight(group.get(0).getOriginalWeight());
            List<BigDecimal> distributedWeights = distributeGroupWeights(group, currentGroupWeight);

            BigDecimal groupAllPrice = BigDecimal.ZERO;
            BigDecimal groupTaxMoney = BigDecimal.ZERO;
            BigDecimal groupTaxLastMoney = BigDecimal.ZERO;
            StringBuilder remarkBuilder = new StringBuilder();

            for (int i = 0; i < group.size(); i++) {
                PriceApprovalItemVo itemVo = group.get(i);
                BigDecimal newWeight = distributedWeights.get(i);
                BigDecimal[] moneyValues = calcMoneyValues(newWeight, itemVo.getUnitPrice(), itemVo.getTaxRate(), materialPriceTaxFlag);

                PriceApprovalItem itemUpd = new PriceApprovalItem();
                itemUpd.setId(itemVo.getId());
                itemUpd.setWeight(newWeight);
                itemUpd.setAllPrice(moneyValues[0]);
                itemUpd.setTaxMoney(moneyValues[1]);
                itemUpd.setTaxLastMoney(moneyValues[2]);
                priceApprovalMapper.updateItemByPrimaryKeySelective(itemUpd);

                itemVo.setWeight(newWeight);
                itemVo.setAllPrice(moneyValues[0]);
                itemVo.setTaxMoney(moneyValues[1]);
                itemVo.setTaxLastMoney(moneyValues[2]);

                groupAllPrice = groupAllPrice.add(moneyValues[0]);
                groupTaxMoney = groupTaxMoney.add(moneyValues[1]);
                groupTaxLastMoney = groupTaxLastMoney.add(moneyValues[2]);

                if (itemVo.getRemark() != null && !itemVo.getRemark().isEmpty()) {
                    if (remarkBuilder.length() > 0) {
                        remarkBuilder.append("；");
                    }
                    remarkBuilder.append(itemVo.getRemark());
                }
            }

            totalWeight = totalWeight.add(currentGroupWeight);
            totalAmount = totalAmount.add(groupAllPrice);
            totalTaxLastMoney = totalTaxLastMoney.add(groupTaxLastMoney);

            if ("1".equals(pa.getStatus())) {
                BigDecimal groupUnitPrice = BigDecimal.ZERO;
                if (currentGroupWeight.compareTo(BigDecimal.ZERO) > 0) {
                    groupUnitPrice = groupAllPrice.divide(currentGroupWeight, 6, RoundingMode.HALF_UP);
                } else if (group.size() == 1 && group.get(0).getUnitPrice() != null) {
                    groupUnitPrice = group.get(0).getUnitPrice();
                }
                DepotItem depotItemUpd = new DepotItem();
                depotItemUpd.setId(entry.getKey());
                depotItemUpd.setUnitPrice(groupUnitPrice);
                depotItemUpd.setAllPrice(groupAllPrice);
                depotItemUpd.setTaxMoney(groupTaxMoney);
                depotItemUpd.setTaxLastMoney(groupTaxLastMoney);
                depotItemUpd.setRemark(remarkBuilder.toString());
                depotItemMapper.updateByPrimaryKeySelective(depotItemUpd);
            }
        }

        pa.setTotalWeight(totalWeight);
        pa.setTotalAmount(totalAmount);
        pa.setUpdateTime(now);
        pa.setUpdater(user != null ? user.getId() : null);
        priceApprovalMapper.updateByPrimaryKey(pa);

        if ("1".equals(pa.getStatus())) {
            DepotHead headUpd = new DepotHead();
            headUpd.setId(pa.getDepotHeadId());
            headUpd.setTotalPrice(totalAmount);
            headUpd.setDiscountLastMoney(totalTaxLastMoney);
            headUpd.setRemark(pa.getRemark());
            depotHeadMapper.updateByPrimaryKeySelective(headUpd);
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

        // 重置 depot_head 的核准标记和已回写金额
        DepotHead headUpd = new DepotHead();
        headUpd.setId(pa.getDepotHeadId());
        headUpd.setTotalPrice(BigDecimal.ZERO);
        headUpd.setDiscountLastMoney(BigDecimal.ZERO);
        headUpd.setPriceApproved("0");
        headUpd.setWeightApproved("0");
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

    private void refreshApprovalTotals(PriceApproval approval, User user) {
        List<PriceApprovalItemVo> items = priceApprovalMapper.getApprovalItems(approval.getId());
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PriceApprovalItemVo item : items) {
            if (item.getWeight() != null) {
                totalWeight = totalWeight.add(item.getWeight());
            }
            if (item.getAllPrice() != null) {
                totalAmount = totalAmount.add(item.getAllPrice());
            }
        }
        approval.setTotalWeight(totalWeight);
        approval.setTotalAmount(totalAmount);
        approval.setUpdateTime(new Date());
        approval.setUpdater(user == null ? null : user.getId());
        priceApprovalMapper.updateByPrimaryKey(approval);
    }

    private BigDecimal normalizeWeight(BigDecimal weight) {
        return weight == null ? BigDecimal.ZERO : weight.setScale(6, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> distributeGroupWeights(List<PriceApprovalItemVo> group, BigDecimal currentGroupWeight) {
        List<BigDecimal> result = new ArrayList<>();
        if (group == null || group.isEmpty()) {
            return result;
        }
        if (group.size() == 1) {
            result.add(currentGroupWeight);
            return result;
        }

        BigDecimal oldGroupWeight = BigDecimal.ZERO;
        for (PriceApprovalItemVo item : group) {
            oldGroupWeight = oldGroupWeight.add(normalizeWeight(item.getWeight()));
        }

        if (oldGroupWeight.compareTo(BigDecimal.ZERO) <= 0) {
            result.add(currentGroupWeight);
            for (int i = 1; i < group.size(); i++) {
                result.add(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            }
            return result;
        }

        BigDecimal allocatedWeight = BigDecimal.ZERO;
        for (int i = 0; i < group.size(); i++) {
            BigDecimal newWeight;
            if (i == group.size() - 1) {
                newWeight = currentGroupWeight.subtract(allocatedWeight).setScale(6, RoundingMode.HALF_UP);
            } else {
                BigDecimal oldWeight = normalizeWeight(group.get(i).getWeight());
                newWeight = currentGroupWeight.multiply(oldWeight)
                        .divide(oldGroupWeight, 6, RoundingMode.HALF_UP);
                allocatedWeight = allocatedWeight.add(newWeight);
            }
            result.add(newWeight);
        }
        return result;
    }

    private BigDecimal[] calcMoneyValues(BigDecimal weight, BigDecimal unitPrice, BigDecimal taxRate,
                                         boolean materialPriceTaxFlag) {
        BigDecimal safeWeight = normalizeWeight(weight);
        BigDecimal safeUnitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal safeTaxRate = taxRate == null ? BigDecimal.ZERO : taxRate;

        BigDecimal allPrice = safeUnitPrice.multiply(safeWeight).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxMoney = BigDecimal.ZERO;
        BigDecimal taxLastMoney = BigDecimal.ZERO;

        if (materialPriceTaxFlag) {
            if (safeTaxRate.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal taxRatePercent = safeTaxRate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                taxMoney = allPrice.divide(BigDecimal.ONE.add(taxRatePercent), 2, RoundingMode.HALF_UP)
                        .multiply(taxRatePercent).setScale(2, RoundingMode.HALF_UP);
            }
            taxLastMoney = allPrice;
        } else {
            if (safeTaxRate.compareTo(BigDecimal.ZERO) != 0) {
                taxMoney = safeTaxRate.multiply(allPrice).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            taxLastMoney = allPrice.add(taxMoney);
        }

        return new BigDecimal[]{allPrice, taxMoney, taxLastMoney};
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
