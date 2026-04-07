package com.jsh.erp.controller;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.datasource.vo.CustomerStatementItemVo;
import com.jsh.erp.service.CustomerStatementService;
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

/**
 * 客户对账单 Controller
 */
@RestController
@RequestMapping(value = "/customerStatement")
@Api(tags = {"客户对账单"})
public class CustomerStatementController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(CustomerStatementController.class);

    @Resource
    private CustomerStatementService customerStatementService;

    /**
     * 未对账销售出库明细列表（按 depot_item 行）
     */
    @GetMapping("/unreconciledItems")
    @ApiOperation(value = "未对账明细列表")
    public BaseResponseInfo listUnreconciledItems(
            @RequestParam(value = "organId", required = false) Long organId,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<CustomerStatementItemVo> list = customerStatementService.listUnreconciledItems(
                    organId, beginTime, endTime, offset, pageSize);
            int total = customerStatementService.countUnreconciledItems(organId, beginTime, endTime);
            Map<String, Object> data = new HashMap<>();
            data.put("rows", list);
            data.put("total", total);
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 生成对账单
     */
    @PostMapping("/generate")
    @ApiOperation(value = "生成对账单")
    public BaseResponseInfo generate(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long organId = params.get("organId") != null ? Long.valueOf(params.get("organId").toString()) : null;
            @SuppressWarnings("unchecked")
            List<Long> itemIds = (List<Long>) params.get("itemIds");
            String remark = (String) params.get("remark");
            String beginTime = (String) params.get("beginTime");
            String endTime = (String) params.get("endTime");
            Long statementId = customerStatementService.generateStatement(organId, itemIds, remark, beginTime, endTime);
            res.code = 200;
            res.data = statementId;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 对账单列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "对账单列表")
    public BaseResponseInfo list(
            @RequestParam(value = "organId", required = false) Long organId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "signStatus", required = false) String signStatus,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<Map<String, Object>> rows = customerStatementService.listStatements(
                    organId, status, signStatus, beginTime, endTime, offset, pageSize);
            int total = customerStatementService.countStatements(organId, status, signStatus, beginTime, endTime);
            Map<String, Object> data = new HashMap<>();
            data.put("rows", rows);
            data.put("total", total);
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 对账单详情（含明细行）
     */
    @GetMapping("/detail")
    @ApiOperation(value = "对账单详情")
    public BaseResponseInfo detail(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            res.code = 200;
            res.data = customerStatementService.getStatementDetail(id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 审核 / 反审核
     */
    @PutMapping("/audit")
    @ApiOperation(value = "审核/反审核")
    public BaseResponseInfo audit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String auditStatus = params.get("status").toString();
            customerStatementService.audit(id, auditStatus);
            res.code = 200;
            res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 签署 / 取消签署
     */
    @PutMapping("/sign")
    @ApiOperation(value = "签署/取消签署")
    public BaseResponseInfo sign(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String signStatus = params.get("signStatus").toString();
            customerStatementService.sign(id, signStatus);
            res.code = 200;
            res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 更新附件路径
     */
    @PutMapping("/updateAttachment")
    @ApiOperation(value = "更新附件")
    public BaseResponseInfo updateAttachment(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String attachment = (String) params.get("attachment");
            customerStatementService.updateAttachment(id, attachment);
            res.code = 200;
            res.data = "操作成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 删除对账单（软删，恢复明细为未对账状态）
     */
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除对账单")
    public BaseResponseInfo delete(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            customerStatementService.deleteStatement(id);
            res.code = 200;
            res.data = "删除成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }
}
