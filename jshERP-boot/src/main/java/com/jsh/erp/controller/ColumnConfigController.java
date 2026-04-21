package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.ColumnConfig;
import com.jsh.erp.service.ColumnConfigService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 列配置Controller
 */
@RestController
@RequestMapping(value = "/columnConfig")
@Tag(name = "列配置")
public class ColumnConfigController {
    private Logger logger = LoggerFactory.getLogger(ColumnConfigController.class);

    @Resource
    private ColumnConfigService columnConfigService;

    @GetMapping(value = "/getByPageCode")
    @Operation(summary = "获取页面列配置")
    public BaseResponseInfo getByPageCode(@RequestParam("pageCode") String pageCode) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            ColumnConfig config = columnConfigService.getByPageCode(pageCode);
            res.code = 200;
            res.data = config;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @PostMapping(value = "/save")
    @Operation(summary = "保存页面列配置")
    public BaseResponseInfo save(@RequestBody JSONObject obj) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String pageCode = obj.getString("pageCode");
            String columnConfig = obj.getString("columnConfig");
            columnConfigService.saveColumnConfig(pageCode, columnConfig);
            res.code = 200;
            res.data = "成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "保存失败";
        }
        return res;
    }

    @DeleteMapping(value = "/reset")
    @Operation(summary = "重置页面列配置")
    public BaseResponseInfo reset(@RequestParam("pageCode") String pageCode) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            columnConfigService.deleteByPageCode(pageCode);
            res.code = 200;
            res.data = "成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "重置失败";
        }
        return res;
    }
}
