package com.jsh.erp.controller;

import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.vo.DepotHeadSelectQuery;
import com.jsh.erp.datasource.vo.DepotHeadWaitBillQuery;
import com.jsh.erp.service.DepotHeadService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DepotHeadControllerTest {

    @Mock
    private DepotHeadService depotHeadService;

    private DepotHeadController depotHeadController;

    @Before
    public void setUp() {
        depotHeadController = new DepotHeadController();
        ReflectionTestUtils.setField(depotHeadController, "depotHeadService", depotHeadService);
    }

    @Test
    public void getListShouldBuildSelectionQueryFromSearchWithMultiOrganIds() throws Exception {
        when(depotHeadService.select(any(DepotHeadSelectQuery.class))).thenReturn(Collections.emptyList());

        String search = "{\"type\":\"入库\",\"subType\":\"采购\",\"hasDebt\":\"1\",\"status\":\"0,1\",\"purchaseStatus\":\"0,3\",\"number\":\"CGRK001\",\"linkApply\":\"QGD001\",\"linkNumber\":\"CGDD001\",\"beginTime\":\"2026-04-01\",\"endTime\":\"2026-04-20\",\"materialParam\":\"螺纹钢\",\"organId\":\"11,22\",\"creator\":\"8\",\"depotId\":\"9\",\"accountId\":\"10\",\"salesMan\":\"张三\",\"remark\":\"急单\",\"linkedFlag\":\"0\",\"saleLinkFlag\":\"1\",\"priceApproved\":\"0\"}";
        TableDataInfo result = depotHeadController.getList(search, null);

        ArgumentCaptor<DepotHeadSelectQuery> captor = ArgumentCaptor.forClass(DepotHeadSelectQuery.class);
        verify(depotHeadService).select(captor.capture());
        DepotHeadSelectQuery query = captor.getValue();
        assertEquals("入库", query.getType());
        assertEquals("采购", query.getSubType());
        assertEquals("1", query.getHasDebt());
        assertEquals("0,1", query.getStatus());
        assertEquals("0,3", query.getPurchaseStatus());
        assertEquals("CGRK001", query.getNumber());
        assertEquals("QGD001", query.getLinkApply());
        assertEquals("CGDD001", query.getLinkNumber());
        assertEquals("2026-04-01", query.getBeginTime());
        assertEquals("2026-04-20", query.getEndTime());
        assertEquals("螺纹钢", query.getMaterialParam());
        assertNull(query.getOrganId());
        assertArrayEquals(new String[]{"11", "22"}, query.getOrganIdList());
        assertEquals(Long.valueOf(8L), query.getCreator());
        assertEquals(Long.valueOf(9L), query.getDepotId());
        assertEquals(Long.valueOf(10L), query.getAccountId());
        assertEquals("张三", query.getSalesMan());
        assertEquals("急单", query.getRemark());
        assertEquals("0", query.getLinkedFlag());
        assertEquals("1", query.getSaleLinkFlag());
        assertEquals("0", query.getPriceApproved());
        assertEquals(200, result.getCode());
    }

    @Test
    public void getListShouldBuildSelectionQueryFromSearchWithSingleOrganId() throws Exception {
        when(depotHeadService.select(any(DepotHeadSelectQuery.class))).thenReturn(Collections.emptyList());

        String search = "{\"type\":\"其它\",\"subType\":\"销售订单\",\"organId\":\"15\"}";
        depotHeadController.getList(search, null);

        ArgumentCaptor<DepotHeadSelectQuery> captor = ArgumentCaptor.forClass(DepotHeadSelectQuery.class);
        verify(depotHeadService).select(captor.capture());
        DepotHeadSelectQuery query = captor.getValue();
        assertEquals("其它", query.getType());
        assertEquals("销售订单", query.getSubType());
        assertEquals(Long.valueOf(15L), query.getOrganId());
        assertNull(query.getOrganIdList());
    }

    @Test
    public void waitBillListShouldBuildCommonQueryFromSearch() throws Exception {
        when(depotHeadService.waitBillList(any(DepotHeadWaitBillQuery.class))).thenReturn(Collections.emptyList());
        when(depotHeadService.waitBillCount(any(DepotHeadWaitBillQuery.class))).thenReturn(0L);

        String search = "{\"number\":\"CGDD001\",\"materialParam\":\"螺纹钢\",\"type\":\"其它\",\"subType\":\"采购订单\",\"beginTime\":\"2026-04-01\",\"endTime\":\"2026-04-20\",\"status\":\"1,3\"}";
        String result = depotHeadController.waitBillList(search, 2, 20, null);

        ArgumentCaptor<DepotHeadWaitBillQuery> listCaptor = ArgumentCaptor.forClass(DepotHeadWaitBillQuery.class);
        verify(depotHeadService).waitBillList(listCaptor.capture());
        DepotHeadWaitBillQuery listQuery = listCaptor.getValue();
        assertEquals("CGDD001", listQuery.getNumber());
        assertEquals("螺纹钢", listQuery.getMaterialParam());
        assertEquals("其它", listQuery.getType());
        assertEquals("采购订单", listQuery.getSubType());
        assertEquals("2026-04-01", listQuery.getBeginTime());
        assertEquals("2026-04-20", listQuery.getEndTime());
        assertEquals("1,3", listQuery.getStatus());
        assertEquals(Integer.valueOf(20), listQuery.getOffset());
        assertEquals(Integer.valueOf(20), listQuery.getRows());

        ArgumentCaptor<DepotHeadWaitBillQuery> countCaptor = ArgumentCaptor.forClass(DepotHeadWaitBillQuery.class);
        verify(depotHeadService).waitBillCount(countCaptor.capture());
        DepotHeadWaitBillQuery countQuery = countCaptor.getValue();
        assertEquals(Integer.valueOf(20), countQuery.getOffset());
        assertEquals(Integer.valueOf(20), countQuery.getRows());
        assertTrue(result.contains("\"total\":0"));
    }

    @Test
    public void waitBillCountShouldBuildCommonQueryWithoutPagination() throws Exception {
        when(depotHeadService.waitBillCount(any(DepotHeadWaitBillQuery.class))).thenReturn(5L);

        String search = "{\"number\":\"XSCK001\",\"materialParam\":\"角钢\",\"type\":\"出库\",\"subType\":\"销售\",\"beginTime\":\"2026-04-10\",\"endTime\":\"2026-04-20\",\"status\":\"1\"}";
        String result = depotHeadController.waitBillCount(search, null);

        ArgumentCaptor<DepotHeadWaitBillQuery> captor = ArgumentCaptor.forClass(DepotHeadWaitBillQuery.class);
        verify(depotHeadService).waitBillCount(captor.capture());
        DepotHeadWaitBillQuery query = captor.getValue();
        assertEquals("XSCK001", query.getNumber());
        assertEquals("角钢", query.getMaterialParam());
        assertEquals("出库", query.getType());
        assertEquals("销售", query.getSubType());
        assertEquals("2026-04-10", query.getBeginTime());
        assertEquals("2026-04-20", query.getEndTime());
        assertEquals("1", query.getStatus());
        assertNull(query.getOffset());
        assertNull(query.getRows());
        assertTrue(result.contains("\"total\":5"));
    }
}
