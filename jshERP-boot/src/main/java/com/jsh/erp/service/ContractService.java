package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Contract;
import com.jsh.erp.datasource.entities.ContractEx;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.ContractMapper;
import com.jsh.erp.datasource.mappers.ContractMapperEx;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 合同管理Service
 */
@Service
public class ContractService {
    private Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Resource
    private ContractMapper contractMapper;
    @Resource
    private ContractMapperEx contractMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;

    /**
     * 分页条件查询合同列表
     */
    public List<ContractEx> select(String contractNo, String contractName,
                                    Long organId, String status) throws Exception {
        List<ContractEx> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo == null ? null : userInfo.getTenantId();
            PageUtils.startPage();
            list = contractMapperEx.selectByCondition(contractNo, contractName, organId, status, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 新增合同
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int add(Contract contract) throws Exception {
        int result = 0;
        try {
            User userInfo = userService.getCurrentUser();
            if (userInfo != null) {
                contract.setTenantId(userInfo.getTenantId());
            }
            contract.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            contract.setCreateTime(new Date());
            contract.setUpdateTime(new Date());
            result = contractMapper.insertSelective(contract);
            logService.insertLog("合同",
                    BusinessConstants.LOG_OPERATION_TYPE_ADD + contract.getContractNo(),
                    ((HttpServletRequest) RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 更新合同
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int update(Contract contract) throws Exception {
        int result = 0;
        try {
            contract.setUpdateTime(new Date());
            result = contractMapper.updateByPrimaryKeySelective(contract);
            logService.insertLog("合同",
                    BusinessConstants.LOG_OPERATION_TYPE_EDIT + contract.getContractNo(),
                    ((HttpServletRequest) RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 逻辑删除合同
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int delete(Long id) throws Exception {
        int result = 0;
        try {
            Contract contract = contractMapper.selectByPrimaryKey(id);
            Contract update = new Contract();
            update.setId(id);
            update.setDeleteFlag("1");
            update.setUpdateTime(new Date());
            result = contractMapper.updateByPrimaryKeySelective(update);
            logService.insertLog("合同",
                    BusinessConstants.LOG_OPERATION_TYPE_DELETE + (contract != null ? contract.getContractNo() : ""),
                    ((HttpServletRequest) RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 批量逻辑删除合同
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteBatch(String ids) throws Exception {
        int result = 0;
        try {
            String[] idArray = ids.split(",");
            for (String idStr : idArray) {
                if (idStr != null && !idStr.trim().isEmpty()) {
                    Contract update = new Contract();
                    update.setId(Long.parseLong(idStr.trim()));
                    update.setDeleteFlag("1");
                    update.setUpdateTime(new Date());
                    result += contractMapper.updateByPrimaryKeySelective(update);
                }
            }
            logService.insertLog("合同",
                    BusinessConstants.LOG_OPERATION_TYPE_DELETE + ids,
                    ((HttpServletRequest) RequestContextHolder
                            .getRequestAttributes().resolveReference("request")));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }
}
