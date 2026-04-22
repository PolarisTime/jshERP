package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;
import java.util.Date;

public class FreightStatement {
    private Long id;
    private String statementNo;
    private Long carrierId;
    private Date beginTime;
    private Date endTime;
    private BigDecimal totalWeight;
    private BigDecimal totalFreight;
    private BigDecimal paidAmount;
    private String status;
    private String signStatus;
    private String attachment;
    private String remark;
    private String deleteFlag;
    private Date createTime;
    private Long creator;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatementNo() { return statementNo; }
    public void setStatementNo(String statementNo) { this.statementNo = statementNo; }
    public Long getCarrierId() { return carrierId; }
    public void setCarrierId(Long carrierId) { this.carrierId = carrierId; }
    public Date getBeginTime() { return beginTime; }
    public void setBeginTime(Date beginTime) { this.beginTime = beginTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public BigDecimal getTotalFreight() { return totalFreight; }
    public void setTotalFreight(BigDecimal totalFreight) { this.totalFreight = totalFreight; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String signStatus) { this.signStatus = signStatus; }
    public String getAttachment() { return attachment; }
    public void setAttachment(String attachment) { this.attachment = attachment; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(String deleteFlag) { this.deleteFlag = deleteFlag; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Long getCreator() { return creator; }
    public void setCreator(Long creator) { this.creator = creator; }
}
