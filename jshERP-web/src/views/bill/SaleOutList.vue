<!-- create j i s h e n g h u a -->
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
                  <a-form-item label="客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择客户" mode="multiple" showSearch allow-clear optionFilterProp="children" v-model="queryParam.organIdArray" @search="handleSearchCustomer" :maxTagCount="1" :maxTagTextLength="6">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer(0)"><a-icon type="reload" /> 刷新列表</div>
                      </div>
                      <a-select-option v-for="(item,index) in cusList" :key="index" :value="item.id">
                        {{ item.supplier }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="仓库名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择仓库" showSearch allow-clear optionFilterProp="children" v-model="queryParam.depotId">
                      <a-select-option v-for="(depot,index) in depotList" :key="index" :value="depot.id">
                        {{ depot.depotName }}
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
                  <a-form-item label="有无欠款" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择有无欠款" allow-clear v-model="queryParam.hasDebt">
                      <a-select-option value="1">有欠款</a-select-option>
                      <a-select-option value="0">无欠款</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="单据状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择单据状态" allow-clear v-model="queryParam.status">
                      <a-select-option value="0">未审核</a-select-option>
                      <a-select-option value="9" v-if="!checkFlag">审核中</a-select-option>
                      <a-select-option value="1">已审核</a-select-option>
                      <a-select-option value="3">部分出库</a-select-option>
                      <a-select-option value="2">完成出库</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="核准状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="全部" allow-clear v-model="queryParam.priceApproved">
                      <a-select-option value="0">未核准</a-select-option>
                      <a-select-option value="1">已核准</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item label="销售人员" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select placeholder="请选择销售人员" showSearch allow-clear optionFilterProp="children" v-model="queryParam.salesMan">
                      <a-select-option v-for="(item,index) in salesManList" :key="index" :value="item.value">
                        {{ item.text }}
                      </a-select-option>
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
          <a-button v-if="quickBtn.saleBack.indexOf(1)>-1 && btnEnableList.indexOf(1)>-1" icon="share-alt" @click="transferBill('转销售退货', quickBtn.saleBack)">转销售退货</a-button>
          <a-tooltip title="可将状态是部分出库的单据强制完成">
            <a-button v-if="inOutManageFlag && btnEnableList.indexOf(1)>-1" icon="issues-close" @click="batchForceClose">强制结单</a-button>
          </a-tooltip>
          <a-button v-if="checkFlag && btnEnableList.indexOf(2)>-1" icon="check" @click="batchSetStatus(1)">审核</a-button>
          <a-button v-if="checkFlag && btnEnableList.indexOf(7)>-1" icon="stop" @click="batchSetStatus(0)">反审核</a-button>
          <a-button icon="audit" @click="batchSetPriceApproved('1')" style="color:#52c41a">价格核准</a-button>
          <a-button icon="undo" @click="batchSetPriceApproved('0')">取消核准</a-button>
          <a-button v-if="isShowExcel && btnEnableList.indexOf(3)>-1" icon="download" @click="handleExport">导出</a-button>
          <a-button icon="export" @click="handleExportSelectedCsv">导出选中</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
          <a-tooltip placement="left" title="销售出库单可以由销售订单转过来，也可以单独创建。
          销售出库单据中的仓库列表只显示当前用户有权限的仓库。销售出库单可以使用多账户收款。
          勾选单据之后可以进行批量操作（删除、审核、反审核）" slot="action">
            <a-icon v-if="btnEnableList.indexOf(1)>-1" type="question-circle" style="font-size:20px;float:right;" />
          </a-tooltip>
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
              <a @click="myHandleDetail(record, '销售出库', prefixNo)">查看</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleCopyAdd(record)">复制</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => myHandleDelete(record)">
                <a>删除</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderDebt" slot-scope="value, record">
              <a-tooltip title="有收款单">
                <span style="color:green" v-if="value>0 && value>record.lastDebt">{{value}}</span>
              </a-tooltip>
              <a-tooltip title="暂未收款">
                <span style="color:red" v-if="value>0 && value===record.lastDebt">{{value}}</span>
              </a-tooltip>
              <span v-if="value===0">{{value}}</span>
            </template>
            <template slot="customRenderFreight" slot-scope="text">
              <a v-if="text" style="color:#1890ff" @click="handleViewFreight(text)">{{text}}</a>
              <a-tag v-else color="orange">未关联</a-tag>
            </template>
            <template slot="customRenderStatus" slot-scope="text, record">
              <a-tag v-if="record.status == '0'" color="red">未审核</a-tag>
              <a-tag v-if="record.status == '1'" color="green">已审核</a-tag>
              <a-tag v-if="record.status == '2'" color="cyan">完成出库</a-tag>
              <a-tag v-if="record.status == '3'" color="blue">部分出库</a-tag>
              <a-tag v-if="record.status == '9'" color="orange">审核中</a-tag>
              <a-tag v-if="record.priceApproved == '1'" color="green">已核准</a-tag>
              <a-tag v-else color="">未核准</a-tag>
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
        <!-- 汇总统计栏 -->
        <div style="margin-top:10px;padding:8px 16px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
          <span>单据数量：<b>{{ summary.count }}</b></span>
          <a-divider type="vertical" />
          <span>合计吨位：<b>{{ summary.totalWeight }}</b></span>
          <a-divider type="vertical" />
          <span>单据金额：<b style="color:red">{{ summary.totalAmount }}</b></span>
        </div>
        <!-- 表单区域 -->
        <sale-out-modal ref="modalForm" @ok="modalFormOk" @close="modalFormClose"></sale-out-modal>
        <sale-back-modal ref="transferModalForm" @ok="modalFormOk" @close="modalFormClose"></sale-back-modal>
        <bill-detail ref="modalDetail" @ok="modalFormOk" @close="modalFormClose"></bill-detail>
        <bill-excel-iframe ref="billExcelIframe" @ok="modalFormOk" @close="modalFormClose"></bill-excel-iframe>
        <freight-bill-modal ref="freightDetailModal"></freight-bill-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import SaleOutModal from './modules/SaleOutModal'
  import SaleBackModal from './modules/SaleBackModal'
  import BillDetail from './dialog/BillDetail'
  import BillExcelIframe from '@/components/tools/BillExcelIframe'
  import FreightBillModal from '@/views/freight/modules/FreightBillModal'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { BillListMixin } from './mixins/BillListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "SaleOutList",
    mixins:[JeecgListMixin,BillListMixin],
    components: {
      SaleOutModal,
      SaleBackModal,
      BillDetail,
      BillExcelIframe,
      FreightBillModal,
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
        // 查询条件
        queryParam: {
          number: "",
          materialParam: "",
          type: "出库",
          subType: "销售",
          organIdArray: [],
          depotId: undefined,
          creator: undefined,
          linkNumber: "",
          accountId: undefined,
          hasDebt: undefined,
          status: undefined,
          salesMan: undefined,
          remark: "",
          priceApproved: '0'
        },
        prefixNo: 'XSCK',
        urlPath: '/bill/sale_out',
        //出入库管理开关，适合独立仓管场景
        inOutManageFlag: false,
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 默认索引
        defDataIndex:['action','organName','projectName','number','materialsList','operTimeStr','userName','materialCount','totalPrice','totalTaxLastMoney',
          'changeAmount','debt','lastDebt','status'],
        // 默认列
        defColumns: [
          {
            title: '操作',
            dataIndex: 'action',
            align:"center", width: 180,
            scopedSlots: { customRender: 'action' },
          },
          { title: '客户', dataIndex: 'organName',width:120, ellipsis:true},
          { title: '项目名称', dataIndex: 'projectName',width:120, ellipsis:true},
          { title: '单据编号', dataIndex: 'number',width:160,
            customRender:function (text,record,index) {
              text = record.linkNumber?text+"[订]":text
              text = record.hasBackFlag?text+"[退]":text
              return text
            }
          },
          { title: '关联订单', dataIndex: 'linkNumber',width:140},
          { title: '商品信息', dataIndex: 'materialsList',width:220, ellipsis:true},
          { title: '单据日期', dataIndex: 'operTimeStr',width:145},
          { title: '操作员', dataIndex: 'userName',width:80, ellipsis:true},
          { title: '数量', dataIndex: 'materialCount',width:60},
          { title: '金额合计', dataIndex: 'totalPrice',width:80},
          { title: '含税合计', dataIndex: 'totalTaxLastMoney',width:80,
            customRender:function (text,record,index) {
              return (record.discountMoney + record.discountLastMoney).toFixed(2);
            }
          },
          { title: '优惠率', dataIndex: 'discount',width:60,
            customRender:function (text,record,index) {
              return text? text + '%':''
            }
          },
          { title: '收款优惠', dataIndex: 'discountMoney',width:80},
          { title: '其它费用', dataIndex: 'otherMoney',width:80},
          { title: '待收金额', dataIndex: 'needOutMoney',width:80,
            customRender:function (text,record,index) {
              let needOutMoney = record.discountLastMoney + record.otherMoney - record.deposit
              return needOutMoney? needOutMoney.toFixed(2):0
            }
          },
          { title: '结算账户', dataIndex: 'accountName',width:80},
          { title: '扣除订金', dataIndex: 'deposit',width:80},
          { title: '本次收款', dataIndex: 'changeAmount',width:80},
          { title: '本次欠款', dataIndex: 'debt',width:80,
            scopedSlots: { customRender: 'customRenderDebt' }
          },
          { title: '最终欠款', dataIndex: 'lastDebt',width:80},
          { title: '销售人员', dataIndex: 'salesManStr',width:120},
          { title: '备注', dataIndex: 'remark',width:200},
          { title: '物流单号', dataIndex: 'freightBillNo', width: 180,
            scopedSlots: { customRender: 'customRenderFreight' }
          },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 100},
          { title: '状态', dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        summary: {
          count: 0,
          totalWeight: '0.000',
          totalAmount: '0.00'
        },
        url: {
          list: "/depotHead/list",
          delete: "/depotHead/delete",
          deleteBatch: "/depotHead/deleteBatch",
          forceCloseBatch: "/depotHead/forceCloseBatch",
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        }
      }
    },
    computed: {
    },
    watch: {
      dataSource() {
        this.calcSummary()
      },
      selectedRowKeys() {
        this.calcSummary()
      }
    },
    created() {
      this.initSystemConfig()
      this.initCustomer()
      this.initSalesman()
      this.getDepotData()
      this.initUser()
      this.initAccount()
      this.initQuickBtn()
      this.getDepotByCurrentUser()
    },
    methods: {
      getQueryParams() {
        // 覆盖父级方法，将客户多选数组转为逗号分隔字符串传给后端
        let queryParamCopy = Object.assign({}, this.queryParam)
        if (queryParamCopy.organIdArray && queryParamCopy.organIdArray.length > 0) {
          queryParamCopy.organId = queryParamCopy.organIdArray.join(',')
        }
        delete queryParamCopy.organIdArray
        let sqp = {}
        if (this.superQueryParams) {
          sqp['superQueryParams'] = encodeURI(this.superQueryParams)
          sqp['superQueryMatchType'] = this.superQueryMatchType
        }
        let searchObj = {}
        searchObj.search = JSON.stringify(queryParamCopy)
        var param = Object.assign(sqp, searchObj, this.isorter, this.filters)
        param.field = this.getQueryField()
        param.currentPage = this.ipagination.current
        param.pageSize = this.ipagination.pageSize
        return param
      },
      calcSummary() {
        let rows = this.dataSource || []
        if (this.selectedRowKeys && this.selectedRowKeys.length > 0) {
          const keySet = new Set(this.selectedRowKeys.map(String))
          rows = rows.filter(r => keySet.has(String(r.id)))
        }
        let totalWeight = 0, totalAmount = 0
        rows.forEach(row => {
          totalWeight += parseFloat(row.totalWeight || 0)
          totalAmount += parseFloat(row.totalPrice || 0)
        })
        this.summary = {
          count: rows.length,
          totalWeight: totalWeight.toFixed(3),
          totalAmount: Math.abs(totalAmount).toFixed(2)
        }
      },
      handleViewFreight(billNo) {
        // 物流单号可能包含多个（逗号分隔），取第一个
        let no = billNo.split(',')[0].trim()
        this.$refs.freightDetailModal.detailByBillNo(no)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>