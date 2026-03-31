package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 过磅重量差异报表VO
 */
public class DepotItemVo4WeightDiff {

    private Long headerId;

    private String billNo;

    private String subType;

    private Date billTime;

    private String billTimeStr;

    private Long itemId;

    private String barCode;

    private String materialName;

    private String standard;

    private String model;

    private String organName;

    private String depotName;

    private BigDecimal basicNumber;

    private BigDecimal unitWeight;

    private BigDecimal theoreticalWeight;

    private BigDecimal actualWeight;

    private BigDecimal weightDiff;

    public Long getHeaderId() {
        return headerId;
    }

    public void setHeaderId(Long headerId) {
        this.headerId = headerId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public Date getBillTime() {
        return billTime;
    }

    public void setBillTime(Date billTime) {
        this.billTime = billTime;
    }

    public String getBillTimeStr() {
        return billTimeStr;
    }

    public void setBillTimeStr(String billTimeStr) {
        this.billTimeStr = billTimeStr;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getDepotName() {
        return depotName;
    }

    public void setDepotName(String depotName) {
        this.depotName = depotName;
    }

    public BigDecimal getBasicNumber() {
        return basicNumber;
    }

    public void setBasicNumber(BigDecimal basicNumber) {
        this.basicNumber = basicNumber;
    }

    public BigDecimal getUnitWeight() {
        return unitWeight;
    }

    public void setUnitWeight(BigDecimal unitWeight) {
        this.unitWeight = unitWeight;
    }

    public BigDecimal getTheoreticalWeight() {
        return theoreticalWeight;
    }

    public void setTheoreticalWeight(BigDecimal theoreticalWeight) {
        this.theoreticalWeight = theoreticalWeight;
    }

    public BigDecimal getActualWeight() {
        return actualWeight;
    }

    public void setActualWeight(BigDecimal actualWeight) {
        this.actualWeight = actualWeight;
    }

    public BigDecimal getWeightDiff() {
        return weightDiff;
    }

    public void setWeightDiff(BigDecimal weightDiff) {
        this.weightDiff = weightDiff;
    }
}
