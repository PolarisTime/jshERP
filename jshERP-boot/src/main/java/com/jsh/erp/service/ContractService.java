package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Contract;
import com.jsh.erp.datasource.entities.ContractEx;
import com.jsh.erp.datasource.entities.ContractPerson;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.ContractMapper;
import com.jsh.erp.datasource.mappers.ContractMapperEx;
import com.jsh.erp.datasource.mappers.ContractPersonMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.constants.ExceptionConstants;
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
import java.util.Map;

@Service
public class ContractService {
    private Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Resource private ContractMapper contractMapper;
    @Resource private ContractMapperEx contractMapperEx;
    @Resource private ContractPersonMapper contractPersonMapper;
    @Resource private UserService userService;
    @Resource private LogService logService;

    /** 分页条件查询 */
    public List<ContractEx> select(String contractNo, String contractName,
                                   Long organId, String auditStatus, String signStatus) throws Exception {
        List<ContractEx> list = new ArrayList<>();
        try {
            User userInfo = userService.getCurrentUser();
            Long tenantId = userInfo == null ? null : userInfo.getTenantId();
            PageUtils.startPage();
            list = contractMapperEx.selectByCondition(contractNo, contractName, organId, auditStatus, signStatus, tenantId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    /** 获取合同的授权人员 */
    public List<ContractPerson> getPersons(Long contractId) {
        return contractPersonMapper.selectByContractId(contractId);
    }

    /** 新增合同（含授权人员） */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int add(Contract contract, List<ContractPerson> persons) throws Exception {
        int result = 0;
        try {
            //tenant_id 由多租户插件自动注入，不手动设置避免重复
            contract.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            contract.setAuditStatus("0");
            contract.setSignStatus("0");
            contract.setCreateTime(new Date());
            contract.setUpdateTime(new Date());
            result = contractMapper.insertSelective(contract);
            if (result > 0 && persons != null && !persons.isEmpty()) {
                contractPersonMapper.insertBatch(contract.getId(), persons);
            }
            logService.insertLog("合同", BusinessConstants.LOG_OPERATION_TYPE_ADD + contract.getContractNo(),
                    (HttpServletRequest) RequestContextHolder.getRequestAttributes().resolveReference("request"));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /** 更新合同（替换授权人员） */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int update(Contract contract, List<ContractPerson> persons) throws Exception {
        int result = 0;
        try {
            contract.setUpdateTime(new Date());
            result = contractMapper.updateByPrimaryKeySelective(contract);
            // 替换授权人员
            contractPersonMapper.deleteByContractId(contract.getId());
            if (persons != null && !persons.isEmpty()) {
                contractPersonMapper.insertBatch(contract.getId(), persons);
            }
            logService.insertLog("合同", BusinessConstants.LOG_OPERATION_TYPE_EDIT + contract.getContractNo(),
                    (HttpServletRequest) RequestContextHolder.getRequestAttributes().resolveReference("request"));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public Contract getContractById(Long id) {
        return contractMapper.selectByPrimaryKey(id);
    }

    /** 审核 / 反审核 */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void audit(Long id, String auditStatus) throws Exception {
        Contract c = contractMapper.selectByPrimaryKey(id);
        if (c == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "合同不存在");
        if ("0".equals(auditStatus) && "1".equals(c.getSignStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "已签署的合同不能反审核");
        }
        Contract upd = new Contract();
        upd.setId(id);
        upd.setAuditStatus(auditStatus);
        upd.setUpdateTime(new Date());
        contractMapper.updateByPrimaryKeySelective(upd);
    }

    /** 签署 / 取消签署 */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void sign(Long id, String signStatus) throws Exception {
        Contract c = contractMapper.selectByPrimaryKey(id);
        if (c == null) throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE, "合同不存在");
        if ("1".equals(signStatus) && !"1".equals(c.getAuditStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE, "请先审核后再签署");
        }
        Contract upd = new Contract();
        upd.setId(id);
        upd.setSignStatus(signStatus);
        upd.setUpdateTime(new Date());
        contractMapper.updateByPrimaryKeySelective(upd);
    }

    /** 更新附件 */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateAttachments(Long id, String attachments) throws Exception {
        Contract upd = new Contract();
        upd.setId(id);
        upd.setAttachments(attachments);
        upd.setUpdateTime(new Date());
        contractMapper.updateByPrimaryKeySelective(upd);
    }

    /** 获取客户合同余额（多份合同累加） */
    public Map<String, Object> getContractBalance(Long organId) throws Exception {
        User userInfo = userService.getCurrentUser();
        Long tenantId = userInfo == null ? null : userInfo.getTenantId();
        return contractMapperEx.getContractBalance(organId, tenantId);
    }

    /** 逻辑删除 */
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
            contractPersonMapper.deleteByContractId(id);
            logService.insertLog("合同", BusinessConstants.LOG_OPERATION_TYPE_DELETE + (contract != null ? contract.getContractNo() : ""),
                    (HttpServletRequest) RequestContextHolder.getRequestAttributes().resolveReference("request"));
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /** 批量逻辑删除 */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteBatch(String ids) throws Exception {
        int result = 0;
        try {
            for (String idStr : ids.split(",")) {
                if (idStr != null && !idStr.trim().isEmpty()) {
                    delete(Long.parseLong(idStr.trim()));
                    result++;
                }
            }
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }
}
