package com.jsh.erp.filter;

import com.jsh.erp.service.RedisService;
import com.jsh.erp.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "LogCostFilter", urlPatterns = {"/*"},
        initParams = {@WebInitParam(name = "filterPath",
                      value = "/jshERP-boot/platformConfig/getPlatform#/jshERP-boot/v2/api-docs#/jshERP-boot/webjars#" +
                              "/jshERP-boot/systemConfig/static#/jshERP-boot/api/plugin/wechat/weChat/share#" +
                              "/jshERP-boot/api/plugin/general-ledger/pdf/voucher#/jshERP-boot/api/plugin/tenant-statistics/tenantClean")})
public class LogCostFilter implements Filter {

    private static final String FILTER_PATH = "filterPath";

    private String[] allowUrls;
    @Resource
    private RedisService redisService;

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
        // 路径穿越检测
        if(requestUrl.contains("..") || requestUrl.contains("%2e") || requestUrl.contains("%2E")) {
            servletResponse.setStatus(400);
            servletResponse.getWriter().write("loginOut");
            return;
        }

        // 提取 token：优先 Authorization: Bearer，兼容 X-Access-Token
        String token = JwtUtil.extractToken(servletRequest);

        if(token != null) {
            // JWT 格式 token（新前端）
            if(!JwtUtil.isLegacyToken(token)) {
                // 检查黑名单
                if(redisService.hasKey(JwtUtil.getBlacklistKey(token))) {
                    sendUnauthorized(servletResponse);
                    return;
                }
                // 验证 JWT 签名和有效期（一次解析，复用 Claims）
                Claims claims = JwtUtil.parseToken(token);
                if(claims != null && claims.get("userId") != null) {
                    chain.doFilter(request, response);
                    return;
                }
            } else {
                // 旧格式 token（UUID_tenantId，兼容旧前端）
                Object userId = redisService.getObjectFromSessionByKey(servletRequest, "userId");
                if(userId != null) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        // 白名单 URL 放行
        if (requestUrl.equals("/jshERP-boot/doc.html") || requestUrl.equals("/jshERP-boot/user/login")
                || requestUrl.equals("/jshERP-boot/user/register") || requestUrl.equals("/jshERP-boot/user/weixinLogin")
                || requestUrl.equals("/jshERP-boot/user/weixinBind") || requestUrl.equals("/jshERP-boot/user/registerUser")
                || requestUrl.equals("/jshERP-boot/user/randomImage")) {
            chain.doFilter(request, response);
            return;
        }
        if (null != allowUrls && allowUrls.length > 0) {
            for (String url : allowUrls) {
                if (requestUrl.startsWith(url)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        // 未认证
        sendUnauthorized(servletResponse);
    }

    /**
     * 返回 401 未授权响应
     */
    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"loginOut\"}");
    }

    @Override
    public void destroy() {
    }
}
