package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.PrintTemplate;
import com.jsh.erp.datasource.mappers.PrintTemplateMapperEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 打印模板Service
 */
@Service
public class PrintTemplateService {
    private Logger logger = LoggerFactory.getLogger(PrintTemplateService.class);

    @Resource
    private PrintTemplateMapperEx printTemplateMapperEx;

    public PrintTemplate getDefaultByBillType(String billType) {
        try {
            return printTemplateMapperEx.selectDefaultByBillType(billType);
        } catch (Exception e) {
            logger.error("获取打印模板异常", e);
            return null;
        }
    }

    public List<PrintTemplate> listByBillType(String billType) {
        try {
            return printTemplateMapperEx.selectListByBillType(billType);
        } catch (Exception e) {
            logger.error("获取打印模板列表异常", e);
            return new ArrayList<>();
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveTemplate(Long id, String billType, String templateName, String templateHtml, String isDefault) throws Exception {
        //设置为默认时先清除同类型其他模板的默认标记
        if ("1".equals(isDefault)) {
            printTemplateMapperEx.clearDefaultByBillType(billType, id);
        }
        if (id != null && id > 0) {
            printTemplateMapperEx.updateById(id, templateName, templateHtml, isDefault);
        } else {
            printTemplateMapperEx.insertTemplate(billType, templateName, templateHtml, isDefault);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteById(Long id) throws Exception {
        printTemplateMapperEx.deleteById(id);
    }

    /**
     * 根据单据类型返回可用字段元数据
     */
    public Map<String, Object> getFieldMeta(String billType) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> headerFields = new ArrayList<>();
        List<Map<String, String>> detailFields = new ArrayList<>();

        // 通用主表字段
        if ("freightBill".equals(billType)) {
            headerFields.add(field("billNo", "单据编号"));
            headerFields.add(field("billTimeStr", "单据日期"));
            headerFields.add(field("carrierName", "结算方"));
            headerFields.add(field("unitPrice", "单价(元/吨)"));
            headerFields.add(field("totalWeight", "总重量(吨)"));
            headerFields.add(field("totalFreight", "总运费(元)"));
            headerFields.add(field("remark", "备注"));
            headerFields.add(field("status", "状态"));
            headerFields.add(field("customerName", "客户名称"));
            headerFields.add(field("projectName", "项目名称"));
            headerFields.add(field("projectAddress", "项目地址"));
            // 物流单明细字段 —— 出库单信息
            detailFields.add(field("billNo", "出库单号"));
            detailFields.add(field("billTimeStr", "出库日期"));
            detailFields.add(field("linkNumber", "关联订单号"));
            detailFields.add(field("salesMan", "业务员"));
            detailFields.add(field("remark", "出库单备注"));
            // 客户信息
            detailFields.add(field("customerName", "客户名称"));
            detailFields.add(field("projectName", "项目名称"));
            detailFields.add(field("projectAddress", "项目地址"));
            detailFields.add(field("contacts", "联系人"));
            detailFields.add(field("telephone", "联系电话"));
            detailFields.add(field("address", "收货地址"));
            // 商品信息
            detailFields.add(field("materialName", "名称"));
            detailFields.add(field("categoryName", "商品类别"));
            detailFields.add(field("standard", "规格"));
            detailFields.add(field("model", "型号/材质"));
            detailFields.add(field("color", "颜色"));
            detailFields.add(field("brand", "品牌"));
            detailFields.add(field("mfrs", "制造商"));
            detailFields.add(field("position", "仓位货架"));
            detailFields.add(field("materialRemark", "商品备注"));
            detailFields.add(field("batchNumber", "批号"));
            // 数量与金额
            detailFields.add(field("operNumber", "数量"));
            detailFields.add(field("materialUnit", "单位"));
            detailFields.add(field("unitWeight", "件重"));
            detailFields.add(field("itemWeight", "重量(吨)"));
            detailFields.add(field("unitPrice", "单价"));
            detailFields.add(field("allPrice", "金额"));
            detailFields.add(field("taxRate", "税率(%)"));
            detailFields.add(field("taxMoney", "税额"));
            detailFields.add(field("taxLastMoney", "价税合计"));
            detailFields.add(field("depotName", "仓库"));
        } else {
            // 出入库单据通用主表字段
            headerFields.add(field("organName", "往来单位"));
            headerFields.add(field("number", "单据编号"));
            headerFields.add(field("operTimeStr", "单据日期"));
            headerFields.add(field("projectName", "项目名称"));
            headerFields.add(field("projectAddress", "项目地址"));
            headerFields.add(field("contacts", "联系人"));
            headerFields.add(field("telephone", "联系电话"));
            headerFields.add(field("address", "地址"));
            headerFields.add(field("linkNumber", "关联订单"));
            headerFields.add(field("freightBillNo", "物流单号"));
            headerFields.add(field("salesMan", "销售人员"));
            headerFields.add(field("creatorName", "制单人"));
            headerFields.add(field("discount", "优惠率"));
            headerFields.add(field("discountMoney", "收款优惠"));
            headerFields.add(field("discountLastMoney", "优惠后金额"));
            headerFields.add(field("otherMoney", "其他费用"));
            headerFields.add(field("deposit", "订金"));
            headerFields.add(field("accountName", "结算账户"));
            headerFields.add(field("changeAmount", "本次付/收款"));
            headerFields.add(field("debt", "本次欠款"));
            headerFields.add(field("totalWeight", "总重量(吨)"));
            headerFields.add(field("remark", "备注"));
            headerFields.add(field("payType", "付款类型"));
            headerFields.add(field("bankName", "开户行"));
            headerFields.add(field("accountNumber", "账号"));
            headerFields.add(field("taxNum", "纳税人识别号"));
            headerFields.add(field("supplierCompany", "供货单位(=客户)"));
            headerFields.add(field("customerName", "收货单位(=客户)"));
            headerFields.add(field("carNo", "车号(用户输入)"));
            headerFields.add(field("sendDate", "送货日期(用户输入)"));
            headerFields.add(field("totalPiece", "总件数(自动汇总)"));
            // 出入库明细字段
            detailFields.add(field("depotName", "仓库名称"));
            detailFields.add(field("barCode", "条码"));
            detailFields.add(field("name", "名称"));
            detailFields.add(field("standard", "规格"));
            detailFields.add(field("model", "型号"));
            detailFields.add(field("color", "颜色"));
            detailFields.add(field("brand", "品牌"));
            detailFields.add(field("mfrs", "制造商"));
            detailFields.add(field("unit", "单位"));
            detailFields.add(field("sku", "多属性"));
            detailFields.add(field("batchNumber", "批号"));
            detailFields.add(field("position", "仓位货架"));
            detailFields.add(field("operNumber", "数量"));
            detailFields.add(field("unitWeight", "件重"));
            detailFields.add(field("unitPrice", "单价"));
            detailFields.add(field("allPrice", "金额"));
            detailFields.add(field("taxRate", "税率(%)"));
            detailFields.add(field("taxMoney", "税额"));
            detailFields.add(field("taxLastMoney", "价税合计"));
            detailFields.add(field("weight", "重量"));
            detailFields.add(field("remark", "备注"));
            detailFields.add(field("otherField1", "自定义1"));
            detailFields.add(field("otherField2", "自定义2"));
            detailFields.add(field("otherField3", "自定义3"));
            detailFields.add(field("categoryName", "商品类别"));
            detailFields.add(field("length", "长度(条码派生)"));
        }

        result.put("headerFields", headerFields);
        result.put("detailFields", detailFields);
        return result;
    }

    private Map<String, String> field(String key, String label) {
        Map<String, String> m = new HashMap<>();
        m.put("key", key);
        m.put("label", label);
        return m;
    }
}
