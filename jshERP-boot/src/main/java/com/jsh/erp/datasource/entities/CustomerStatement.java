package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户对账单主表实体
 */
public class CustomerStatement {

    private Long id;

    /** 对账单号，格式 DZ+yyyyMMdd+4位序号 */
    private String statementNo;

    /** 客户ID */
    private Long organId;

    /** 账期开始 */
    private Date beginTime;

    /** 账期结束 */
    private Date endTime;

    /** 总重量(吨) */
    private BigDecimal totalWeight;

    /** 总金额(元) */
    private BigDecimal totalAmount;

    /** 审核状态：0未审核 1已审核 */
    private String status;

    /** 签署状态：0未签署 1已签署 */
    private String signStatus;

    /** 附件路径，多个逗号分隔 */
    private String attachment;

    private String remark;

    private Long tenantId;

    private String deleteFlag;

    private Date createTime;

    private Long creator;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatementNo() { return statementNo; }
    public void setStatementNo(String statementNo) { this.statementNo = statementNo; }

    public Long getOrganId() { return organId; }
    public void setOrganId(Long organId) { this.organId = organId; }

    public Date getBeginTime() { return beginTime; }
    public void setBeginTime(Date beginTime) { this.beginTime = beginTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String signStatus) { this.signStatus = signStatus; }

    public String getAttachment() { return attachment; }
    public void setAttachment(String attachment) { this.attachment = attachment; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(String deleteFlag) { this.deleteFlag = deleteFlag; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Long getCreator() { return creator; }
    public void setCreator(Long creator) { this.creator = creator; }
}
