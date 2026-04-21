package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.PrintTemplate;
import com.jsh.erp.service.PrintTemplateService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 打印模板Controller
 */
@RestController
@RequestMapping(value = "/printTemplate")
@Tag(name = "打印模板")
public class PrintTemplateController {
    private Logger logger = LoggerFactory.getLogger(PrintTemplateController.class);

    @Resource
    private PrintTemplateService printTemplateService;

    @GetMapping(value = "/getByBillType")
    @Operation(summary = "获取指定单据类型的启用模板")
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
    @Operation(summary = "获取指定单据类型的所有模板列表（合并数据库+文件模板）")
    public BaseResponseInfo listByBillType(@RequestParam("billType") String billType) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<Map<String, Object>> merged = new ArrayList<>();
            // 1. 数据库模板
            List<PrintTemplate> dbList = printTemplateService.listByBillType(billType);
            for (PrintTemplate t : dbList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", t.getId());
                item.put("templateName", t.getTemplateName());
                item.put("templateHtml", t.getTemplateHtml());
                item.put("isDefault", t.getIsDefault());
                item.put("source", "db");
                merged.add(item);
            }
            // 2. 文件模板
            List<Map<String, String>> fileList = printTemplateService.listFileTemplates(billType);
            for (Map<String, String> f : fileList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "file_" + f.get("fileName"));
                item.put("templateName", f.get("name"));
                item.put("templateHtml", f.get("content"));
                item.put("isDefault", "0");
                item.put("source", "file");
                item.put("fileName", f.get("fileName"));
                merged.add(item);
            }
            res.code = 200;
            res.data = merged;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @PostMapping(value = "/save")
    @Operation(summary = "保存打印模板")
    public BaseResponseInfo save(@RequestBody JSONObject obj) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String source = obj.getString("source");
            String billType = obj.getString("billType");
            String templateName = obj.getString("templateName");
            String templateHtml = obj.getString("templateHtml");
            if ("file".equals(source)) {
                // 保存到文件
                printTemplateService.saveFileTemplate(billType, templateName, templateHtml);
            } else {
                // 保存到数据库
                Long id = obj.getLong("id");
                String isDefault = obj.getString("isDefault");
                if (isDefault == null) isDefault = "1";
                printTemplateService.saveTemplate(id, billType, templateName, templateHtml, isDefault);
            }
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
    @Operation(summary = "删除打印模板")
    public BaseResponseInfo delete(@RequestParam(value = "id", required = false) Long id,
                                   @RequestParam(value = "source", required = false) String source,
                                   @RequestParam(value = "billType", required = false) String billType,
                                   @RequestParam(value = "fileName", required = false) String fileName) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            if ("file".equals(source)) {
                printTemplateService.deleteFileTemplate(billType, fileName);
            } else {
                printTemplateService.deleteById(id);
            }
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
    @Operation(summary = "获取单据类型的可用字段元数据")
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
