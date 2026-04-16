package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.entities.Contract;
import com.jsh.erp.datasource.entities.ContractEx;
import com.jsh.erp.datasource.entities.ContractPerson;
import com.jsh.erp.rbac.RbacMode;
import com.jsh.erp.rbac.RbacPermission;
import com.jsh.erp.service.ContractService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;

@RestController
@RequestMapping(value = "/contract")
@Api(tags = {"合同管理"})
@RbacPermission(resource = "/contract/contract_list")
public class ContractController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Resource
    private ContractService contractService;

    // ─── 列表 ─────────────────────────────────────────────────────
    @GetMapping(value = "/list")
    @ApiOperation(value = "合同列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request) throws Exception {
        String contractNo   = StringUtil.getInfo(search, "contractNo");
        String contractName = StringUtil.getInfo(search, "contractName");
        String organIdStr   = StringUtil.getInfo(search, "organId");
        String auditStatus  = StringUtil.getInfo(search, "auditStatus");
        String signStatus   = StringUtil.getInfo(search, "signStatus");
        Long organId = (organIdStr != null && !organIdStr.isEmpty()) ? Long.parseLong(organIdStr) : null;
        List<ContractEx> list = contractService.select(contractNo, contractName, organId, auditStatus, signStatus);
        return getDataTable(list);
    }

    // ─── 详情（含授权人员） ────────────────────────────────────────
    @GetMapping(value = "/detail")
    @ApiOperation(value = "合同详情")
    public BaseResponseInfo detail(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<ContractPerson> persons = contractService.getPersons(id);
            Map<String, Object> data = new HashMap<>();
            data.put("persons", persons);
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    // ─── 新增 ─────────────────────────────────────────────────────
    @PostMapping(value = "/add")
    @ApiOperation(value = "新增合同")
    public String add(@RequestBody Map<String, Object> body, HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        Contract contract = parseContract(body);
        List<ContractPerson> persons = parsePersons(body);
        int res = contractService.add(contract, persons);
        return res > 0 ? returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code)
                       : returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
    }

    // ─── 更新 ─────────────────────────────────────────────────────
    @PutMapping(value = "/update")
    @ApiOperation(value = "更新合同")
    public String update(@RequestBody Map<String, Object> body, HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        Contract contract = parseContract(body);
        List<ContractPerson> persons = parsePersons(body);
        int res = contractService.update(contract, persons);
        return res > 0 ? returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code)
                       : returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
    }

    // ─── 审核 / 反审核 ────────────────────────────────────────────
    @PutMapping(value = "/audit")
    @ApiOperation(value = "审核/反审核")
    @RbacPermission(mode = RbacMode.AUDIT_STATUS, statusField = "auditStatus")
    public BaseResponseInfo audit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String auditStatus = params.get("auditStatus").toString();
            contractService.audit(id, auditStatus);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    // ─── 签署 / 取消签署 ──────────────────────────────────────────
    @PutMapping(value = "/sign")
    @ApiOperation(value = "签署/取消签署")
    public BaseResponseInfo sign(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String signStatus = params.get("signStatus").toString();
            contractService.sign(id, signStatus);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    // ─── 附件 ─────────────────────────────────────────────────────
    @PutMapping(value = "/updateAttachments")
    @ApiOperation(value = "更新附件")
    public BaseResponseInfo updateAttachments(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String attachments = (String) params.get("attachments");
            contractService.updateAttachments(id, attachments);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    // ─── 合同余额 ─────────────────────────────────────────────────
    @GetMapping(value = "/balance")
    @ApiOperation(value = "获取客户合同余额")
    public BaseResponseInfo balance(@RequestParam("organId") Long organId, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            res.code = 200;
            res.data = contractService.getContractBalance(organId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    // ─── 删除 ─────────────────────────────────────────────────────
    @DeleteMapping(value = "/delete")
    @ApiOperation(value = "删除合同")
    public String delete(@RequestParam("id") Long id, HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.delete(id);
        return res > 0 ? returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code)
                       : returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
    }

    @DeleteMapping(value = "/deleteBatch")
    @ApiOperation(value = "批量删除合同")
    public String deleteBatch(@RequestParam("ids") String ids, HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int res = contractService.deleteBatch(ids);
        return res > 0 ? returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code)
                       : returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
    }

    // ─── 工具方法 ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Contract parseContract(Map<String, Object> body) {
        Object contractObj = body.get("contract");
        if (contractObj == null) {
            // 兼容旧格式：整个 body 就是 contract
            contractObj = body;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(contractObj), Contract.class);
    }

    @SuppressWarnings("unchecked")
    private List<ContractPerson> parsePersons(Map<String, Object> body) {
        List<ContractPerson> persons = new ArrayList<>();
        Object pObj = body.get("persons");
        if (pObj instanceof List) {
            for (Object item : (List<?>) pObj) {
                ContractPerson p = JSONObject.parseObject(JSONObject.toJSONString(item), ContractPerson.class);
                persons.add(p);
            }
        }
        return persons;
    }
}
