package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格核准单明细 VO（含关联展示字段）
 */
public class PriceApprovalItemVo {

    /** price_approval_item.id */
    private Long id;

    private Long approvalId;

    private Long depotItemId;

    private Long materialId;

    private Long materialExtendId;

    private String barCode;

    private String name;

    private String standard;

    private String model;

    private String color;

    private String brand;

    /** 件数 */
    private BigDecimal operNumber;

    /** 重量 */
    private BigDecimal weight;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 */
    private BigDecimal allPrice;

    private BigDecimal taxRate;

    private BigDecimal taxMoney;

    private BigDecimal taxLastMoney;

    private String remark;

    // ─── 关联展示字段 ────────────────────────────────────────────

    /** 出库单号 */
    private String billNo;

    /** 出库日期（格式化） */
    private String billTimeStr;

    /** 销售人员 */
    private String salesMan;

    /** 商品类别 */
    private String categoryName;

    /** 客户名称 */
    private String customerName;

    /** 项目名称 */
    private String projectName;

    /** 件重(吨/件) */
    private BigDecimal unitWeight;

    /** 制造商 */
    private String mfrs;

    /** depot_item原始重量（拆分组校验用） */
    private BigDecimal originalWeight;

    /** 批号 */
    private String batchNumber;

    /** SKU */
    private String sku;

    /** 单位 */
    private String materialUnit;

    /** 仓库名称 */
    private String depotName;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }

    public Long getDepotItemId() { return depotItemId; }
    public void setDepotItemId(Long depotItemId) { this.depotItemId = depotItemId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public Long getMaterialExtendId() { return materialExtendId; }
    public void setMaterialExtendId(Long materialExtendId) { this.materialExtendId = materialExtendId; }

    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getOperNumber() { return operNumber; }
    public void setOperNumber(BigDecimal operNumber) { this.operNumber = operNumber; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAllPrice() { return allPrice; }
    public void setAllPrice(BigDecimal allPrice) { this.allPrice = allPrice; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getTaxMoney() { return taxMoney; }
    public void setTaxMoney(BigDecimal taxMoney) { this.taxMoney = taxMoney; }

    public BigDecimal getTaxLastMoney() { return taxLastMoney; }
    public void setTaxLastMoney(BigDecimal taxLastMoney) { this.taxLastMoney = taxLastMoney; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public String getBillTimeStr() { return billTimeStr; }
    public void setBillTimeStr(String billTimeStr) { this.billTimeStr = billTimeStr; }

    public String getSalesMan() { return salesMan; }
    public void setSalesMan(String salesMan) { this.salesMan = salesMan; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public BigDecimal getUnitWeight() { return unitWeight; }
    public void setUnitWeight(BigDecimal unitWeight) { this.unitWeight = unitWeight; }

    public String getMfrs() { return mfrs; }
    public void setMfrs(String mfrs) { this.mfrs = mfrs; }

    public BigDecimal getOriginalWeight() { return originalWeight; }
    public void setOriginalWeight(BigDecimal originalWeight) { this.originalWeight = originalWeight; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getMaterialUnit() { return materialUnit; }
    public void setMaterialUnit(String materialUnit) { this.materialUnit = materialUnit; }

    public String getDepotName() { return depotName; }
    public void setDepotName(String depotName) { this.depotName = depotName; }
}
