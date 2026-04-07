package com.jsh.erp.controller;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.entities.FreightCarrier;
import com.jsh.erp.service.FreightCarrierService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.Constants;
import com.jsh.erp.utils.ErpInfo;
import com.jsh.erp.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;
import com.jsh.erp.service.UserService;
import static com.jsh.erp.utils.ResponseJsonUtil.returnForbidden;

/**
 * 运费结算方Controller
 */
@RestController
@RequestMapping(value = "/freightCarrier")
@Api(tags = {"运费结算方"})
public class FreightCarrierController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(FreightCarrierController.class);

        @Resource
    private UserService userService;

    @Resource
    private FreightCarrierService freightCarrierService;

    /**
     * 结算方列表查询（分页）
     */
    @GetMapping(value = "/list")
    @ApiOperation(value = "获取结算方列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request) throws Exception {
        String name = StringUtil.getInfo(search, "name");
        List<FreightCarrier> list = freightCarrierService.select(name);
        return getDataTable(list);
    }

    /**
     * 查询所有启用的结算方（下拉框用）
     */
    @GetMapping(value = "/selectAll")
    @ApiOperation(value = "获取全部结算方")
    public BaseResponseInfo selectAll(HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<FreightCarrier> list = freightCarrierService.selectAll();
            res.code = 200;
            res.data = list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 新增结算方
     */
    @PostMapping(value = "/add")
    @ApiOperation(value = "新增结算方")
    public String add(@RequestBody FreightCarrier freightCarrier,
                      HttpServletRequest request) throws Exception {
        if (!userService.isCurrentUserAdmin()) return returnForbidden();
        Map<String, Object> objectMap = new HashMap<>();
        int res = freightCarrierService.add(freightCarrier);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 更新结算方
     */
    @PutMapping(value = "/update")
    @ApiOperation(value = "更新结算方")
    public String update(@RequestBody FreightCarrier freightCarrier,
                         HttpServletRequest request) throws Exception {
        if (!userService.isCurrentUserAdmin()) return returnForbidden();
        Map<String, Object> objectMap = new HashMap<>();
        int res = freightCarrierService.update(freightCarrier);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 删除结算方
     */
    @DeleteMapping(value = "/delete")
    @ApiOperation(value = "删除结算方")
    public String delete(@RequestParam("id") Long id,
                         HttpServletRequest request) throws Exception {
        if (!userService.isCurrentUserAdmin()) return returnForbidden();
        Map<String, Object> objectMap = new HashMap<>();
        int res = freightCarrierService.delete(id);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 批量删除结算方
     */
    @DeleteMapping(value = "/deleteBatch")
    @ApiOperation(value = "批量删除结算方")
    public String deleteBatch(@RequestParam("ids") String ids,
                              HttpServletRequest request) throws Exception {
        if (!userService.isCurrentUserAdmin()) return returnForbidden();
        Map<String, Object> objectMap = new HashMap<>();
        int res = freightCarrierService.deleteBatch(ids);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }
}
