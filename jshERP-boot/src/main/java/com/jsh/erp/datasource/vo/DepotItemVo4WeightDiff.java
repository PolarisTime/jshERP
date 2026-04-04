package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;

/**
 * 长短款报表VO — 采购入库理论重量 vs 销售出库过磅重量差异
 * 按供应商+商品+批号汇总
 */
public class DepotItemVo4WeightDiff {

    /** 供应商名称 */
    private String organName;

    /** 商品名称 */
    private String materialName;

    /** 规格 */
    private String standard;

    /** 型号 */
    private String model;

    /** 条码 */
    private String barCode;

    /** 批号 */
    private String batchNumber;

    /** 数量（件） */
    private BigDecimal operNumber;

    /** 采购入库单号（可能多个逗号分隔） */
    private String purchaseBillNo;

    /** 入库理论重量(吨) = SUM(basic_number × m.weight) */
    private BigDecimal inTheoreticalWeight;

    /** 销售出库单号（可能多个逗号分隔） */
    private String saleBillNo;

    /** 出库过磅重量(吨) = SUM(di_sale.weight) */
    private BigDecimal outActualWeight;

    /** 差额(吨) = 入库理论重量 - 出库过磅重量 */
    private BigDecimal weightDiff;

    /** 采购单价（加权平均） */
    private BigDecimal purchaseUnitPrice;

    /** 差额金额 = 差额 × 采购单价 */
    private BigDecimal diffAmount;

    /** 仓库名称 */
    private String depotName;

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public BigDecimal getOperNumber() {
        return operNumber;
    }

    public void setOperNumber(BigDecimal operNumber) {
        this.operNumber = operNumber;
    }

    public String getPurchaseBillNo() {
        return purchaseBillNo;
    }

    public void setPurchaseBillNo(String purchaseBillNo) {
        this.purchaseBillNo = purchaseBillNo;
    }

    public BigDecimal getInTheoreticalWeight() {
        return inTheoreticalWeight;
    }

    public void setInTheoreticalWeight(BigDecimal inTheoreticalWeight) {
        this.inTheoreticalWeight = inTheoreticalWeight;
    }

    public String getSaleBillNo() {
        return saleBillNo;
    }

    public void setSaleBillNo(String saleBillNo) {
        this.saleBillNo = saleBillNo;
    }

    public BigDecimal getOutActualWeight() {
        return outActualWeight;
    }

    public void setOutActualWeight(BigDecimal outActualWeight) {
        this.outActualWeight = outActualWeight;
    }

    public BigDecimal getWeightDiff() {
        return weightDiff;
    }

    public void setWeightDiff(BigDecimal weightDiff) {
        this.weightDiff = weightDiff;
    }

    public BigDecimal getPurchaseUnitPrice() {
        return purchaseUnitPrice;
    }

    public void setPurchaseUnitPrice(BigDecimal purchaseUnitPrice) {
        this.purchaseUnitPrice = purchaseUnitPrice;
    }

    public BigDecimal getDiffAmount() {
        return diffAmount;
    }

    public void setDiffAmount(BigDecimal diffAmount) {
        this.diffAmount = diffAmount;
    }

    public String getDepotName() {
        return depotName;
    }

    public void setDepotName(String depotName) {
        this.depotName = depotName;
    }
}
