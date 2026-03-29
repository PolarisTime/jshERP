package com.jsh.erp.datasource.entities;

/**
 * 运费单主表扩展VO，包含结算方名称和创建人名称
 */
public class FreightHeadVo extends FreightHead {

    private String carrierName;

    private String creatorName;

    private String billTimeStr;

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getBillTimeStr() {
        return billTimeStr;
    }

    public void setBillTimeStr(String billTimeStr) {
        this.billTimeStr = billTimeStr;
    }
}
