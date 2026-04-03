package com.jsh.erp.controller;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.entities.Contract;
import com.jsh.erp.datasource.entities.ContractEx;
import com.jsh.erp.service.ContractService;
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

/**
 * 合同管理Controller
 */
@RestController
@RequestMapping(value = "/contract")
@Api(tags = {"合同管理"})
public class ContractController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Resource
    private ContractService contractService;

    /**
     * 合同列表查询（分页）
     */
    @GetMapping(value = "/list")
    @ApiOperation(value = "获取合同列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request) throws Exception {
        String contractNo = StringUtil.getInfo(search, "contractNo");
        String contractName = StringUtil.getInfo(search, "contractName");
        String organIdStr = StringUtil.getInfo(search, "organId");
        String status = StringUtil.getInfo(search, "status");
        Long organId = null;
        if (organIdStr != null && !organIdStr.isEmpty()) {
            organId = Long.parseLong(organIdStr);
        }
        List<ContractEx> list = contractService.select(contractNo, contractName, organId, status);
        return getDataTable(list);
    }

    /**
     * 新增合同
     */
    @PostMapping(value = "/add")
    @ApiOperation(value = "新增合同")
    public String add(@RequestBody Contract contract,
                      HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.add(contract);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 更新合同
     */
    @PutMapping(value = "/update")
    @ApiOperation(value = "更新合同")
    public String update(@RequestBody Contract contract,
                         HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.update(contract);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 删除合同
     */
    @DeleteMapping(value = "/delete")
    @ApiOperation(value = "删除合同")
    public String delete(@RequestParam("id") Long id,
                         HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.delete(id);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 批量删除合同
     */
    @DeleteMapping(value = "/deleteBatch")
    @ApiOperation(value = "批量删除合同")
    public String deleteBatch(@RequestParam("ids") String ids,
                              HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.deleteBatch(ids);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }
}
