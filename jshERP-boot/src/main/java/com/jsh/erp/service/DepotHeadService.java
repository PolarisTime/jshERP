package com.jsh.erp.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotItemMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.vo.*;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.CsvUtils;
import com.jsh.erp.utils.ExcelUtils;
import com.jsh.erp.utils.FileUtils;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import jxl.Workbook;
import jxl.write.WritableWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.jsh.erp.utils.Tools.getCenternTime;
import static com.jsh.erp.utils.Tools.getNow3;

@Service
public class DepotHeadService {
    private Logger logger = LoggerFactory.getLogger(DepotHeadService.class);
    private static final int FEATURE_DISABLED_CODE = 500;
    private static final Set<String> DISABLED_SUB_TYPES = new HashSet<>(Arrays.asList(
            "零售", "零售退货", "采购订单", "采购退货", "销售退货", "组装单", "拆卸单"
    ));

    @Resource
    private DepotHeadMapper depotHeadMapper;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private DepotService depotService;
    @Resource
    DepotItemService depotItemService;
    @Resource
    private SupplierService supplierService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private SerialNumberService serialNumberService;
    @Resource
    private OrgaUserRelService orgaUserRelService;
    @Resource
    private PersonService personService;
    @Resource
    private AccountService accountService;
    @Resource
    private AccountHeadService accountHeadService;
    @Resource
    private AccountItemService accountItemService;
    @Resource
    private SequenceService sequenceService;
    @Resource
    private RedisService redisService;
    @Resource
    DepotItemMapperEx depotItemMapperEx;
    @Resource
    private com.jsh.erp.datasource.mappers.DepotItemMapper depotItemMapper;
    @Resource
    private com.jsh.erp.datasource.mappers.FreightItemMapperEx freightItemMapperEx;
    @Resource
    private FreightHeadService freightHeadService;
    @Resource
    private LogService logService;
    @Resource
    private com.jsh.erp.datasource.mappers.PriceApprovalMapper priceApprovalMapper;
    @Resource
    private PriceApprovalService priceApprovalService;

    @Value(value="${file.exportTmp}")
    private String fileExportTmp;

    public DepotHead getDepotHead(long id)throws Exception {
        DepotHead result=null;
        try{
            result=depotHeadMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotHead> getDepotHead()throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list=null;
        try{
            list=depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotHeadVo4List> select(String type, String subType, String hasDebt, String status, String purchaseStatus, String number, String linkApply, String linkNumber,
           String beginTime, String endTime, String materialParam, Long organId, String[] organIdList, Long creator, Long depotId, Long accountId, String salesMan, String remark,
           String linkedFlag, String saleLinkFlag, String priceApproved) throws Exception {
        List<DepotHeadVo4List> list = new ArrayList<>();
        try{
            validateFeatureEnabled(subType);
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            String billCategory = getBillCategory(subType);
            String [] depotArray = getDepotArray(subType);
            String [] creatorArray = getCreatorArray();
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            String [] purchaseStatusArray = StringUtil.isNotEmpty(purchaseStatus) ? purchaseStatus.split(",") : null;
            String [] organArray = getOrganArray(subType, purchaseStatus);
            //以销定购，查看全部数据
            creatorArray = StringUtil.isNotEmpty(purchaseStatus) ? null: creatorArray;
            Map<Long,String> personMap = personService.getPersonMap();
            Map<Long,String> accountMap = accountService.getAccountMap();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = depotHeadMapperEx.selectByConditionDepotHead(type, subType, creatorArray, hasDebt,
                    statusArray, purchaseStatusArray, number, linkApply, linkNumber, beginTime, endTime,
                    materialParam, organId, organIdList, organArray, creator, depotId, depotArray, accountId, salesMan, remark, linkedFlag, saleLinkFlag, priceApproved);
            if (null != list) {
                List<Long> idList = new ArrayList<>();
                List<String> numberList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                    numberList.add(dh.getNumber());
                }
                //通过批量查询去构造map
                Map<String,BigDecimal> finishDepositMap = getFinishDepositMapByNumberList(numberList);
                Map<Long,BigDecimal> financialBillPriceMap = getFinancialBillPriceMapByBillIdList(idList);
                Map<String,Integer> billSizeMap = getBillSizeMapByLinkNumberList(numberList);
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                Map<Long,String> freightBillNoMap = getFreightBillNoMapByDepotHeadIdList(idList);
                Map<Long,String> freightCarNoMap = getFreightCarNoMapByDepotHeadIdList(idList);
                Map<Long,BigDecimal> totalWeightMap = getTotalWeightMapByHeaderIdList(idList);
                //被销售出库引用的单号（仅采购入库场景）
                Map<String,String> referencedByMap = null;
                if("采购".equals(subType)) {
                    referencedByMap = getReferencedByNumbersMapByList(numberList);
                }
                //被采购入库引用的单号（仅采购订单场景）
                Map<String,String> purchaseInRefMap = null;
                if("采购订单".equals(subType)) {
                    purchaseInRefMap = getPurchaseInByOrderNumbersMapByList(numberList);
                }
                for (DepotHeadVo4List dh : list) {
                    if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                        String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                        dh.setAccountName(accountStr);
                    }
                    if(dh.getAccountIdList() != null) {
                        String accountidlistStr = dh.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", "");
                        dh.setAccountIdList(accountidlistStr);
                    }
                    if(dh.getAccountMoneyList() != null) {
                        String accountmoneylistStr = dh.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
                        dh.setAccountMoneyList(accountmoneylistStr);
                    }
                    if(dh.getChangeAmount() != null) {
                        dh.setChangeAmount(roleService.parseBillPriceByLimit(dh.getChangeAmount().abs(), billCategory, priceLimit, request));
                    } else {
                        dh.setChangeAmount(BigDecimal.ZERO);
                    }
                    if(dh.getTotalPrice() != null) {
                        BigDecimal lastTotalPrice = BusinessConstants.SUB_TYPE_CHECK_ENTER.equals(dh.getSubType())||
                                BusinessConstants.SUB_TYPE_REPLAY.equals(dh.getSubType())?dh.getTotalPrice():dh.getTotalPrice().abs();
                        dh.setTotalPrice(roleService.parseBillPriceByLimit(lastTotalPrice, billCategory, priceLimit, request));
                    }
                    BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
                    dh.setDiscountLastMoney(roleService.parseBillPriceByLimit(discountLastMoney, billCategory, priceLimit, request));
                    BigDecimal backAmount = dh.getBackAmount()!=null?dh.getBackAmount():BigDecimal.ZERO;
                    dh.setBackAmount(roleService.parseBillPriceByLimit(backAmount, billCategory, priceLimit, request));
                    if(dh.getDeposit() == null) {
                        dh.setDeposit(BigDecimal.ZERO);
                    } else {
                        dh.setDeposit(roleService.parseBillPriceByLimit(dh.getDeposit(), billCategory, priceLimit, request));
                    }
                    //已经完成的欠款
                    if(finishDepositMap!=null) {
                        BigDecimal finishDeposit = finishDepositMap.get(dh.getNumber()) != null ? finishDepositMap.get(dh.getNumber()) : BigDecimal.ZERO;
                        dh.setFinishDeposit(roleService.parseBillPriceByLimit(finishDeposit, billCategory, priceLimit, request));
                    }
                    //欠款计算
                    BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
                    BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
                    BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount():BigDecimal.ZERO;
                    BigDecimal debt = discountLastMoney.add(otherMoney).subtract((deposit.add(changeAmount)));
                    dh.setDebt(roleService.parseBillPriceByLimit(debt, billCategory, priceLimit, request));
                    //最终欠款的金额
                    if(financialBillPriceMap!=null) {
                        BigDecimal financialBillPrice = financialBillPriceMap.get(dh.getId())!=null?financialBillPriceMap.get(dh.getId()):BigDecimal.ZERO;
                        BigDecimal lastDebt = debt.subtract(financialBillPrice);
                        dh.setLastDebt(roleService.parseBillPriceByLimit(lastDebt, billCategory, priceLimit, request));
                    }
                    //是否有退款单
                    if(billSizeMap!=null) {
                        Integer billListSize = billSizeMap.get(dh.getNumber());
                        dh.setHasBackFlag(billListSize!=null && billListSize>0);
                    }
                    if(StringUtil.isNotEmpty(dh.getSalesMan())) {
                        dh.setSalesManStr(personService.getPersonByMapAndIds(personMap,dh.getSalesMan()));
                    }
                    if(dh.getOperTime() != null) {
                        dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                    }
                    //商品信息简述
                    if(materialsListMap!=null) {
                        dh.setMaterialsList(materialsListMap.get(dh.getId()));
                    }
                    //商品总数量
                    if(materialCountListMap!=null) {
                        dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                    }
                    //关联物流单号
                    if(freightBillNoMap!=null) {
                        dh.setFreightBillNo(freightBillNoMap.get(dh.getId()));
                    }
                    //关联物流单的运输车号
                    if(freightCarNoMap!=null) {
                        dh.setFreightCarNo(freightCarNoMap.get(dh.getId()));
                    }
                    //总重量(吨)
                    if(totalWeightMap!=null) {
                        dh.setTotalWeight(totalWeightMap.get(dh.getId()));
                    }
                    //被引用的出库单号（采购入库场景）
                    if(referencedByMap!=null && referencedByMap.containsKey(dh.getNumber())) {
                        dh.setReferencedByNumbers(referencedByMap.get(dh.getNumber()));
                    }
                    //关联的采购入库单号（采购订单场景）
                    if(purchaseInRefMap!=null && purchaseInRefMap.containsKey(dh.getNumber())) {
                        dh.setPurchaseInNumbers(purchaseInRefMap.get(dh.getNumber()));
                    }
                    //以销定购的情况（不能显示销售单据的金额和客户名称）
                    if(StringUtil.isNotEmpty(purchaseStatus)) {
                        dh.setOrganName("****");
                        dh.setTotalPrice(null);
                        dh.setDiscountLastMoney(null);
                    }
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 根据单据类型获取仓库数组
     * @param subType
     * @return
     * @throws Exception
     */
    public String[] getDepotArray(String subType) throws Exception {
        String [] depotArray = null;
        if(!BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(subType)
                && !BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(subType)
                && !BusinessConstants.SUB_TYPE_SALES_ORDER.equals(subType)) {
            String depotIds = depotService.findDepotStrByCurrentUser();
            depotArray = StringUtil.isNotEmpty(depotIds) ? depotIds.split(",") : null;
        }
        return depotArray;
    }

    /**
     * 根据角色类型获取操作员数组
     * @return
     * @throws Exception
     */
    public String[] getCreatorArray() throws Exception {
        String creator = getCreatorByCurrentUser();
        String [] creatorArray=null;
        if(StringUtil.isNotEmpty(creator)){
            creatorArray = creator.split(",");
        }
        return creatorArray;
    }

    /**
     * 根据角色类型获取操作员数组
     * @param organizationId
     * @return
     * @throws Exception
     */
    public String[] getCreatorArrayByOrg(Long organizationId) throws Exception {
        List<Long> userIdList = orgaUserRelService.getUserIdListByOrgId(organizationId);
        if(userIdList.size()>0) {
            List<String> userIdStrList = userIdList.stream().map(Object::toString).collect(Collectors.toList());
            return StringUtil.listToStringArray(userIdStrList);
        } else {
            return "-1".split(",");
        }
    }

    /**
     * 获取机构数组
     * @return
     */
    /**
     * 根据单据类型和子类型获取编号前缀
     */
    private String getPrefixByTypeAndSubType(String type, String subType) {
        if ("入库".equals(type)) {
            if ("采购".equals(subType)) return "CGRK";
            if ("采购退货".equals(subType)) return "CGTH";
            if ("销售退货".equals(subType)) return "XSTH";
            if ("其它".equals(subType)) return "QTRK";
            if ("调拨".equals(subType)) return "DBRK";
            if ("组装单".equals(subType)) return "ZZD";
            if ("拆卸单".equals(subType)) return "CXD";
        } else if ("出库".equals(type)) {
            if ("销售".equals(subType)) return "XSCK";
            if ("采购退货".equals(subType)) return "CGTH";
            if ("销售退货".equals(subType)) return "XSTH";
            if ("其它".equals(subType)) return "QTCK";
            if ("调拨".equals(subType)) return "DBCK";
            if ("零售".equals(subType)) return "LSCK";
            if ("零售退货".equals(subType)) return "LSTH";
        } else if ("其它".equals(type)) {
            if ("采购订单".equals(subType)) return "CGDD";
            if ("销售订单".equals(subType)) return "XSDD";
            if ("请购单".equals(subType)) return "QGD";
        }
        return "BILL";
    }

    public String[] getOrganArray(String subType, String purchaseStatus) throws Exception {
        String [] organArray = null;
        String type = "UserCustomer";
        Long userId = userService.getCurrentUser().getId();
        //获取权限信息
        String ubValue = userBusinessService.getUBValueByTypeAndKeyId(type, userId.toString());
        List<SupplierSimple> supplierList = supplierService.getAllCustomer();
        if(BusinessConstants.SUB_TYPE_SALES_ORDER.equals(subType) || BusinessConstants.SUB_TYPE_SALES.equals(subType)
                ||BusinessConstants.SUB_TYPE_SALES_RETURN.equals(subType) ) {
            //采购订单里面选择销售订单的时候不要过滤
            if(StringUtil.isEmpty(purchaseStatus)) {
                if (null != supplierList && supplierList.size() > 0) {
                    boolean customerFlag = systemConfigService.getCustomerFlag();
                    List<String> organList = new ArrayList<>();
                    for (SupplierSimple supplier : supplierList) {
                        boolean flag = ubValue.contains("[" + supplier.getId().toString() + "]");
                        if (!customerFlag || flag) {
                            organList.add(supplier.getId().toString());
                        }
                    }
                    if(organList.size() > 0) {
                        organArray = StringUtil.listToStringArray(organList);
                    }
                }
            }
        }
        return organArray;
    }

    /**
     * 根据角色类型获取操作员
     * @return
     * @throws Exception
     */
    public String getCreatorByCurrentUser() throws Exception {
        String creator = "";
        User user = userService.getCurrentUser();
        String roleType = userService.getRoleTypeByUserId(user.getId()).getType(); //角色类型
        if(BusinessConstants.ROLE_TYPE_PRIVATE.equals(roleType)) {
            creator = user.getId().toString();
        } else if(BusinessConstants.ROLE_TYPE_THIS_ORG.equals(roleType)) {
            creator = orgaUserRelService.getUserIdListByUserId(user.getId());
        }
        return creator;
    }

    public Map<String, BigDecimal> getFinishDepositMapByNumberList(List<String> numberList) {
        Map<String,BigDecimal> finishDepositMap = new HashMap<>();
        if(numberList.size()>0) {
            List<FinishDepositVo> list = depotHeadMapperEx.getFinishDepositByNumberList(numberList);
            if(list!=null && list.size()>0) {
                for (FinishDepositVo finishDepositVo : list) {
                    if(finishDepositVo!=null) {
                        finishDepositMap.put(finishDepositVo.getNumber(), finishDepositVo.getFinishDeposit());
                    }
                }
            }
        }
        return finishDepositMap;
    }

    public Map<String, Integer> getBillSizeMapByLinkNumberList(List<String> numberList) throws Exception {
        Map<String, Integer> billListMap = new HashMap<>();
        if(numberList.size()>0) {
            List<DepotHead> list = getBillListByLinkNumberList(numberList);
            if(list!=null && list.size()>0) {
                for (DepotHead depotHead : list) {
                    if(depotHead!=null) {
                        billListMap.put(depotHead.getLinkNumber(), list.size());
                    }
                }
            }
        }
        return billListMap;
    }

    public Map<Long, BigDecimal> getFinancialBillPriceMapByBillIdList(List<Long> idList) {
        Map<Long, BigDecimal> billListMap = new HashMap<>();
        if(!idList.isEmpty()) {
            List<AccountItem> list = accountHeadService.getFinancialBillPriceByBillIdList(idList);
            if(list!=null && !list.isEmpty()) {
                for (AccountItem accountItem : list) {
                    if(accountItem!=null) {
                        billListMap.put(accountItem.getBillId(), accountItem.getEachAmount().abs());
                    }
                }
            }
        }
        return billListMap;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotHead(JSONObject obj, HttpServletRequest request)throws Exception {
        DepotHead depotHead = JSONObject.parseObject(obj.toJSONString(), DepotHead.class);
        depotHead.setCreateTime(new Timestamp(System.currentTimeMillis()));
        depotHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        int result=0;
        try{
            result=depotHeadMapper.insert(depotHead);
            logService.insertLog("单据", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotHead(JSONObject obj, HttpServletRequest request) throws Exception{
        DepotHead depotHead = JSONObject.parseObject(obj.toJSONString(), DepotHead.class);
        DepotHead dh=null;
        try{
            dh = depotHeadMapper.selectByPrimaryKey(depotHead.getId());
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        depotHead.setStatus(dh.getStatus());
        depotHead.setCreateTime(dh.getCreateTime());
        int result=0;
        try{
            result = depotHeadMapper.updateByPrimaryKey(depotHead);
            logService.insertLog("单据",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(depotHead.getId()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDepotHead(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteBillByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotHead(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteBillByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteBillByIds(String ids)throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<DepotHead> dhList = getDepotHeadListByIds(ids);
        for(DepotHead depotHead: dhList){
            //只有未审核的单据才能被删除
            if(!"0".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_MSG));
            }
        }
        for(DepotHead depotHead: dhList){
            sb.append("[").append(depotHead.getNumber()).append("]");
            User userInfo = userService.getCurrentUser();
            //删除入库单据，先校验序列号是否出库，如果未出库则同时删除序列号，如果已出库则不能删除单据
            if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())) {
                List<DepotItem> depotItemList = depotItemMapperEx.findDepotItemListBydepotheadId(depotHead.getId(), BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED);
                if (depotItemList != null && depotItemList.size() > 0) {
                    //单据明细里面存在序列号商品
                    int serialNumberSellCount = depotHeadMapperEx.getSerialNumberBySell(depotHead.getNumber());
                    if (serialNumberSellCount > 0) {
                        //已出库则不能删除单据
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SERIAL_IS_SELL_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_SERIAL_IS_SELL_MSG, depotHead.getNumber()));
                    } else {
                        //删除序列号
                        SerialNumberExample example = new SerialNumberExample();
                        example.createCriteria().andInBillNoEqualTo(depotHead.getNumber());
                        serialNumberService.deleteByExample(example);
                    }
                }
            }
            //删除出库数据回收序列号
            if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                    && !BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                //查询单据子表列表
                List<DepotItem> depotItemList = depotItemMapperEx.findDepotItemListBydepotheadId(depotHead.getId(), BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED);
                /**回收序列号*/
                if (depotItemList != null && depotItemList.size() > 0) {
                    for (DepotItem depotItem : depotItemList) {
                        //BasicNumber=OperNumber*ratio
                        serialNumberService.cancelSerialNumber(depotItem.getMaterialId(), depotHead.getNumber(), (depotItem.getBasicNumber() == null ? 0 : depotItem.getBasicNumber()).intValue(), userInfo);
                    }
                }
            }
            List<DepotItem> list = depotItemService.getListByHeaderId(depotHead.getId());
            //删除单据子表数据
            depotItemMapperEx.batchDeleteDepotItemByDepotHeadIds(new Long[]{depotHead.getId()});
            //删除单据主表信息
            batchDeleteDepotHeadByIds(depotHead.getId().toString());
            //将关联的单据置为审核状态-针对采购入库、销售出库、盘点复盘、其它入库、其它出库
            if(StringUtil.isNotEmpty(depotHead.getLinkNumber())){
                if((BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_REPLAY.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType()))) {
                    //支持逗号分隔的多关联单号，逐个检查并回退关联单据状态
                    List<String> linkNoList = StringUtil.strToStringList(depotHead.getLinkNumber());
                    for(String singleLinkNo : linkNoList) {
                        singleLinkNo = singleLinkNo.trim();
                        if(StringUtil.isEmpty(singleLinkNo)) continue;
                        String status = BusinessConstants.BILLS_STATUS_AUDIT;
                        //查询除当前单据之外，link_number中包含该单号的同类型活跃单据
                        List<DepotHead> exceptCurrentList = depotHeadMapperEx.getListByLinkNumberContaining(
                                singleLinkNo, depotHead.getNumber(), depotHead.getType());
                        if(exceptCurrentList != null && exceptCurrentList.size() > 0) {
                            status = BusinessConstants.BILLS_STATUS_SKIPING;
                        }
                        DepotHead dh = new DepotHead();
                        dh.setStatus(status);
                        DepotHeadExample example = new DepotHeadExample();
                        example.createCriteria().andNumberEqualTo(singleLinkNo);
                        depotHeadMapper.updateByExampleSelective(dh, example);
                        //销售出库删除时，清除采购入库的关联标记（无其他引用时）
                        if(BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                                && BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                                && (exceptCurrentList == null || exceptCurrentList.size() == 0)) {
                            setLinkedFlag(singleLinkNo, "0");
                        }
                    }
                }
            }
            //将关联的单据置为审核状态-针对请购单转采购订单的情况
            if(StringUtil.isNotEmpty(depotHead.getLinkApply())){
                if(BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                    String status = BusinessConstants.BILLS_STATUS_AUDIT;
                    //查询除当前单据之外的关联单据列表
                    List<DepotHead> exceptCurrentList = getListByLinkApplyExceptCurrent(depotHead.getLinkApply(), depotHead.getNumber(), depotHead.getType());
                    if(exceptCurrentList!=null && exceptCurrentList.size()>0) {
                        status = BusinessConstants.BILLS_STATUS_SKIPING;
                    }
                    DepotHead dh = new DepotHead();
                    dh.setStatus(status);
                    DepotHeadExample example = new DepotHeadExample();
                    example.createCriteria().andNumberEqualTo(depotHead.getLinkApply());
                    depotHeadMapper.updateByExampleSelective(dh, example);
                }
            }
            //将关联的销售订单单据置为未采购状态-针对销售订单转采购订单的情况
            if(StringUtil.isNotEmpty(depotHead.getLinkNumber())){
                if(BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                    //支持逗号分隔的多关联单号，逐个更新销售订单的采购状态
                    List<String> linkNoList = StringUtil.strToStringList(depotHead.getLinkNumber());
                    for(String singleLinkNo : linkNoList) {
                        singleLinkNo = singleLinkNo.trim();
                        if(StringUtil.isEmpty(singleLinkNo)) continue;
                        DepotHead dh = new DepotHead();
                        //获取分批操作后单据的商品和商品数量（汇总）
                        List<DepotItemVo4MaterialAndSum> batchList = depotItemMapperEx.getBatchBillDetailMaterialSum(singleLinkNo, "normal", depotHead.getType());
                        if(batchList.size() > 0) {
                            dh.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_SKIPING);
                        } else {
                            dh.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_UN_AUDIT);
                        }
                        DepotHeadExample example = new DepotHeadExample();
                        example.createCriteria().andNumberEqualTo(singleLinkNo);
                        depotHeadMapper.updateByExampleSelective(dh, example);
                    }
                }
            }
            //对于零售出库单据，更新会员的预收款信息
            if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType())){
                if(BusinessConstants.PAY_TYPE_PREPAID.equals(depotHead.getPayType())) {
                    if (depotHead.getOrganId() != null) {
                        //更新会员预付款
                        supplierService.updateAdvanceIn(depotHead.getOrganId());
                    }
                }
            }
            for (DepotItem depotItem : list) {
                //更新当前库存
                depotItemService.updateCurrentStock(depotItem);
                //更新当前成本价
                depotItemService.updateCurrentUnitPrice(depotItem);
            }
        }
        //路径列表
        List<String> pathList = new ArrayList<>();
        for(DepotHead depotHead: dhList){
            if(StringUtil.isNotEmpty(depotHead.getFileName())) {
                pathList.add(depotHead.getFileName());
            }
        }
        //逻辑删除文件
        systemConfigService.deleteFileByPathList(pathList);
        logService.insertLog("单据", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return 1;
    }

    /**
     * 删除单据主表信息
     * @param ids
     * @return
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotHeadByIds(String ids)throws Exception {
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result=0;
        try{
            result = depotHeadMapperEx.batchDeleteDepotHeadByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<DepotHead> getDepotHeadListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<DepotHead> list = new ArrayList<>();
        try{
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(idList);
            list = depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 校验单据编号是否存在
     * @param id
     * @param number
     * @return
     * @throws Exception
     */
    public int checkIsBillNumberExist(Long id, String number)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andIdNotEqualTo(id).andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list = null;
        try{
            list = depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchForceClose(String ids, HttpServletRequest request) throws Exception {
        int result = 0;
        StringBuilder billNoStr = new StringBuilder();
        List<Long> idList = StringUtil.strToLongList(ids);
        for(Long id: idList) {
            DepotHead depotHead = getDepotHead(id);
            validateFeatureEnabled(depotHead);
            //状态里面不包含部分不能强制结单
            if(!"3".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_MSG, depotHead.getNumber()));
            } else {
                billNoStr.append(depotHead.getNumber()).append(" ");
            }
        }
        if(idList.size()>0) {
            DepotHead depotHead = new DepotHead();
            //完成状态
            depotHead.setStatus("2");
            //给备注后面追加：强制结单
            String remark = StringUtil.isNotEmpty(depotHead.getRemark())? depotHead.getRemark() + "[强制结单]": "[强制结单]";
            depotHead.setRemark(remark);
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(idList);
            result = depotHeadMapper.updateByExampleSelective(depotHead, example);
            //记录日志
            String billNos = billNoStr.toString();
            if(StringUtil.isNotEmpty(billNos)) {
                logService.insertLog("单据", "强制结单：" + billNos,
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchForceClosePurchase(String ids, HttpServletRequest request) throws Exception {
        int result = 0;
        StringBuilder billNoStr = new StringBuilder();
        List<Long> idList = StringUtil.strToLongList(ids);
        for(Long id: idList) {
            DepotHead depotHead = getDepotHead(id);
            validateFeatureEnabled(depotHead);
            //状态里面不包含部分不能强制结单
            if(!"3".equals(depotHead.getPurchaseStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_MSG, depotHead.getNumber()));
            } else {
                billNoStr.append(depotHead.getNumber()).append(" ");
            }
        }
        if(idList.size()>0) {
            DepotHead depotHead = new DepotHead();
            //完成状态
            depotHead.setPurchaseStatus("2");
            //给备注后面追加：强制结单-以销定购
            String remark = StringUtil.isNotEmpty(depotHead.getRemark())? depotHead.getRemark() + "[强制结单-以销定购]": "[强制结单-以销定购]";
            depotHead.setRemark(remark);
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(idList);
            result = depotHeadMapper.updateByExampleSelective(depotHead, example);
            //记录日志
            String billNos = billNoStr.toString();
            if(StringUtil.isNotEmpty(billNos)) {
                logService.insertLog("单据", "强制结单-以销定购：" + billNos,
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(String status, String depotHeadIDs)throws Exception {
        int result = 0;
        boolean forceApprovalFlag = systemConfigService.getForceApprovalFlag();
        boolean minusStockFlag = systemConfigService.getMinusStockFlag();
        boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        List<Long> dhIds = new ArrayList<>();
        List<String> noList = new ArrayList<>();
        List<Long> ids = StringUtil.strToLongList(depotHeadIDs);
        for(Long id: ids) {
            DepotHead depotHead = getDepotHead(id);
            validateFeatureEnabled(depotHead);
            if("0".equals(status)){
                //进行反审核操作
                if("1".equals(depotHead.getStatus()) && "0".equals(depotHead.getPurchaseStatus())) {
                    dhIds.add(id);
                    noList.add(depotHead.getNumber());
                } else if(("2".equals(depotHead.getStatus()) || "3".equals(depotHead.getStatus()))
                        && "0".equals(depotHead.getPurchaseStatus())) {
                    //status='2'或'3'（历史脏数据兼容）：检查是否还有下游单据引用，若无则允许反审核
                    if(!hasActiveDownstreamBills(depotHead.getNumber())) {
                        dhIds.add(id);
                        noList.add(depotHead.getNumber());
                    } else {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG));
                    }
                } else if("2".equals(depotHead.getPurchaseStatus()) || "3".equals(depotHead.getPurchaseStatus())) {
                    //完成采购/部分采购：检查下游单据是否已全部删除，若无引用则允许反审核
                    if(!hasActiveDownstreamBills(depotHead.getNumber())) {
                        dhIds.add(id);
                        noList.add(depotHead.getNumber());
                        //同时重置采购状态
                        DepotHead resetUpdate = new DepotHead();
                        resetUpdate.setId(id);
                        resetUpdate.setPurchaseStatus("0");
                        resetUpdate.setLinkedFlag("0");
                        depotHeadMapper.updateByPrimaryKeySelective(resetUpdate);
                    } else {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_TWO_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_TWO_MSG));
                    }
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG));
                }
            } else if("1".equals(status)){
                //进行审核操作
                if("0".equals(depotHead.getStatus())) {
                    dhIds.add(id);
                    noList.add(depotHead.getNumber());
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_MSG));
                }
            }
            // 开启强审核，并且没有开启负库存：
            // 1、开启出入库管理，销售出库和采购退货单据审核的时候不做校验，其它出库做校验；
            // 2、未开启出入库管理，销售出库和采购退货单据审核的时候做校验，其它出库不做校验。
            if("1".equals(status)) {
                if(forceApprovalFlag && !minusStockFlag) {
                    if(inOutManageFlag) {
                        if("出库".equals(depotHead.getType()) && "其它".equals(depotHead.getSubType())) {
                            //校验单据中的商品库存是否不足
                            depotItemService.checkMaterialStock(depotHead.getNumber(), depotHead.getId());
                        }
                    } else {
                        if(("出库".equals(depotHead.getType()) && "销售".equals(depotHead.getSubType()))
                                || ("出库".equals(depotHead.getType()) && "采购退货".equals(depotHead.getSubType()))) {
                            //校验单据中的商品库存是否不足
                            depotItemService.checkMaterialStock(depotHead.getNumber(), depotHead.getId());
                        }
                    }
                }
            }
        }
        if(!dhIds.isEmpty()) {
            DepotHead depotHead = new DepotHead();
            depotHead.setStatus(status);
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(dhIds);
            result = depotHeadMapper.updateByExampleSelective(depotHead, example);
            //更新当前库存
            if(systemConfigService.getForceApprovalFlag()) {
                for(Long dhId: dhIds) {
                    List<DepotItem> list = depotItemService.getListByHeaderId(dhId);
                    for (DepotItem depotItem : list) {
                        depotItemService.updateCurrentStock(depotItem);
                    }
                }
            }
            //审核销售出库单时，同步更新关联物流单的重量
            if("1".equals(status)) {
                List<Long> saleOutIds = new ArrayList<>();
                for(Long dhId: dhIds) {
                    DepotHead dh = getDepotHead(dhId);
                    if("出库".equals(dh.getType()) && "销售".equals(dh.getSubType())) {
                        saleOutIds.add(dhId);
                    }
                }
                if(!saleOutIds.isEmpty()) {
                    freightHeadService.recalcByDepotHeadIds(saleOutIds);
                }
                //审核销售出库单时，自动核准非过磅类别（全部明细均无 weightEditable）的单据
                for(Long saleOutId: saleOutIds) {
                    List<DepotItemVo4WithInfoEx> detailList = depotItemService.getDetailList(saleOutId);
                    boolean hasWeightEditable = false;
                    if(detailList != null) {
                        for(DepotItemVo4WithInfoEx item: detailList) {
                            if("1".equals(item.getWeightEditable())) {
                                hasWeightEditable = true;
                                break;
                            }
                        }
                    }
                    if(!hasWeightEditable) {
                        DepotHead waUpdate = new DepotHead();
                        waUpdate.setId(saleOutId);
                        waUpdate.setWeightApproved("1");
                        depotHeadMapper.updateByPrimaryKeySelective(waUpdate);
                    }
                }
            }
            //记录日志
            if(!noList.isEmpty() && ("0".equals(status) || "1".equals(status))) {
                String statusStr = status.equals("1")?"[审核]":"[反审核]";
                logService.insertLog("单据",
                        new StringBuffer(statusStr).append(String.join(", ", noList)).toString(),
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    /**
     * 批量设置价格核准状态（纯标记，不影响业务流程）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void batchSetPriceApproved(String priceApproved, String ids) throws Exception {
        String[] idArr = ids.split(",");
        for (String idStr : idArr) {
            Long id = Long.parseLong(idStr.trim());
            DepotHead update = new DepotHead();
            update.setId(id);
            update.setPriceApproved(priceApproved);
            try {
                depotHeadMapper.updateByPrimaryKeySelective(update);
            } catch (Exception e) {
                JshException.writeFail(logger, e);
            }
        }
    }

    /**
     * 批量设置重量核准状态（纯标记，不影响业务流程）
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetWeightApproved(String weightApproved, String ids) throws Exception {
        if (!"0".equals(weightApproved) && !"1".equals(weightApproved)) {
            throw new BusinessRunTimeException(0, "重量核准状态不合法");
        }
        if (StringUtil.isEmpty(ids)) {
            throw new BusinessRunTimeException(0, "缺少单据ID");
        }
        String[] idArr = ids.split(",");
        int updateCount = 0;
        for (String idStr : idArr) {
            if (StringUtil.isEmpty(idStr)) {
                continue;
            }
            Long id = Long.parseLong(idStr.trim());
            DepotHead existing = getDepotHead(id);
            if (existing == null) {
                throw new BusinessRunTimeException(0, "单据不存在");
            }
            if (!"出库".equals(existing.getType()) || !"销售".equals(existing.getSubType())) {
                throw new BusinessRunTimeException(0, "仅销售出库单支持重量核准");
            }
            if (!"1".equals(existing.getStatus())) {
                throw new BusinessRunTimeException(0, "仅已审核的销售出库单支持重量核准");
            }
            if ("1".equals(weightApproved) && "1".equals(existing.getWeightApproved())) {
                throw new BusinessRunTimeException(0, "所选单据已完成重量核准");
            }
            if ("0".equals(weightApproved) && !"1".equals(existing.getWeightApproved())) {
                throw new BusinessRunTimeException(0, "仅已重量核准的单据可以取消核准");
            }
            DepotHead update = new DepotHead();
            update.setId(id);
            update.setWeightApproved(weightApproved);
            int rows = depotHeadMapper.updateByPrimaryKeySelective(update);
            if (rows <= 0) {
                throw new BusinessRunTimeException(0, "重量核准状态更新失败");
            }
            updateCount += rows;
        }
        return updateCount;
    }

    /**
     * 更新已审核单据的明细单价（价格修改模式）
     * 仅更新明细行的 unit_price、all_price、tax_money、tax_last_money，不改主表状态
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateItemPrices(String beanJson, String rows, HttpServletRequest request) throws Exception {
        DepotHead depotHead = JSONObject.parseObject(beanJson, DepotHead.class);
        DepotHead existing = getDepotHead(depotHead.getId());
        if (existing == null) {
            throw new BusinessRunTimeException(0, "单据不存在");
        }
        //仅允许已审核+未核准的单据
        if (!"1".equals(existing.getStatus()) || "1".equals(existing.getPriceApproved())) {
            throw new BusinessRunTimeException(0, "仅未核准的已审核单据可以修改单价");
        }
        //更新明细行的价格字段
        JSONArray itemArr = JSONArray.parseArray(rows);
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (itemArr != null) {
            for (int i = 0; i < itemArr.size(); i++) {
                JSONObject item = itemArr.getJSONObject(i);
                if (item.getLong("id") != null) {
                    com.jsh.erp.datasource.entities.DepotItem upd = new com.jsh.erp.datasource.entities.DepotItem();
                    upd.setId(item.getLong("id"));
                    upd.setUnitPrice(item.getBigDecimal("unitPrice"));
                    upd.setAllPrice(item.getBigDecimal("allPrice"));
                    if (item.getBigDecimal("taxMoney") != null) {
                        upd.setTaxMoney(item.getBigDecimal("taxMoney"));
                    }
                    if (item.getBigDecimal("taxLastMoney") != null) {
                        upd.setTaxLastMoney(item.getBigDecimal("taxLastMoney"));
                    }
                    depotItemMapper.updateByPrimaryKeySelective(upd);
                    if (item.getBigDecimal("allPrice") != null) {
                        totalPrice = totalPrice.add(item.getBigDecimal("allPrice"));
                    }
                }
            }
        }
        //同步更新主表合计金额、优惠、应收及日期备注
        DepotHead headUpd = new DepotHead();
        headUpd.setId(depotHead.getId());
        headUpd.setTotalPrice(totalPrice);
        headUpd.setDiscount(depotHead.getDiscount());
        headUpd.setDiscountMoney(depotHead.getDiscountMoney());
        headUpd.setDiscountLastMoney(depotHead.getDiscountLastMoney());
        headUpd.setChangeAmount(depotHead.getChangeAmount());
        headUpd.setOperTime(depotHead.getOperTime());
        headUpd.setRemark(depotHead.getRemark());
        depotHeadMapper.updateByPrimaryKeySelective(headUpd);
        boolean dateChanged = depotHead.getOperTime() != null
                && !depotHead.getOperTime().equals(existing.getOperTime());
        boolean remarkChanged = depotHead.getRemark() != null
                && !depotHead.getRemark().equals(existing.getRemark());
        String logFlag = "价格修改:";
        if (dateChanged && remarkChanged) {
            logFlag = "价格/日期/备注修改:";
        } else if (dateChanged) {
            logFlag = "价格/日期修改:";
        } else if (remarkChanged) {
            logFlag = "价格/备注修改:";
        }
        logService.insertLog("单据",
                BusinessConstants.LOG_OPERATION_TYPE_EDIT + logFlag + existing.getNumber(),
                request);
    }

    /**
     * 更新已审核单据的明细重量（重量编辑模式）
     * 仅更新明细行的 weight、remark 字段，不改主表状态
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateItemWeights(String beanJson, String rows, HttpServletRequest request) throws Exception {
        DepotHead depotHead = JSONObject.parseObject(beanJson, DepotHead.class);
        DepotHead existing = getDepotHead(depotHead.getId());
        if (existing == null) {
            throw new BusinessRunTimeException(0, "单据不存在");
        }
        //仅允许已审核+重量未核准的单据
        if (!"1".equals(existing.getStatus()) || "1".equals(existing.getWeightApproved())) {
            throw new BusinessRunTimeException(0, "仅重量未核准的已审核单据可以修改重量");
        }
        //更新明细行的重量、备注字段
        JSONArray itemArr = JSONArray.parseArray(rows);
        if (itemArr != null) {
            for (int i = 0; i < itemArr.size(); i++) {
                JSONObject item = itemArr.getJSONObject(i);
                if (item.getLong("id") != null) {
                    com.jsh.erp.datasource.entities.DepotItem upd = new com.jsh.erp.datasource.entities.DepotItem();
                    upd.setId(item.getLong("id"));
                    if (item.getBigDecimal("weight") != null) {
                        upd.setWeight(item.getBigDecimal("weight"));
                    }
                    if (item.containsKey("remark")) {
                        upd.setRemark(item.getString("remark"));
                    }
                    depotItemMapper.updateByPrimaryKeySelective(upd);
                }
            }
        }
        //同步刷新关联价格核准单及已核准销售出库金额
        priceApprovalService.syncByDepotHeadAfterWeightChange(depotHead.getId());
        //同步更新关联物流单的重量
        List<Long> ids = new ArrayList<>();
        ids.add(depotHead.getId());
        freightHeadService.recalcByDepotHeadIds(ids);
        logService.insertLog("单据",
                BusinessConstants.LOG_OPERATION_TYPE_EDIT + "重量修改:" + existing.getNumber(),
                request);
    }

    public Map<Long,String> findMaterialsListMapByHeaderIdList(List<Long> idList)throws Exception {
        Map<Long,String> materialsListMap = new HashMap<>();
        if(idList.size()>0) {
            List<MaterialsListVo> list = depotHeadMapperEx.findMaterialsListMapByHeaderIdList(idList);
            for (MaterialsListVo materialsListVo : list) {
                String materialsList = materialsListVo.getMaterialsList();
                if(StringUtil.isNotEmpty(materialsList)) {
                    materialsList = materialsList.replace(",","，");
                }
                materialsListMap.put(materialsListVo.getHeaderId(), materialsList);
            }
        }
        return materialsListMap;
    }

    /**
     * 根据出库单ID列表批量查询关联的物流单号
     */
    public Map<Long,String> getFreightBillNoMapByDepotHeadIdList(List<Long> idList) {
        Map<Long,String> freightBillNoMap = new HashMap<>();
        if(idList != null && idList.size() > 0) {
            try {
                List<Map<String, Object>> list = freightItemMapperEx.selectFreightBillNoByDepotHeadIds(idList);
                if(list != null) {
                    for(Map<String, Object> map : list) {
                        Long depotHeadId = Long.parseLong(map.get("depotHeadId").toString());
                        String freightBillNo = map.get("freightBillNo") != null ? map.get("freightBillNo").toString() : "";
                        freightBillNoMap.put(depotHeadId, freightBillNo);
                    }
                }
            } catch (Exception e) {
                logger.error("查询物流单关联信息异常", e);
            }
        }
        return freightBillNoMap;
    }

    /**
     * 根据出库单ID列表批量查询关联物流单的运输车号
     */
    public Map<Long,String> getFreightCarNoMapByDepotHeadIdList(List<Long> idList) {
        Map<Long,String> carNoMap = new HashMap<>();
        if(idList != null && idList.size() > 0) {
            try {
                List<Map<String, Object>> list = freightItemMapperEx.selectFreightBillNoByDepotHeadIds(idList);
                if(list != null) {
                    for(Map<String, Object> map : list) {
                        Long depotHeadId = Long.parseLong(map.get("depotHeadId").toString());
                        String carNo = map.get("carNo") != null ? map.get("carNo").toString() : "";
                        carNoMap.put(depotHeadId, carNo);
                    }
                }
            } catch (Exception e) {
                logger.error("查询物流单车号信息异常", e);
            }
        }
        return carNoMap;
    }

    /**
     * 批量查询被采购入库引用的单号（采购订单场景）
     */
    public Map<String,String> getPurchaseInByOrderNumbersMapByList(List<String> numberList) {
        Map<String,String> map = new HashMap<>();
        if(numberList!=null && numberList.size()>0) {
            try {
                List<Map<String, String>> list = depotHeadMapperEx.getPurchaseInByOrderNumbersMap(numberList);
                if(list!=null) {
                    for(Map<String, String> item : list) {
                        String billNumber = item.get("billNumber") != null ? item.get("billNumber").toString() : "";
                        String refNumbers = item.get("refNumbers") != null ? item.get("refNumbers").toString() : "";
                        if(StringUtil.isNotEmpty(billNumber)) {
                            map.put(billNumber, refNumbers);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("查询采购入库关联信息异常", e);
            }
        }
        return map;
    }

    /**
     * 批量查询被销售出库引用的单号
     */
    public Map<String,String> getReferencedByNumbersMapByList(List<String> numberList) {
        Map<String,String> map = new HashMap<>();
        if(numberList!=null && numberList.size()>0) {
            try {
                List<Map<String, String>> list = depotHeadMapperEx.getReferencedByNumbersMap(numberList);
                if(list!=null) {
                    for(Map<String, String> item : list) {
                        String billNumber = item.get("billNumber") != null ? item.get("billNumber").toString() : "";
                        String refNumbers = item.get("refNumbers") != null ? item.get("refNumbers").toString() : "";
                        if(StringUtil.isNotEmpty(billNumber)) {
                            map.put(billNumber, refNumbers);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("查询被引用单号异常", e);
            }
        }
        return map;
    }

    public Map<Long,BigDecimal> getMaterialCountListMapByHeaderIdList(List<Long> idList)throws Exception {
        Map<Long,BigDecimal> materialCountListMap = new HashMap<>();
        if(idList.size()>0) {
            List<MaterialCountVo> list = depotHeadMapperEx.getMaterialCountListByHeaderIdList(idList);
            for(MaterialCountVo materialCountVo : list){
                materialCountListMap.put(materialCountVo.getHeaderId(), materialCountVo.getMaterialCount());
            }
        }
        return materialCountListMap;
    }

    public List<Map<String, Object>> getDashboardPurchaseInList() {
        return depotHeadMapperEx.getDashboardPurchaseInList();
    }

    public List<Map<String, Object>> getDashboardSaleOutList() {
        return depotHeadMapperEx.getDashboardSaleOutList();
    }

    public List<Map<String, Object>> getDashboardFreightList() {
        return depotHeadMapperEx.getDashboardFreightList();
    }

    /**
     * 根据单据ID列表批量查询总重量(吨)
     */
    public Map<Long,BigDecimal> getTotalWeightMapByHeaderIdList(List<Long> idList) {
        Map<Long,BigDecimal> totalWeightMap = new HashMap<>();
        if(idList.size()>0) {
            List<TotalWeightVo> list = depotHeadMapperEx.getTotalWeightListByHeaderIdList(idList);
            for(TotalWeightVo vo : list){
                totalWeightMap.put(vo.getHeaderId(), vo.getTotalWeight());
            }
        }
        return totalWeightMap;
    }

    public List<DepotHeadVo4InDetail> findInOutDetail(String beginTime, String endTime, String type, String[] creatorArray,
                                                      String[] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag,
                                                      String materialParam, List<Long> depotList, Integer oId, String number,
                                                      Long creator, String remark, String column, String order, Integer offset, Integer rows) throws Exception{
        List<DepotHeadVo4InDetail> list = null;
        try{
            list =depotHeadMapperEx.findInOutDetail(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findInOutDetailCount(String beginTime, String endTime, String type, String[] creatorArray,
                                    String[] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag, String materialParam, List<Long> depotList, Integer oId, String number,
                                    Long creator, String remark) throws Exception{
        int result = 0;
        try{
            result =depotHeadMapperEx.findInOutDetailCount(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InDetail findInOutDetailStatistic(String beginTime, String endTime, String type, String [] creatorArray,
                                                      String [] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag,
                                                      String materialParam, List<Long> depotList, Integer oId, String number,
                                                      Long creator, String remark) throws Exception{
        DepotHeadVo4InDetail item = new DepotHeadVo4InDetail();
        try{
            List<DepotHeadVo4InDetail> list =depotHeadMapperEx.findInOutDetailStatistic(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark);
            if(list.size()>0) {
                item.setOperNumber(list.get(0).getOperNumber());
                item.setAllPrice(list.get(0).getAllPrice());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4InOutMCount> findInOutMaterialCount(String beginTime, String endTime, String type, List<Long> categoryList,
                                                                Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                                                List<Long> depotList, Long organizationId, Integer oId, String column, String order,
                                                                Integer offset, Integer rows)throws Exception {
        List<DepotHeadVo4InOutMCount> list = null;
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            list =depotHeadMapperEx.findInOutMaterialCount(beginTime, endTime, type, categoryList, forceFlag, inOutManageFlag, materialParam, depotList, oId,
                    creatorArray, organArray, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findInOutMaterialCountTotal(String beginTime, String endTime, String type, List<Long> categoryList,
                                           Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                           List<Long> depotList, Long organizationId, Integer oId)throws Exception {
        int result = 0;
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            result =depotHeadMapperEx.findInOutMaterialCountTotal(beginTime, endTime, type, categoryList, forceFlag, inOutManageFlag, materialParam, depotList, oId,
                    creatorArray, organArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InOutMCount findInOutMaterialCountStatistic(String beginTime, String endTime, String type, List<Long> categoryList,
                                                                Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                                                List<Long> depotList, Long organizationId, Integer oId) throws Exception {
        DepotHeadVo4InOutMCount item = new DepotHeadVo4InOutMCount();
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            List<DepotHeadVo4InOutMCount> list = depotHeadMapperEx.findInOutMaterialCountStatistic(beginTime, endTime, type, categoryList,
                    forceFlag, inOutManageFlag, materialParam, depotList, oId, creatorArray, organArray);
            if(list.size()>0) {
                item.setNumSum(list.get(0).getNumSum());
                item.setPriceSum(list.get(0).getPriceSum());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4InDetail> findAllocationDetail(String beginTime, String endTime, String subType, String number,
                            String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList, List<Long> depotFList,
                            String remark, String column, String order, Integer offset, Integer rows) throws Exception{
        List<DepotHeadVo4InDetail> list = null;
        try{
            list =depotHeadMapperEx.findAllocationDetail(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findAllocationDetailCount(String beginTime, String endTime, String subType, String number,
                            String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList,  List<Long> depotFList,
                            String remark) throws Exception{
        int result = 0;
        try{
            result =depotHeadMapperEx.findAllocationDetailCount(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InDetail findAllocationStatistic(String beginTime, String endTime, String subType, String number,
                                                        String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList, List<Long> depotFList,
                                                        String remark) throws Exception{
        DepotHeadVo4InDetail item = new DepotHeadVo4InDetail();
        try{
            List<DepotHeadVo4InDetail> list =depotHeadMapperEx.findAllocationStatistic(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark);
            if(list.size()>0) {
                item.setOperNumber(list.get(0).getOperNumber());
                item.setAllPrice(list.get(0).getAllPrice());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4StatementAccount> getStatementAccount(String beginTime, String endTime, Integer organId, String [] organArray,
                                                                  String[] projectNames, Integer hasDebt, String supplierType, String type, String subType, String typeBack,
                                                                  String subTypeBack, String billType, Integer offset, Integer rows) {
        List<DepotHeadVo4StatementAccount> list = null;
        try{
            list = depotHeadMapperEx.getStatementAccount(beginTime, endTime, organId, organArray, projectNames, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType, offset, rows);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int getStatementAccountCount(String beginTime, String endTime, Integer organId, String [] organArray,
                                        String[] projectNames, Integer hasDebt, String supplierType, String type, String subType, String typeBack, String subTypeBack, String billType) {
        int result = 0;
        try{
            result = depotHeadMapperEx.getStatementAccountCount(beginTime, endTime, organId, organArray, projectNames, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotHeadVo4StatementAccount> getStatementAccountTotalPay(String beginTime, String endTime, Integer organId, String [] organArray,
                                                                          String[] projectNames, Integer hasDebt, String supplierType, String type, String subType,
                                                                          String typeBack, String subTypeBack, String billType) {
        List<DepotHeadVo4StatementAccount> list = null;
        try{
            list = depotHeadMapperEx.getStatementAccountTotalPay(beginTime, endTime, organId, organArray, projectNames, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int getNeedCount(String supplierType) throws Exception {
        String type = "";
        String subType = "";
        String typeBack = "";
        String subTypeBack = "";
        String billType = "";
        if (("供应商").equals(supplierType)) {
            type = "入库";
            subType = "采购";
            typeBack = "出库";
            subTypeBack = "采购退货";
            billType = "付款";
        } else if (("客户").equals(supplierType)) {
            type = "出库";
            subType = "销售";
            typeBack = "入库";
            subTypeBack = "销售退货";
            billType = "收款";
        }
        String beginTime = Tools.parseDayToTime(Tools.getYearBegin(), BusinessConstants.DAY_FIRST_TIME);
        String endTime = Tools.getCenternTime(new Date());
        String [] organArray = getOrganArray(subType, "");
        return getStatementAccountCount(beginTime, endTime, null, organArray,
                null, 1, supplierType, type, subType,typeBack, subTypeBack, billType);
    }

    public List<DepotHeadVo4List> getDetailByNumber(String number, HttpServletRequest request)throws Exception {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            Map<Long,String> personMap = personService.getPersonMap();
            Map<Long,String> accountMap = accountService.getAccountMap();
            List<DepotHeadVo4List> list = depotHeadMapperEx.getDetailByNumber(number);
            if (null != list) {
                validateFeatureEnabled(list.get(0).getSubType());
                List<Long> idList = new ArrayList<>();
                List<String> numberList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                    numberList.add(dh.getNumber());
                }
                //通过批量查询去构造map
                Map<String,Integer> billSizeMap = getBillSizeMapByLinkNumberList(numberList);
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                DepotHeadVo4List dh = list.get(0);
                String billCategory = getBillCategory(dh.getSubType());
                if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                    String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                    dh.setAccountName(accountStr);
                }
                if(dh.getAccountIdList() != null) {
                    String accountidlistStr = dh.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", "");
                    dh.setAccountIdList(accountidlistStr);
                }
                if(dh.getAccountMoneyList() != null) {
                    String accountmoneylistStr = dh.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
                    dh.setAccountMoneyList(accountmoneylistStr);
                }
                if(dh.getChangeAmount() != null) {
                    dh.setChangeAmount(roleService.parseBillPriceByLimit(dh.getChangeAmount().abs(), billCategory, priceLimit, request));
                } else {
                    dh.setChangeAmount(BigDecimal.ZERO);
                }
                if(dh.getTotalPrice() != null) {
                    dh.setTotalPrice(roleService.parseBillPriceByLimit(dh.getTotalPrice().abs(), billCategory, priceLimit, request));
                }
                BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
                dh.setDiscountLastMoney(roleService.parseBillPriceByLimit(discountLastMoney, billCategory, priceLimit, request));
                BigDecimal backAmount = dh.getBackAmount()!=null?dh.getBackAmount():BigDecimal.ZERO;
                dh.setBackAmount(roleService.parseBillPriceByLimit(backAmount, billCategory, priceLimit, request));
                if(dh.getDeposit() == null) {
                    dh.setDeposit(BigDecimal.ZERO);
                } else {
                    dh.setDeposit(roleService.parseBillPriceByLimit(dh.getDeposit(), billCategory, priceLimit, request));
                }
                //欠款计算
                BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
                BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
                BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount():BigDecimal.ZERO;
                BigDecimal debt = discountLastMoney.add(otherMoney).subtract((deposit.add(changeAmount)));
                dh.setDebt(roleService.parseBillPriceByLimit(debt, billCategory, priceLimit, request));
                //是否有退款单
                if(billSizeMap!=null) {
                    Integer billListSize = billSizeMap.get(dh.getNumber());
                    dh.setHasBackFlag(billListSize!=null && billListSize>0);
                }
                if(StringUtil.isNotEmpty(dh.getSalesMan())) {
                    dh.setSalesManStr(personService.getPersonByMapAndIds(personMap,dh.getSalesMan()));
                }
                if(dh.getOperTime() != null) {
                    dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                }
                //商品信息简述
                if(materialsListMap!=null) {
                    dh.setMaterialsList(materialsListMap.get(dh.getId()));
                }
                //商品总数量
                if(materialCountListMap!=null) {
                    dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                }
                User creatorUser = userService.getUser(dh.getCreator());
                if(creatorUser!=null) {
                    dh.setCreatorName(creatorUser.getUsername());
                }
                //物流单号
                Map<Long,String> freightBillNoMap = getFreightBillNoMapByDepotHeadIdList(idList);
                if(freightBillNoMap!=null && freightBillNoMap.containsKey(dh.getId())) {
                    dh.setFreightBillNo(freightBillNoMap.get(dh.getId()));
                }
                //物流单运输车号
                Map<Long,String> freightCarNoMap = getFreightCarNoMapByDepotHeadIdList(idList);
                if(freightCarNoMap!=null && freightCarNoMap.containsKey(dh.getId())) {
                    dh.setFreightCarNo(freightCarNoMap.get(dh.getId()));
                }
                //总重量
                Map<Long,BigDecimal> totalWeightMap = getTotalWeightMapByHeaderIdList(idList);
                if(totalWeightMap!=null && totalWeightMap.containsKey(dh.getId())) {
                    dh.setTotalWeight(totalWeightMap.get(dh.getId()));
                }
                //价格核准送到日期（用于自定义打印默认送货日期）
                try {
                    Long tenantId = userService.getCurrentUser() != null ? userService.getCurrentUser().getTenantId() : null;
                    com.jsh.erp.datasource.entities.PriceApproval pa =
                            priceApprovalMapper.selectByDepotHeadId(dh.getId(), tenantId);
                    if (pa != null && pa.getDeliveryDate() != null) {
                        dh.setPriceApprovalDeliveryDate(
                                new java.text.SimpleDateFormat("yyyyMMdd").format(pa.getDeliveryDate()));
                    }
                } catch (Exception ex) {
                    logger.warn("查询价格核准送到日期失败: {}", ex.getMessage());
                }
                resList.add(dh);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    /**
     * 检查是否存在引用指定单据号的下游活跃单据（未删除）
     * 用于反审核时判断status='3'的单据是否可以安全回退
     * @param number 被引用的单据号
     * @return true=有下游引用，false=无下游引用
     */
    public boolean hasActiveDownstreamBills(String number) {
        //使用FIND_IN_SET查询，支持下游单据link_number为逗号分隔值的场景
        int count = depotHeadMapperEx.countActiveDownstreamBillsByLinkNumber(number);
        return count > 0;
    }

    /**
     * 设置单据的关联标记（linked_flag），独立于status字段
     * 按主键更新（selective，仅更新非null字段）
     */
    public void updateById(DepotHead dh) {
        depotHeadMapper.updateByPrimaryKeySelective(dh);
    }

    /**
     * @param number 单据号
     * @param flag "0"=未关联, "1"=已关联
     */
    public void setLinkedFlag(String number, String flag) {
        DepotHead dh = new DepotHead();
        dh.setLinkedFlag(flag);
        DepotHeadExample example = new DepotHeadExample();
        List<String> numberList = StringUtil.strToStringList(number);
        example.createCriteria().andNumberIn(numberList);
        depotHeadMapper.updateByExampleSelective(dh, example);
    }

    /**
     * 根据明细的 linkId 重新计算并同步单据头的 link_number，确保一致性
     * @param headerId 单据主表ID
     */
    public void syncLinkNumberByDetail(Long headerId) {
        try {
            List<String> linkNumbers = depotHeadMapperEx.getDistinctLinkNumbersByHeaderId(headerId);
            DepotHead update = new DepotHead();
            update.setId(headerId);
            if(linkNumbers != null && !linkNumbers.isEmpty()) {
                update.setLinkNumber(String.join(",", linkNumbers));
            } else {
                update.setLinkNumber("");
            }
            depotHeadMapper.updateByPrimaryKeySelective(update);
        } catch (Exception e) {
            logger.error("同步link_number异常, headerId=" + headerId, e);
        }
    }

    /**
     * 查询除当前单据之外的关联单据列表
     * @param linkNumber
     * @param number
     * @return
     * @throws Exception
     */
    public List<DepotHead> getListByLinkNumberExceptCurrent(String linkNumber, String number, String type)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(linkNumber).andNumberNotEqualTo(number).andTypeEqualTo(type)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return depotHeadMapper.selectByExample(example);
    }

    /**
     * 查询除当前单据之外的关联单据列表
     * @param linkApply
     * @param number
     * @return
     * @throws Exception
     */
    public List<DepotHead> getListByLinkApplyExceptCurrent(String linkApply, String number, String type)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkApplyEqualTo(linkApply).andNumberNotEqualTo(number).andTypeEqualTo(type)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return depotHeadMapper.selectByExample(example);
    }

    /**
     * 根据原单号查询关联的单据列表(批量)
     * @param linkNumberList
     * @return
     * @throws Exception
     */
    public List<DepotHead> getBillListByLinkNumberList(List<String> linkNumberList)throws Exception {
        if(linkNumberList!=null && linkNumberList.size()>0) {
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andLinkNumberIn(linkNumberList).andSubTypeLike("退货").andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            return depotHeadMapper.selectByExample(example);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 根据原单号查询关联的单据列表
     * @param linkNumber
     * @return
     * @throws Exception
     */
    public List<DepotHead> getBillListByLinkNumber(String linkNumber)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(linkNumber).andSubTypeLike("退货").andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list = depotHeadMapper.selectByExample(example);
        return list.stream()
                .filter(item -> !isDisabledSubType(item.getSubType()))
                .collect(Collectors.toList());
    }

    /**
     * 新增单据主表及单据子表信息
     * @param beanJson
     * @param rows
     * @param request
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addDepotHeadAndDetail(String beanJson, String rows,
                                      HttpServletRequest request) throws Exception {
        /**处理单据主表数据*/
        DepotHead depotHead = JSONObject.parseObject(beanJson, DepotHead.class);
        validateFeatureEnabled(depotHead);
        //判断用户是否已经登录过，登录过不再处理
        User userInfo=userService.getCurrentUser();
        //通过redis去校验重复
        checkExistByRedis(userInfo, depotHead);
        //保存时生成编号，确保编号连续性（不在打开窗口时消耗序列号）
        String prefixNo = getPrefixByTypeAndSubType(depotHead.getType(), depotHead.getSubType());
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String newNumber = year + prefixNo + sequenceService.buildOnlyNumber();
        depotHead.setNumber(newNumber);
        depotHead.setDefaultNumber(newNumber);
        //校验单号是否重复
        if(checkIsBillNumberExist(0L, depotHead.getNumber())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_MSG));
        }
        //校验是否同时录入关联请购单号和关联订单号
        if(StringUtil.isNotEmpty(depotHead.getLinkNumber()) && StringUtil.isNotEmpty(depotHead.getLinkApply())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_MSG));
        }
        String subType = depotHead.getSubType();
        //销售出库强制关联采购入库单校验
        if("出库".equals(depotHead.getType()) && "销售".equals(subType)) {
            if(systemConfigService.getForceLinkPurchaseFlag() && StringUtil.isEmpty(depotHead.getLinkNumber())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_CODE,
                        "系统已开启强制关联入库单，销售出库必须关联采购入库单后才能保存");
            }
        }
        //结算账户校验
        if("采购".equals(subType) || "采购退货".equals(subType) || "销售".equals(subType) || "销售退货".equals(subType)) {
            if (StringUtil.isEmpty(depotHead.getAccountIdList()) && depotHead.getAccountId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG));
            }
        }
        depotHead.setCreator(userInfo==null?null:userInfo.getId());
        depotHead.setCreateTime(new Timestamp(System.currentTimeMillis()));
        if(StringUtil.isEmpty(depotHead.getStatus())) {
            depotHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        }
        depotHead.setPurchaseStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        depotHead.setPayType(depotHead.getPayType()==null?"现付":depotHead.getPayType());
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())){
            depotHead.setAccountIdList(depotHead.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", ""));
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountMoneyList())) {
            //校验多账户的结算金额
            String accountMoneyList = depotHead.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
            BigDecimal sum = StringUtil.getArrSum(accountMoneyList.split(","));
            BigDecimal manyAccountSum = sum.abs();
            if(manyAccountSum.compareTo(depotHead.getChangeAmount().abs())!=0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_MSG));
            }
            depotHead.setAccountMoneyList(accountMoneyList);
        }
        //校验累计扣除订金是否超出订单中的金额
        if(depotHead.getDeposit()!=null && StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            BigDecimal finishDeposit = depotHeadMapperEx.getFinishDepositByNumberExceptCurrent(depotHead.getLinkNumber(), depotHead.getNumber());
            //订单中的订金金额
            BigDecimal changeAmount = getDepotHead(depotHead.getLinkNumber()).getChangeAmount();
            if(changeAmount!=null) {
                BigDecimal preDeposit = changeAmount.abs();
                if(depotHead.getDeposit().add(finishDeposit).compareTo(preDeposit)>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG));
                }
            }
        }
        //校验附件的数量
        if(StringUtil.isNotEmpty(depotHead.getFileName())) {
            String[] fileArr = depotHead.getFileName().split(",");
            if(fileArr.length>4) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_MSG, 4));
            }
        }
        depotHeadMapper.insertSelective(depotHead);
        /**入库和出库处理预付款信息*/
        if(BusinessConstants.PAY_TYPE_PREPAID.equals(depotHead.getPayType())){
            if(depotHead.getOrganId()!=null) {
                BigDecimal currentAdvanceIn = supplierService.getSupplier(depotHead.getOrganId()).getAdvanceIn();
                if(currentAdvanceIn.compareTo(depotHead.getTotalPrice())>=0) {
                    //更新会员的预付款
                    supplierService.updateAdvanceIn(depotHead.getOrganId());
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_MSG));
                }
            }
        }
        //根据单据编号查询单据id
        DepotHeadExample dhExample = new DepotHeadExample();
        dhExample.createCriteria().andNumberEqualTo(depotHead.getNumber()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list = depotHeadMapper.selectByExample(dhExample);
        if(list!=null) {
            Long headId = list.get(0).getId();
            depotHead.setId(headId); //回填ID供后续使用
            /**入库和出库处理单据子表信息*/
            depotItemService.saveDetials(rows,headId, "add",request);
            //根据明细实际关联的源单据，同步校验并修正 link_number
            syncLinkNumberByDetail(headId);
        }
        String statusStr = depotHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(depotHead.getNumber()).append(statusStr).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //采购订单：自动生成采购入库单
        if("采购订单".equals(depotHead.getSubType())) {
            JSONObject billJson = JSONObject.parseObject(beanJson);
            if(billJson.getBooleanValue("autoGeneratePurchaseIn")) {
                autoGeneratePurchaseIn(depotHead, request);
            }
        }
    }

    /**
     * 根据采购订单自动生成采购入库单
     * @param orderHead 采购订单主表（已保存，含id和number）
     */
    private void autoGeneratePurchaseIn(DepotHead orderHead, HttpServletRequest request) throws Exception {
        //获取默认仓库
        Long defaultDepotId = depotService.getDefaultDepotId();
        //生成入库单编号
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String newNumber = year + "CGRK" + sequenceService.buildOnlyNumber();
        //创建入库单主表
        DepotHead purchaseIn = new DepotHead();
        purchaseIn.setType("入库");
        purchaseIn.setSubType("采购");
        purchaseIn.setDefaultNumber(newNumber);
        purchaseIn.setNumber(newNumber);
        purchaseIn.setCreateTime(new Timestamp(System.currentTimeMillis()));
        purchaseIn.setOperTime(orderHead.getOperTime() != null ? orderHead.getOperTime() : new Timestamp(System.currentTimeMillis()));
        purchaseIn.setOrganId(orderHead.getOrganId());
        purchaseIn.setCreator(orderHead.getCreator());
        purchaseIn.setLinkNumber(orderHead.getNumber());
        purchaseIn.setStatus(orderHead.getStatus()); //跟随订单状态
        purchaseIn.setPurchaseStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        purchaseIn.setPayType("现付");
        purchaseIn.setAccountId(orderHead.getAccountId());
        purchaseIn.setDiscount(orderHead.getDiscount());
        purchaseIn.setDiscountMoney(orderHead.getDiscountMoney() != null ? orderHead.getDiscountMoney().abs() : null);
        purchaseIn.setDiscountLastMoney(orderHead.getDiscountLastMoney() != null ? orderHead.getDiscountLastMoney().abs() : null);
        purchaseIn.setOtherMoney(orderHead.getOtherMoney());
        purchaseIn.setChangeAmount(orderHead.getChangeAmount() != null ? orderHead.getChangeAmount().abs() : BigDecimal.ZERO);
        purchaseIn.setTotalPrice(orderHead.getTotalPrice() != null ? orderHead.getTotalPrice().abs() : BigDecimal.ZERO);
        purchaseIn.setTenantId(orderHead.getTenantId());
        purchaseIn.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        depotHeadMapper.insertSelective(purchaseIn);
        //通过编号查回自增ID
        DepotHeadExample piExample = new DepotHeadExample();
        piExample.createCriteria().andNumberEqualTo(newNumber).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> piList = depotHeadMapper.selectByExample(piExample);
        Long purchaseInId = piList.get(0).getId();
        //查询订单明细
        DepotItemExample itemExample = new DepotItemExample();
        itemExample.createCriteria().andHeaderIdEqualTo(orderHead.getId())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotItem> orderItems = depotItemMapper.selectByExample(itemExample);
        //复制明细到入库单（自动填入批号和日期，格式：年份+MMdd+S+HHmmss）
        String todayBatch = new java.text.SimpleDateFormat("yyyyMMdd'S'HHmmss").format(new java.util.Date());
        java.util.Date today = new java.util.Date();
        for(DepotItem orderItem : orderItems) {
            DepotItem inItem = new DepotItem();
            inItem.setHeaderId(purchaseInId);
            inItem.setMaterialId(orderItem.getMaterialId());
            inItem.setMaterialExtendId(orderItem.getMaterialExtendId());
            inItem.setMaterialUnit(orderItem.getMaterialUnit());
            inItem.setOperNumber(orderItem.getOperNumber());
            inItem.setBasicNumber(orderItem.getBasicNumber());
            inItem.setUnitPrice(orderItem.getUnitPrice());
            inItem.setAllPrice(orderItem.getAllPrice());
            inItem.setTaxRate(orderItem.getTaxRate());
            inItem.setTaxMoney(orderItem.getTaxMoney());
            inItem.setTaxLastMoney(orderItem.getTaxLastMoney());
            inItem.setWeight(orderItem.getWeight());
            inItem.setDepotId(defaultDepotId);
            inItem.setBatchNumber(todayBatch);
            inItem.setExpirationDate(today);
            inItem.setLinkId(orderItem.getId()); //关联订单明细
            inItem.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            inItem.setTenantId(orderHead.getTenantId());
            depotItemMapper.insertSelective(inItem);
        }
        //更新入库单的 discount_last_money（从明细汇总）
        syncLinkNumberByDetail(purchaseInId);
        //更新订单状态为"完成采购"，并标记已被关联
        DepotHead orderUpdate = new DepotHead();
        orderUpdate.setId(orderHead.getId());
        orderUpdate.setStatus("2");
        orderUpdate.setPurchaseStatus("2");
        orderUpdate.setLinkedFlag("1");
        depotHeadMapper.updateByPrimaryKeySelective(orderUpdate);
        //使用insertLogWithUserId避免与主单据日志在同一秒内触发防重检测导致用户被踢出
        logService.insertLogWithUserId(orderHead.getCreator(), orderHead.getTenantId(),
                "单据", "自动生成采购入库单" + newNumber, request);
    }

    /**
     * 更新单据主表及单据子表信息
     * @param beanJson
     * @param rows
     * @param request
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateDepotHeadAndDetail(String beanJson, String rows,HttpServletRequest request)throws Exception {
        /**更新单据主表信息*/
        DepotHead depotHead = JSONObject.parseObject(beanJson, DepotHead.class);
        validateFeatureEnabled(depotHead);
        //判断用户是否已经登录过，登录过不再处理
        User userInfo=userService.getCurrentUser();
        //通过redis去校验重复
        checkExistByRedis(userInfo, depotHead);
        //校验单号是否重复
        if(checkIsBillNumberExist(depotHead.getId(), depotHead.getNumber())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_MSG));
        }
        //校验是否同时录入关联请购单号和关联订单号
        if(StringUtil.isNotEmpty(depotHead.getLinkNumber()) && StringUtil.isNotEmpty(depotHead.getLinkApply())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_MSG));
        }
        //校验单据状态，如果不是未审核则提示
        if(!"0".equals(getDepotHead(depotHead.getId()).getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_CANNOT_EDIT_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_BILL_CANNOT_EDIT_MSG));
        }
        //获取之前的会员id
        Long preOrganId = getDepotHead(depotHead.getId()).getOrganId();
        String subType = depotHead.getSubType();
        //结算账户校验
        if("采购".equals(subType) || "采购退货".equals(subType) || "销售".equals(subType) || "销售退货".equals(subType)) {
            if (StringUtil.isEmpty(depotHead.getAccountIdList()) && depotHead.getAccountId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG));
            }
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())){
            depotHead.setAccountIdList(depotHead.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", ""));
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountMoneyList())) {
            //校验多账户的结算金额
            String accountMoneyList = depotHead.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
            BigDecimal sum = StringUtil.getArrSum(accountMoneyList.split(","));
            BigDecimal manyAccountSum = sum.abs();
            if(manyAccountSum.compareTo(depotHead.getChangeAmount().abs())!=0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_MSG));
            }
            depotHead.setAccountMoneyList(accountMoneyList);
        }
        //校验累计扣除订金是否超出订单中的金额
        if(depotHead.getDeposit()!=null && StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            BigDecimal finishDeposit = depotHeadMapperEx.getFinishDepositByNumberExceptCurrent(depotHead.getLinkNumber(), depotHead.getNumber());
            //订单中的订金金额
            BigDecimal changeAmount = getDepotHead(depotHead.getLinkNumber()).getChangeAmount();
            if(changeAmount!=null) {
                BigDecimal preDeposit = changeAmount.abs();
                if(depotHead.getDeposit().add(finishDeposit).compareTo(preDeposit)>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG));
                }
            }
        }
        //校验附件的数量
        if(StringUtil.isNotEmpty(depotHead.getFileName())) {
            String[] fileArr = depotHead.getFileName().split(",");
            if(fileArr.length>4) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_MSG, 4));
            }
        }
        depotHeadMapper.updateByPrimaryKeySelective(depotHead);
        //如果存在多账户结算需要将原账户的id置空
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())) {
            depotHeadMapperEx.setAccountIdToNull(depotHead.getId());
        }
        /**入库和出库处理预付款信息*/
        if(BusinessConstants.PAY_TYPE_PREPAID.equals(depotHead.getPayType())){
            if(depotHead.getOrganId()!=null){
                BigDecimal currentAdvanceIn = supplierService.getSupplier(depotHead.getOrganId()).getAdvanceIn();
                if(currentAdvanceIn.compareTo(depotHead.getTotalPrice())>=0) {
                    //更新会员的预付款
                    supplierService.updateAdvanceIn(depotHead.getOrganId());
                    if(null != preOrganId && !preOrganId.equals(depotHead.getOrganId())) {
                        //更新之前会员的预付款
                        supplierService.updateAdvanceIn(preOrganId);
                    }
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_MSG));
                }
            }
        }
        /**入库和出库处理单据子表信息*/
        depotItemService.saveDetials(rows,depotHead.getId(), "update",request);
        //根据明细实际关联的源单据，同步校验并修正 link_number
        syncLinkNumberByDetail(depotHead.getId());
        //销售出库单编辑后，同步更新关联物流单的重量和运费
        if("出库".equals(depotHead.getType()) && "销售".equals(depotHead.getSubType())) {
            List<Long> ids = new ArrayList<>();
            ids.add(depotHead.getId());
            freightHeadService.recalcByDepotHeadIds(ids);
        }
        String statusStr = depotHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(depotHead.getNumber()).append(statusStr).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    /**
     * 通过redis去校验重复
     * @param userInfo
     * @param depotHead
     */
    private void checkExistByRedis(User userInfo, DepotHead depotHead) {
        String keyNo = userInfo.getLoginName() + "_" + depotHead.getNumber();
        String keyValue = redisService.getCacheObject(keyNo);
        if(StringUtil.isNotEmpty(keyValue)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SUBMIT_REPEAT_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SUBMIT_REPEAT_FAILED_MSG));
        } else {
            redisService.storageKeyWithTime(keyNo, depotHead.getNumber(), 2L);
        }
    }

    public Map<String, Object> getBuyAndSaleStatistics(String today, String monthFirstDay, String yesterdayBegin, String yesterdayEnd,
                                                       String yearBegin, String yearEnd, HttpServletRequest request) throws Exception {
        Long userId = userService.getUserId(request);
        String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        String[] creatorArray = getCreatorArray();
        List<InOutPriceVo> inOutPriceVoList = depotHeadMapperEx.getBuyAndSaleStatisticsList(yearBegin, yearEnd, creatorArray, forceFlag);

        String[] periods = {"today", "month", "yesterday", "year"};
        String[] types = {"Buy", "BuyBack", "Sale", "SaleBack", "RetailSale", "RetailSaleBack"};

        Map<String, BigDecimal> statistics = new HashMap<>();

        // 初始化 statistics Map
        for (String period : periods) {
            for (String type : types) {
                statistics.put(period + type, BigDecimal.ZERO);
            }
        }

        Date todayDate = Tools.strToDate(today);
        Date monthFirstDate = Tools.strToDate(monthFirstDay);
        Date yesterdayStartDate = Tools.strToDate(yesterdayBegin);
        Date yesterdayEndDate = Tools.strToDate(yesterdayEnd);
        Date yearStartDate = Tools.strToDate(yearBegin);
        Date yearEndDate = Tools.strToDate(yearEnd);

        for (InOutPriceVo item : inOutPriceVoList) {
            Date operTime = item.getOperTime();
            BigDecimal discountLastMoney = item.getDiscountLastMoney();
            BigDecimal totalPriceAbs = item.getTotalPrice().abs();

            if (isWithinRange(operTime, todayDate, Tools.strToDate(getNow3()))) {
                updateStatistics(statistics, item, "today", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, monthFirstDate, Tools.strToDate(getNow3()))) {
                updateStatistics(statistics, item, "month", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, yesterdayStartDate, yesterdayEndDate)) {
                updateStatistics(statistics, item, "yesterday", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, yearStartDate, yearEndDate)) {
                updateStatistics(statistics, item, "year", discountLastMoney, totalPriceAbs);
            }
        }

        Map<String, Object> result = new HashMap<>();
        for (String period : periods) {
            result.put(period + "Buy", roleService.parseHomePriceByLimit(statistics.get(period + "Buy").subtract(statistics.get(period + "BuyBack")), "buy", priceLimit, "***", request));
            result.put(period + "Sale", roleService.parseHomePriceByLimit(statistics.get(period + "Sale").subtract(statistics.get(period + "SaleBack")), "sale", priceLimit, "***", request));
            result.put(period + "RetailSale", roleService.parseHomePriceByLimit(statistics.get(period + "RetailSale").subtract(statistics.get(period + "RetailSaleBack")), "retail", priceLimit, "***", request));
        }

        return result;
    }

    private boolean isWithinRange(Date operTime, Date startDate, Date endDate) {
        return operTime.compareTo(startDate) >= 0 && operTime.compareTo(endDate) <= 0;
    }

    private void updateStatistics(Map<String, BigDecimal> statistics, InOutPriceVo item, String period, BigDecimal discountLastMoney, BigDecimal totalPriceAbs) {
        switch (item.getType()) {
            case "入库":
                switch (item.getSubType()) {
                    case "采购":
                        statistics.put(period + "Buy", statistics.get(period + "Buy").add(discountLastMoney));
                        break;
                    case "销售退货":
                        statistics.put(period + "SaleBack", statistics.get(period + "SaleBack").add(discountLastMoney));
                        break;
                    case "零售退货":
                        statistics.put(period + "RetailSaleBack", statistics.get(period + "RetailSaleBack").add(totalPriceAbs));
                        break;
                }
                break;
            case "出库":
                switch (item.getSubType()) {
                    case "采购退货":
                        statistics.put(period + "BuyBack", statistics.get(period + "BuyBack").add(discountLastMoney));
                        break;
                    case "销售":
                        statistics.put(period + "Sale", statistics.get(period + "Sale").add(discountLastMoney));
                        break;
                    case "零售":
                        statistics.put(period + "RetailSale", statistics.get(period + "RetailSale").add(totalPriceAbs));
                        break;
                }
                break;
        }
    }

    private void validateFeatureEnabled(DepotHead depotHead) {
        if (depotHead != null) {
            validateFeatureEnabled(depotHead.getSubType());
        }
    }

    private void validateFeatureEnabled(String subType) {
        if (isDisabledSubType(subType)) {
            throw new BusinessRunTimeException(FEATURE_DISABLED_CODE, String.format("功能已裁剪，禁止继续使用：%s", subType));
        }
    }

    private boolean isDisabledSubType(String subType) {
        return StringUtil.isNotEmpty(subType) && DISABLED_SUB_TYPES.contains(subType);
    }


    public DepotHead getDepotHead(String number)throws Exception {
        DepotHead depotHead = new DepotHead();
        try{
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<DepotHead> list = depotHeadMapper.selectByExample(example);
            if(null!=list && list.size()>0) {
                depotHead = list.get(0);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return depotHead;
    }

    public List<DepotHeadVo4List> debtList(Long organId, String materialParam, String number, String beginTime, String endTime,
                                           String status, Integer offset, Integer rows) {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            String depotIds = depotService.findDepotStrByCurrentUser();
            String [] depotArray=depotIds.split(",");
            String [] creatorArray = getCreatorArray();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> list=depotHeadMapperEx.debtList(organId, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray, offset, rows);
            if (null != list) {
                resList = parseDebtBillList(list);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    public int debtListCount(Long organId, String materialParam, String number, String beginTime, String endTime,
                             String status) {
        int total = 0;
        try {
            String depotIds = depotService.findDepotStrByCurrentUser();
            String[] depotArray = depotIds.split(",");
            String[] creatorArray = getCreatorArray();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            total = depotHeadMapperEx.debtListCount(organId, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return total;
    }

    public void debtExport(Long organId, String materialParam, String number, String type, String subType,
                           String beginTime, String endTime, String status, String mpList,
                           HttpServletRequest request, HttpServletResponse response) {
        try {
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            String billCategory = getBillCategory(subType);
            String depotIds = depotService.findDepotStrByCurrentUser();
            String[] depotArray = depotIds.split(",");
            String[] creatorArray = getCreatorArray();
            status = StringUtil.isNotEmpty(status) ? status : null;
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> dhList = new ArrayList<>();
            List<DepotHeadVo4List> list = depotHeadMapperEx.debtList(organId, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray, null, null);
            if (null != list) {
                dhList = parseDebtBillList(list);
            }
            //生成CSV文件
            String fileName = "单据信息_" + System.currentTimeMillis() + ".csv";
            FileUtils.makedir(fileExportTmp);
            File file = new File(fileExportTmp + fileName);
            OutputStreamWriter csvWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            csvWriter.write("\uFEFF");
            String oneTip = "";
            String sheetOneStr = "";
            if("采购".equals(subType)) {
                oneTip = "供应商对账列表";
                sheetOneStr = "供应商,单据编号,关联单据,商品信息,单据日期,操作员,单据金额,本单欠款,已付欠款,待付欠款,备注";
            } else if("出库".equals(type) && "销售".equals(subType)) {
                oneTip = "客户对账列表";
                sheetOneStr = "客户,单据编号,关联单据,商品信息,单据日期,操作员,单据金额,本单欠款,已收欠款,待收欠款,备注";
            }
            if(StringUtil.isNotEmpty(beginTime) && StringUtil.isNotEmpty(endTime)) {
                oneTip = oneTip + "（" + beginTime + "至" + endTime + "）";
            }
            List<String> sheetOneList = StringUtil.strToStringList(sheetOneStr);
            String[] sheetOneArr = StringUtil.listToStringArray(sheetOneList);
            List<Long> idList = new ArrayList<>();
            List<String[]> billList = new ArrayList<>();
            Map<Long, BillListCacheVo> billListCacheVoMap = new HashMap<>();
            for (DepotHeadVo4List dh : dhList) {
                idList.add(dh.getId());
                BillListCacheVo billListCacheVo = new BillListCacheVo();
                billListCacheVo.setNumber(dh.getNumber());
                billListCacheVo.setOrganName(dh.getOrganName());
                billListCacheVo.setOperTimeStr(getCenternTime(dh.getOperTime()));
                billListCacheVoMap.put(dh.getId(), billListCacheVo);
                String[] objs = new String[sheetOneArr.length];
                objs[0] = dh.getOrganName();
                objs[1] = dh.getNumber();
                objs[2] = dh.getLinkNumber();
                objs[3] = dh.getMaterialsList();
                objs[4] = dh.getOperTimeStr();
                objs[5] = dh.getUserName();
                BigDecimal discountLastMoney = dh.getDiscountLastMoney() == null ? BigDecimal.ZERO : dh.getDiscountLastMoney();
                BigDecimal otherMoney = dh.getOtherMoney() == null ? BigDecimal.ZERO : dh.getOtherMoney();
                BigDecimal deposit = dh.getDeposit() == null ? BigDecimal.ZERO : dh.getDeposit();
                objs[6] = parseDecimalToStr(discountLastMoney.add(otherMoney).subtract(deposit), 2);
                objs[7] = parseDecimalToStr(dh.getNeedDebt(), 2);
                objs[8] = parseDecimalToStr(dh.getFinishDebt(), 2);
                objs[9] = parseDecimalToStr(dh.getDebt(), 2);
                objs[10] = dh.getRemark();
                billList.add(objs);
            }
            CsvUtils.exportCsvManySheet(csvWriter, oneTip, sheetOneArr, billList);
            //导出明细数据
            if(idList.size()>0) {
                List<DepotItemVo4WithInfoEx> dataList = depotItemMapperEx.getBillDetailListByIds(idList);
                String twoTip = "";
                String sheetTwoStr = "";
                if ("采购".equals(subType)) {
                    twoTip = "供应商单据明细";
                    sheetTwoStr = "供应商,单据编号,单据日期,仓库名称,条码,名称,规格,型号,颜色,品牌,制造商," + mpList + ",单位,序列号,批号,有效期,多属性,数量,单价,金额,税率(%),税额,价税合计,重量,备注";
                } else if ("销售".equals(subType)) {
                    twoTip = "客户单据明细";
                    sheetTwoStr = "客户,单据编号,单据日期,仓库名称,条码,名称,规格,型号,颜色,品牌,制造商," + mpList + ",单位,序列号,批号,有效期,多属性,数量,单价,金额,税率(%),税额,价税合计,重量,备注";
                }
                if (StringUtil.isNotEmpty(beginTime) && StringUtil.isNotEmpty(endTime)) {
                    twoTip = twoTip + "（" + beginTime + "至" + endTime + "）";
                }
                List<String> sheetTwoList = StringUtil.strToStringList(sheetTwoStr);
                String[] sheetTwoArr = StringUtil.listToStringArray(sheetTwoList);
                List<String[]> billDetail = new ArrayList<>();
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    String[] objs = new String[sheetTwoArr.length];
                    BillListCacheVo billListCacheVo = billListCacheVoMap.get(diEx.getHeaderId());
                    objs[0] = billListCacheVo != null ? billListCacheVo.getOrganName() : "";
                    objs[1] = billListCacheVo != null ? billListCacheVo.getNumber() : "";
                    objs[2] = billListCacheVo != null ? billListCacheVo.getOperTimeStr() : "";
                    objs[3] = diEx.getDepotId() == null ? "" : diEx.getDepotName();
                    objs[4] = diEx.getBarCode();
                    objs[5] = diEx.getMName();
                    objs[6] = diEx.getMStandard();
                    objs[7] = diEx.getMModel();
                    objs[8] = diEx.getMColor();
                    objs[9] = diEx.getBrand();
                    objs[10] = diEx.getMMfrs();
                    objs[11] = diEx.getMOtherField1();
                    objs[12] = diEx.getMOtherField2();
                    objs[13] = diEx.getMOtherField3();
                    objs[14] = diEx.getMaterialUnit();
                    objs[15] = diEx.getSnList();
                    objs[16] = diEx.getBatchNumber();
                    objs[17] = Tools.parseDateToStr(diEx.getExpirationDate());
                    objs[18] = diEx.getSku();
                    objs[19] = parseDecimalToStr(diEx.getOperNumber(), 2);
                    objs[20] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getUnitPrice(), billCategory, priceLimit, request), 2);
                    objs[21] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getAllPrice(), billCategory, priceLimit, request), 2);
                    objs[22] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxRate(), billCategory, priceLimit, request), 2);
                    objs[23] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxMoney(), billCategory, priceLimit, request), 2);
                    objs[24] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxLastMoney(), billCategory, priceLimit, request), 2);
                    BigDecimal allWeight = diEx.getBasicNumber() == null || diEx.getUnitWeight() == null ? BigDecimal.ZERO : diEx.getBasicNumber().multiply(diEx.getUnitWeight());
                    objs[25] = parseDecimalToStr(allWeight, 2);
                    objs[26] = diEx.getRemark();
                    billDetail.add(objs);
                }
                csvWriter.write("\n"); // 空行分隔两个数据块
                CsvUtils.exportCsvManySheet(csvWriter, twoTip, sheetTwoArr, billDetail);
            }
            csvWriter.flush();
            csvWriter.close();
            CsvUtils.downloadCsv(file, file.getName(), response);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
    }

    public List<DepotHeadVo4List> parseDebtBillList(List<DepotHeadVo4List> list) throws Exception {
        List<Long> idList = new ArrayList<>();
        List<DepotHeadVo4List> dhList = new ArrayList<>();
        for (DepotHeadVo4List dh : list) {
            idList.add(dh.getId());
        }
        //通过批量查询去构造map
        Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
        for (DepotHeadVo4List dh : list) {
            if(dh.getChangeAmount() != null) {
                dh.setChangeAmount(dh.getChangeAmount().abs());
            }
            if(dh.getTotalPrice() != null) {
                dh.setTotalPrice(dh.getTotalPrice().abs());
            }
            if(dh.getDeposit() == null) {
                dh.setDeposit(BigDecimal.ZERO);
            }
            if(dh.getOperTime() != null) {
                dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
            }
            BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
            BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
            BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
            BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount().abs():BigDecimal.ZERO;
            //本单欠款(如果退货则为负数)
            dh.setNeedDebt(discountLastMoney.add(otherMoney).subtract(deposit.add(changeAmount)));
            if(BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(dh.getSubType()) || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(dh.getSubType())) {
                dh.setNeedDebt(BigDecimal.ZERO.subtract(dh.getNeedDebt()));
            }
            BigDecimal needDebt = dh.getNeedDebt()!=null?dh.getNeedDebt():BigDecimal.ZERO;
            BigDecimal finishDebt = accountItemService.getEachAmountByBillId(dh.getId());
            finishDebt = finishDebt!=null?finishDebt:BigDecimal.ZERO;
            //已收欠款
            dh.setFinishDebt(finishDebt);
            //待收欠款
            dh.setDebt(needDebt.subtract(finishDebt));
            //商品信息简述
            if(materialsListMap!=null) {
                dh.setMaterialsList(materialsListMap.get(dh.getId()));
            }
            dhList.add(dh);
        }
        return dhList;
    }

    public String getBillCategory(String subType) {
        if(subType.equals("零售") || subType.equals("零售退货")) {
            return "retail";
        } else if(subType.equals("销售订单") || subType.equals("销售") || subType.equals("销售退货")) {
            return "sale";
        } else {
            return "buy";
        }
    }

    /**
     * 格式化金额样式
     * @param decimal
     * @param num
     * @return
     */
    private String parseDecimalToStr(BigDecimal decimal, Integer num) {
        return decimal == null ? "" : decimal.setScale(num, BigDecimal.ROUND_HALF_UP).toString();
    }

    private String parseStatusToStr(String status, String type) {
        if(StringUtil.isNotEmpty(status)) {
            if("purchase".equals(type)) {
                switch (status) {
                    case "2":
                        return "完成采购";
                    case "3":
                        return "部分采购";
                }
            } else if("sale".equals(type)) {
                switch (status) {
                    case "2":
                        return "完成销售";
                    case "3":
                        return "部分销售";
                }
            }
            switch (status) {
                case "0":
                    return "未审核";
                case "1":
                    return "已审核";
                case "9":
                    return "审核中";
            }
        }
        return "";
    }

    public List<DepotHeadVo4List> waitBillList(String number, String materialParam, String type, String subType,
                                               String beginTime, String endTime, String status, int offset, int rows) {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            String [] depotArray = getDepotArray("其它");
            //给仓管可以看全部的单据（此时可以通过分配仓库去控制权限）
            String [] creatorArray = null;
            String [] subTypeArray = StringUtil.isNotEmpty(subType) ? subType.split(",") : null;
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            Map<Long,String> accountMap = accountService.getAccountMap();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> list = depotHeadMapperEx.waitBillList(type, subTypeArray, creatorArray, statusArray, number, beginTime, endTime,
                    materialParam, depotArray, offset, rows);
            if (null != list) {
                List<Long> idList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                }
                //通过批量查询去构造map
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                for (DepotHeadVo4List dh : list) {
                    if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                        String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                        dh.setAccountName(accountStr);
                    }
                    if(dh.getOperTime() != null) {
                        dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                    }
                    //商品信息简述
                    if(materialsListMap!=null) {
                        dh.setMaterialsList(materialsListMap.get(dh.getId()));
                    }
                    //商品总数量
                    if(materialCountListMap!=null) {
                        dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                    }
                    resList.add(dh);
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    public Long waitBillCount(String number, String materialParam, String type, String subType,
                             String beginTime, String endTime, String status) {
        Long result=null;
        try{
            String [] depotArray = getDepotArray("其它");
            //给仓管可以看全部的单据（此时可以通过分配仓库去控制权限）
            String [] creatorArray = null;
            String [] subTypeArray = StringUtil.isNotEmpty(subType) ? subType.split(",") : null;
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            result=depotHeadMapperEx.waitBillCount(type, subTypeArray, creatorArray, statusArray, number, beginTime, endTime,
                    materialParam, depotArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void batchAddDepotHeadAndDetail(String ids, HttpServletRequest request) throws Exception {
        List<DepotHead> dhList = getDepotHeadListByIds(ids);
        StringBuilder sb = new StringBuilder();
        User userInfo=userService.getCurrentUser();
        for(DepotHead depotHead : dhList) {
            String prefixNo = BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())?"QTRK":"QTCK";
            //关联单据单号
            String oldNumber = depotHead.getNumber();
            //校验单据最新状态不能进行批量操作
            if("0".equals(depotHead.getStatus()) || "2".equals(depotHead.getStatus()) || "9".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_MSG, oldNumber, depotHead.getType()));
            }
            //校验是否是部分入库或者部分出库
            if("3".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_PARTIALLY_STATUS_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_ITEM_EXIST_PARTIALLY_STATUS_FAILED_MSG, oldNumber, depotHead.getType()));
            }
            depotHead.setLinkNumber(oldNumber);
            //给单号重新赋值
            String number = prefixNo + sequenceService.buildOnlyNumber();
            depotHead.setNumber(number);
            depotHead.setDefaultNumber(number);
            depotHead.setOperTime(new Date());
            depotHead.setSubType(BusinessConstants.SUB_TYPE_OTHER);
            depotHead.setChangeAmount(BigDecimal.ZERO);
            depotHead.setTotalPrice(BigDecimal.ZERO);
            depotHead.setDiscountLastMoney(BigDecimal.ZERO);
            depotHead.setCreator(userInfo==null?null:userInfo.getId());
            depotHead.setOrganId(null);
            depotHead.setAccountId(null);
            depotHead.setAccountIdList(null);
            depotHead.setAccountMoneyList(null);
            depotHead.setFileName(null);
            depotHead.setSalesMan(null);
            depotHead.setStatus("0");
            depotHead.setTenantId(null);
            //查询明细
            List<DepotItemVo4WithInfoEx> itemList = depotItemService.getDetailList(depotHead.getId());
            depotHead.setId(null);
            JSONArray rowArr = new JSONArray();
            for(DepotItemVo4WithInfoEx item: itemList) {
                if("1".equals(item.getEnableSerialNumber())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_MSG, oldNumber));
                }
                if("1".equals(item.getEnableBatchNumber())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_MSG, oldNumber));
                }
                item.setUnitPrice(BigDecimal.ZERO);
                item.setAllPrice(BigDecimal.ZERO);
                item.setLinkId(item.getId());
                item.setTenantId(null);
                String itemStr = JSONObject.toJSONString(item);
                JSONObject itemObj = JSONObject.parseObject(itemStr);
                itemObj.put("unit", itemObj.getString("materialUnit"));
                rowArr.add(itemObj.toJSONString());
            }
            String rows = rowArr.toJSONString();
            //新增其它入库单或其它出库单
            sb.append("[").append(depotHead.getNumber()).append("]");
            depotHeadMapper.insertSelective(depotHead);
            //根据单据编号查询单据id
            DepotHeadExample dhExample = new DepotHeadExample();
            dhExample.createCriteria().andNumberEqualTo(depotHead.getNumber()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<DepotHead> list = depotHeadMapper.selectByExample(dhExample);
            if(list!=null) {
                Long headId = list.get(0).getId();
                /**入库和出库处理单据子表信息*/
                depotItemService.saveDetials(rows, headId, "add", request);
            }
        }
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_ADD).append(sb).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }
}
