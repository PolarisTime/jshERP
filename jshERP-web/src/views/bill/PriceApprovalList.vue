<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="4" :sm="24">
                <a-form-item label="客户" :labelCol="{span:6}" :wrapperCol="{span:18}">
                  <a-select placeholder="全部" v-model="selectedCustomerName"
                    showSearch allow-clear optionFilterProp="children"
                    @search="handleSearchCustomer" @change="onCustomerNameChange">
                    <a-select-option v-for="name in uniqueCustomerNames" :key="name" :value="name">{{ name }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="5" :sm="24">
                <a-form-item label="项目" :labelCol="{span:5}" :wrapperCol="{span:19}">
                  <a-select placeholder="全部" v-model="queryParam.organId"
                    showSearch allow-clear optionFilterProp="children"
                    :disabled="projectListForOrgan.length === 0">
                    <a-select-option v-for="item in projectListForOrgan" :key="item.id" :value="item.id">
                      {{ item.projectName || item.project_name || '(无项目名称)' }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item label="出库单号" :labelCol="{span:8}" :wrapperCol="{span:16}">
                  <a-input placeholder="请输入" v-model="queryParam.billNo" allow-clear></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="5" :sm="24">
                <a-form-item label="出库日期" :labelCol="{span:7}" :wrapperCol="{span:17}">
                  <a-range-picker
                    style="width:100%"
                    v-model="queryParam.dateRange"
                    format="YYYY-MM-DD"
                    :placeholder="['开始','结束']"
                    @change="onDateChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :md="3" :sm="24">
                <a-form-item label="状态" :labelCol="{span:8}" :wrapperCol="{span:16}">
                  <a-select placeholder="全部" allow-clear v-model="queryParam.status">
                    <a-select-option value="0">待核准</a-select-option>
                    <a-select-option value="1">已核准</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="3" :sm="24">
                <a-button type="primary" @click="searchQuery">查询</a-button>
                <a-button style="margin-left:8px" @click="searchReset">重置</a-button>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="margin-top:5px;display:flex;align-items:center;flex-wrap:wrap;gap:6px;">
          <a-button type="primary" icon="plus" @click="handleAdd">新增</a-button>
          <a-button icon="check" @click="handleBatchConfirm" style="color:#52c41a">核准</a-button>
          <a-button icon="undo" @click="handleBatchCancel">取消核准</a-button>
          <a-button icon="delete" @click="handleBatchDelete">批量删除</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleResetColumns"
          />
        </div>
        <!-- 表格区域 -->
        <a-table
          ref="table"
          size="middle"
          bordered
          rowKey="id"
          :columns="visibleColumns"
          :dataSource="dataSource"
          :components="handleDrag(visibleColumns)"
          :pagination="ipagination"
          :loading="loading"
          :scroll="{ x: scrollX }"
          :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
          :rowClassName="approvalRowClassName"
          @change="handleTableChange">
          <span slot="action" slot-scope="text, record">
            <a @click="handleEdit(record)">{{ record.status === '1' ? '查看' : '编辑' }}</a>
            <a-divider v-if="record.status === '0'" type="vertical" />
            <a-popconfirm v-if="record.status === '0'" title="确定删除吗?" @confirm="() => handleDelete(record)">
              <a>删除</a>
            </a-popconfirm>
            <a-divider type="vertical" />
            <a @click="showAttach(record)" style="white-space:nowrap">
              <a-icon type="paper-clip" /> 附件
              <a-badge v-if="record.fileName" :count="record.fileName.split(',').filter(f=>f).length" :numberStyle="{fontSize:'10px',minWidth:'16px',height:'16px',lineHeight:'16px'}" />
              <a-icon v-else type="close-circle" style="color:#ccc;font-size:12px" />
            </a>
          </span>
          <template slot="customRenderStatus" slot-scope="text">
            <a-tag v-if="text === '1'" color="green">已核准</a-tag>
            <a-tag v-else color="orange">待核准</a-tag>
          </template>
        </a-table>
        <!-- 弹窗 -->
        <price-approval-modal ref="modalForm" @ok="loadData(1)"></price-approval-modal>
        <attachment-modal ref="attachModal" bizPath="bill" @change="onAttachChange"></attachment-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
import PriceApprovalModal from './modules/PriceApprovalModal'
import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import AttachmentModal from '@/components/tools/AttachmentModal'
import { listPriceApprovals, confirmPriceApproval, cancelPriceApproval, deletePriceApproval,
  findBySelectCus } from '@/api/api'
import { putAction } from '@/api/manage'
import {
  bindColumnSettingForceSync,
  unbindColumnSettingForceSync,
  loadColumnSetting,
  saveColumnSetting,
  resetColumnSetting,
  forceSyncColumnSetting
} from '@/utils/columnSetting'

export default {
  name: 'PriceApprovalList',
  components: { PriceApprovalModal, ColumnSettingPopover, AttachmentModal },
  data() {
    return {
      queryParam: {
        billNo: '',
        organId: undefined,
        status: undefined,
        dateRange: [],
        beginTime: '',
        endTime: ''
      },
      // 默认显示的列索引（与 defColumns 中的 dataIndex 对应）
      defDataIndex: ['action','approvalNo','billNo','customerName','projectName','billTimeStr',
                      'deliveryDateStr','totalWeight','totalAmount','status','remark','createTimeStr'],
      // 全量列定义
      defColumns: [
        { title: '操作', dataIndex: 'action', align: 'center', width: 160,
          scopedSlots: { customRender: 'action' } },
        { title: '核准单号', dataIndex: 'approvalNo', width: 160, sorter: true },
        { title: '出库单号', dataIndex: 'billNo', width: 160, sorter: true },
        { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true, sorter: true },
        { title: '项目名称', dataIndex: 'projectName', width: 120, ellipsis: true },
        { title: '出库日期', dataIndex: 'billTimeStr', width: 110, sorter: true },
        { title: '送到日期', dataIndex: 'deliveryDateStr', width: 110, sorter: true },
        { title: '总重量(吨)', dataIndex: 'totalWeight', width: 100, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(3) : '0.000' },
        { title: '总金额(元)', dataIndex: 'totalAmount', width: 100, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '0.00' },
        { title: '状态', dataIndex: 'status', width: 80, align: 'center',
          scopedSlots: { customRender: 'customRenderStatus' } },
        { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
        { title: '创建时间', dataIndex: 'createTimeStr', width: 160, sorter: true }
      ],
      settingDataIndex: [],
      dataSource: [],
      loading: false,
      selectedRowKeys: [],
      selectedRows: [],
      ipagination: {
        current: 1,
        pageSize: 20,
        pageSizeOptions: ['10', '20', '50'],
        showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      // 客户级联选择
      cusList: [],
      selectedCustomerName: undefined,
      projectListForOrgan: [],
      searchTimer: null,
      // 前端排序
      sortField: '',
      sortOrder: ''
    }
  },
  computed: {
    /** 根据 settingDataIndex 过滤可见列 */
    visibleColumns() {
      let colMap = {}
      this.defColumns.forEach(c => { colMap[c.dataIndex] = c })
      return this.settingDataIndex.filter(di => colMap[di]).map(di => colMap[di])
    },
    scrollX() {
      let sum = 0
      this.visibleColumns.forEach(c => { sum += (c.width || 100) })
      return sum
    },
    /** 从客户列表中提取去重的客户名称 */
    uniqueCustomerNames() {
      let seen = new Set()
      return this.cusList.map(c => c.supplier).filter(name => {
        if (!name || seen.has(name)) return false
        seen.add(name)
        return true
      })
    }
  },
  created() {
    this._forceSyncColumnSettingsHandler = () => { this.forceSyncColumnSettings() }
    bindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
    this.initColumnsSetting()
    this.loadData(1)
    this.initCustomer()
  },
  beforeDestroy() {
    if (this._forceSyncColumnSettingsHandler) {
      unbindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
      this._forceSyncColumnSettingsHandler = null
    }
  },
  methods: {
    loadData(page) {
      if (page) this.ipagination.current = page
      this.loading = true
      let params = {
        billNo: this.queryParam.billNo || undefined,
        organId: this.queryParam.organId || undefined,
        status: this.queryParam.status || undefined,
        beginTime: this.queryParam.beginTime || undefined,
        endTime: this.queryParam.endTime || undefined,
        currentPage: this.ipagination.current,
        pageSize: this.ipagination.pageSize
      }
      listPriceApprovals(params).then(res => {
        if (res && res.code === 200) {
          let rows = res.data.rows || []
          // 前端排序
          if (this.sortField) {
            let field = this.sortField
            let asc = this.sortOrder === 'ascend'
            rows.sort((a, b) => {
              let va = a[field], vb = b[field]
              if (va == null) va = ''
              if (vb == null) vb = ''
              let numA = Number(va), numB = Number(vb)
              let cmp = (!isNaN(numA) && !isNaN(numB)) ? numA - numB : String(va).localeCompare(String(vb))
              return asc ? cmp : -cmp
            })
          }
          this.dataSource = rows
          this.ipagination.total = res.data.total || 0
        }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    searchQuery() {
      this.loadData(1)
    },
    searchReset() {
      this.selectedCustomerName = undefined
      this.projectListForOrgan = []
      this.queryParam = { billNo: '', organId: undefined, status: undefined, dateRange: [], beginTime: '', endTime: '' }
      this.sortField = ''
      this.sortOrder = ''
      this.loadData(1)
    },
    onDateChange(dates, dateStrings) {
      this.queryParam.beginTime = dateStrings[0] ? dateStrings[0] + ' 00:00:00' : ''
      this.queryParam.endTime = dateStrings[1] ? dateStrings[1] + ' 23:59:59' : ''
    },
    handleTableChange(pagination, filters, sorter) {
      this.ipagination.current = pagination.current
      this.ipagination.pageSize = pagination.pageSize
      if (sorter && sorter.field) {
        this.sortField = sorter.field
        this.sortOrder = sorter.order || ''
      } else {
        this.sortField = ''
        this.sortOrder = ''
      }
      this.loadData()
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },

    // ─── 客户选择（参照对账单模式：客户名称 + 项目级联） ─────────
    initCustomer() {
      findBySelectCus({ limit: 1 }).then(res => { if (res) this.cusList = res })
    },
    handleSearchCustomer(val) {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        findBySelectCus({ key: val, limit: 1 }).then(res => { if (res) this.cusList = res })
      }, 400)
    },
    onCustomerNameChange(supplierName) {
      this.queryParam.organId = undefined
      if (supplierName) {
        this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
        if (this.projectListForOrgan.length === 1) {
          this.queryParam.organId = this.projectListForOrgan[0].id
          this.loadData(1)
        }
      } else {
        this.projectListForOrgan = []
        this.loadData(1)
      }
    },

    // ─── 列设置（云同步） ─────────────────────────────────────
    initColumnsSetting() {
      return loadColumnSetting({
        pageCode: 'HJHZ',
        storageKey: 'HJHZ',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },
    onColChange(orderedArr) {
      this.settingDataIndex = [...orderedArr]
      return saveColumnSetting({
        pageCode: 'HJHZ',
        storageKey: 'HJHZ',
        dataIndexArr: this.settingDataIndex
      })
    },
    handleResetColumns() {
      return resetColumnSetting({
        pageCode: 'HJHZ',
        storageKey: 'HJHZ',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },
    forceSyncColumnSettings() {
      return forceSyncColumnSetting({
        pageCode: 'HJHZ',
        storageKey: 'HJHZ',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },

    // ─── 列宽拖拽 ────────────────────────────────────────────
    handleDrag(columns) {
      return {
        header: {
          cell: (h, props, children) => {
            const { key, ...restProps } = props
            const col = columns.find(c => (c.dataIndex || c.key) === key)
            if (!col || !col.width) {
              return h('th', { ...restProps }, children)
            }
            const handle = h('div', {
              class: 'resize-handle',
              on: {
                mousedown: function(e) {
                  e.stopPropagation()
                  e.preventDefault()
                  var th = e.target.parentNode
                  var startX = e.clientX
                  var startWidth = th.offsetWidth
                  var colIndex = Array.prototype.indexOf.call(th.parentNode.children, th)
                  var onMove = function(ev) {
                    var newWidth = Math.max(startWidth + (ev.clientX - startX), 50)
                    var wrapper = th.closest('.ant-table-scroll') || th.closest('.ant-table-content')
                    if (wrapper) {
                      wrapper.querySelectorAll('colgroup').forEach(function(cg) {
                        var targetCol = cg.children[colIndex]
                        if (targetCol) {
                          targetCol.style.width = newWidth + 'px'
                          targetCol.style.minWidth = newWidth + 'px'
                        }
                      })
                    }
                    th.style.width = newWidth + 'px'
                    th.style.minWidth = newWidth + 'px'
                    col.width = newWidth
                  }
                  var onUp = function() {
                    document.removeEventListener('mousemove', onMove)
                    document.removeEventListener('mouseup', onUp)
                    document.body.style.cursor = ''
                    document.body.style.userSelect = ''
                  }
                  document.body.style.cursor = 'col-resize'
                  document.body.style.userSelect = 'none'
                  document.addEventListener('mousemove', onMove)
                  document.addEventListener('mouseup', onUp)
                }
              }
            })
            return h('th', { ...restProps, class: 'resize-table-th' }, [children, handle])
          }
        }
      }
    },

    // ─── 附件（与销售出库共享 DepotHead.fileName） ─────────
    showAttach(record) {
      this.$refs.attachModal.show({
        id: record.id,
        uploadBillId: record.depotHeadId,
        fileName: record.fileName
      }, 'fileName')
    },
    onAttachChange({ id, attachments }) {
      putAction('/depotHead/updateFileById', { id, fileName: attachments }).then(res => {
        if (res && res.code === 200) {
          this.$message.success('附件已保存')
          let row = this.dataSource.find(r => r.depotHeadId === id)
          if (row) row.fileName = attachments
        }
      })
    },

    // ─── 操作 ────────────────────────────────────────────
    handleAdd() {
      this.$refs.modalForm.showNew()
    },
    handleEdit(record) {
      this.$refs.modalForm.show(record.id, record.status === '1')
    },
    handleConfirm(record) {
      let that = this
      this.$confirm({
        title: '确认核准？',
        content: '核准后将回写销售出库的重量、价格、送到日期、备注和行备注，确定继续吗？',
        onOk() {
          confirmPriceApproval({ id: record.id }).then(res => {
            if (res && res.code === 200) {
              that.$message.success('核准成功')
              that.loadData()
            } else {
              that.$message.error(res.data || '核准失败')
            }
          })
        }
      })
    },
    handleCancel(record) {
      let that = this
      this.$confirm({
        title: '确认取消核准？',
        content: '取消后将撤销销售出库的重量/价格核准状态，确定继续吗？',
        onOk() {
          cancelPriceApproval({ id: record.id }).then(res => {
            if (res && res.code === 200) {
              that.$message.success('取消核准成功')
              that.loadData()
            } else {
              that.$message.error(res.data || '取消失败')
            }
          })
        }
      })
    },
    handleDelete(record) {
      deletePriceApproval({ id: record.id }).then(res => {
        if (res && res.code === 200) {
          this.$message.success('删除成功')
          this.loadData()
        } else {
          this.$message.error(res.data || '删除失败')
        }
      })
    },
    handleBatchConfirm() {
      if (!this.selectedRowKeys.length) { this.$message.warning('请先选择记录'); return }
      let pending = this.selectedRows.filter(r => r.status === '0')
      if (!pending.length) { this.$message.warning('没有待核准的记录'); return }
      let that = this
      let count = 0
      this.$confirm({
        title: `确认批量核准 ${pending.length} 条记录？`,
        onOk() {
          let promises = pending.map(r => confirmPriceApproval({ id: r.id }))
          Promise.allSettled(promises).then(results => {
            results.forEach(r => { if (r.status === 'fulfilled' && r.value.code === 200) count++ })
            that.$message.success(`成功核准 ${count} 条`)
            that.loadData()
            that.selectedRowKeys = []
          })
        }
      })
    },
    handleBatchCancel() {
      if (!this.selectedRowKeys.length) { this.$message.warning('请先选择记录'); return }
      let approved = this.selectedRows.filter(r => r.status === '1')
      if (!approved.length) { this.$message.warning('没有已核准的记录'); return }
      let that = this
      let count = 0
      this.$confirm({
        title: `确认批量取消核准 ${approved.length} 条记录？`,
        onOk() {
          let promises = approved.map(r => cancelPriceApproval({ id: r.id }))
          Promise.allSettled(promises).then(results => {
            results.forEach(r => { if (r.status === 'fulfilled' && r.value.code === 200) count++ })
            that.$message.success(`成功取消 ${count} 条`)
            that.loadData()
            that.selectedRowKeys = []
          })
        }
      })
    },
    // ─── 未核准行染色（与主题色一致，参照物流单） ──────────────
    approvalRowClassName(record) {
      return (record.status === '0' || record.status === 0) ? 'approval-row-incomplete' : ''
    },
    handleBatchDelete() {
      if (!this.selectedRowKeys.length) { this.$message.warning('请先选择记录'); return }
      let pending = this.selectedRows.filter(r => r.status === '0')
      if (!pending.length) { this.$message.warning('没有可删除的记录（已核准记录不能删除）'); return }
      let that = this
      let count = 0
      this.$confirm({
        title: `确认批量删除 ${pending.length} 条记录？`,
        onOk() {
          let promises = pending.map(r => deletePriceApproval({ id: r.id }))
          Promise.allSettled(promises).then(results => {
            results.forEach(r => { if (r.status === 'fulfilled' && r.value.code === 200) count++ })
            that.$message.success(`成功删除 ${count} 条`)
            that.loadData()
            that.selectedRowKeys = []
          })
        }
      })
    }
  }
}
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>
<style>
  .approval-row-incomplete td {
    background-color: var(--erp-primary-light, #e6f7ff) !important;
  }
</style>
