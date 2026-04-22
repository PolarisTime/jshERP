package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.Tenant;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.utils.Tools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TenantModeService {

    @Value("${tenant.enabled:true}")
    private boolean tenantEnabled;

    @Value("${tenant.userNumLimit:1000000}")
    private Integer defaultUserNumLimit;

    public boolean isEnabled() {
        return tenantEnabled;
    }

    public Long tenantIdOrNull(Long tenantId) {
        return tenantEnabled ? tenantId : null;
    }

    public Long tenantIdFromToken(String token) {
        return tenantEnabled ? Tools.getTenantIdByToken(token) : null;
    }

    public boolean isTenantOwner(User user) {
        return tenantEnabled
                && user != null
                && user.getId() != null
                && user.getTenantId() != null
                && user.getId().equals(user.getTenantId());
    }

    public User sanitizeUser(User user) {
        if (!tenantEnabled && user != null) {
            user.setTenantId(null);
        }
        return user;
    }

    public Tenant buildDefaultTenant(User user) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(user == null ? null : user.getTenantId());
        tenant.setLoginName(user == null ? "single-tenant" : user.getLoginName());
        tenant.setUserNumLimit(defaultUserNumLimit);
        tenant.setType("1");
        tenant.setEnabled(true);
        tenant.setCreateTime(new Date());
        return tenant;
    }
}
