<!-- create j i s h e n g h u a -->
<template>
  <a-row :gutter="24" class="sale-out-list-page">
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
                  <a-input placeholder="请输入条码、名称、助记码、规格、材质、总重量等信息" v-model="queryParam.materialParam"></a-input>
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
              <a-col :md="6" :sm="24">
                <a-form-item label="关联订单" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入关联订单" v-model="queryParam.linkNumber"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择客户" showSearch allow-clear optionFilterProp="children"
                    v-model="selectedCustomerName" @change="onCustomerNameChange" @search="handleSearchCustomer">
                    <div slot="dropdownRender" slot-scope="menu">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0;" />
                      <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer(0)"><a-icon type="reload" /> 刷新列表</div>
                    </div>
                    <a-select-option v-for="(name,index) in uniqueCustomerNames" :key="index" :value="name">
                      {{ name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="项目名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="全部" showSearch allow-clear optionFilterProp="children"
                    v-model="queryParam.organId" @search="handleSearchProject" @change="onProjectChange">
                    <a-select-option v-for="(item,index) in availableProjectList" :key="index" :value="item.id">
                      {{ item.projectName || item.supplier }}
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
                    <a-select-option value="3">部分出库</a-select-option>
                    <a-select-option value="2">完成出库</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="单据备注" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入单据备注" v-model="queryParam.remark"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="24" :sm="24">
                <div class="sale-out-search-actions table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                  <div class="sale-out-list-summary">
                    <span>单据数量：<b>{{ summary.count }}</b></span>
                    <a-divider type="vertical" />
                    <span>合计吨位：<b>{{ summary.totalWeight }}</b></span>
                    <a-divider type="vertical" />
                    <span>单据金额：<b style="color:red">{{ summary.totalAmount }}</b></span>
                    <template v-if="summary.contractInfo">
                      <a-divider type="vertical" />
                      <template v-if="!summary.contractInfo.hasContract">
                        <span style="color:red">金额超限：{{ Math.abs(summary.contractInfo.remainAmount) }} 元&nbsp;</span>
                        <span style="color:red">吨位超限：{{ Math.abs(summary.contractInfo.remainTonnage) }} 吨&nbsp;</span>
                        <span style="color:#333">未签署合同</span>
                      </template>
                      <template v-else-if="summary.contractInfo.remainAmount < 0 || summary.contractInfo.remainTonnage < 0">
                        <span v-if="summary.contractInfo.remainAmount < 0" style="color:red">金额超限：{{ Math.abs(summary.contractInfo.remainAmount) }} 元&nbsp;</span>
                        <span v-else>剩余金额：<b style="color:#1890ff">{{ summary.contractInfo.remainAmount }}</b> 元&nbsp;</span>
                        <span v-if="summary.contractInfo.remainTonnage < 0" style="color:red">吨位超限：{{ Math.abs(summary.contractInfo.remainTonnage) }} 吨&nbsp;</span>
                        <span v-else>剩余吨位：<b style="color:#1890ff">{{ summary.contractInfo.remainTonnage }}</b> 吨&nbsp;</span>
                        <a-tag color="orange" style="vertical-align:middle">已签署合同</a-tag>
                      </template>
                      <template v-else>
                        剩余金额：<b style="color:#1890ff">{{ summary.contractInfo.remainAmount }}</b> 元&nbsp;
                        剩余吨位：<b style="color:#1890ff">{{ summary.contractInfo.remainTonnage }}</b> 吨&nbsp;
                        <a-tag color="green" style="vertical-align:middle">已签署合同</a-tag>
                      </template>
                    </template>
                  </div>
                </div>
              </a-col>
            </a-row>
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
          <a-button v-if="isShowExcel && btnEnableList.indexOf(3)>-1" icon="download" @click="handleExport">导出</a-button>
          <a-button icon="export" @click="handleExportSelectedCsv">导出选中</a-button>
          <a-button icon="printer" @click="handleClodopPrint">CLodop打印</a-button>
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
            :rowClassName="billRowClassName"
            @expand="onExpand"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="myHandleDetail(record, '销售出库', prefixNo)">查看</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => myHandleDelete(record)">
                <a>删除</a>
              </a-popconfirm>
              <a-divider type="vertical" />
              <a @click="showAttach(record)" style="white-space:nowrap">
                <a-icon type="paper-clip" /> 附件
                <a-badge v-if="record.fileName" :count="record.fileName.split(',').filter(f=>f).length" :numberStyle="{fontSize:'10px',minWidth:'16px',height:'16px',lineHeight:'16px'}" />
                <a-icon v-else type="close-circle" style="color:#ccc;font-size:12px" />
              </a>
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
        <sale-out-modal ref="modalForm" @ok="modalFormOk" @close="modalFormClose"></sale-out-modal>
        <sale-back-modal ref="transferModalForm" @ok="modalFormOk" @close="modalFormClose"></sale-back-modal>
        <bill-detail ref="modalDetail" @ok="modalFormOk" @close="modalFormClose"></bill-detail>
        <bill-excel-iframe ref="billExcelIframe" @ok="modalFormOk" @close="modalFormClose"></bill-excel-iframe>
        <freight-bill-modal ref="freightDetailModal"></freight-bill-modal>
        <attachment-modal ref="attachModal" bizPath="bill" @change="onAttachChange"></attachment-modal>
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
  import { getContractBalance } from '@/api/api'
  import AttachmentModal from '@/components/tools/AttachmentModal'
  import { putAction } from '@/api/manage'
  import { getFormatDate, getPrevMonthFormatDate } from '@/utils/util'
  import moment from 'moment'
  export default {
    name: "SaleOutList",
    mixins:[JeecgListMixin,BillListMixin],
    components: {
      SaleOutModal,
      SaleBackModal,
      BillDetail,
      BillExcelIframe,
      FreightBillModal,
      AttachmentModal,
      ColumnSettingPopover,
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
          organId: undefined,
          linkNumber: "",
          status: undefined,
          remark: ""
        },
        selectedCustomerName: undefined,
        projectListForOrgan: [],
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
        defDataIndex:['action','organName','projectName','number','linkNumber','materialsList','operTimeStr','userName','materialCount','totalPrice','totalTaxLastMoney',
          'changeAmount','debt','lastDebt','freightBillNo','totalWeight','status'],
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
          totalAmount: '0.00',
          contractInfo: null
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
      uniqueCustomerNames() {
        let seen = new Set()
        return this.cusList.map(c => c.supplier).filter(name => {
          if (!name || seen.has(name)) return false
          seen.add(name)
          return true
        })
      },
      availableProjectList() {
        let source = this.projectListForOrgan && this.projectListForOrgan.length ? this.projectListForOrgan : this.cusList
        let result = []
        let seen = new Set()
        source.forEach(item => {
          if (!item || !item.id || seen.has(String(item.id))) return
          seen.add(String(item.id))
          result.push(item)
        })
        return result
      }
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
    activated() {
      // keep-alive 组件重新激活时，刷新 endTime 到当天，避免跨日缓存导致新单据查不到
      let today = getFormatDate()
      if (this.queryParam.endTime && this.queryParam.endTime < today) {
        this.queryParam.endTime = today
        this.queryParam.createTimeRange = [
          moment(this.queryParam.beginTime || getPrevMonthFormatDate(3)),
          moment(today)
        ]
        this.loadData(1)
      }
    },
    methods: {
      // 保存成功后刷新列表：确保 endTime 包含当天，且回到第1页
      modalFormOk() {
        this._ensureEndDateCurrent()
        this.loadData(1)
      },
      modalFormClose() {
        this._ensureEndDateCurrent()
        this.loadData(1)
      },
      _ensureEndDateCurrent() {
        let today = getFormatDate()
        if (!this.queryParam.endTime || this.queryParam.endTime < today) {
          this.queryParam.endTime = today
          this.queryParam.createTimeRange = [
            moment(this.queryParam.beginTime || getPrevMonthFormatDate(3)),
            moment(today)
          ]
        }
      },
      searchReset() {
        this.selectedCustomerName = undefined
        this.projectListForOrgan = []
        this.queryParam = {
          type: '出库',
          subType: '销售',
          beginTime: getPrevMonthFormatDate(3),
          endTime: getFormatDate(),
          createTimeRange: [moment(getPrevMonthFormatDate(3)), moment(getFormatDate())]
        }
        this.loadData(1)
      },
      onCustomerNameChange(supplierName) {
        this.queryParam.organId = undefined
        if (supplierName) {
          this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
          // 只有一个项目时自动选中并刷新
          if (this.projectListForOrgan.length === 1) {
            this.queryParam.organId = this.projectListForOrgan[0].id
            this.searchQuery()
          }
        } else {
          this.projectListForOrgan = []
          this.searchQuery()
        }
      },
      handleSearchProject(value) {
        this.handleSearchCustomer(value)
      },
      onProjectChange(value) {
        if (value) {
          let selectedProject = this.cusList.find(item => String(item.id) === String(value))
          if (selectedProject) {
            this.selectedCustomerName = selectedProject.supplier
            this.projectListForOrgan = this.cusList.filter(c => c.supplier === selectedProject.supplier)
          }
        } else if (this.selectedCustomerName) {
          this.projectListForOrgan = this.cusList.filter(c => c.supplier === this.selectedCustomerName)
        } else {
          this.projectListForOrgan = []
        }
        this.searchQuery()
      },
      calcSummary() {
        let rows = this.dataSource || []
        let selectedRows = []
        if (this.selectedRowKeys && this.selectedRowKeys.length > 0) {
          const keySet = new Set(this.selectedRowKeys.map(String))
          selectedRows = rows.filter(r => keySet.has(String(r.id)))
          rows = selectedRows
        }
        let totalWeight = 0, totalAmount = 0
        rows.forEach(row => {
          totalWeight += parseFloat(row.totalWeight || 0)
          totalAmount += parseFloat(row.totalPrice || 0)
        })
        this.summary = {
          count: rows.length,
          totalWeight: totalWeight.toFixed(3),
          totalAmount: Math.abs(totalAmount).toFixed(2),
          contractInfo: null
        }
        // 未选择单据时隐藏合同提示；选择后按首条选中单据显示
        if (selectedRows.length > 0 && selectedRows[0].organId) {
          getContractBalance({ organId: selectedRows[0].organId }).then(res => {
            if (res && res.code === 200 && res.data) {
              let d = res.data
              let totalAmt = Number(d.totalAmount || 0)
              let totalTon = Number(d.totalTonnage || 0)
              this.summary.contractInfo = {
                hasContract: totalAmt > 0 || totalTon > 0,
                remainAmount: Number(d.remainAmount || 0).toFixed(2),
                remainTonnage: Number(d.remainTonnage || 0).toFixed(3)
              }
            } else {
              this.summary.contractInfo = { hasContract: false }
            }
          }).catch(() => {
            this.summary.contractInfo = { hasContract: false }
          })
        }
      },
      handleViewFreight(billNo) {
        // 物流单号可能包含多个（逗号分隔），取第一个
        let no = billNo.split(',')[0].trim()
        this.$refs.freightDetailModal.detailByBillNo(no)
      },
      showAttach(record) {
        this.$refs.attachModal.show({
          ...record,
          uploadMeta: {
            freightBillNo: record.freightBillNo,
            totalWeight: record.totalWeight
          }
        }, 'fileName')
      },
      billRowClassName(record) {
        // 未审核的出库单高亮
        let s = String(record.status || '0')
        return (s !== '1' && s !== '2' && s !== '3') ? 'bill-row-incomplete' : ''
      },
      onAttachChange({ id, attachments }) {
        putAction('/depotHead/updateFileById', { id, fileName: attachments }).then(res => {
          if (res && res.code === 200) this.$message.success('附件已保存')
        })
      },
      handleClodopPrint() {
        if (this.selectedRowKeys.length !== 1) {
          this.$message.warning('请选择一条单据进行打印')
          return
        }
        let record = this.selectionRows[0]
        this.$refs.modalDetail.show(record, '销售出库', this.prefixNo, true)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
<style>
  .sale-out-list-page .sale-out-search-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px 16px;
  }

  .sale-out-list-page .sale-out-list-summary {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0;
    white-space: normal;
  }

  .sale-out-list-page .sale-out-list-summary .ant-divider {
    margin: 0 8px;
  }

  @media (max-width: 1400px) {
    .sale-out-list-page .sale-out-search-actions {
      align-items: flex-start;
    }

    .sale-out-list-page .sale-out-list-summary {
      margin-top: 4px;
    }
  }

  .bill-row-incomplete td {
    background-color: var(--erp-primary-light, #e6f7ff) !important;
  }
</style>
