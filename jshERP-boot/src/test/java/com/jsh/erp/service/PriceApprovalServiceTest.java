package com.jsh.erp.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.datasource.entities.DepotHead;
import com.jsh.erp.datasource.entities.DepotItem;
import com.jsh.erp.datasource.entities.PriceApproval;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapper;
import com.jsh.erp.datasource.mappers.PriceApprovalMapper;
import com.jsh.erp.datasource.vo.PriceApprovalItemVo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PriceApprovalServiceTest {

    @Mock
    private PriceApprovalMapper priceApprovalMapper;

    @Mock
    private DepotHeadMapper depotHeadMapper;

    @Mock
    private DepotItemMapper depotItemMapper;

    @Mock
    private UserService userService;

    private PriceApprovalService priceApprovalService;

    @Before
    public void setUp() {
        priceApprovalService = new PriceApprovalService();
        ReflectionTestUtils.setField(priceApprovalService, "priceApprovalMapper", priceApprovalMapper);
        ReflectionTestUtils.setField(priceApprovalService, "depotHeadMapper", depotHeadMapper);
        ReflectionTestUtils.setField(priceApprovalService, "depotItemMapper", depotItemMapper);
        ReflectionTestUtils.setField(priceApprovalService, "userService", userService);
    }

    @Test
    public void saveApprovalItemsShouldAllowAdjustedGroupWeight() throws Exception {
        PriceApproval approval = new PriceApproval();
        approval.setId(1L);
        approval.setStatus("0");
        when(priceApprovalMapper.selectByPrimaryKey(1L)).thenReturn(approval);
        when(userService.getCurrentUser()).thenReturn(buildUser(7L));

        JSONArray items = new JSONArray();
        items.add(buildSaveItem(11L, "螺纹钢", "HRB400", "0.700", "5.20", "3.64", "13", "0.42", "4.06", "首行"));
        items.add(buildSaveItem(11L, "螺纹钢", "HRB400", "0.600", "5.20", "3.12", "13", "0.41", "3.53", "次行"));

        priceApprovalService.saveApprovalItems(1L, "2026-04-20", "头部备注", items.toJSONString());

        verify(priceApprovalMapper).deleteItemsByApprovalId(1L);
        ArgumentCaptor<List> itemListCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceApprovalMapper).batchInsertItems(itemListCaptor.capture());
        assertEquals(2, itemListCaptor.getValue().size());
        assertEquals(0, approval.getTotalWeight().compareTo(new BigDecimal("1.300")));
        assertEquals(0, approval.getTotalAmount().compareTo(new BigDecimal("6.76")));
        assertEquals("头部备注", approval.getRemark());
        assertNotNull(approval.getDeliveryDate());
        assertEquals(Long.valueOf(7L), approval.getUpdater());
        verify(priceApprovalMapper).updateByPrimaryKey(approval);
    }

    @Test
    public void confirmApprovalShouldBackwriteWeightPriceAndRemarks() throws Exception {
        PriceApproval approval = new PriceApproval();
        approval.setId(1L);
        approval.setDepotHeadId(99L);
        approval.setStatus("0");
        approval.setRemark("整单备注");
        when(priceApprovalMapper.selectByPrimaryKey(1L)).thenReturn(approval);
        when(userService.getCurrentUser()).thenReturn(buildUser(7L));
        when(priceApprovalMapper.getApprovalItems(1L)).thenReturn(Arrays.asList(
                buildApprovalItem(100L, "1.200", "5.00", "6.00", "0.78", "6.78", "行备注1"),
                buildApprovalItem(100L, "0.300", "5.00", "1.50", "0.20", "1.70", "行备注2"),
                buildApprovalItem(101L, "2.000", "10.00", "20.00", "2.60", "22.60", "行备注3")
        ));

        priceApprovalService.confirmApproval(1L);

        assertEquals("1", approval.getStatus());
        assertEquals(0, approval.getTotalAmount().compareTo(new BigDecimal("27.50")));
        assertEquals(Long.valueOf(7L), approval.getUpdater());

        ArgumentCaptor<DepotHead> headCaptor = ArgumentCaptor.forClass(DepotHead.class);
        verify(depotHeadMapper).updateByPrimaryKeySelective(headCaptor.capture());
        DepotHead head = headCaptor.getValue();
        assertEquals(Long.valueOf(99L), head.getId());
        assertEquals("1", head.getPriceApproved());
        assertEquals("1", head.getWeightApproved());
        assertEquals("整单备注", head.getRemark());
        assertEquals(0, head.getTotalPrice().compareTo(new BigDecimal("27.50")));
        assertEquals(0, head.getDiscountLastMoney().compareTo(new BigDecimal("31.08")));

        ArgumentCaptor<DepotItem> itemCaptor = ArgumentCaptor.forClass(DepotItem.class);
        verify(depotItemMapper, times(2)).updateByPrimaryKeySelective(itemCaptor.capture());
        DepotItem first = findDepotItem(itemCaptor.getAllValues(), 100L);
        DepotItem second = findDepotItem(itemCaptor.getAllValues(), 101L);
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(0, first.getWeight().compareTo(new BigDecimal("1.500")));
        assertEquals(0, first.getUnitPrice().compareTo(new BigDecimal("5.000000")));
        assertEquals(0, first.getAllPrice().compareTo(new BigDecimal("7.50")));
        assertEquals(0, first.getTaxMoney().compareTo(new BigDecimal("0.98")));
        assertEquals(0, first.getTaxLastMoney().compareTo(new BigDecimal("8.48")));
        assertEquals("行备注1；行备注2", first.getRemark());
        assertEquals(0, second.getWeight().compareTo(new BigDecimal("2.000")));
        assertEquals(0, second.getUnitPrice().compareTo(new BigDecimal("10.000000")));
        assertEquals("行备注3", second.getRemark());
    }

    @Test
    public void cancelApprovalShouldResetHeadFlags() throws Exception {
        PriceApproval approval = new PriceApproval();
        approval.setId(1L);
        approval.setDepotHeadId(99L);
        approval.setStatus("1");
        when(priceApprovalMapper.selectByPrimaryKey(1L)).thenReturn(approval);
        when(priceApprovalMapper.countStatementItemsByApprovalId(1L)).thenReturn(0);
        when(priceApprovalMapper.getApprovalItems(1L)).thenReturn(Arrays.asList(
                buildApprovalItem(100L, "1.200", "5.00", "6.00", "0.78", "6.78", "行备注1"),
                buildApprovalItem(100L, "0.300", "5.00", "1.50", "0.20", "1.70", "行备注2"),
                buildApprovalItem(101L, "2.000", "10.00", "20.00", "2.60", "22.60", "行备注3")
        ));
        when(userService.getCurrentUser()).thenReturn(buildUser(8L));

        priceApprovalService.cancelApproval(1L);

        assertEquals("0", approval.getStatus());
        assertEquals(Long.valueOf(8L), approval.getUpdater());

        ArgumentCaptor<DepotHead> headCaptor = ArgumentCaptor.forClass(DepotHead.class);
        verify(depotHeadMapper).updateByPrimaryKeySelective(headCaptor.capture());
        DepotHead head = headCaptor.getValue();
        assertEquals(Long.valueOf(99L), head.getId());
        assertEquals("0", head.getPriceApproved());
        assertEquals("0", head.getWeightApproved());
        assertEquals("", head.getRemark());
        assertEquals(0, head.getTotalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, head.getDiscountLastMoney().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<DepotItem> itemCaptor = ArgumentCaptor.forClass(DepotItem.class);
        verify(depotItemMapper, times(2)).updateByPrimaryKeySelective(itemCaptor.capture());
        for (DepotItem item : itemCaptor.getAllValues()) {
            assertEquals(0, item.getUnitPrice().compareTo(BigDecimal.ZERO));
            assertEquals(0, item.getAllPrice().compareTo(BigDecimal.ZERO));
            assertEquals(0, item.getTaxMoney().compareTo(BigDecimal.ZERO));
            assertEquals(0, item.getTaxLastMoney().compareTo(BigDecimal.ZERO));
            assertEquals("", item.getRemark());
        }
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setTenantId(1L);
        return user;
    }

    private JSONObject buildSaveItem(Long depotItemId, String name, String standard, String weight,
                                     String unitPrice, String allPrice, String taxRate,
                                     String taxMoney, String taxLastMoney, String remark) {
        JSONObject item = new JSONObject();
        item.put("depotItemId", depotItemId);
        item.put("materialId", 1L);
        item.put("materialExtendId", 2L);
        item.put("barCode", "BC001");
        item.put("name", name);
        item.put("standard", standard);
        item.put("model", "M1");
        item.put("color", "黑");
        item.put("brand", "品牌A");
        item.put("operNumber", new BigDecimal("3"));
        item.put("weight", new BigDecimal(weight));
        item.put("unitPrice", new BigDecimal(unitPrice));
        item.put("allPrice", new BigDecimal(allPrice));
        item.put("taxRate", new BigDecimal(taxRate));
        item.put("taxMoney", new BigDecimal(taxMoney));
        item.put("taxLastMoney", new BigDecimal(taxLastMoney));
        item.put("remark", remark);
        item.put("originalWeight", new BigDecimal("1.000"));
        return item;
    }

    private PriceApprovalItemVo buildApprovalItem(Long depotItemId, String weight, String unitPrice,
                                                  String allPrice, String taxMoney,
                                                  String taxLastMoney, String remark) {
        PriceApprovalItemVo item = new PriceApprovalItemVo();
        item.setDepotItemId(depotItemId);
        item.setName("商品");
        item.setStandard("规格");
        item.setWeight(new BigDecimal(weight));
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setAllPrice(new BigDecimal(allPrice));
        item.setTaxMoney(new BigDecimal(taxMoney));
        item.setTaxLastMoney(new BigDecimal(taxLastMoney));
        item.setRemark(remark);
        item.setTaxRate(new BigDecimal("13"));
        item.setOriginalWeight(new BigDecimal(weight));
        return item;
    }

    private DepotItem findDepotItem(List<DepotItem> items, Long id) {
        for (DepotItem item : items) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }
}
