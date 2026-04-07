package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;

/**
 * 合同扩展实体（列表查询用）
 */
public class ContractEx extends Contract {

    private String organName;

    /** 已送货金额（统计自销售出库） */
    private BigDecimal deliveredAmount;

    /** 已送货吨位（统计自销售出库） */
    private BigDecimal deliveredTonnage;

    public String getOrganName() { return organName; }
    public void setOrganName(String organName) { this.organName = organName; }

    public BigDecimal getDeliveredAmount() { return deliveredAmount; }
    public void setDeliveredAmount(BigDecimal deliveredAmount) { this.deliveredAmount = deliveredAmount; }

    public BigDecimal getDeliveredTonnage() { return deliveredTonnage; }
    public void setDeliveredTonnage(BigDecimal deliveredTonnage) { this.deliveredTonnage = deliveredTonnage; }
}
