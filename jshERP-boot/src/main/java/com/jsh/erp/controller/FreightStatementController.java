package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.rbac.RbacMode;
import com.jsh.erp.rbac.RbacPermission;
import com.jsh.erp.service.FreightStatementService;
import com.jsh.erp.utils.BaseResponseInfo;
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

@RestController
@RequestMapping(value = "/freightStatement")
@Api(tags = {"物流对账单"})
@RbacPermission(resource = "/freight/reconciliation")
public class FreightStatementController {
    private Logger logger = LoggerFactory.getLogger(FreightStatementController.class);

    @Resource
    private FreightStatementService freightStatementService;

    @GetMapping("/unreconciledItems")
    @ApiOperation(value = "未对账物流单列表")
    public BaseResponseInfo unreconciledItems(
            @RequestParam(value = "carrierId", required = false) Long carrierId,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<Map<String, Object>> rows = freightStatementService.listUnreconciledItems(carrierId, beginTime, endTime, offset, pageSize);
            int total = freightStatementService.countUnreconciledItems(carrierId, beginTime, endTime);
            Map<String, Object> data = new HashMap<>();
            data.put("rows", rows);
            data.put("total", total);
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @PostMapping("/generate")
    @ApiOperation(value = "生成物流对账单")
    public BaseResponseInfo generate(@RequestBody JSONObject params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long carrierId = params.getLong("carrierId");
            List<Long> itemIds = params.getJSONArray("itemIds").toJavaList(Long.class);
            String remark = params.getString("remark");
            String beginTime = params.getString("beginTime");
            String endTime = params.getString("endTime");
            Long id = freightStatementService.generateStatement(carrierId, itemIds, remark, beginTime, endTime);
            res.code = 200; res.data = id;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @GetMapping("/list")
    @ApiOperation(value = "物流对账单列表")
    public BaseResponseInfo list(
            @RequestParam(value = "carrierId", required = false) Long carrierId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "signStatus", required = false) String signStatus,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<Map<String, Object>> rows = freightStatementService.listStatements(carrierId, status, signStatus, beginTime, endTime, offset, pageSize);
            int total = freightStatementService.countStatements(carrierId, status, signStatus, beginTime, endTime);
            Map<String, Object> data = new HashMap<>();
            data.put("rows", rows);
            data.put("total", total);
            res.code = 200; res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @GetMapping("/detail")
    @ApiOperation(value = "对账单详情")
    public BaseResponseInfo detail(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            res.code = 200;
            res.data = freightStatementService.getDetail(id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @PutMapping("/audit")
    @ApiOperation(value = "审核/反审核")
    @RbacPermission(mode = RbacMode.AUDIT_STATUS, statusField = "status")
    public BaseResponseInfo audit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String status = params.get("status").toString();
            freightStatementService.audit(id, status);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @PutMapping("/sign")
    @ApiOperation(value = "签署/取消签署")
    public BaseResponseInfo sign(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String signStatus = params.get("signStatus").toString();
            freightStatementService.sign(id, signStatus);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @PutMapping("/updateAttachment")
    @ApiOperation(value = "更新附件")
    public BaseResponseInfo updateAttachment(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String attachment = (String) params.get("attachment");
            freightStatementService.updateAttachment(id, attachment);
            res.code = 200; res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "删除对账单")
    public BaseResponseInfo delete(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            freightStatementService.deleteStatement(id);
            res.code = 200; res.data = "删除成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500; res.data = e.getMessage();
        }
        return res;
    }
}
