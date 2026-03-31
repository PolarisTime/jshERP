package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.AccountHeadVo4Body;
import com.jsh.erp.datasource.entities.FreightHeadVo;
import com.jsh.erp.service.FreightHeadService;
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

/**
 * 运费单Controller
 */
@RestController
@RequestMapping(value = "/freightHead")
@Api(tags = {"运费管理"})
public class FreightHeadController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(FreightHeadController.class);

    @Resource
    private FreightHeadService freightHeadService;

    /**
     * 生成运费单编号（格式: yyyyW0001）
     */
    @GetMapping(value = "/buildBillNo")
    @ApiOperation(value = "生成运费单编号")
    public BaseResponseInfo buildBillNo(HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String billNo = freightHeadService.buildFreightBillNo();
            Map<String, Object> map = new HashMap<>();
            map.put("billNo", billNo);
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "生成编号失败";
        }
        return res;
    }

    /**
     * 运费单列表查询
     */
    @GetMapping(value = "/list")
    @ApiOperation(value = "获取运费单列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request) throws Exception {
        String billNo = StringUtil.getInfo(search, "billNo");
        Long carrierId = StringUtil.parseStrLong(StringUtil.getInfo(search, "carrierId"));
        String status = StringUtil.getInfo(search, "status");
        String paymentStatus = StringUtil.getInfo(search, "paymentStatus");
        String beginTime = StringUtil.getInfo(search, "beginTime");
        String endTime = StringUtil.getInfo(search, "endTime");
        List<FreightHeadVo> list = freightHeadService.select(billNo, carrierId, status, paymentStatus, beginTime, endTime);
        return getDataTable(list);
    }

    /**
     * 新增运费单（主表+明细）
     */
    @PostMapping(value = "/addFreightBill")
    @ApiOperation(value = "新增运费单")
    public Object addFreightBill(@RequestBody AccountHeadVo4Body body,
                                 HttpServletRequest request) throws Exception {
        JSONObject result = ExceptionConstants.standardSuccess();
        String beanJson = body.getInfo();
        String rows = body.getRows();
        freightHeadService.addFreightBill(beanJson, rows, request);
        return result;
    }

    /**
     * 编辑运费单（主表+明细）
     */
    @PutMapping(value = "/updateFreightBill")
    @ApiOperation(value = "编辑运费单")
    public Object updateFreightBill(@RequestBody AccountHeadVo4Body body,
                                    HttpServletRequest request) throws Exception {
        JSONObject result = ExceptionConstants.standardSuccess();
        String beanJson = body.getInfo();
        String rows = body.getRows();
        freightHeadService.updateFreightBill(beanJson, rows, request);
        return result;
    }

    /**
     * 删除运费单
     */
    @DeleteMapping(value = "/deleteFreightBill")
    @ApiOperation(value = "删除运费单")
    public String deleteFreightBill(@RequestParam("id") Long id,
                                    HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        freightHeadService.deleteFreightBill(id, request);
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * 批量删除运费单
     */
    @DeleteMapping(value = "/deleteBatchFreightBill")
    @ApiOperation(value = "批量删除运费单")
    public String deleteBatchFreightBill(@RequestParam("ids") String ids,
                                         HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        freightHeadService.deleteBatchFreightBill(ids, request);
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * 批量审核/反审核
     */
    @PostMapping(value = "/batchSetStatus")
    @ApiOperation(value = "批量设置状态-审核或者反审核")
    public String batchSetStatus(@RequestBody JSONObject jsonObject,
                                 HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        String status = jsonObject.getString("status");
        String ids = jsonObject.getString("ids");
        int res = freightHeadService.batchSetStatus(status, ids);
        if (res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 获取可关联的销售出库单
     */
    @GetMapping(value = "/availableSaleOut")
    @ApiOperation(value = "获取可关联的销售出库单")
    public TableDataInfo getAvailableSaleOut(@RequestParam(value = "billNo", required = false) String billNo,
                                             HttpServletRequest request) throws Exception {
        List<Map<String, Object>> list = freightHeadService.getAvailableSaleOut(billNo, request);
        return getDataTable(list);
    }

    /**
     * 获取运费单详情
     */
    @GetMapping(value = "/detail")
    @ApiOperation(value = "获取运费单详情")
    public BaseResponseInfo getDetail(@RequestParam(value = "id", required = false) Long id,
                                      @RequestParam(value = "billNo", required = false) String billNo,
                                      HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data;
            if (id != null) {
                data = freightHeadService.getDetail(id);
            } else if (billNo != null && !billNo.isEmpty()) {
                data = freightHeadService.getDetailByBillNo(billNo);
            } else {
                res.code = 400;
                res.data = "参数id或billNo必须提供一个";
                return res;
            }
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 运费对账聚合查询（按物流方汇总已审核物流单）
     */
    @GetMapping(value = "/reconciliation")
    @ApiOperation(value = "运费对账汇总")
    public TableDataInfo getReconciliation(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                            HttpServletRequest request) throws Exception {
        Long carrierId = StringUtil.parseStrLong(StringUtil.getInfo(search, "carrierId"));
        String beginTime = StringUtil.getInfo(search, "beginTime");
        String endTime = StringUtil.getInfo(search, "endTime");
        List<Map<String, Object>> list = freightHeadService.selectReconciliation(carrierId, beginTime, endTime);
        return getDataTable(list);
    }

    /**
     * 计算出库单重量
     */
    @GetMapping(value = "/calcWeight")
    @ApiOperation(value = "计算出库单重量")
    public BaseResponseInfo calcWeight(@RequestParam("depotHeadId") Long depotHeadId,
                                       HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            res.code = 200;
            res.data = freightHeadService.calcDepotHeadWeight(depotHeadId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @GetMapping(value = "/getDepotItems")
    @ApiOperation(value = "按出库单ID列表查询商品明细行")
    public BaseResponseInfo getDepotItems(@RequestParam("headerIds") String headerIds,
                                          HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<Long> idList = new ArrayList<>();
            if (StringUtil.isNotEmpty(headerIds)) {
                for (String s : headerIds.split(",")) {
                    idList.add(Long.parseLong(s.trim()));
                }
            }
            res.code = 200;
            res.data = freightHeadService.getDepotItemsByHeaderIds(idList);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @GetMapping(value = "/reconciliationDetail")
    @ApiOperation(value = "对账明细查询")
    public TableDataInfo reconciliationDetail(@RequestParam(value = "search", required = false) String search,
                                              HttpServletRequest request) throws Exception {
        Long carrierId = null;
        String beginTime = "";
        String endTime = "";
        String paymentStatus = "";
        if (StringUtil.isNotEmpty(search)) {
            JSONObject obj = JSONObject.parseObject(search);
            carrierId = obj.getLong("carrierId");
            beginTime = obj.getString("beginTime");
            endTime = obj.getString("endTime");
            paymentStatus = obj.getString("paymentStatus");
        }
        List<Map<String, Object>> list = freightHeadService.selectReconciliationDetail(carrierId, beginTime, endTime, paymentStatus);
        return getDataTable(list);
    }

    @PostMapping(value = "/batchSetPaymentStatus")
    @ApiOperation(value = "批量标记付款状态")
    public BaseResponseInfo batchSetPaymentStatus(@RequestBody JSONObject jsonObject,
                                                   HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String paymentStatus = jsonObject.getString("paymentStatus");
            java.math.BigDecimal paidAmount = jsonObject.getBigDecimal("paidAmount");
            String ids = jsonObject.getString("ids");
            int result = freightHeadService.batchSetPaymentStatus(paymentStatus, paidAmount, ids);
            if (result > 0) {
                res.code = 200;
                res.data = "操作成功";
            } else {
                res.code = 500;
                res.data = "操作失败";
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "操作失败";
        }
        return res;
    }

    @PostMapping(value = "/cancelPayment")
    @ApiOperation(value = "取消付款标记")
    public BaseResponseInfo cancelPayment(@RequestBody JSONObject jsonObject,
                                           HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String ids = jsonObject.getString("ids");
            int result = freightHeadService.cancelPayment(ids);
            if (result > 0) {
                res.code = 200;
                res.data = "操作成功";
            } else {
                res.code = 500;
                res.data = "操作失败";
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "操作失败";
        }
        return res;
    }

    @GetMapping(value = "/exportFreightViewList")
    @ApiOperation(value = "导出运费查看列表Excel")
    public void exportFreightViewList(@RequestParam(value = "billNo", required = false) String billNo,
                                       @RequestParam(value = "carrierId", required = false) Long carrierId,
                                       @RequestParam(value = "status", required = false) String status,
                                       @RequestParam(value = "paymentStatus", required = false) String paymentStatus,
                                       @RequestParam(value = "beginTime", required = false) String beginTime,
                                       @RequestParam(value = "endTime", required = false) String endTime,
                                       HttpServletRequest request,
                                       javax.servlet.http.HttpServletResponse response) throws Exception {
        try {
            List<FreightHeadVo> dataList = freightHeadService.selectForExport(billNo, carrierId, status, paymentStatus, beginTime, endTime);
            String[] names = {"单据编号", "日期", "结算方", "总重量(吨)", "单价(元/吨)", "总运费(元)", "审核状态", "付款状态", "已付金额", "未付金额", "付款时间", "操作人", "备注"};
            String title = "运费查看列表";
            List<Object[]> objects = new ArrayList<>();
            for (FreightHeadVo row : dataList) {
                String statusName = "1".equals(row.getStatus()) ? "已审核" : "未审核";
                String ps = row.getPaymentStatus() != null ? row.getPaymentStatus() : "0";
                String psName = "0".equals(ps) ? "未付款" : "1".equals(ps) ? "已付款" : "2".equals(ps) ? "部分付款" : "未付款";
                java.math.BigDecimal totalFreight = row.getTotalFreight() != null ? row.getTotalFreight() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal paidAmount = row.getPaidAmount() != null ? row.getPaidAmount() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal unpaidAmount = totalFreight.subtract(paidAmount);
                objects.add(new Object[]{
                        row.getBillNo(),
                        row.getBillTimeStr(),
                        row.getCarrierName(),
                        row.getTotalWeight(),
                        row.getUnitPrice(),
                        totalFreight,
                        statusName,
                        psName,
                        paidAmount,
                        unpaidAmount,
                        row.getPaymentTimeStr() != null ? row.getPaymentTimeStr() : "",
                        row.getPaymentOperatorName() != null ? row.getPaymentOperatorName() : "",
                        row.getRemark() != null ? row.getRemark() : ""
                });
            }
            String path = System.getProperty("java.io.tmpdir");
            String fileName = "freight_view_list_" + System.currentTimeMillis() + ".csv";
            java.io.File file = com.jsh.erp.utils.CsvUtils.exportCsv(path, fileName, title, names, objects);
            com.jsh.erp.utils.CsvUtils.downloadCsv(file, file.getName(), response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GetMapping(value = "/exportReconciliation")
    @ApiOperation(value = "导出对账明细Excel")
    public void exportReconciliation(@RequestParam(value = "carrierId", required = false) Long carrierId,
                                      @RequestParam(value = "beginTime", required = false) String beginTime,
                                      @RequestParam(value = "endTime", required = false) String endTime,
                                      @RequestParam(value = "paymentStatus", required = false) String paymentStatus,
                                      @RequestParam(value = "carrierName", required = false) String carrierName,
                                      HttpServletRequest request,
                                      javax.servlet.http.HttpServletResponse response) throws Exception {
        try {
            List<Map<String, Object>> dataList = freightHeadService.getReconciliationDetailForExport(carrierId, beginTime, endTime, paymentStatus);
            // 使用 ExcelUtils 导出
            String tip = "运费对账明细" + (carrierName != null ? " - " + carrierName : "");
            String[] names = {"单据编号", "日期", "结算方", "总重量(吨)", "单价(元/吨)", "总运费(元)", "付款状态", "已付金额", "付款时间", "操作人", "备注"};
            String title = "运费对账明细";
            List<Object[]> objects = new ArrayList<>();
            for (Map<String, Object> row : dataList) {
                String ps = String.valueOf(row.get("paymentStatus"));
                String psName = "0".equals(ps) ? "未付款" : "1".equals(ps) ? "已付款" : "2".equals(ps) ? "部分付款" : "未付款";
                objects.add(new Object[]{
                        row.get("billNo"),
                        row.get("billTimeStr"),
                        row.get("carrierName"),
                        row.get("totalWeight"),
                        row.get("unitPrice"),
                        row.get("totalFreight"),
                        psName,
                        row.get("paidAmount"),
                        row.get("paymentTimeStr") != null ? row.get("paymentTimeStr") : "",
                        row.get("paymentOperatorName") != null ? row.get("paymentOperatorName") : "",
                        row.get("remark") != null ? row.get("remark") : ""
                });
            }
            String path = System.getProperty("java.io.tmpdir");
            String fileName = "freight_reconciliation_" + System.currentTimeMillis() + ".csv";
            java.io.File file = com.jsh.erp.utils.CsvUtils.exportCsv(path, fileName, tip, names, objects);
            com.jsh.erp.utils.CsvUtils.downloadCsv(file, file.getName(), response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
