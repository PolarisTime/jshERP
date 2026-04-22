package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格核准单头实体
 */
public class PriceApproval {

    private Long id;

    /** 核准单号，格式 HZ+yyyyMMdd+4位序号 */
    private String approvalNo;

    /** 关联销售出库单ID */
    private Long depotHeadId;

    /** 客户ID */
    private Long organId;

    /** 送到日期 */
    private Date deliveryDate;

    private String remark;

    /** 总重量 */
    private BigDecimal totalWeight;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 0=待核准 1=已核准 */
    private String status;

    private Date createTime;

    private Long creator;

    private Date updateTime;

    private Long updater;

    private String deleteFlag;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }

    public Long getDepotHeadId() { return depotHeadId; }
    public void setDepotHeadId(Long depotHeadId) { this.depotHeadId = depotHeadId; }

    public Long getOrganId() { return organId; }
    public void setOrganId(Long organId) { this.organId = organId; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Long getCreator() { return creator; }
    public void setCreator(Long creator) { this.creator = creator; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Long getUpdater() { return updater; }
    public void setUpdater(Long updater) { this.updater = updater; }

    public String getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(String deleteFlag) { this.deleteFlag = deleteFlag; }
}
