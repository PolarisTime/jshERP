package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;

/**
 * 价格核准单明细实体
 */
public class PriceApprovalItem {

    private Long id;

    /** 关联核准单头ID */
    private Long approvalId;

    /** 关联原出库明细ID（同ID多行=拆分组） */
    private Long depotItemId;

    private Long materialId;

    private Long materialExtendId;

    private String barCode;

    private String name;

    /** 规格 */
    private String standard;

    /** 型号 */
    private String model;

    /** 颜色 */
    private String color;

    /** 品牌 */
    private String brand;

    /** 件数（只读，同组相同） */
    private BigDecimal operNumber;

    /** 重量 */
    private BigDecimal weight;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 */
    private BigDecimal allPrice;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxMoney;

    /** 价税合计 */
    private BigDecimal taxLastMoney;

    private String remark;

    private Long tenantId;

    private String deleteFlag;

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

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(String deleteFlag) { this.deleteFlag = deleteFlag; }
}
