<template>
  <j-modal
    title="新建客户对账单"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    fullscreen
    okText="生成对账单"
    cancelText="取消"
    :okButtonProps="{ props: { disabled: selectedRowKeys.length === 0 } }"
    @ok="handleGenerate"
    @cancel="handleCancel">

    <!-- 查询条件 -->
    <a-row :gutter="16" style="margin-bottom:12px;">
      <a-col :span="8">
        <a-form-item label="客户名称" :labelCol="{span:5}" :wrapperCol="{span:19}">
          <a-select v-model="selectedCustomerName" placeholder="请选择客户名称"
            showSearch allow-clear optionFilterProp="children"
            @search="handleSearchCustomer" @change="handleCustomerNameChange" style="width:100%">
            <a-select-option v-for="name in uniqueCustomerNames" :key="name" :value="name">{{ name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="16">
        <a-form-item label="项目名称" :labelCol="{span:3}" :wrapperCol="{span:21}">
          <a-select v-model="queryParam.organId" placeholder="请先选择客户名称"
            showSearch allow-clear optionFilterProp="children"
            :disabled="projectListForOrgan.length === 0" style="width:100%" @change="handleProjectChange">
            <a-select-option v-for="item in projectListForOrgan" :key="item.id" :value="item.id">
              {{ item.projectName || '(无项目名称)' }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="16" style="margin-bottom:12px;">
      <a-col :span="10">
        <a-form-item label="出库日期" :labelCol="{span:4}" :wrapperCol="{span:20}">
          <a-range-picker v-model="queryParam.dateRange" format="YYYY-MM-DD"
            :placeholder="['开始日期','结束日期']" @change="onDateChange" style="width:100%" />
        </a-form-item>
      </a-col>
      <a-col :span="4">
        <a-button type="primary" @click="loadItems(1)">查询</a-button>
        <a-button style="margin-left:8px" @click="resetQuery">重置</a-button>
      </a-col>
    </a-row>

    <!-- 备注 -->
    <a-row :gutter="16" style="margin-bottom:10px;">
      <a-col :span="16">
        <a-form-item label="备注" :labelCol="{span:3}" :wrapperCol="{span:21}">
          <a-input v-model="remark" placeholder="对账单备注（可选）" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="4" style="text-align:right;padding-top:4px;">
        <column-setting-popover
          :defColumns="defColumns"
          :settingDataIndex.sync="settingDataIndex"
          @change="onColChange"
          @reset="handleResetColumns"
        />
      </a-col>
    </a-row>
    <div v-if="selectedRowKeys.length > 0" style="margin-bottom:8px;color:#1890ff;">
      已选 {{ selectedRowKeys.length }} 行 |
      重量：{{ selectedWeight }} 吨 |
      金额：{{ selectedAmount }} 元
    </div>

    <!-- 未对账明细表格 -->
    <a-table
      size="small"
      bordered
      rowKey="id"
      :columns="tableColumns"
      :dataSource="dataSource"
      :components="dragComponents"
      :loading="loading"
      :rowSelection="rowSelection"
      :pagination="false"
      :scroll="{ x: 1100, y: 380 }">
      <span slot="action" slot-scope="text, record">
        <a @click="handleSplit(record)">拆分</a>
      </span>
    </a-table>

    <!-- 分页 -->
    <div style="margin-top:8px;text-align:right;">
      <a-pagination size="small" show-size-changer show-quick-jumper
        :current="currentPage" :pageSize="pageSize"
        :total="total" :pageSizeOptions="['20','50','100','200']"
        @change="onPageChange" @showSizeChange="onPageSizeChange"
        :show-total="t => `共 ${t} 条`" />
    </div>

    <row-split-modal ref="splitModal" @ok="onSplitOk" />
  </j-modal>
</template>

<script>
  import { findBySelectCus, listUnreconciledStatementItems, generateCustomerStatement, splitCustomerStatementItem } from '@/api/api'
  import JModal from '@/components/jeecg/JModal'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import RowSplitModal from '@/views/bill/dialog/RowSplitModal'
  import moment from 'moment'
  import {
    bindColumnSettingForceSync,
    unbindColumnSettingForceSync,
    loadColumnSetting,
    saveColumnSetting,
    resetColumnSetting,
    forceSyncColumnSetting
  } from '@/utils/columnSetting'

  export default {
    name: 'CustomerStatementGenerateModal',
    components: { JModal, ColumnSettingPopover, RowSplitModal },
    data() {
      return {
        visible: false,
        confirmLoading: false,
        loading: false,
        remark: '',
        queryParam: {
          organId: undefined,
          dateRange: [moment().subtract(3, 'months'), moment()],
          beginTime: moment().subtract(3, 'months').format('YYYY-MM-DD'),
          endTime: moment().format('YYYY-MM-DD')
        },
        cusList: [],
        selectedCustomerName: undefined,
        projectListForOrgan: [],
        dataSource: [],
        selectedRowKeys: [],
        selectedRows: [],
        lockedOrganId: '',
        currentPage: 1,
        pageSize: 50,
        total: 0,
        searchTimer: null,
        defDataIndex: ['billTimeStr','billNo','customerName','categoryName','brand','model','standard','unitWeight','operNumber','itemWeight','unitPrice','allPrice','billRemark'],
        settingDataIndex: [],
        defColumns: [
          { title: '日期', dataIndex: 'billTimeStr', width: 100, customRender: (t, r) => r.billTime ? r.billTime.substring(0, 10) : '' },
          { title: '出库单号', dataIndex: 'billNo', width: 150 },
          { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true },
          { title: '商品类别', dataIndex: 'categoryName', width: 100, ellipsis: true },
          { title: '品牌', dataIndex: 'brand', width: 90 },
          { title: '材质', dataIndex: 'model', width: 90 },
          { title: '规格', dataIndex: 'standard', width: 100 },
          { title: '件重', dataIndex: 'unitWeight', width: 85, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '件数', dataIndex: 'operNumber', width: 70 },
          { title: '重量小计(吨)', dataIndex: 'itemWeight', width: 110, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 110 },
          { title: '总金额(元)', dataIndex: 'allPrice', width: 110 },
          { title: '销售单备注', dataIndex: 'billRemark', width: 150, ellipsis: true }
        ]
      }
    },
    computed: {
      visibleColumns() {
        let colMap = {}
        this.defColumns.forEach(c => { colMap[c.dataIndex] = c })
        return this.settingDataIndex.filter(di => colMap[di]).map(di => colMap[di])
      },
      tableColumns() {
        return [...this.visibleColumns, { title: '操作', dataIndex: 'action', width: 70, align: 'center', scopedSlots: { customRender: 'action' } }]
      },
      dragComponents() {
        return this.buildDragComponents(this.visibleColumns)
      },
      rowSelection() {
        return {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange,
          getCheckboxProps: (record) => ({
            disabled: !!this.lockedOrganId && this.normalizeOrganId(record.organId) !== this.lockedOrganId
          })
        }
      },
      selectedWeight() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.itemWeight) || 0), 0).toFixed(3)
      },
      selectedAmount() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.allPrice) || 0), 0).toFixed(2)
      },
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
      this._forceSyncColumnSettingsHandler = () => {
        this.forceSyncColumnSettings()
      }
      bindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
      this.initColumnsSetting()
      this.initCustomer()
    },
    beforeDestroy() {
      if (this._forceSyncColumnSettingsHandler) {
        unbindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
        this._forceSyncColumnSettingsHandler = null
      }
    },
    methods: {
      getColumnSettingPageCode() {
        return 'CustomerStatementGenerateModal'
      },
      getColumnSettingStorageKey() {
        return 'CustomerStatementGenerateModal'
      },
      initColumnsSetting() {
        return loadColumnSetting({
          pageCode: this.getColumnSettingPageCode(),
          storageKey: this.getColumnSettingStorageKey(),
          defaultDataIndex: this.defDataIndex,
          applySetting: (dataIndexArr) => {
            this.settingDataIndex = [...dataIndexArr]
          }
        })
      },
      normalizeOrganId(organId) {
        return organId === null || organId === undefined || organId === '' ? '' : String(organId)
      },
      resetSelection(organId) {
        this.selectedRowKeys = []
        this.selectedRows = []
        this.lockedOrganId = this.normalizeOrganId(organId)
      },
      //列设置相关
      onColChange(orderedArr) {
        this.settingDataIndex = orderedArr
        saveColumnSetting({
          pageCode: this.getColumnSettingPageCode(),
          storageKey: this.getColumnSettingStorageKey(),
          dataIndexArr: orderedArr
        })
      },
      handleResetColumns() {
        resetColumnSetting({
          pageCode: this.getColumnSettingPageCode(),
          storageKey: this.getColumnSettingStorageKey(),
          defaultDataIndex: this.defDataIndex,
          applySetting: (dataIndexArr) => {
            this.settingDataIndex = [...dataIndexArr]
          }
        })
      },
      forceSyncColumnSettings() {
        return forceSyncColumnSetting({
          pageCode: this.getColumnSettingPageCode(),
          storageKey: this.getColumnSettingStorageKey(),
          defaultDataIndex: this.defDataIndex,
          applySetting: (dataIndexArr) => {
            this.settingDataIndex = [...dataIndexArr]
          }
        })
      },
      //列宽拖拽
      buildDragComponents(columns) {
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
                    var wrapper = th.closest('.ant-table-scroll') || th.closest('.ant-table-content')
                    var onMove = function(ev) {
                      var newWidth = Math.max(startWidth + (ev.clientX - startX), 50)
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
      show() {
        this.visible = true
        this.selectedCustomerName = undefined
        this.projectListForOrgan = []
        this.queryParam.organId = undefined
        this.resetSelection('')
        this.remark = ''
        this.loadItems(1)
      },
      handleCancel() {
        this.visible = false
      },
      initCustomer() {
        findBySelectCus({ limit: 1 }).then(res => {
          if (res) this.cusList = res
        })
      },
      handleCustomerNameChange(supplierName) {
        this.queryParam.organId = undefined
        this.resetSelection('')
        if (supplierName) {
          this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
          if (this.projectListForOrgan.length === 1) {
            this.queryParam.organId = this.projectListForOrgan[0].id
            this.lockedOrganId = this.normalizeOrganId(this.queryParam.organId)
          }
        } else {
          this.projectListForOrgan = []
        }
      },
      handleProjectChange(value) {
        this.resetSelection(value)
      },
      handleSearchCustomer(val) {
        if (this.searchTimer) clearTimeout(this.searchTimer)
        this.searchTimer = setTimeout(() => {
          findBySelectCus({ key: val, limit: 1 }).then(res => {
            if (res) this.cusList = res
          })
        }, 400)
      },
      onDateChange(dates, dateStrings) {
        this.queryParam.beginTime = dateStrings[0] || ''
        this.queryParam.endTime = dateStrings[1] || ''
        this.resetSelection(this.queryParam.organId)
      },
      resetQuery() {
        this.queryParam = {
          organId: undefined,
          dateRange: [moment().subtract(3, 'months'), moment()],
          beginTime: moment().subtract(3, 'months').format('YYYY-MM-DD'),
          endTime: moment().format('YYYY-MM-DD')
        }
        this.resetSelection('')
        this.loadItems(1)
      },
      loadItems(page) {
        if (page) this.currentPage = page
        this.loading = true
        const params = {
          organId: this.queryParam.organId,
          beginTime: this.queryParam.beginTime,
          endTime: this.queryParam.endTime,
          currentPage: this.currentPage,
          pageSize: this.pageSize
        }
        listUnreconciledStatementItems(params).then(res => {
          if (res && res.code === 200) {
            this.dataSource = res.data.rows || []
            this.total = res.data.total || 0
          }
        }).finally(() => { this.loading = false })
      },
      onSelectChange(keys, rows) {
        if (!keys || keys.length === 0) {
          this.resetSelection(this.queryParam.organId)
          return
        }
        let baseOrganId = this.lockedOrganId || this.normalizeOrganId(this.queryParam.organId) || this.normalizeOrganId(rows[0] && rows[0].organId)
        let filteredRows = rows.filter(row => this.normalizeOrganId(row.organId) === baseOrganId)
        let filteredKeys = filteredRows.map(row => row.id)
        if (filteredRows.length !== rows.length) {
          this.$message.warning('一次只能选择同一项目名称的明细')
        }
        this.lockedOrganId = baseOrganId
        this.selectedRowKeys = filteredKeys
        this.selectedRows = filteredRows
      },
      onPageChange(page) {
        this.currentPage = page
        this.loadItems()
      },
      onPageSizeChange(current, size) {
        this.pageSize = size
        this.loadItems(1)
      },
      handleSplit(record) {
        if (!record || !record.id) return
        this.$refs.splitModal.show({
          rowId: record.id,
          name: record.brand,
          standard: record.standard,
          operNumber: record.operNumber,
          weight: record.itemWeight
        })
      },
      onSplitOk({ sourceRowId, splitRows }) {
        this.confirmLoading = true
        splitCustomerStatementItem({
          itemId: sourceRowId,
          splitWeights: splitRows.map(r => r.weight)
        }).then(res => {
          if (res && res.code === 200) {
            this.selectedRowKeys = this.selectedRowKeys.filter(id => id !== sourceRowId)
            this.selectedRows = this.selectedRows.filter(row => row.id !== sourceRowId)
            this.$message.success('拆分成功')
            this.loadItems(this.currentPage)
          } else {
            this.$message.error(res.data || '拆分失败')
          }
        }).finally(() => { this.confirmLoading = false })
      },
      handleGenerate() {
        if (this.selectedRowKeys.length === 0) {
          this.$message.warning('请至少勾选一条明细')
          return
        }
        if (!this.queryParam.organId && this.selectedRows.length > 0) {
          this.queryParam.organId = this.selectedRows[0].organId
        }
        // 从勾选的明细行中取最早和最晚送货日期作为账期
        let dates = this.selectedRows
          .map(r => r.billTime ? r.billTime.substring(0, 10) : '')
          .filter(d => d)
          .sort()
        let actualBegin = dates.length > 0 ? dates[0] : this.queryParam.beginTime
        let actualEnd = dates.length > 0 ? dates[dates.length - 1] : this.queryParam.endTime
        this.confirmLoading = true
        const params = {
          organId: this.queryParam.organId,
          itemIds: this.selectedRowKeys,
          remark: this.remark,
          beginTime: actualBegin,
          endTime: actualEnd
        }
        generateCustomerStatement(params).then(res => {
          if (res && res.code === 200) {
            this.$message.success('对账单生成成功')
            this.visible = false
            this.$emit('ok')
          } else {
            this.$message.error(res.data || '生成失败')
          }
        }).finally(() => { this.confirmLoading = false })
      }
    }
  }
</script>
