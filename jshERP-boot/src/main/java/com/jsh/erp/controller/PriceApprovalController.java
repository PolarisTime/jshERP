package com.jsh.erp.controller;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.service.PriceApprovalService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格核准 Controller
 */
@RestController
@RequestMapping(value = "/priceApproval")
@Tag(name = "价格核准")
public class PriceApprovalController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(PriceApprovalController.class);

    @Resource
    private PriceApprovalService priceApprovalService;

    /**
     * 核准单列表
     */
    @GetMapping("/list")
    @Operation(summary = "核准单列表")
    public BaseResponseInfo list(
            @RequestParam(value = "organId", required = false) Long organId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<Map<String, Object>> rows = priceApprovalService.listApprovals(
                    organId, status, billNo, beginTime, endTime, offset, pageSize);
            int total = priceApprovalService.countApprovals(organId, status, billNo, beginTime, endTime);
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
     * 核准单详情（含明细行）
     */
    @GetMapping("/detail")
    @Operation(summary = "核准单详情")
    public BaseResponseInfo detail(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            res.code = 200;
            res.data = priceApprovalService.getDetail(id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 可导入的出库单列表
     */
    @GetMapping("/availableSaleOut")
    @Operation(summary = "可导入的出库单")
    public BaseResponseInfo availableSaleOut(
            @RequestParam(value = "organId", required = false) Long organId,
            @RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            int offset = (currentPage - 1) * pageSize;
            List<Map<String, Object>> rows = priceApprovalService.listAvailableSaleOut(organId, billNo, beginTime, endTime, offset, pageSize);
            int total = priceApprovalService.countAvailableSaleOut(organId, billNo, beginTime, endTime);
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
     * 从出库单创建核准记录
     */
    @PostMapping("/createFromSaleOut")
    @Operation(summary = "从出库单创建核准记录")
    public BaseResponseInfo createFromSaleOut(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long depotHeadId = Long.valueOf(params.get("depotHeadId").toString());
            Long approvalId = priceApprovalService.createFromSaleOut(depotHeadId);
            res.code = 200;
            res.data = approvalId;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 保存核准明细（含拆分行）
     */
    @PutMapping("/saveItems")
    @Operation(summary = "保存核准明细")
    public BaseResponseInfo saveItems(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long approvalId = Long.valueOf(params.get("id").toString());
            String deliveryDate = (String) params.get("deliveryDate");
            String remark = (String) params.get("remark");
            String itemsJson = params.get("items").toString();
            priceApprovalService.saveApprovalItems(approvalId, deliveryDate, remark, itemsJson);
            res.code = 200;
            res.data = "保存成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 核准确认（回写depot_head）
     */
    @PutMapping("/confirm")
    @Operation(summary = "核准确认")
    public BaseResponseInfo confirm(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long approvalId = Long.valueOf(params.get("id").toString());
            priceApprovalService.confirmApproval(approvalId);
            res.code = 200;
            res.data = "核准成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 取消核准
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消核准")
    public BaseResponseInfo cancel(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long approvalId = Long.valueOf(params.get("id").toString());
            priceApprovalService.cancelApproval(approvalId);
            res.code = 200;
            res.data = "取消核准成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = e.getMessage();
        }
        return res;
    }

    /**
     * 删除核准记录（软删）
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除核准记录")
    public BaseResponseInfo delete(@RequestParam("id") Long id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            priceApprovalService.deleteApproval(id);
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
