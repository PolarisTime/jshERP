<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <!-- 搜索区域 -->
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="单据编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入单据编号" v-model="queryParam.number"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="商品信息" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入条码、名称、助记码、规格、型号等信息" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="单据日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width:100%"
                    v-model="queryParam.createTimeRange"
                    format="YYYY-MM-DD"
                    :placeholder="['开始时间', '结束时间']"
                    @change="onDateChange"
                    @ok="onDateOk"
                  />
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? '收起' : '展开' }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </a-col>
              </span>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item label="供应商" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择供应商" showSearch allow-clear optionFilterProp="children" v-model="queryParam.organId" @search="handleSearchSupplier">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initSupplier(0)"><a-icon type="reload" /> 刷新列表</div>
                      </div>
                      <a-select-option v-for="(item,index) in supList" :key="index" :value="item.id">
                        {{ item.supplier }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="操作员" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择操作员" showSearch allow-clear optionFilterProp="children" v-model="queryParam.creator">
                      <a-select-option v-for="(item,index) in userList" :key="index" :value="item.id">
                        {{ item.userName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="关联请购单" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input placeholder="请输入关联请购单" v-model="queryParam.linkApply"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24" v-if="purchaseBySaleFlag">
                  <a-form-item label="关联订单" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input placeholder="请输入关联订单" v-model="queryParam.linkNumber"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="结算账户" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择结算账户" showSearch allow-clear optionFilterProp="children" v-model="queryParam.accountId">
                      <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                        {{ item.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="单据状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择单据状态" allow-clear v-model="queryParam.status">
                      <a-select-option value="0">未审核</a-select-option>
                      <a-select-option value="9" v-if="!checkFlag">审核中</a-select-option>
                      <a-select-option value="1">已审核</a-select-option>
                      <a-select-option value="3">部分采购</a-select-option>
                      <a-select-option value="2">完成采购</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="单据备注" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input placeholder="请输入单据备注" v-model="queryParam.remark"></a-input>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator"  style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="myHandleAdd" type="primary" icon="plus">新增</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" icon="delete" @click="batchDel">删除</a-button>
          <a-button v-if="quickBtn.purchaseIn.indexOf(1)>-1 && btnEnableList.indexOf(1)>-1" icon="share-alt" @click="transferBill('转采购入库', quickBtn.purchaseIn)">转采购入库</a-button>
          <a-tooltip title="可将状态是部分采购的单据强制完成">
            <a-button v-if="btnEnableList.indexOf(1)>-1" icon="issues-close" @click="batchForceClose">强制结单</a-button>
          </a-tooltip>
          <a-button v-if="checkFlag && btnEnableList.indexOf(2)>-1" icon="check" @click="batchSetStatus(1)">审核</a-button>
          <a-button v-if="checkFlag && btnEnableList.indexOf(7)>-1" icon="stop" @click="batchSetStatus(0)">反审核</a-button>
          <a-button v-if="isShowExcel && btnEnableList.indexOf(3)>-1" icon="download" @click="handleExport">导出</a-button>
          <a-button icon="export" @click="handleExportSelectedCsv">导出选中</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
          <a-tooltip placement="left" title="采购订单不涉及付款金额，采购订单可以转采购入库单，但需要先对采购订单进行审核。
          勾选单据之后可以进行批量操作（删除、审核、反审核）" slot="action">
            <a-icon v-if="btnEnableList.indexOf(1)>-1" type="question-circle" style="font-size:20px;float:right;" />
          </a-tooltip>

          <!-- CLodop -->
          <span style="margin-left:8px;display:flex;align-items:center;gap:6px;">
            <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
            <a-tag v-else color="orange" style="cursor:pointer;" @click="initClodop">CLodop未连接（点击重试）</a-tag>
            <a-select v-if="clodopReady && printTemplateList.length" v-model="selectedTemplateId"
              style="width:160px;" placeholder="选择打印模板">
              <a-select-option v-for="t in printTemplateList" :key="t.id" :value="t.id">{{ t.templateName }}</a-select-option>
            </a-select>
            <a-select v-if="clodopReady && printerList.length" v-model="selectedPrinter"
              style="width:180px;" placeholder="默认打印机">
              <a-select-option value="">默认打印机</a-select-option>
              <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
            </a-select>
            <a-button icon="eye" :disabled="!clodopReady || selectedRowKeys.length !== 1" @click="doPrint(true)">预览</a-button>
            <a-button type="primary" icon="printer" :disabled="!clodopReady || selectedRowKeys.length === 0" @click="doPrint(false)">打印</a-button>
          </span>
                </div>
        <!-- table区域-begin -->
        <div>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :expandedRowKeys="expandedRowKeys"
            @expand="onExpand"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="myHandleDetail(record, '采购订单', prefixNo)">查看</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleCopyAdd(record)">复制</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => myHandleDelete(record)">
                <a>删除</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderStatus" slot-scope="status">
              <a-tag v-if="status == '0'" color="red">未审核</a-tag>
              <a-tag v-if="status == '1'" color="green">已审核</a-tag>
              <a-tag v-if="status == '2'" color="cyan">完成采购</a-tag>
              <a-tag v-if="status == '3'" color="blue">部分采购</a-tag>
              <a-tag v-if="status == '9'" color="orange">审核中</a-tag>
            </template>
            <a-table
              bordered
              size="small"
              slot="expandedRowRender"
              slot-scope="record"
              :loading="record.loading"
              :columns="detailColumns"
              :dataSource="record.childrens"
              :row-key="record => record.id"
              :pagination="false">
            </a-table>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <purchase-order-modal ref="modalForm" @ok="modalFormOk" @close="modalFormClose"></purchase-order-modal>
        <purchase-in-modal ref="transferModalForm" @ok="modalFormOk" @close="modalFormClose"></purchase-in-modal>
        <bill-detail ref="modalDetail" @ok="modalFormOk" @close="modalFormClose"></bill-detail>
        <bill-excel-iframe ref="billExcelIframe" @ok="modalFormOk" @close="modalFormClose"></bill-excel-iframe>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- by  ji  sheng  hua-->
<script>
  import PurchaseOrderModal from './modules/PurchaseOrderModal'
  import PurchaseInModal from './modules/PurchaseInModal'
  import BillDetail from './dialog/BillDetail'
  import BillExcelIframe from '@/components/tools/BillExcelIframe'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { BillListMixin } from './mixins/BillListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
import { ClodopMixin } from '@/mixins/ClodopMixin'
  export default {
    name: "PurchaseOrderList",
    mixins:[ClodopMixin, JeecgListMixin,BillListMixin],
    components: {
      PurchaseOrderModal,
      PurchaseInModal,
      BillDetail,
      BillExcelIframe,
      ColumnSettingPopover,
      JEllipsis,
      JDate,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        clodopBillType: 'purchaseOrder',
        // 查询条件
        queryParam: {
          number: "",
          materialParam: "",
          type: "其它",
          subType: "采购订单",
          organId: undefined,
          depotId: undefined,
          linkApply: "",
          linkNumber: "",
          accountId: undefined,
          creator: undefined,
          status: undefined,
          remark: ""
        },
        prefixNo: 'CGDD',
        urlPath: '/bill/purchase_order',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 默认索引
        defDataIndex:['action','organName','number','materialsList','operTimeStr','userName','materialCount','totalPrice','totalTaxLastMoney',
          'changeAmount','status'],
        // 默认列
        defColumns: [
          {
            title: '操作',
            dataIndex: 'action',
            align:"center", width: 180,
            scopedSlots: { customRender: 'action' },
          },
          { title: '供应商', dataIndex: 'organName',width:120, ellipsis:true},
          { title: '单据编号', dataIndex: 'number',width:160,
            customRender:function (text,record,index) {
              text = record.linkApply?text+"[请]":text
              text = record.linkNumber?text+"[转]":text
              return text
            }
          },
          { title: '关联请购单', dataIndex: 'linkApply',width:140},
          { title: '关联订单', dataIndex: 'linkNumber',width:140},
          { title: '商品信息', dataIndex: 'materialsList',width:220, ellipsis:true},
          { title: '单据日期', dataIndex: 'operTimeStr',width:145},
          { title: '操作员', dataIndex: 'userName',width:80, ellipsis:true},
          { title: '数量', dataIndex: 'materialCount',width:60},
          { title: '金额合计', dataIndex: 'totalPrice',width:80},
          { title: '含税合计', dataIndex: 'totalTaxLastMoney',width:80,
            customRender:function (text,record,index) {
              if(record.discountLastMoney) {
                return (record.discountMoney + record.discountLastMoney).toFixed(2);
              } else {
                return record.totalPrice;
              }
            }
          },
          { title: '优惠率', dataIndex: 'discount',width:60,
            customRender:function (text,record,index) {
              return text? text + '%':''
            }
          },
          { title: '付款优惠', dataIndex: 'discountMoney',width:80},
          { title: '优惠后金额', dataIndex: 'discountLastMoney',width:100},
          { title: '结算账户', dataIndex: 'accountName',width:80},
          { title: '支付订金', dataIndex: 'changeAmount',width:80},
          { title: '备注', dataIndex: 'remark',width:200},
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 100},
          { title: '关联采购入库', dataIndex: 'purchaseInNumbers', width: 180, ellipsis: true},
          { title: '状态', dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        url: {
          list: "/depotHead/list",
          delete: "/depotHead/delete",
          deleteBatch: "/depotHead/deleteBatch",
          forceCloseBatch: "/depotHead/forceCloseBatch",
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        }
      }
    },
    created() {
      this.initSystemConfig()
      this.initSupplier()
      this.initUser()
      this.initAccount()
      this.initQuickBtn()
      this.getDepotByCurrentUser()
    },
    computed: {
    },
    methods: {
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>