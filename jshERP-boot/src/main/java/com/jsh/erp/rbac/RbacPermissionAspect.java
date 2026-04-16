package com.jsh.erp.rbac;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.rbac.RbacService.CurrentUserRbac;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

@Aspect
@Component
public class RbacPermissionAspect {

    @Resource
    private RbacService rbacService;

    @Pointcut("execution(public * com.jsh.erp.controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        HttpServletResponse response = getCurrentResponse();
        if (request == null || response == null) {
            return joinPoint.proceed();
        }
        if (isWhitelisted(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        PermissionContext permissionContext = buildPermissionContext(joinPoint);
        if (permissionContext.publicAccess) {
            return joinPoint.proceed();
        }

        User currentUser = rbacService.getCurrentUserOrNull();
        if (currentUser == null) {
            writeJson(response, 401, "loginOut");
            return null;
        }
        if (permissionContext.adminOnly && !rbacService.isAdmin(currentUser)) {
            writeJson(response, 403, "无权限");
            return null;
        }
        if (permissionContext.loginOnly || rbacService.isAdmin(currentUser)) {
            return joinPoint.proceed();
        }
        if (permissionContext.resource == null || permissionContext.resource.isEmpty()) {
            return joinPoint.proceed();
        }
        if (!rbacService.hasFunctionDefinition(permissionContext.resource)) {
            return joinPoint.proceed();
        }

        CurrentUserRbac currentUserRbac = rbacService.loadCurrentUserRbac(currentUser);
        if (!rbacService.hasResource(currentUserRbac, permissionContext.resource)) {
            writeJson(response, 403, "无权限");
            return null;
        }
        Integer button = resolveButton(permissionContext, request, joinPoint.getArgs());
        if (button != null
                && rbacService.resourceHasButtonDefinition(permissionContext.resource, button)
                && !rbacService.hasButton(currentUserRbac, permissionContext.resource, button)) {
            writeJson(response, 403, "无权限");
            return null;
        }
        return joinPoint.proceed();
    }

    private PermissionContext buildPermissionContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RbacPermission classPermission = AnnotatedElementUtils.findMergedAnnotation(targetClass, RbacPermission.class);
        RbacPermission methodPermission = AnnotatedElementUtils.findMergedAnnotation(method, RbacPermission.class);

        PermissionContext context = new PermissionContext();
        if (classPermission != null) {
            context.resource = classPermission.resource();
            context.mode = classPermission.mode();
            context.statusField = classPermission.statusField();
            context.publicAccess = classPermission.publicAccess();
            context.loginOnly = classPermission.loginOnly();
            context.adminOnly = classPermission.adminOnly();
        }
        if (methodPermission != null) {
            if (!methodPermission.resource().isEmpty()) {
                context.resource = methodPermission.resource();
            }
            context.mode = methodPermission.mode();
            context.statusField = methodPermission.statusField();
            context.publicAccess = methodPermission.publicAccess();
            context.loginOnly = methodPermission.loginOnly();
            context.adminOnly = methodPermission.adminOnly();
        }
        return context;
    }

    private Integer resolveButton(PermissionContext permissionContext, HttpServletRequest request, Object[] args) {
        RbacMode mode = permissionContext.mode == null ? RbacMode.AUTO : permissionContext.mode;
        switch (mode) {
            case READ:
                return null;
            case WRITE:
                return 1;
            case APPROVE:
                return 2;
            case UNAUDIT:
                return 7;
            case AUDIT_STATUS:
                return rbacService.resolveStatusButton(args, permissionContext.statusField);
            case AUTO:
            default:
                return "GET".equalsIgnoreCase(request.getMethod()) ? null : 1;
        }
    }

    private boolean isWhitelisted(String requestUri) {
        return "/jshERP-boot/doc.html".equals(requestUri)
                || "/jshERP-boot/user/login".equals(requestUri)
                || "/jshERP-boot/user/weixinLogin".equals(requestUri)
                || "/jshERP-boot/user/weixinBind".equals(requestUri)
                || "/jshERP-boot/user/registerUser".equals(requestUri)
                || "/jshERP-boot/user/randomImage".equals(requestUri);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getResponse();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":%s,\"message\":\"%s\"}", code, message));
    }

    private static class PermissionContext {
        private String resource = "";
        private RbacMode mode = RbacMode.AUTO;
        private String statusField = "status";
        private boolean publicAccess = false;
        private boolean loginOnly = false;
        private boolean adminOnly = false;
    }
}
