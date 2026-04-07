package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.FreightCarrier;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.FreightCarrierMapper;
import com.jsh.erp.datasource.mappers.FreightCarrierMapperEx;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 运费结算方Service
 */
@Service
public class FreightCarrierService {
    private Logger logger = LoggerFactory.getLogger(FreightCarrierService.class);

    @Resource
    private FreightCarrierMapper freightCarrierMapper;
    @Resource
    private FreightCarrierMapperEx freightCarrierMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;

    /**
     * 分页条件查询结算方列表
     */
    public List<FreightCarrier> select(String name) throws Exception {
        List<FreightCarrier> list = new ArrayList<>();
        try {
            PageUtils.startPage();
            list = freightCarrierMapperEx.selectByCondition(name, null, null);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 查询所有启用的结算方（下拉框用）
     */
    public List<FreightCarrier> selectAll() throws Exception {
        List<FreightCarrier> list = new ArrayList<>();
        try {
            list = freightCarrierMapperEx.selectAll();
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 新增结算方
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int add(FreightCarrier freightCarrier) throws Exception {
        int result = 0;
        try {
            freightCarrier.setEnabled(true);
            freightCarrier.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            result = freightCarrierMapper.insertSelective(freightCarrier);
            logService.insertLog("结算方",
                    BusinessConstants.LOG_OPERATION_TYPE_ADD + freightCarrier.getName(),
                    ((javax.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 更新结算方
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int update(FreightCarrier freightCarrier) throws Exception {
        int result = 0;
        try {
            result = freightCarrierMapper.updateByPrimaryKeySelective(freightCarrier);
            logService.insertLog("结算方",
                    BusinessConstants.LOG_OPERATION_TYPE_EDIT + freightCarrier.getName(),
                    ((javax.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 逻辑删除结算方
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int delete(Long id) throws Exception {
        int result = 0;
        try {
            FreightCarrier freightCarrier = freightCarrierMapper.selectByPrimaryKey(id);
            FreightCarrier update = new FreightCarrier();
            update.setId(id);
            update.setDeleteFlag("1");
            result = freightCarrierMapper.updateByPrimaryKeySelective(update);
            logService.insertLog("结算方",
                    BusinessConstants.LOG_OPERATION_TYPE_DELETE + (freightCarrier != null ? freightCarrier.getName() : ""),
                    ((javax.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 批量逻辑删除结算方
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteBatch(String ids) throws Exception {
        int result = 0;
        try {
            String[] idArray = ids.split(",");
            for (String idStr : idArray) {
                if (idStr != null && !idStr.trim().isEmpty()) {
                    FreightCarrier update = new FreightCarrier();
                    update.setId(Long.parseLong(idStr.trim()));
                    update.setDeleteFlag("1");
                    result += freightCarrierMapper.updateByPrimaryKeySelective(update);
                }
            }
            logService.insertLog("结算方",
                    BusinessConstants.LOG_OPERATION_TYPE_DELETE + ids,
                    ((javax.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }
}
