package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.PrintTemplate;
import com.jsh.erp.datasource.mappers.PrintTemplateMapperEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 打印模板Service
 */
@Service
public class PrintTemplateService {
    private Logger logger = LoggerFactory.getLogger(PrintTemplateService.class);

    @Value(value="${file.printTemplatePath:/opt/jshERP/printTemplates}")
    private String printTemplatePath;

    @Resource
    private PrintTemplateMapperEx printTemplateMapperEx;

    /**
     * 应用启动时初始化打印模板目录结构
     */
    @javax.annotation.PostConstruct
    public void initTemplateDirectories() {
        try {
            for (String dirName : new java.util.LinkedHashSet<>(BILL_TYPE_DIR_MAP.values())) {
                Path dir = Paths.get(printTemplatePath, dirName);
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                    logger.info("创建打印模板目录: {}", dir);
                }
            }
        } catch (IOException e) {
            logger.error("初始化打印模板目录失败", e);
        }
    }

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

    // ═══ billType → 目录名映射 ═══

    private static final Map<String, String> BILL_TYPE_DIR_MAP = new LinkedHashMap<>();
    static {
        BILL_TYPE_DIR_MAP.put("saleOut", "销售管理");
        BILL_TYPE_DIR_MAP.put("saleOrder", "销售管理");
        BILL_TYPE_DIR_MAP.put("saleBack", "销售管理");
        BILL_TYPE_DIR_MAP.put("purchaseIn", "采购管理");
        BILL_TYPE_DIR_MAP.put("purchaseOrder", "采购管理");
        BILL_TYPE_DIR_MAP.put("purchaseBack", "采购管理");
        BILL_TYPE_DIR_MAP.put("purchaseApply", "采购管理");
        BILL_TYPE_DIR_MAP.put("freightBill", "物流单模版");
        BILL_TYPE_DIR_MAP.put("otherIn", "仓库管理");
        BILL_TYPE_DIR_MAP.put("otherOut", "仓库管理");
        BILL_TYPE_DIR_MAP.put("allocationOut", "仓库管理");
        BILL_TYPE_DIR_MAP.put("assemble", "仓库管理");
        BILL_TYPE_DIR_MAP.put("disassemble", "仓库管理");
        BILL_TYPE_DIR_MAP.put("retailOut", "零售管理");
        BILL_TYPE_DIR_MAP.put("retailBack", "零售管理");
        BILL_TYPE_DIR_MAP.put("customerStatement", "对账单模版");
        BILL_TYPE_DIR_MAP.put("freightStatement", "物流对账模版");
    }

    private String getDirForBillType(String billType) {
        return BILL_TYPE_DIR_MAP.getOrDefault(billType, "其他");
    }

    // ═══ 文件系统模板读写 ═══

    /**
     * 获取指定billType对应目录下的所有模板文件
     * @return 模板列表，每个包含 name(模板名), content(内容), source("file")
     */
    public List<Map<String, String>> listFileTemplates(String billType) {
        List<Map<String, String>> result = new ArrayList<>();
        String dirName = getDirForBillType(billType);
        Path dir = Paths.get(printTemplatePath, dirName);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{lodop,html}")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                String content = new String(Files.readAllBytes(entry), StandardCharsets.UTF_8);
                Map<String, String> item = new HashMap<>();
                item.put("name", name);
                item.put("content", content);
                item.put("source", "file");
                item.put("fileName", fileName);
                result.add(item);
            }
        } catch (IOException e) {
            logger.error("读取打印模板目录异常: {}", dir, e);
        }
        // 按文件名排序
        result.sort(Comparator.comparing(m -> m.get("name")));
        return result;
    }

    /**
     * 保存模板到文件
     */
    public void saveFileTemplate(String billType, String name, String content) throws IOException {
        String dirName = getDirForBillType(billType);
        Path dir = Paths.get(printTemplatePath, dirName);
        Files.createDirectories(dir);
        // 根据内容判断扩展名：包含 LODOP. 的为 .lodop，否则 .html
        String ext = content.contains("LODOP.") ? ".lodop" : ".html";
        // 清除文件名非法字符
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "");
        Path file = dir.resolve(safeName + ext);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 删除模板文件
     */
    public boolean deleteFileTemplate(String billType, String fileName) throws IOException {
        String dirName = getDirForBillType(billType);
        Path file = Paths.get(printTemplatePath, dirName, fileName);
        // 安全检查：确保不会跨目录
        if (!file.normalize().startsWith(Paths.get(printTemplatePath, dirName).normalize())) {
            throw new SecurityException("非法文件路径");
        }
        return Files.deleteIfExists(file);
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
            headerFields.add(field("saleRemark", "出库单备注"));
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
