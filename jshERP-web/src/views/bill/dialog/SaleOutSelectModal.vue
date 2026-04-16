<template>
  <a-modal
    title="选择销售出库单"
    :width="1300"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    @ok="handleOk"
    @cancel="handleCancel">
    <!-- 搜索区域 -->
    <div class="table-page-search-wrapper" style="margin-bottom:12px">
      <a-form layout="inline" @keyup.enter.native="loadData(1)">
        <a-row :gutter="16">
          <a-col :span="5">
            <a-form-item label="出库单号" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input placeholder="请输入单号" v-model="searchBillNo" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="客户" :labelCol="{span:6}" :wrapperCol="{span:18}">
              <a-select placeholder="全部" v-model="selectedCustomerName"
                showSearch allow-clear optionFilterProp="children"
                @search="handleSearchCustomer" @change="onCustomerNameChange">
                <a-select-option v-for="name in uniqueCustomerNames" :key="name" :value="name">{{ name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="项目" :labelCol="{span:6}" :wrapperCol="{span:18}">
              <a-select placeholder="全部" v-model="searchOrganId"
                showSearch allow-clear optionFilterProp="children"
                :disabled="projectListForOrgan.length === 0">
                <a-select-option v-for="item in projectListForOrgan" :key="item.id" :value="item.id">
                  {{ item.projectName || item.project_name || '(无项目名称)' }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="出库日期" :labelCol="{span:7}" :wrapperCol="{span:17}">
              <a-range-picker style="width:100%" v-model="dateRange" format="YYYY-MM-DD"
                :placeholder="['开始','结束']" @change="onDateChange" />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-button type="primary" @click="loadData(1)">查询</a-button>
            <a-button style="margin-left:8px" @click="searchReset">重置</a-button>
          </a-col>
        </a-row>
      </a-form>
    </div>
    <!-- 列设置 + 表格 -->
    <div style="margin-bottom:8px;text-align:right;">
      <column-setting-popover
        :defColumns="defColumns"
        :settingDataIndex.sync="settingDataIndex"
        @change="onColChange"
        @reset="handleResetColumns"
      />
    </div>
    <a-table
      size="small"
      bordered
      rowKey="id"
      :columns="visibleColumns"
      :dataSource="dataSource"
      :pagination="ipagination"
      :loading="loading"
      :scroll="{ x: scrollX }"
      :rowSelection="{ type: 'radio', selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      :customRow="customRow"
      @change="handleTableChange">
    </a-table>
  </a-modal>
</template>
<script>
import { listAvailableSaleOutForApproval, createPriceApprovalFromSaleOut, findBySelectCus,
  getColumnConfig, saveColumnConfig, resetColumnConfig } from '@/api/api'
import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'

export default {
  name: 'SaleOutSelectModal',
  components: { ColumnSettingPopover },
  data() {
    return {
      visible: false,
      confirmLoading: false,
      searchBillNo: '',
      selectedCustomerName: undefined,
      searchOrganId: undefined,
      dateRange: [],
      beginTime: '',
      endTime: '',
      cusList: [],
      projectListForOrgan: [],
      searchTimer: null,
      defDataIndex: ['billNo','customerName','projectName','operTimeStr','linkNumber',
                     'userName','totalPrice','totalWeight','salesMan','remark'],
      settingDataIndex: [],
      defColumns: [
        { title: '出库单号', dataIndex: 'billNo', width: 160, sorter: true },
        { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true, sorter: true },
        { title: '项目名称', dataIndex: 'projectName', width: 120, ellipsis: true },
        { title: '出库日期', dataIndex: 'operTimeStr', width: 110, sorter: true },
        { title: '关联订单', dataIndex: 'linkNumber', width: 140, ellipsis: true },
        { title: '操作员', dataIndex: 'userName', width: 80, ellipsis: true },
        { title: '金额', dataIndex: 'totalPrice', width: 100, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '-' },
        { title: '总重量(吨)', dataIndex: 'totalWeight', width: 100, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(3) : '0.000' },
        { title: '销售人员', dataIndex: 'salesMan', width: 100, ellipsis: true },
        { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true }
      ],
      dataSource: [],
      loading: false,
      selectedRowKeys: [],
      ipagination: {
        current: 1,
        pageSize: 15,
        pageSizeOptions: ['10', '15', '30', '50'],
        showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      sortField: '',
      sortOrder: ''
    }
  },
  computed: {
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
    uniqueCustomerNames() {
      let seen = new Set()
      return this.cusList.map(c => c.supplier).filter(name => {
        if (!name || seen.has(name)) return false
        seen.add(name)
        return true
      })
    }
  },
  methods: {
    show() {
      this.visible = true
      this.selectedRowKeys = []
      this.initColumnsSetting()
      this.loadData(1)
      this.initCustomer()
    },
    loadData(page) {
      if (page) this.ipagination.current = page
      this.loading = true
      listAvailableSaleOutForApproval({
        billNo: this.searchBillNo || undefined,
        organId: this.searchOrganId || undefined,
        beginTime: this.beginTime || undefined,
        endTime: this.endTime || undefined,
        currentPage: this.ipagination.current,
        pageSize: this.ipagination.pageSize
      }).then(res => {
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
              let cmp = typeof va === 'number' ? va - vb : String(va).localeCompare(String(vb))
              return asc ? cmp : -cmp
            })
          }
          this.dataSource = rows
          this.ipagination.total = res.data.total || 0
        }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    // ─── 客户选择（参照对账单模式：客户名称 + 项目级联） ───────────
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
      this.searchOrganId = undefined
      if (supplierName) {
        this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
        if (this.projectListForOrgan.length === 1) {
          this.searchOrganId = this.projectListForOrgan[0].id
        }
      } else {
        this.projectListForOrgan = []
      }
    },
    onDateChange(dates, dateStrings) {
      this.beginTime = dateStrings[0] ? dateStrings[0] + ' 00:00:00' : ''
      this.endTime = dateStrings[1] ? dateStrings[1] + ' 23:59:59' : ''
    },
    searchReset() {
      this.searchBillNo = ''
      this.selectedCustomerName = undefined
      this.searchOrganId = undefined
      this.projectListForOrgan = []
      this.dateRange = []
      this.beginTime = ''
      this.endTime = ''
      this.sortField = ''
      this.sortOrder = ''
      this.loadData(1)
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
    onSelectChange(keys) {
      this.selectedRowKeys = keys
    },
    handleOk() {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择一条出库单')
        return
      }
      this.confirmLoading = true
      createPriceApprovalFromSaleOut({ depotHeadId: this.selectedRowKeys[0] }).then(res => {
        this.confirmLoading = false
        if (res && res.code === 200) {
          this.$message.success('导入成功')
          this.visible = false
          this.$emit('ok', res.data) // res.data = approvalId
        } else {
          this.$message.error(res.data || '导入失败')
        }
      }).catch(() => { this.confirmLoading = false })
    },
    // ─── 列设置（云同步） ──────────────────────────────────────
    initColumnsSetting() {
      if (!this.settingDataIndex.length) {
        this.settingDataIndex = [...this.defDataIndex]
      }
      getColumnConfig({ pageCode: 'HJHZ_select' }).then(res => {
        if (res && res.code === 200 && res.data && res.data.columnConfig) {
          try {
            let arr = JSON.parse(res.data.columnConfig)
            if (arr && arr.length > 0) { this.settingDataIndex = arr }
          } catch (e) { /* ignore */ }
        }
      }).catch(() => {})
    },
    onColChange(orderedArr) {
      this.settingDataIndex = orderedArr
      saveColumnConfig({ pageCode: 'HJHZ_select', columnConfig: JSON.stringify(orderedArr) })
    },
    handleResetColumns() {
      this.settingDataIndex = [...this.defDataIndex]
      resetColumnConfig({ pageCode: 'HJHZ_select' })
    },
    // ─── 双击行确认导入 ─────────────────────────────────────
    customRow(record) {
      return {
        on: {
          dblclick: () => {
            this.selectedRowKeys = [record.id]
            this.handleOk()
          }
        }
      }
    },
    handleCancel() {
      this.visible = false
    }
  }
}
</script>
