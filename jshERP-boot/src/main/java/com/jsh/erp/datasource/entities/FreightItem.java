package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;

/**
 * 运费单明细实体类
 */
public class FreightItem {
    private Long id;

    private Long headerId;

    private Long depotHeadId;

    private String depotNumber;

    private BigDecimal weight;

    private String remark;

    private String deleteFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHeaderId() {
        return headerId;
    }

    public void setHeaderId(Long headerId) {
        this.headerId = headerId;
    }

    public Long getDepotHeadId() {
        return depotHeadId;
    }

    public void setDepotHeadId(Long depotHeadId) {
        this.depotHeadId = depotHeadId;
    }

    public String getDepotNumber() {
        return depotNumber;
    }

    public void setDepotNumber(String depotNumber) {
        this.depotNumber = depotNumber == null ? null : depotNumber.trim();
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public String getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(String deleteFlag) {
        this.deleteFlag = deleteFlag == null ? null : deleteFlag.trim();
    }
}
