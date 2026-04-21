package com.jsh.erp.filter;

import com.jsh.erp.service.RedisService;
import com.jsh.erp.service.UserService;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "LogCostFilter", urlPatterns = {"/*"},
        initParams = {@WebInitParam(name = "filterPath",
                      value = "/jshERP-boot/platformConfig/getPlatform/name#/jshERP-boot/platformConfig/getPlatform/url#" +
                              "/jshERP-boot/platformConfig/getPlatform/registerFlag#/jshERP-boot/platformConfig/getPlatform/checkcodeFlag#" +
                              "/jshERP-boot/v3/api-docs#/jshERP-boot/swagger-ui")})
public class LogCostFilter implements Filter {

    private static final String FILTER_PATH = "filterPath";
    private static final String STATIC_PATH = "/systemConfig/static";
    private static final String SWAGGER_UI_PATH = "/jshERP-boot/swagger-ui";

    private String[] allowUrls;
    @Resource
    private RedisService redisService;
    @Resource
    private UserService userService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String filterPath = filterConfig.getInitParameter(FILTER_PATH);
        if (!StringUtils.isEmpty(filterPath)) {
            allowUrls = filterPath.contains("#") ? filterPath.split("#") : new String[]{filterPath};
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        String requestUrl = servletRequest.getRequestURI();
        if(requestUrl.contains("..") || requestUrl.contains("%2e") || requestUrl.contains("%2E")) {
            writeLoginOut(servletRequest, servletResponse);
            return;
        }
        if (isPublicPath(requestUrl)) {
            chain.doFilter(request, response);
            return;
        }
        if (isStaticPath(servletRequest)) {
            Object staticUserId = redisService.getObjectFromStaticSessionByKey(servletRequest, "userId");
            if (isValidUser(staticUserId)) {
                chain.doFilter(request, response);
                return;
            }
            writeLoginOut(servletRequest, servletResponse);
            return;
        }
        Object userId = redisService.getObjectFromSessionByKey(servletRequest,"userId");
        if(isValidUser(userId)) {
            chain.doFilter(request, response);
            return;
        }
        if(userId!=null) {
            redisService.deleteSessionByRequest(servletRequest);
        }
        writeLoginOut(servletRequest, servletResponse);
    }

    private boolean isValidUser(Object userId) {
        if (userId == null) {
            return false;
        }
        try {
            Long currentUserId = Long.parseLong(userId.toString());
            return userService.isUserSessionAvailable(currentUserId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isStaticPath(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String contextPath = request.getContextPath();
        String prefix = (contextPath == null ? "" : contextPath) + STATIC_PATH;
        return requestUrl.equals(prefix) || requestUrl.startsWith(prefix + "/");
    }

    private boolean isPublicPath(String requestUrl) {
        if (requestUrl.equals("/jshERP-boot/doc.html") || requestUrl.equals("/jshERP-boot/user/login")
                || requestUrl.equals("/jshERP-boot/user/register") || requestUrl.equals("/jshERP-boot/user/weixinLogin")
                || requestUrl.equals("/jshERP-boot/user/weixinBind") || requestUrl.equals("/jshERP-boot/user/registerUser")
                || requestUrl.equals("/jshERP-boot/user/randomImage")) {
            return true;
        }
        if (null != allowUrls && allowUrls.length > 0) {
            for (String url : allowUrls) {
                if (SWAGGER_UI_PATH.equals(url)) {
                    if (requestUrl.equals(url) || requestUrl.startsWith(url + "/")) {
                        return true;
                    }
                } else if (requestUrl.equals(url)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeLoginOut(HttpServletRequest request, HttpServletResponse servletResponse) throws IOException {
        servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String requestUrl = request.getRequestURI();
        if(!requestUrl.equals("/jshERP-boot/user/logout") && !requestUrl.equals("/jshERP-boot/function/findMenuByPNumber")) {
            servletResponse.getWriter().write("loginOut");
        }
    }

    @Override
    public void destroy() {

    }
}
