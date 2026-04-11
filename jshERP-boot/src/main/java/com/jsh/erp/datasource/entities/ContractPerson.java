package com.jsh.erp.datasource.entities;

/**
 * 合同授权人员（签收人 / 对账人）
 */
public class ContractPerson {
    private Long id;
    private Long contractId;
    /** 类型：签收人 / 对账人 */
    private String type;
    private String name;
    private String phone;
    private String deleteFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(String deleteFlag) { this.deleteFlag = deleteFlag; }
}
