package com.jsh.erp.rbac;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.Function;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.service.FunctionService;
import com.jsh.erp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RbacService {
    private static final Logger logger = LoggerFactory.getLogger(RbacService.class);
    private static final long FUNCTION_CACHE_TTL_MS = 60_000L;
    private static final String BUTTON_PATTERN = "[%s]";

    @Resource
    private UserService userService;

    @Resource
    private FunctionService functionService;

    private volatile long functionCacheTime = 0L;
    private volatile Map<String, String> functionPushBtnMap = Collections.emptyMap();
    private final Set<String> missingFunctionWarnings = ConcurrentHashMap.newKeySet();

    public User getCurrentUserOrNull() {
        try {
            return userService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAdmin(User user) {
        return UserService.isAdmin(user);
    }

    public CurrentUserRbac loadCurrentUserRbac(User user) throws Exception {
        if (user == null) {
            return CurrentUserRbac.empty();
        }
        if (isAdmin(user)) {
            return CurrentUserRbac.admin();
        }
        List<Long> funIdList = functionService.getCurrentUserFunIdList();
        Map<Long, String> functionIdUrlMap = loadFunctionIdUrlMap();
        Set<String> resourceSet = ConcurrentHashMap.newKeySet();
        for (Long funId : funIdList) {
            String url = functionIdUrlMap.get(funId);
            if (url != null && !url.isEmpty()) {
                resourceSet.add(url);
            }
        }
        Map<String, String> buttonMap = new LinkedHashMap<>();
        JSONArray btnArr = userService.getBtnStrArrById(user.getId());
        if (btnArr != null) {
            for (Object obj : btnArr) {
                JSONObject jsonObject = JSONObject.parseObject(obj.toString());
                String url = jsonObject.getString("url");
                if (url != null && !url.isEmpty()) {
                    buttonMap.put(url, jsonObject.getString("btnStr"));
                }
            }
        }
        return new CurrentUserRbac(false, resourceSet, buttonMap);
    }

    public boolean hasFunctionDefinition(String resource) {
        refreshFunctionCacheIfNeeded();
        boolean exists = functionPushBtnMap.containsKey(resource);
        if (!exists && missingFunctionWarnings.add(resource)) {
            logger.warn("RBAC resource is not defined in jsh_function, fallback to login-only: {}", resource);
        }
        return exists;
    }

    public boolean resourceHasButtonDefinition(String resource, int button) {
        refreshFunctionCacheIfNeeded();
        String pushBtn = functionPushBtnMap.get(resource);
        if (pushBtn == null || pushBtn.trim().isEmpty() || "0".equals(pushBtn.trim())) {
            return false;
        }
        String[] btnArray = pushBtn.split(",");
        for (String item : btnArray) {
            if (String.valueOf(button).equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasResource(CurrentUserRbac currentUserRbac, String resource) {
        return currentUserRbac.isAdmin() || currentUserRbac.getResources().contains(resource);
    }

    public boolean hasButton(CurrentUserRbac currentUserRbac, String resource, int button) {
        if (currentUserRbac.isAdmin()) {
            return true;
        }
        String btnStr = currentUserRbac.getButtons().get(resource);
        if (btnStr == null || btnStr.isEmpty()) {
            return false;
        }
        return btnStr.contains(String.format(BUTTON_PATTERN, button));
    }

    public Integer resolveStatusButton(Object[] args, String statusField) {
        Object value = findFieldValue(args, statusField);
        if (value == null) {
            return 1;
        }
        String status = String.valueOf(value).trim();
        if ("1".equals(status) || "true".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("0".equals(status) || "false".equalsIgnoreCase(status)) {
            return 7;
        }
        return 1;
    }

    private Map<Long, String> loadFunctionIdUrlMap() throws Exception {
        List<Function> functionList = functionService.getFunction();
        Map<Long, String> functionIdUrlMap = new HashMap<>();
        for (Function function : functionList) {
            functionIdUrlMap.put(function.getId(), function.getUrl());
        }
        return functionIdUrlMap;
    }

    private void refreshFunctionCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - functionCacheTime < FUNCTION_CACHE_TTL_MS && !functionPushBtnMap.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (now - functionCacheTime < FUNCTION_CACHE_TTL_MS && !functionPushBtnMap.isEmpty()) {
                return;
            }
            try {
                List<Function> functionList = functionService.getFunction();
                Map<String, String> newMap = new HashMap<>();
                for (Function function : functionList) {
                    if (function.getUrl() != null && !function.getUrl().isEmpty()) {
                        newMap.put(function.getUrl(), function.getPushBtn());
                    }
                }
                functionPushBtnMap = newMap;
                functionCacheTime = now;
            } catch (Exception e) {
                logger.error("refresh function cache failed", e);
            }
        }
    }

    private Object findFieldValue(Object[] args, String fieldName) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) arg;
                if (map.containsKey(fieldName)) {
                    return map.get(fieldName);
                }
                continue;
            }
            if (arg instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) arg;
                if (jsonObject.containsKey(fieldName)) {
                    return jsonObject.get(fieldName);
                }
                continue;
            }
            Object getterValue = invokeGetter(arg, fieldName);
            if (getterValue != null) {
                return getterValue;
            }
            Object fieldValue = readField(arg, fieldName);
            if (fieldValue != null) {
                return fieldValue;
            }
        }
        return null;
    }

    private Object invokeGetter(Object arg, String fieldName) {
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = arg.getClass().getMethod(getterName);
            return getter.invoke(arg);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readField(Object arg, String fieldName) {
        Class<?> clazz = arg.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(arg);
            } catch (Exception e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static class CurrentUserRbac {
        private final boolean admin;
        private final Set<String> resources;
        private final Map<String, String> buttons;

        public CurrentUserRbac(boolean admin, Set<String> resources, Map<String, String> buttons) {
            this.admin = admin;
            this.resources = resources;
            this.buttons = buttons;
        }

        public static CurrentUserRbac admin() {
            return new CurrentUserRbac(true, Collections.<String>emptySet(), Collections.<String, String>emptyMap());
        }

        public static CurrentUserRbac empty() {
            return new CurrentUserRbac(false, Collections.<String>emptySet(), Collections.<String, String>emptyMap());
        }

        public boolean isAdmin() {
            return admin;
        }

        public Set<String> getResources() {
            return resources;
        }

        public Map<String, String> getButtons() {
            return buttons;
        }
    }
}
