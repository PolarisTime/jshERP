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

    private String paymentOperatorName;

    private String paymentTimeStr;

    public String getPaymentOperatorName() {
        return paymentOperatorName;
    }

    public void setPaymentOperatorName(String paymentOperatorName) {
        this.paymentOperatorName = paymentOperatorName;
    }

    public String getPaymentTimeStr() {
        return paymentTimeStr;
    }

    public void setPaymentTimeStr(String paymentTimeStr) {
        this.paymentTimeStr = paymentTimeStr;
    }

    private String saleOutNumbers;

    private String customerNames;

    private String projectNames;

    public String getSaleOutNumbers() {
        return saleOutNumbers;
    }

    public void setSaleOutNumbers(String saleOutNumbers) {
        this.saleOutNumbers = saleOutNumbers;
    }

    public String getCustomerNames() {
        return customerNames;
    }

    public void setCustomerNames(String customerNames) {
        this.customerNames = customerNames;
    }

    public String getProjectNames() {
        return projectNames;
    }

    public void setProjectNames(String projectNames) {
        this.projectNames = projectNames;
    }
}
