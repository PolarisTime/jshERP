<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择客户" v-model="queryParam.organId"
                    showSearch allow-clear optionFilterProp="children"
                    @search="handleSearchCustomer">
                    <a-select-option v-for="item in cusList" :key="item.id" :value="item.id">
                      {{ item.supplier }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="7" :sm="24">
                <a-form-item label="创建日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker v-model="queryParam.dateRange" format="YYYY-MM-DD"
                    :placeholder="['开始','结束']" @change="onDateChange" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item label="审核" :labelCol="{span:6}" :wrapperCol="{span:16}">
                  <a-select v-model="queryParam.status" allow-clear placeholder="全部">
                    <a-select-option value="0">未审核</a-select-option>
                    <a-select-option value="1">已审核</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item label="签署" :labelCol="{span:6}" :wrapperCol="{span:16}">
                  <a-select v-model="queryParam.signStatus" allow-clear placeholder="全部">
                    <a-select-option value="0">未签署</a-select-option>
                    <a-select-option value="1">已签署</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="3" :sm="24">
                <span class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left:8px" @click="searchReset">重置</a-button>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <!-- 操作按钮区 -->
        <div class="table-operator" style="margin-top:5px;display:flex;align-items:center;flex-wrap:wrap;gap:6px;">
          <a-button type="primary" icon="plus" @click="handleNew">新建对账单</a-button>
          <a-button icon="check" @click="handleAudit('1')" :disabled="!hasSingleSelected">审核</a-button>
          <a-button icon="rollback" @click="handleAudit('0')" :disabled="!hasSingleSelected">反审核</a-button>
          <a-button icon="edit" @click="handleSign('1')" :disabled="!hasSingleSelected">签署</a-button>
          <a-button icon="close-circle" @click="handleSign('0')" :disabled="!hasSingleSelected">取消签署</a-button>
          <a-button icon="printer" @click="handleCustomPrint" :disabled="!hasSingleSelected">打印</a-button>
          <a-button icon="download" @click="handleExportCsv">导出</a-button>
        </div>

        <!-- 表格 -->
        <a-table
          bordered
          size="middle"
          rowKey="id"
          :columns="columns"
          :dataSource="dataSource"
          :components="handleDrag(columns)"
          :loading="loading"
          :pagination="false"
          :scroll="scroll"
          :rowSelection="{ selectedRowKeys, onChange: onSelectChange, type: 'radio' }">
          <span slot="action" slot-scope="text, record">
            <a @click="handleDetail(record)">查看</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确定删除该对账单？" @confirm="handleDelete(record)">
              <a style="color:#ff4d4f">删除</a>
            </a-popconfirm>
                        <a-divider type="vertical" />
              <a @click="$refs.attachModal.show(record, 'attachment')" style="white-space:nowrap">
                <a-icon type="paper-clip" /> 附件
                <a-badge v-if="record.attachment" :count="record.attachment.split(',').filter(f=>f).length" :numberStyle="{fontSize:'10px',minWidth:'16px',height:'16px',lineHeight:'16px'}" />
                <a-icon v-else type="close-circle" style="color:#ccc;font-size:12px" />
              </a>
              </span>
          <template slot="statusRender" slot-scope="text, record">
            <a-tag :color="record.status === '1' ? 'green' : 'red'">
              {{ record.status === '1' ? '已审核' : '未审核' }}
            </a-tag>
          </template>
          <template slot="signRender" slot-scope="text, record">
            <a-tag :color="record.signStatus === '1' ? 'blue' : 'orange'">
              {{ record.signStatus === '1' ? '已签署' : '未签署' }}
            </a-tag>
          </template>
        </a-table>

        <!-- 分页 -->
        <a-row style="margin-top:8px;text-align:right;">
          <a-pagination size="small" show-size-changer show-quick-jumper
            :current="ipagination.current" :pageSize="ipagination.pageSize"
            :total="ipagination.total" :pageSizeOptions="['10','20','50','100']"
            @change="paginationChange" @showSizeChange="paginationShowSizeChange"
            :show-total="t => `共 ${t} 条`" />
        </a-row>

        <!-- 子组件 -->
        <customer-statement-generate-modal ref="generateModal" @ok="loadData(1)" />
        <customer-statement-detail-modal ref="detailModal" @refresh="loadData" />
        <attachment-modal ref="attachModal" bizPath="statement" @change="onAttachChange"></attachment-modal>
        <custom-print-modal ref="customPrintModal" billType="customerStatement" :model="printModel" :dataSource="printDataSource" />
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { findBySelectCus, deleteCustomerStatement, getCustomerStatementDetail,
           auditCustomerStatement, signCustomerStatement } from '@/api/api'
  import { getAction } from '@/api/manage'
  import CustomerStatementGenerateModal from './modules/CustomerStatementGenerateModal'
  import CustomerStatementDetailModal from './modules/CustomerStatementDetailModal'
  import CustomPrintModal from '../bill/dialog/CustomPrintModal'

  import AttachmentModal from '@/components/tools/AttachmentModal'
  import { putAction } from '@/api/manage'

  export default {
    name: 'CustomerStatementList',
    mixins: [JeecgListMixin],
    components: {
      AttachmentModal, CustomerStatementGenerateModal, CustomerStatementDetailModal, CustomPrintModal },
    data() {
      return {
        labelCol: { span: 5 },
        wrapperCol: { span: 18, offset: 1 },
        queryParam: {
          organId: undefined,
          status: undefined,
          signStatus: undefined,
          beginTime: '',
          endTime: '',
          dateRange: []
        },
        cusList: [],
        searchTimer: null,
        // 行选择
        selectedRowKeys: [],
        selectedRows: [],
        columns: [
          { title: '操作', dataIndex: 'action', width: 100, align: 'center', scopedSlots: { customRender: 'action' } },
          { title: '对账单号', dataIndex: 'statementNo', width: 170 },
          { title: '客户', dataIndex: 'customerName', width: 140, ellipsis: true },
          { title: '账期开始', dataIndex: 'beginTimeStr', width: 110 },
          { title: '账期结束', dataIndex: 'endTimeStr', width: 110 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110 },
          { title: '总金额(元)', dataIndex: 'totalAmount', width: 110 },
          { title: '审核状态', dataIndex: 'status', width: 90, align: 'center', scopedSlots: { customRender: 'statusRender' } },
          { title: '签署状态', dataIndex: 'signStatus', width: 90, align: 'center', scopedSlots: { customRender: 'signRender' } },
          { title: '创建时间', dataIndex: 'createTimeStr', width: 160 },
          { title: '备注', dataIndex: 'remark', ellipsis: true }
        ],
        printModel: {},
        printDataSource: [],
        url: { list: '/customerStatement/list' }
      }
    },
    computed: {
      hasSingleSelected() {
        return this.selectedRowKeys.length === 1
      }
    },
    created() {
      this.initCustomer()
    },
    mounted() {},
    methods: {
      // ─── 数据加载 ───────────────────────────────────────────────
      getQueryParams() {
        const p = Object.assign({}, this.queryParam)
        delete p.dateRange
        p.currentPage = this.ipagination.current
        p.pageSize = this.ipagination.pageSize
        return p
      },
      loadData(arg) {
        if (arg === 1) this.ipagination.current = 1
        this.loading = true
        getAction(this.url.list, this.getQueryParams()).then(res => {
          if (res.code === 200) {
            this.dataSource = res.data.rows || []
            this.ipagination.total = res.data.total || 0
          } else {
            this.$message.warning(res.data || '查询失败')
          }
        }).finally(() => { this.loading = false })
      },
      searchQuery() { this.loadData(1) },
      searchReset() {
        this.queryParam = { organId: undefined, status: undefined, signStatus: undefined, beginTime: '', endTime: '', dateRange: [] }
        this.loadData(1)
      },
      onDateChange(dates, dateStrings) {
        this.queryParam.beginTime = dateStrings[0] || ''
        this.queryParam.endTime = dateStrings[1] || ''
      },
      onSelectChange(keys, rows) {
        this.selectedRowKeys = keys
        this.selectedRows = rows
      },
      paginationChange(page) {
        this.ipagination.current = page
        this.loadData()
      },
      paginationShowSizeChange(current, size) {
        this.ipagination.pageSize = size
        this.loadData(1)
      },

      // ─── 客户搜索 ────────────────────────────────────────────────
      initCustomer() {
        findBySelectCus({ limit: 1 }).then(res => { if (res) this.cusList = res })
      },
      handleSearchCustomer(val) {
        if (this.searchTimer) clearTimeout(this.searchTimer)
        this.searchTimer = setTimeout(() => {
          findBySelectCus({ key: val, limit: 1 }).then(res => { if (res) this.cusList = res })
        }, 400)
      },

      // ─── 行操作 ──────────────────────────────────────────────────
      handleNew() { this.$refs.generateModal.show() },
      handleDetail(record) { this.$refs.detailModal.show(record) },
      handleDelete(record) {
        deleteCustomerStatement({ id: record.id }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('删除成功')
            this.selectedRowKeys = []
            this.loadData()
          } else {
            this.$message.error(res.data || '删除失败')
          }
        })
      },

      // ─── 审核 / 签署 ─────────────────────────────────────────────
      handleAudit(status) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        if (status === '0' && row.signStatus === '1') {
          this.$message.warning('已签署的对账单不能反审核')
          return
        }
        auditCustomerStatement({ id, status }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(status === '1' ? '审核成功' : '反审核成功')
            this.selectedRowKeys = []
            this.selectedRows = []
            this.loadData()
          } else {
            this.$message.error(res.data || '操作失败')
          }
        })
      },
      handleSign(signStatus) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        // 从最新的 dataSource 中获取行数据（避免使用过时的 selectedRows）
        const row = this.dataSource.find(r => r.id === id) || this.selectedRows[0] || {}
        if (signStatus === '1' && String(row.status || '0') !== '1') {
          this.$message.warning('请先审核再签署')
          return
        }
        signCustomerStatement({ id, signStatus }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(signStatus === '1' ? '签署成功' : '已取消签署')
            this.loadData()
          } else {
            this.$message.error(res.data || '操作失败')
          }
        })
      },

      // ─── 打印 ──────────────────────────────────────────────────
      async handleCustomPrint() {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        const res = await getCustomerStatementDetail({ id })
        if (!res || res.code !== 200) { this.$message.error('获取详情失败'); return }
        const rawItems = res.data.items || []
        // 格式化日期：billTime 可能是时间戳数字或字符串
        const items = rawItems.map((it, idx) => {
          let bt = it.billTime
          if (typeof bt === 'number') bt = new Date(bt).toISOString().substring(0, 10)
          else if (typeof bt === 'string' && bt.length > 10) bt = bt.substring(0, 10)
          return { ...it, _index: idx + 1, billTime: bt || '' }
        })
        const model = { ...row, ...(res.data.header || {}), details: items }
        this.printModel = model
        this.printDataSource = items
        this.$nextTick(() => { this.$refs.customPrintModal.show(model, items) })
      },
      // ─── 导出（含明细） ──────────────────────────────────────────
      async handleExportCsv() {
        if (!this.hasSingleSelected) { this.$message.warning('请选中一条对账单'); return }
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        const res = await getCustomerStatementDetail({ id })
        if (!res || res.code !== 200) { this.$message.error('获取详情失败'); return }
        const items = res.data.items || []
        // 表头行
        const headers = ['对账单号','客户','账期','日期','出库单号','商品类别','品牌','材质','规格','件重(吨)','件数','重量(吨)','单价(元/吨)','金额(元)']
        let csv = '\uFEFF' + headers.join(',') + '\n'
        const statementNo = row.statementNo || ''
        const customerName = row.customerName || ''
        const period = (row.beginTimeStr || '') + '~' + (row.endTimeStr || '')
        items.forEach(it => {
          let vals = [statementNo, customerName, period,
            it.billTime ? it.billTime.substring(0,10) : '', it.billNo || '',
            it.categoryName || '', it.brand || '', it.model || '', it.standard || '',
            it.unitWeight || '', it.operNumber || '', it.itemWeight || '',
            it.unitPrice || '', it.allPrice || '']
          csv += vals.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',') + '\n'
        })
        // 合计行
        csv += '"","","","","","","","","","","","' + (row.totalWeight || '') + '","","' + (row.totalAmount || '') + '"\n'
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = (statementNo || '对账单') + '.csv'
        link.click()
      },
      onAttachChange({ id, attachments }) {
        putAction('/customerStatement/updateAttachment', { id, attachment: attachments }).then(res => {
          if (res && res.code === 200) this.$message.success('附件已保存')
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
