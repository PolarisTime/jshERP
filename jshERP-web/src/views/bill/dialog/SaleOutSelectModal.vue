<template>
  <j-modal
    title="选择销售出库单"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :keyboard="false"
    :forceRender="true"
    fullscreen
    switchFullscreen
    :maskClosable="false"
    @cancel="handleCancel"
    style="top:20px;height:95%;">
    <template slot="footer">
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOk">确定</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <div class="table-page-search-wrapper sale-out-select-search">
        <a-form layout="inline" @keyup.enter.native="loadData(1)">
          <a-row :gutter="24">
            <a-col :md="6" :sm="24">
              <a-form-item label="出库单号" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input placeholder="请输入单号" v-model="searchBillNo" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item label="客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select placeholder="全部" v-model="selectedCustomerName"
                  showSearch allow-clear optionFilterProp="children"
                  @search="handleSearchCustomer" @change="onCustomerNameChange">
                  <div slot="dropdownRender" slot-scope="menu">
                    <v-nodes :vnodes="menu" />
                    <a-divider style="margin: 4px 0;" />
                    <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer()"><a-icon type="reload" /> 刷新列表</div>
                  </div>
                  <a-select-option v-for="name in uniqueCustomerNames" :key="name" :value="name">{{ name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item label="项目名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select placeholder="全部" v-model="searchOrganId"
                  showSearch allow-clear optionFilterProp="children"
                  :disabled="projectListForOrgan.length === 0">
                  <a-select-option v-for="item in projectListForOrgan" :key="item.id" :value="item.id">
                    {{ item.projectName || item.project_name || '(无项目名称)' }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item label="出库日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-range-picker style="width:100%" v-model="dateRange" format="YYYY-MM-DD"
                  :placeholder="['开始时间', '结束时间']" @change="onDateChange" />
              </a-form-item>
            </a-col>
            <a-col :md="24" :sm="24">
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="loadData(1)">查询</a-button>
                <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <div class="table-operator sale-out-select-operator">
        <column-setting-popover
          :defColumns="defColumns"
          :settingDataIndex.sync="settingDataIndex"
          @change="onColChange"
          @reset="handleResetColumns"
        />
      </div>
      <a-table
        size="middle"
        bordered
        rowKey="id"
        :columns="visibleColumns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :scroll="{ x: scrollX, y: tableScrollY }"
        :rowSelection="{ type: 'radio', selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        :customRow="customRow"
        @change="handleTableChange">
      </a-table>
    </a-spin>
  </j-modal>
</template>
<script>
import { listAvailableSaleOutForApproval, findBySelectCus } from '@/api/api'
import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import {
  bindColumnSettingForceSync,
  unbindColumnSettingForceSync,
  loadColumnSetting,
  saveColumnSetting,
  resetColumnSetting,
  forceSyncColumnSetting
} from '@/utils/columnSetting'

export default {
  name: 'SaleOutSelectModal',
  components: {
    ColumnSettingPopover,
    VNodes: {
      functional: true,
      render: (h, ctx) => ctx.props.vnodes,
    }
  },
  data() {
    return {
      width: '1600px',
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
      labelCol: { span: 5 },
      wrapperCol: { span: 18, offset: 1 },
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
    tableScrollY() {
      return Math.max(window.innerHeight - 360, 300)
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
  created() {
    this._forceSyncColumnSettingsHandler = () => { this.forceSyncColumnSettings() }
    bindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
  },
  beforeDestroy() {
    if (this._forceSyncColumnSettingsHandler) {
      unbindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
      this._forceSyncColumnSettingsHandler = null
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
      let record = this.dataSource.find(item => item.id === this.selectedRowKeys[0])
      if (!record) {
        this.$message.error('未找到选中的出库单')
        return
      }
      this.visible = false
      this.$emit('ok', { ...record })
    },
    // ─── 列设置（云同步） ──────────────────────────────────────
    initColumnsSetting() {
      return loadColumnSetting({
        pageCode: 'HJHZ_select',
        storageKey: 'HJHZ_select',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },
    onColChange(orderedArr) {
      this.settingDataIndex = [...orderedArr]
      return saveColumnSetting({
        pageCode: 'HJHZ_select',
        storageKey: 'HJHZ_select',
        dataIndexArr: this.settingDataIndex
      })
    },
    handleResetColumns() {
      return resetColumnSetting({
        pageCode: 'HJHZ_select',
        storageKey: 'HJHZ_select',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },
    forceSyncColumnSettings() {
      if (!this.visible) return Promise.resolve([])
      return forceSyncColumnSetting({
        pageCode: 'HJHZ_select',
        storageKey: 'HJHZ_select',
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
        }
      })
    },
    // ─── 双击行确认导入 ─────────────────────────────────────
    customRow(record) {
      return {
        on: {
          dblclick: () => {
            this.selectedRowKeys = [record.id]
            this.visible = false
            this.$emit('ok', { ...record })
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
<style scoped>
  .sale-out-select-search {
    margin-bottom: 12px;
  }

  .sale-out-select-operator {
    margin-top: 5px;
    margin-bottom: 8px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
</style>
