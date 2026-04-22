package com.jsh.erp.controller;

import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/role")
@Tag(name = "角色管理")
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = true)
public class TenantRoleController {

    @Resource
    private RoleService roleService;

    @GetMapping(value = "/tenantRoleList")
    @Operation(summary = "查询租户角色列表")
    public List<Role> tenantRoleList(HttpServletRequest request) throws Exception {
        return roleService.tenantRoleList();
    }
}
