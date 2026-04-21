package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 对账单明细行 VO（供未对账列表和对账单详情共用）
 */
public class CustomerStatementItemVo {

    /** price_approval_item.id */
    private Long id;

    /** price_approval.id */
    private Long approvalId;

    /** depot_head.id */
    private Long depotHeadId;

    /** customer/project id */
    private Long organId;

    /** 出库单号 */
    private String billNo;

    /** 出库日期 */
    private Date billTime;

    /** 出库单备注 */
    private String billRemark;

    /** 商品类别 */
    private String categoryName;

    /** 品牌 */
    private String brand;

    /** 材质（material.model） */
    private String model;

    /** 规格（material.standard） */
    private String standard;

    /** 件重(吨/件)，material.weight */
    private BigDecimal unitWeight;

    /** 件数 */
    private BigDecimal operNumber;

    /** 重量小计(吨)，depot_item.weight */
    private BigDecimal itemWeight;

    /** 单价(元/吨) */
    private BigDecimal unitPrice;

    /** 总金额(元) */
    private BigDecimal allPrice;

    /** 客户名称（用于未对账列表展示） */
    private String customerName;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }

    public Long getDepotHeadId() { return depotHeadId; }
    public void setDepotHeadId(Long depotHeadId) { this.depotHeadId = depotHeadId; }

    public Long getOrganId() { return organId; }
    public void setOrganId(Long organId) { this.organId = organId; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Date getBillTime() { return billTime; }
    public void setBillTime(Date billTime) { this.billTime = billTime; }

    public String getBillRemark() { return billRemark; }
    public void setBillRemark(String billRemark) { this.billRemark = billRemark; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    public BigDecimal getUnitWeight() { return unitWeight; }
    public void setUnitWeight(BigDecimal unitWeight) { this.unitWeight = unitWeight; }

    public BigDecimal getOperNumber() { return operNumber; }
    public void setOperNumber(BigDecimal operNumber) { this.operNumber = operNumber; }

    public BigDecimal getItemWeight() { return itemWeight; }
    public void setItemWeight(BigDecimal itemWeight) { this.itemWeight = itemWeight; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAllPrice() { return allPrice; }
    public void setAllPrice(BigDecimal allPrice) { this.allPrice = allPrice; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
