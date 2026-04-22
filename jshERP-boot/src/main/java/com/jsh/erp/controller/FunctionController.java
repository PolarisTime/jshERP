package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.service.FunctionService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;
import static com.jsh.erp.utils.ResponseJsonUtil.returnStr;

/**
 * @author ji-sheng-hua  jshERP
 */
@RestController
@RequestMapping(value = "/function")
@Tag(name = "功能管理")
public class FunctionController extends BaseController {
    private static final Long TENANT_MANAGEMENT_FUNCTION_ID = 18L;
    private static final Long REMOVED_PLUGIN_FUNCTION_ID = 245L;
    private Logger logger = LoggerFactory.getLogger(FunctionController.class);

    @Resource
    private FunctionService functionService;

    @Resource
    private UserService userService;

    @Resource
    private UserBusinessService userBusinessService;

    @Resource
    private SystemConfigService systemConfigService;

    @GetMapping(value = "/info")
    @Operation(summary = "根据id获取信息")
    public String getList(@RequestParam("id") Long id,
                          HttpServletRequest request) throws Exception {
        assertSystemAdmin();
        Function function = functionService.getFunction(id);
        Map<String, Object> objectMap = new HashMap<>();
        if(function != null) {
            objectMap.put("info", function);
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    @GetMapping(value = "/list")
    @Operation(summary = "获取信息列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        String name = StringUtil.getInfo(search, "name");
        String type = StringUtil.getInfo(search, "type");
        List<FunctionEx> list = functionService.select(name, type);
        return getDataTable(list);
    }

    @PostMapping(value = "/add")
    @Operation(summary = "新增")
    public String addResource(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<>();
        int insert = functionService.insertFunction(obj, request);
        return returnStr(objectMap, insert);
    }

    @PutMapping(value = "/update")
    @Operation(summary = "修改")
    public String updateResource(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<>();
        int update = functionService.updateFunction(obj, request);
        return returnStr(objectMap, update);
    }

    @DeleteMapping(value = "/delete")
    @Operation(summary = "删除")
    public String deleteResource(@RequestParam("id") Long id, HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<>();
        int delete = functionService.deleteFunction(id, request);
        return returnStr(objectMap, delete);
    }

    @DeleteMapping(value = "/deleteBatch")
    @Operation(summary = "批量删除")
    public String batchDeleteResource(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<>();
        int delete = functionService.batchDeleteFunction(ids, request);
        return returnStr(objectMap, delete);
    }

    @GetMapping(value = "/checkIsNameExist")
    @Operation(summary = "检查名称是否存在")
    public String checkIsNameExist(@RequestParam Long id, @RequestParam(value ="name", required = false) String name,
                                   HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<>();
        int exist = functionService.checkIsNameExist(id, name);
        if(exist > 0) {
            objectMap.put("status", true);
        } else {
            objectMap.put("status", false);
        }
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    @GetMapping(value = "/checkIsNumberExist")
    @Operation(summary = "检查编号是否存在")
    public String checkIsNumberExist(@RequestParam Long id,
                                     @RequestParam(value ="number", required = false) String number,
                                     HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        int exist = functionService.checkIsNumberExist(id, number);
        if(exist > 0) {
            objectMap.put("status", true);
        } else {
            objectMap.put("status", false);
        }
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * 根据父编号查询菜单
     * @param jsonObject
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/findMenuByPNumber")
    @Operation(summary = "根据父编号查询菜单")
    public JSONArray findMenuByPNumber(@RequestBody JSONObject jsonObject,
                              HttpServletRequest request)throws Exception {
        String pNumber = jsonObject.getString("pNumber");
        String userId = jsonObject.getString("userId");
        //存放数据json数组
        JSONArray dataArray = new JSONArray();
        try {
            Long roleId = 0L;
            String fc = "";
            List<UserBusiness> roleList = userBusinessService.getBasicData(userId, "UserRole");
            if(roleList!=null && roleList.size()>0){
                String value = roleList.get(0).getValue();
                if(StringUtil.isNotEmpty(value)){
                    String roleIdStr = value.replace("[", "").replace("]", "");
                    roleId = Long.parseLong(roleIdStr);
                }
            }
            //当前用户所拥有的功能列表，格式如：[1][2][5]
            List<UserBusiness> funList = userBusinessService.getBasicData(roleId.toString(), "RoleFunctions");
            if(funList!=null && funList.size()>0){
                fc = funList.get(0).getValue();
            }
            //获取系统配置信息-是否开启多级审核
            String approvalFlag = "0";
            List<SystemConfig> list = systemConfigService.getSystemConfig();
            if(list.size()>0) {
                approvalFlag = list.get(0).getMultiLevelApprovalFlag();
            }

            List<Function> dataList = functionService.getRoleFunction(pNumber);
            if (dataList.size() != 0) {
                User userInfo = userService.getCurrentUser();
                dataArray = getMenuByFunction(dataList, fc, approvalFlag, userInfo);
                //增加首页菜单项
                JSONObject homeItem = new JSONObject();
                homeItem.put("id", 0);
                homeItem.put("text", "首页");
                homeItem.put("icon", "home");
                homeItem.put("url", "/dashboard/analysis");
                homeItem.put("component", "/layouts/TabLayout");
                dataArray.add(0,homeItem);
            }
        } catch (DataAccessException e) {
            logger.error(">>>>>>>>>>>>>>>>>>>查找异常", e);
        }
        return dataArray;
    }

    /**
     * 系统管理员专属功能ID集合：系统配置、权限管理、平台配置。
     */
    private static final Set<Long> ADMIN_ONLY_FUN_IDS = new HashSet<>(Arrays.asList(13L, 14L, 15L, 16L, 234L, 236L, 243L, 245L, 258L));

    private boolean shouldHideFunction(Function function, boolean isAdmin) {
        Long functionId = function.getId();
        if (functionId == null) {
            return false;
        }
        if (TENANT_MANAGEMENT_FUNCTION_ID.equals(functionId) || REMOVED_PLUGIN_FUNCTION_ID.equals(functionId)) {
            return true;
        }
        return !isAdmin && ADMIN_ONLY_FUN_IDS.contains(functionId);
    }

    public JSONArray getMenuByFunction(List<Function> dataList, String fc, String approvalFlag, User userInfo) throws Exception {
        JSONArray dataArray = new JSONArray();
        boolean isAdmin = userService.isSystemAdmin(userInfo);
        for (Function function : dataList) {
            if (shouldHideFunction(function, isAdmin)) {
                continue;
            }
            if("0".equals(approvalFlag) && "/workflow".equals(function.getUrl())) {
                continue;
            }
            JSONObject item = new JSONObject();
            List<Function> newList = functionService.getRoleFunction(function.getNumber());
            item.put("id", function.getId());
            item.put("text", function.getName());
            item.put("icon", function.getIcon());
            item.put("url", function.getUrl());
            item.put("component", function.getComponent());
            JSONArray childrenArr = new JSONArray();
            if (newList.size() > 0) {
                childrenArr = getMenuByFunction(newList, fc, approvalFlag, userInfo);
                if(childrenArr.size() > 0) {
                    item.put("children", childrenArr);
                }
            }
            boolean hasPermission = isAdmin || fc.indexOf("[" + function.getId() + "]") != -1;
            if (childrenArr.size() > 0 || hasPermission) {
                dataArray.add(item);
            }
        }
        return dataArray;
    }

    /**
     * 角色对应功能显示
     * @param request
     * @return
     */
    @GetMapping(value = "/findRoleFunction")
    @Operation(summary = "角色对应功能显示")
    public JSONArray findRoleFunction(@RequestParam("UBType") String type, @RequestParam("UBKeyId") String keyId,
                                 HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        JSONArray arr = new JSONArray();
        try {
            User userInfo = userService.getCurrentUser();
            //获取当前用户所拥有的功能id列表
            List<Long> funIdList = functionService.getCurrentUserFunIdList();
            if(userService.isSystemAdmin(userInfo)) {
                funIdList = null;
            }
            List<Function> dataListFun = functionService.findRoleFunction("0", null);
            //开始拼接json数据
            JSONObject outer = new JSONObject();
            outer.put("id", 0);
            outer.put("key", 0);
            outer.put("value", 0);
            outer.put("title", "功能列表");
            outer.put("attributes", "功能列表");
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataListFun) {
                dataArray = getFunctionList(dataListFun, type, keyId, funIdList);
                outer.put("children", dataArray);
            }
            arr.add(outer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return arr;
    }

    public JSONArray getFunctionList(List<Function> dataList, String type, String keyId, List<Long> funIdList) throws Exception {
        JSONArray dataArray = new JSONArray();
        //获取权限信息
        String ubValue = userBusinessService.getUBValueByTypeAndKeyId(type, keyId);
        if (null != dataList) {
            for (Function function : dataList) {
                if (TENANT_MANAGEMENT_FUNCTION_ID.equals(function.getId())
                        || REMOVED_PLUGIN_FUNCTION_ID.equals(function.getId())) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("id", function.getId());
                item.put("key", function.getId());
                item.put("value", function.getId());
                item.put("title", function.getName());
                item.put("attributes", function.getName());
                List<Function> funList = functionService.findRoleFunction(function.getNumber(), funIdList);
                if(funList.size()>0) {
                    JSONArray funArr = getFunctionList(funList, type, keyId, funIdList);
                    item.put("children", funArr);
                    dataArray.add(item);
                } else {
                    Boolean flag = ubValue.contains("[" + function.getId().toString() + "]");
                    item.put("checked", flag);
                    dataArray.add(item);
                }
            }
        }
        return dataArray;
    }

    /**
     * 根据id列表查找功能信息
     * @param roleId
     * @param request
     * @return
     */
    @GetMapping(value = "/findRoleFunctionsById")
    @Operation(summary = "根据id列表查找功能信息")
    public BaseResponseInfo findByIds(@RequestParam("roleId") Long roleId,
                                      HttpServletRequest request)throws Exception {
        assertSystemAdmin();
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<UserBusiness> list = userBusinessService.getBasicData(roleId.toString(), "RoleFunctions");
            if(null!=list && list.size()>0) {
                //按钮
                Map<Long,String> btnMap = new HashMap<>();
                String btnStr = list.get(0).getBtnStr();
                if(StringUtil.isNotEmpty(btnStr)) {
                    JSONArray btnArr = JSONArray.parseArray(btnStr);
                    for(Object obj: btnArr) {
                        JSONObject btnObj = JSONObject.parseObject(obj.toString());
                        if(btnObj.get("funId")!=null && btnObj.get("btnStr")!=null) {
                            btnMap.put(btnObj.getLong("funId"), btnObj.getString("btnStr"));
                        }
                    }
                }
                //菜单
                String funIds = list.get(0).getValue();
                funIds = funIds.substring(1, funIds.length() - 1);
                funIds = funIds.replace("][",",");
                List<Function> dataList = functionService.findByIds(funIds);
                JSONObject outer = new JSONObject();
                User userInfo = userService.getCurrentUser();
                Map<Long, Long> funIdMap = functionService.getCurrentUserFunIdMap();
                boolean isSystemAdmin = userService.isSystemAdmin(userInfo);
                //存放数据json数组
                JSONArray dataArray = new JSONArray();
                if (null != dataList) {
                    for (Function function : dataList) {
                        //如果不是超管需要校验，防止分配下级用户的按钮权限，大于自身的权限
                        if(isSystemAdmin || funIdMap.get(function.getId())!=null) {
                            JSONObject item = new JSONObject();
                            item.put("id", function.getId());
                            item.put("name", function.getName());
                            item.put("pushBtn", function.getPushBtn());
                            item.put("btnStr", btnMap.get(function.getId()));
                            dataArray.add(item);
                        }
                    }
                }
                outer.put("rows", dataArray);
                outer.put("total", dataArray.size());
                res.code = 200;
                res.data = outer;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    private void assertSystemAdmin() throws Exception {
        userService.assertCurrentUserSystemAdmin();
    }
}
