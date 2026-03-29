package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;

public class TotalWeightVo {

    private Long headerId;

    private BigDecimal totalWeight;

    public Long getHeaderId() {
        return headerId;
    }

    public void setHeaderId(Long headerId) {
        this.headerId = headerId;
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }
}
