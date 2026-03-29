package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.PrintTemplate;
import com.jsh.erp.service.PrintTemplateService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 打印模板Controller
 */
@RestController
@RequestMapping(value = "/printTemplate")
@Api(tags = {"打印模板"})
public class PrintTemplateController {
    private Logger logger = LoggerFactory.getLogger(PrintTemplateController.class);

    @Resource
    private PrintTemplateService printTemplateService;

    @GetMapping(value = "/getByBillType")
    @ApiOperation(value = "获取指定单据类型的启用模板")
    public BaseResponseInfo getByBillType(@RequestParam("billType") String billType) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            PrintTemplate template = printTemplateService.getDefaultByBillType(billType);
            res.code = 200;
            res.data = template;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @GetMapping(value = "/listByBillType")
    @ApiOperation(value = "获取指定单据类型的所有模板列表")
    public BaseResponseInfo listByBillType(@RequestParam("billType") String billType) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<PrintTemplate> list = printTemplateService.listByBillType(billType);
            res.code = 200;
            res.data = list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @PostMapping(value = "/save")
    @ApiOperation(value = "保存打印模板")
    public BaseResponseInfo save(@RequestBody JSONObject obj) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long id = obj.getLong("id");
            String billType = obj.getString("billType");
            String templateName = obj.getString("templateName");
            String templateHtml = obj.getString("templateHtml");
            String isDefault = obj.getString("isDefault");
            if (isDefault == null) isDefault = "1";
            printTemplateService.saveTemplate(id, billType, templateName, templateHtml, isDefault);
            res.code = 200;
            res.data = "成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "保存失败";
        }
        return res;
    }

    @DeleteMapping(value = "/delete")
    @ApiOperation(value = "删除打印模板")
    public BaseResponseInfo delete(@RequestParam("id") Long id) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            printTemplateService.deleteById(id);
            res.code = 200;
            res.data = "成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "删除失败";
        }
        return res;
    }

    @GetMapping(value = "/getFieldMeta")
    @ApiOperation(value = "获取单据类型的可用字段元数据")
    public BaseResponseInfo getFieldMeta(@RequestParam("billType") String billType) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> meta = printTemplateService.getFieldMeta(billType);
            res.code = 200;
            res.data = meta;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取失败";
        }
        return res;
    }
}
