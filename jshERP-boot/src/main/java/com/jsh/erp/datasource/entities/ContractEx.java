package com.jsh.erp.datasource.entities;

/**
 * 合同扩展实体（列表查询用，包含关联的客户名称和项目名称）
 */
public class ContractEx extends Contract {

    private String organName;

    private String projectName;

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
