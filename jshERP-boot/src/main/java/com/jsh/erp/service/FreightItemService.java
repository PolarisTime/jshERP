package com.jsh.erp.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.FreightItem;
import com.jsh.erp.datasource.mappers.FreightItemMapper;
import com.jsh.erp.datasource.mappers.FreightItemMapperEx;
import com.jsh.erp.exception.JshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 运费单明细Service
 */
@Service
public class FreightItemService {
    private Logger logger = LoggerFactory.getLogger(FreightItemService.class);

    @Resource
    private FreightItemMapper freightItemMapper;
    @Resource
    private FreightItemMapperEx freightItemMapperEx;

    /**
     * 批量保存运费单明细
     * @param headerId 运费单主表id
     * @param itemsJson 明细JSON数组字符串
     * @param tenantId 租户id
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveItems(Long headerId, String itemsJson, Long tenantId) throws Exception {
        //先物理删除历史软删除记录（防止唯一索引冲突），再软删除当前明细
        freightItemMapperEx.purgeDeletedByHeaderId(headerId);
        freightItemMapperEx.deleteByHeaderId(headerId);
        //解析明细JSON并逐条插入
        JSONArray itemArr = JSONArray.parseArray(itemsJson);
        if (itemArr != null && itemArr.size() > 0) {
            for (int i = 0; i < itemArr.size(); i++) {
                JSONObject itemObj = itemArr.getJSONObject(i);
                FreightItem item = new FreightItem();
                item.setHeaderId(headerId);
                if (itemObj.get("depotHeadId") != null && !"".equals(itemObj.getString("depotHeadId"))) {
                    item.setDepotHeadId(itemObj.getLong("depotHeadId"));
                }
                if (itemObj.get("weight") != null && !"".equals(itemObj.getString("weight"))) {
                    item.setWeight(itemObj.getBigDecimal("weight"));
                } else {
                    //如果未传入重量，则自动计算
                    if (item.getDepotHeadId() != null) {
                        BigDecimal calcWeight = freightItemMapperEx.calcDepotHeadWeight(item.getDepotHeadId());
                        item.setWeight(calcWeight);
                    }
                }
                if (itemObj.get("remark") != null) {
                    item.setRemark(itemObj.getString("remark"));
                }
                item.setDeleteFlag("0");
                try {
                    freightItemMapper.insertSelective(item);
                } catch (Exception e) {
                    JshException.writeFail(logger, e);
                }
            }
        }
    }
}
