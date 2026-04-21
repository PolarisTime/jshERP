package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.datasource.entities.DepotHead;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.vo.DepotHeadSelectQuery;
import com.jsh.erp.datasource.vo.DepotHeadVo4List;
import com.jsh.erp.datasource.vo.DepotHeadWaitBillQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DepotHeadServiceTest {

    @Mock
    private DepotHeadMapperEx depotHeadMapperEx;

    @Mock
    private DepotHeadMapper depotHeadMapper;

    @Mock
    private DepotService depotService;

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @Mock
    private PersonService personService;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private DepotItemService depotItemService;

    @Mock
    private FreightHeadService freightHeadService;

    @Mock
    private LogService logService;

    @Mock
    private RedisService redisService;

    @Mock
    private PriceApprovalService priceApprovalService;

    private DepotHeadService depotHeadService;

    @Before
    public void setUp() throws Exception {
        depotHeadService = spy(new DepotHeadService());
        ReflectionTestUtils.setField(depotHeadService, "depotHeadMapperEx", depotHeadMapperEx);
        ReflectionTestUtils.setField(depotHeadService, "depotHeadMapper", depotHeadMapper);
        ReflectionTestUtils.setField(depotHeadService, "depotService", depotService);
        ReflectionTestUtils.setField(depotHeadService, "accountService", accountService);
        ReflectionTestUtils.setField(depotHeadService, "userService", userService);
        ReflectionTestUtils.setField(depotHeadService, "personService", personService);
        ReflectionTestUtils.setField(depotHeadService, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(depotHeadService, "depotItemService", depotItemService);
        ReflectionTestUtils.setField(depotHeadService, "freightHeadService", freightHeadService);
        ReflectionTestUtils.setField(depotHeadService, "logService", logService);
        ReflectionTestUtils.setField(depotHeadService, "redisService", redisService);
        ReflectionTestUtils.setField(depotHeadService, "priceApprovalService", priceApprovalService);
        doReturn(new HashMap<Long, String>()).when(depotHeadService).findMaterialsListMapByHeaderIdList(anyList());
        doReturn(new HashMap<Long, BigDecimal>()).when(depotHeadService).getMaterialCountListMapByHeaderIdList(anyList());
        when(depotService.findDepotStrByCurrentUser()).thenReturn("1,2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void waitBillListShouldReusePreparedSelectionQuery() throws Exception {
        when(accountService.getAccountMap()).thenReturn(Collections.emptyMap());
        when(depotHeadMapperEx.waitBillList(anyString(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.<String[]>isNull(), org.mockito.ArgumentMatchers.<String[]>any(), anyString(), anyString(), anyString(),
                anyString(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Collections.<DepotHeadVo4List>emptyList());

        DepotHeadWaitBillQuery query = new DepotHeadWaitBillQuery();
        query.setNumber("CGDD001");
        query.setMaterialParam("螺纹钢");
        query.setType("其它");
        query.setSubType("采购订单,销售订单");
        query.setBeginTime("2026-04-01");
        query.setEndTime("2026-04-20");
        query.setStatus("1,3");
        query.setOffset(20);
        query.setRows(10);

        List<DepotHeadVo4List> result = depotHeadService.waitBillList(query);

        assertEquals(0, result.size());
        ArgumentCaptor<String[]> subTypeCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> statusCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> depotCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> beginCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> offsetCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> rowsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(depotHeadMapperEx).waitBillList(org.mockito.ArgumentMatchers.eq("其它"), subTypeCaptor.capture(),
                org.mockito.ArgumentMatchers.<String[]>isNull(), statusCaptor.capture(), org.mockito.ArgumentMatchers.eq("CGDD001"),
                beginCaptor.capture(), endCaptor.capture(), org.mockito.ArgumentMatchers.eq("螺纹钢"),
                depotCaptor.capture(), offsetCaptor.capture(), rowsCaptor.capture());
        assertArrayEquals(new String[]{"采购订单", "销售订单"}, subTypeCaptor.getValue());
        assertArrayEquals(new String[]{"1", "3"}, statusCaptor.getValue());
        assertArrayEquals(new String[]{"1", "2"}, depotCaptor.getValue());
        assertEquals("2026-04-01 00:00:00", beginCaptor.getValue());
        assertEquals("2026-04-20 23:59:59", endCaptor.getValue());
        assertEquals(Integer.valueOf(20), offsetCaptor.getValue());
        assertEquals(Integer.valueOf(10), rowsCaptor.getValue());
    }

    @Test
    public void waitBillCountShouldReusePreparedSelectionQuery() throws Exception {
        when(depotHeadMapperEx.waitBillCount(anyString(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.<String[]>isNull(), org.mockito.ArgumentMatchers.<String[]>any(), anyString(), anyString(), anyString(),
                anyString(), org.mockito.ArgumentMatchers.<String[]>any())).thenReturn(8L);

        DepotHeadWaitBillQuery query = new DepotHeadWaitBillQuery();
        query.setNumber("XSCK001");
        query.setMaterialParam("角钢");
        query.setType("出库");
        query.setSubType("销售");
        query.setBeginTime("2026-04-10");
        query.setEndTime("2026-04-20");
        query.setStatus("1");

        Long total = depotHeadService.waitBillCount(query);

        assertEquals(Long.valueOf(8L), total);
        ArgumentCaptor<String[]> subTypeCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> statusCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> depotCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> beginCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCaptor = ArgumentCaptor.forClass(String.class);
        verify(depotHeadMapperEx).waitBillCount(org.mockito.ArgumentMatchers.eq("出库"), subTypeCaptor.capture(),
                org.mockito.ArgumentMatchers.<String[]>isNull(), statusCaptor.capture(), org.mockito.ArgumentMatchers.eq("XSCK001"),
                beginCaptor.capture(), endCaptor.capture(), org.mockito.ArgumentMatchers.eq("角钢"),
                depotCaptor.capture());
        assertArrayEquals(new String[]{"销售"}, subTypeCaptor.getValue());
        assertArrayEquals(new String[]{"1"}, statusCaptor.getValue());
        assertArrayEquals(new String[]{"1", "2"}, depotCaptor.getValue());
        assertEquals("2026-04-10 00:00:00", beginCaptor.getValue());
        assertEquals("2026-04-20 23:59:59", endCaptor.getValue());
    }

    @Test
    public void selectShouldReusePreparedLinkBillQuery() throws Exception {
        Role role = new Role();
        role.setPriceLimit("");
        when(userService.getUserId(org.mockito.ArgumentMatchers.any())).thenReturn(7L);
        when(userService.getRoleTypeByUserId(7L)).thenReturn(role);
        when(personService.getPersonMap()).thenReturn(Collections.emptyMap());
        when(accountService.getAccountMap()).thenReturn(Collections.emptyMap());
        doReturn(new String[]{"D1", "D2"}).when(depotHeadService).getDepotArray("销售");
        doReturn(new String[]{"C1"}).when(depotHeadService).getCreatorArray();
        doReturn(new String[]{"O1", "O2"}).when(depotHeadService).getOrganArray("销售", null);
        when(depotHeadMapperEx.selectByConditionDepotHead(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.<DepotHeadVo4List>emptyList());

        DepotHeadSelectQuery query = new DepotHeadSelectQuery();
        query.setType("出库");
        query.setSubType("销售");
        query.setHasDebt("1");
        query.setStatus("1,3");
        query.setNumber("XSCK001");
        query.setLinkApply("XSDD001");
        query.setLinkNumber("CGRK001");
        query.setBeginTime("2026-04-10");
        query.setEndTime("2026-04-20");
        query.setMaterialParam("角钢");
        query.setOrganId(12L);
        query.setCreator(21L);
        query.setDepotId(31L);
        query.setAccountId(41L);
        query.setSalesMan("张三");
        query.setRemark("测试");
        query.setLinkedFlag("0");
        query.setSaleLinkFlag("1");
        query.setPriceApproved("0");

        List<DepotHeadVo4List> result = depotHeadService.select(query);

        assertEquals(0, result.size());
        ArgumentCaptor<String[]> creatorCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> statusCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> organCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> depotCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> beginCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCaptor = ArgumentCaptor.forClass(String.class);
        verify(depotHeadMapperEx).selectByConditionDepotHead(org.mockito.ArgumentMatchers.eq("出库"),
                org.mockito.ArgumentMatchers.eq("销售"), creatorCaptor.capture(), org.mockito.ArgumentMatchers.eq("1"),
                statusCaptor.capture(), org.mockito.ArgumentMatchers.<String[]>isNull(),
                org.mockito.ArgumentMatchers.eq("XSCK001"), org.mockito.ArgumentMatchers.eq("XSDD001"),
                org.mockito.ArgumentMatchers.eq("CGRK001"), beginCaptor.capture(), endCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("角钢"), org.mockito.ArgumentMatchers.eq(Long.valueOf(12L)),
                org.mockito.ArgumentMatchers.<String[]>isNull(), organCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Long.valueOf(21L)), org.mockito.ArgumentMatchers.eq(Long.valueOf(31L)),
                depotCaptor.capture(), org.mockito.ArgumentMatchers.eq(Long.valueOf(41L)),
                org.mockito.ArgumentMatchers.eq("张三"), org.mockito.ArgumentMatchers.eq("测试"),
                org.mockito.ArgumentMatchers.eq("0"), org.mockito.ArgumentMatchers.eq("1"),
                org.mockito.ArgumentMatchers.eq("0"));
        assertArrayEquals(new String[]{"C1"}, creatorCaptor.getValue());
        assertArrayEquals(new String[]{"1", "3"}, statusCaptor.getValue());
        assertArrayEquals(new String[]{"O1", "O2"}, organCaptor.getValue());
        assertArrayEquals(new String[]{"D1", "D2"}, depotCaptor.getValue());
        assertEquals("2026-04-10 00:00:00", beginCaptor.getValue());
        assertEquals("2026-04-20 23:59:59", endCaptor.getValue());
    }

    @Test
    public void selectShouldClearCreatorArrayWhenPurchaseStatusPresent() throws Exception {
        Role role = new Role();
        role.setPriceLimit("");
        when(userService.getUserId(org.mockito.ArgumentMatchers.any())).thenReturn(7L);
        when(userService.getRoleTypeByUserId(7L)).thenReturn(role);
        when(personService.getPersonMap()).thenReturn(Collections.emptyMap());
        when(accountService.getAccountMap()).thenReturn(Collections.emptyMap());
        doReturn((String[]) null).when(depotHeadService).getDepotArray("销售订单");
        doReturn(new String[]{"C9"}).when(depotHeadService).getCreatorArray();
        doReturn((String[]) null).when(depotHeadService).getOrganArray("销售订单", "0,3");
        when(depotHeadMapperEx.selectByConditionDepotHead(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.<String[]>any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.<String[]>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.<DepotHeadVo4List>emptyList());

        DepotHeadSelectQuery query = new DepotHeadSelectQuery();
        query.setType("其它");
        query.setSubType("销售订单");
        query.setStatus("1,3");
        query.setPurchaseStatus("0,3");
        query.setNumber("XSDD001");
        query.setBeginTime("2026-04-01");
        query.setEndTime("2026-04-20");
        query.setMaterialParam("槽钢");
        query.setOrganIdList(new String[]{"11", "22"});

        List<DepotHeadVo4List> result = depotHeadService.select(query);

        assertEquals(0, result.size());
        ArgumentCaptor<String[]> statusCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> purchaseStatusCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> organIdListCaptor = ArgumentCaptor.forClass(String[].class);
        verify(depotHeadMapperEx).selectByConditionDepotHead(org.mockito.ArgumentMatchers.eq("其它"),
                org.mockito.ArgumentMatchers.eq("销售订单"), org.mockito.ArgumentMatchers.<String[]>isNull(),
                org.mockito.ArgumentMatchers.isNull(), statusCaptor.capture(), purchaseStatusCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("XSDD001"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("2026-04-01 00:00:00"),
                org.mockito.ArgumentMatchers.eq("2026-04-20 23:59:59"), org.mockito.ArgumentMatchers.eq("槽钢"),
                org.mockito.ArgumentMatchers.isNull(), organIdListCaptor.capture(),
                org.mockito.ArgumentMatchers.<String[]>isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.<String[]>isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull());
        assertArrayEquals(new String[]{"1", "3"}, statusCaptor.getValue());
        assertArrayEquals(new String[]{"0", "3"}, purchaseStatusCaptor.getValue());
        assertArrayEquals(new String[]{"11", "22"}, organIdListCaptor.getValue());
    }

    @Test
    public void batchSetStatusShouldNotAutoApproveWeightForSaleOut() throws Exception {
        DepotHead saleOutHead = new DepotHead();
        saleOutHead.setId(99L);
        saleOutHead.setNumber("XSCK001");
        saleOutHead.setStatus("0");
        saleOutHead.setType("出库");
        saleOutHead.setSubType("销售");
        doReturn(saleOutHead).when(depotHeadService).getDepotHead(99L);
        when(systemConfigService.getForceApprovalFlag()).thenReturn(false);
        when(systemConfigService.getMinusStockFlag()).thenReturn(false);
        when(systemConfigService.getInOutManageFlag()).thenReturn(false);
        when(depotHeadMapper.updateByExampleSelective(any(DepotHead.class), any())).thenReturn(1);

        int result = depotHeadService.batchSetStatus("1", "99");

        assertEquals(1, result);
        verify(freightHeadService).recalcByDepotHeadIds(Collections.singletonList(99L));
        verify(depotHeadMapper, never()).updateByPrimaryKeySelective(any(DepotHead.class));
    }

    @Test
    public void updateDepotHeadAndDetailShouldSyncPriceApprovalAfterSaleOutWeightChange() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setLoginName("tester");
        when(userService.getCurrentUser()).thenReturn(user);
        when(redisService.getCacheObject("tester_XSCK001")).thenReturn(null);
        doReturn(0).when(depotHeadService).checkIsBillNumberExist(99L, "XSCK001");

        DepotHead existing = new DepotHead();
        existing.setId(99L);
        existing.setStatus("0");
        existing.setOrganId(1L);
        doReturn(existing).when(depotHeadService).getDepotHead(99L);
        doNothing().when(depotHeadService).syncLinkNumberByDetail(99L);

        String beanJson = "{\"id\":99,\"number\":\"XSCK001\",\"type\":\"出库\",\"subType\":\"销售\",\"status\":\"0\",\"accountId\":1,\"changeAmount\":0}";

        depotHeadService.updateDepotHeadAndDetail(beanJson, "[]", new MockHttpServletRequest());

        verify(depotItemService).saveDetials(anyString(), org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.eq("update"), any(HttpServletRequest.class));
        verify(freightHeadService).recalcByDepotHeadIds(Collections.singletonList(99L));
        verify(priceApprovalService).syncByDepotHeadAfterWeightChange(99L);
    }
}
