<template>
  <j-modal
    :title="modalTitle"
    :visible="visible"
    :confirmLoading="confirmLoading"
    fullscreen
    switchFullscreen
    @cancel="handleCancel"
    style="top:20px;height:95%;">
    <template slot="footer">
      <a-button @click="handleCancel">{{ readOnly ? '关闭' : '取消' }}</a-button>
      <a-button v-if="!readOnly" type="primary" :loading="confirmLoading" :disabled="!approvalId" @click="handleSave">保存</a-button>
      <a-button v-if="!readOnly" type="primary" style="background:#52c41a;border-color:#52c41a"
                :loading="confirmLoading" :disabled="!approvalId" @click="handleSaveAndConfirm">保存并核准</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <!-- 第一行：单据编号 | 出库单号 | 送到日期 -->
      <a-row class="form-row" :gutter="24">
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="单据编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span>{{ header.approvalNo }}</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="出库单号" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span>{{ billNo }}</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="送到日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-date-picker v-if="!readOnly" v-model="deliveryDate" format="YYYY-MM-DD" style="width:100%"/>
            <span v-else>{{ deliveryDateStr }}</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12"></a-col>
      </a-row>
      <!-- 第二行：客户 | 项目名称 | 总吨位 | 总金额 -->
      <a-row class="form-row" :gutter="24">
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span>{{ customerName }}</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="项目名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span>{{ projectName }}</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="总吨位" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span style="font-weight:bold;font-size:15px">{{ totalWeight }} 吨</span>
          </a-form-item>
        </a-col>
        <a-col :lg="6" :md="6" :sm="12">
          <a-form-item label="总金额" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <span style="font-weight:bold;font-size:15px;color:#f5222d">{{ totalAmount }} 元</span>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 操作按钮行（明细表格上方，风格同出库单） -->
      <div style="margin-bottom:8px;display:flex;align-items:center;flex-wrap:wrap;">
        <a-row v-if="!readOnly && !approvalId" :gutter="24" style="float:left;padding-bottom:5px;">
          <a-button type="primary" icon="import" @click="handleImportSaleOut">导入出库单</a-button>
        </a-row>
        <a-row v-if="!readOnly && approvalId && selectedRowKeys.length" :gutter="24" style="float:left;padding-bottom:5px;">
          <a-button icon="scissor" @click="handleSplitSelected">拆分</a-button>
        </a-row>
        <a-row v-if="!readOnly && approvalId && selectedRowKeys.length && isSelectedSplitRow" :gutter="24" style="float:left;padding-bottom:5px;padding-left:20px;">
          <a-popconfirm title="删除选中的拆分行？" @confirm="handleRemoveSelectedSplitRow">
            <a-button icon="delete" type="danger">删除拆分行</a-button>
          </a-popconfirm>
        </a-row>
        <div style="flex:1"></div>
        <column-setting-popover
          :defColumns="defColumns"
          :settingDataIndex.sync="settingDataIndex"
          @change="onColChange"
          @reset="handleResetColumns"
        />
      </div>

      <!-- 明细表格 -->
      <a-table
        size="small"
        bordered
        rowKey="rowId"
        :columns="visibleColumns"
        :dataSource="processedItems"
        :pagination="false"
        :scroll="{ y: tableScrollY }"
        :rowSelection="!readOnly && approvalId ? { selectedRowKeys: selectedRowKeys, onChange: onRowSelectChange, type: 'radio' } : null"
        @change="handleTableChange">
        <!-- 可编辑的单价列 -->
        <template slot="unitPriceSlot" slot-scope="text, record">
          <a-input-number v-if="!readOnly" :value="record.unitPrice" :precision="2" :min="0" style="width:100%"
                          @change="v => onCellChange(record, 'unitPrice', v)"/>
          <span v-else>{{ text ? Number(text).toFixed(2) : '' }}</span>
        </template>
      </a-table>

      <!-- 备注（表格下方） -->
      <a-row class="form-row" :gutter="24" style="margin-top:12px">
        <a-col :lg="24" :md="24" :sm="24">
          <a-form-item label="备注" :labelCol="{span:2}" :wrapperCol="{span:22}">
            <a-textarea v-if="!readOnly" v-model="remark" placeholder="请输入备注" :rows="2"/>
            <span v-else>{{ remark }}</span>
          </a-form-item>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 拆分弹窗 -->
    <row-split-modal ref="splitModal" @ok="onSplitOk"/>
    <!-- 出库单选择弹窗 -->
    <sale-out-select-modal ref="selectModal" @ok="onSaleOutImported"/>
  </j-modal>
</template>
<script>
import moment from 'moment'
import RowSplitModal from '../dialog/RowSplitModal'
import SaleOutSelectModal from '../dialog/SaleOutSelectModal'
import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import { getPriceApprovalDetail, savePriceApprovalItems, confirmPriceApproval,
  getColumnConfig, saveColumnConfig, resetColumnConfig } from '@/api/api'

let rowIdSeq = 1

export default {
  name: 'PriceApprovalModal',
  components: { RowSplitModal, SaleOutSelectModal, ColumnSettingPopover },
  data() {
    const mergeCell = (record) => {
      const span = record._groupRowSpan
      if (span === 0) return { style: { display: 'none' } }
      return { attrs: { rowSpan: span } }
    }
    return {
      visible: false,
      confirmLoading: false,
      readOnly: false,
      isNew: false,
      approvalId: null,
      header: {},
      billNo: '',
      customerName: '',
      projectName: '',
      deliveryDate: null,
      deliveryDateStr: '',
      remark: '',
      items: [],
      selectedRowKeys: [],
      labelCol: { span: 8 },
      wrapperCol: { span: 16 },
      // 列设置 — 默认显示列
      defDataIndex: ['name','standard','model','color','brand','operNumber','weight','unitPrice','allPrice'],
      settingDataIndex: [],
      defColumns: [
        { title: '条码', dataIndex: 'barCode', width: 140, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '名称', dataIndex: 'name', width: 120, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '规格', dataIndex: 'standard', width: 90, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '型号', dataIndex: 'model', width: 90, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '颜色', dataIndex: 'color', width: 70, sorter: true,
          customCell: mergeCell },
        { title: '品牌', dataIndex: 'brand', width: 100, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '商品类别', dataIndex: 'categoryName', width: 100, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '批号', dataIndex: 'batchNumber', width: 110, ellipsis: true, sorter: true },
        { title: 'SKU', dataIndex: 'sku', width: 100, ellipsis: true, sorter: true },
        { title: '单位', dataIndex: 'materialUnit', width: 60, sorter: true,
          customCell: mergeCell },
        { title: '件数', dataIndex: 'operNumber', width: 70, align: 'center', sorter: true,
          customCell: mergeCell,
          customRender: (text) => text ? Number(text) : '' },
        { title: '件重(吨)', dataIndex: 'unitWeight', width: 90, sorter: true,
          customCell: mergeCell,
          customRender: (text) => text ? Number(text).toFixed(4) : '' },
        { title: '重量(吨)', dataIndex: 'weight', width: 110, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(3) : '' },
        { title: '单价', dataIndex: 'unitPrice', width: 140, sorter: true,
          scopedSlots: { customRender: 'unitPriceSlot' } },
        { title: '金额', dataIndex: 'allPrice', width: 140, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '' },
        { title: '税率(%)', dataIndex: 'taxRate', width: 80, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '' },
        { title: '税额', dataIndex: 'taxMoney', width: 100, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '' },
        { title: '价税合计', dataIndex: 'taxLastMoney', width: 110, sorter: true,
          customRender: (text) => text ? Number(text).toFixed(2) : '' },
        { title: '制造商', dataIndex: 'mfrs', width: 100, ellipsis: true, sorter: true,
          customCell: mergeCell },
        { title: '仓库', dataIndex: 'depotName', width: 90, ellipsis: true, sorter: true },
        { title: '出库日期', dataIndex: 'billTimeStr', width: 100, sorter: true },
        { title: '销售人员', dataIndex: 'salesMan', width: 90, ellipsis: true, sorter: true }
      ],
      // 前端排序
      sortField: '',
      sortOrder: ''
    }
  },
  computed: {
    modalTitle() {
      if (this.readOnly) return '查看核准单'
      if (this.isNew && !this.approvalId) return '新增核准单'
      return '编辑核准单'
    },
    visibleColumns() {
      let colMap = {}
      this.defColumns.forEach(c => { colMap[c.dataIndex] = c })
      return this.settingDataIndex
        .filter(di => colMap[di])
        .map(di => colMap[di])
    },
    tableScrollY() {
      return Math.max(window.innerHeight - 420, 300)
    },
    processedItems() {
      let grouped = {}
      this.items.forEach(item => {
        let key = item.depotItemId
        if (!grouped[key]) grouped[key] = []
        grouped[key].push(item)
      })
      let result = []
      let seen = new Set()
      this.items.forEach(item => {
        let key = item.depotItemId
        if (!seen.has(key)) {
          seen.add(key)
          let group = grouped[key]
          group.forEach((g, idx) => {
            g._groupRowSpan = idx === 0 ? group.length : 0
          })
          result.push(...group)
        }
      })
      // 前端排序
      if (this.sortField) {
        let field = this.sortField
        let asc = this.sortOrder === 'ascend'
        result.sort((a, b) => {
          let va = a[field], vb = b[field]
          if (va == null) va = ''
          if (vb == null) vb = ''
          let numA = Number(va), numB = Number(vb)
          let cmp = (!isNaN(numA) && !isNaN(numB)) ? numA - numB : String(va).localeCompare(String(vb))
          return asc ? cmp : -cmp
        })
      }
      return result
    },
    totalWeight() {
      let sum = 0
      this.items.forEach(i => { sum += Number(i.weight) || 0 })
      return sum.toFixed(3)
    },
    totalAmount() {
      let sum = 0
      this.items.forEach(i => { sum += Number(i.allPrice) || 0 })
      return sum.toFixed(2)
    },
    /** 选中的行是否属于拆分组（组内行数 > 1） */
    isSelectedSplitRow() {
      if (!this.selectedRowKeys.length) return false
      let record = this.items.find(i => i.rowId === this.selectedRowKeys[0])
      if (!record) return false
      return this.items.filter(i => i.depotItemId === record.depotItemId).length > 1
    }
  },
  methods: {
    showNew() {
      this.approvalId = null
      this.isNew = true
      this.readOnly = false
      this.header = {}
      this.billNo = ''
      this.customerName = ''
      this.projectName = ''
      this.deliveryDate = null
      this.deliveryDateStr = ''
      this.remark = ''
      this.items = []
      this.selectedRowKeys = []
      this.visible = true
      this.initColumnsSetting()
    },
    show(approvalId, readOnly) {
      this.approvalId = approvalId
      this.isNew = false
      this.readOnly = readOnly || false
      this.visible = true
      this.initColumnsSetting()
      this.loadDetail()
    },
    loadDetail() {
      getPriceApprovalDetail({ id: this.approvalId }).then(res => {
        if (res && res.code === 200) {
          let data = res.data
          this.header = data.header || {}
          this.deliveryDate = this.header.deliveryDate ? moment(this.header.deliveryDate) : null
          this.deliveryDateStr = this.header.deliveryDate ? moment(this.header.deliveryDate).format('YYYY-MM-DD') : ''
          this.remark = this.header.remark || ''
          let itemList = data.items || []
          if (itemList.length > 0) {
            this.billNo = itemList[0].billNo || ''
            this.customerName = itemList[0].customerName || ''
            this.projectName = itemList[0].projectName || ''
          }
          this.items = itemList.map(item => {
            return { ...item, rowId: 'r' + (rowIdSeq++) }
          })
        }
      })
    },
    // ─── 单元格编辑（仅单价，金额自动联动） ─────────────
    onCellChange(record, field, value) {
      let item = this.items.find(i => i.rowId === record.rowId)
      if (!item) return
      if (field === 'unitPrice') {
        item.unitPrice = value
        if (item.weight) {
          item.allPrice = parseFloat((item.weight * value).toFixed(2))
        }
      }
      this.items = [...this.items]
    },
    // ─── 列设置（云同步） ──────────────────────────────────
    initColumnsSetting() {
      if (!this.settingDataIndex.length) {
        this.settingDataIndex = [...this.defDataIndex]
      }
      getColumnConfig({ pageCode: 'HJHZ_detail' }).then(res => {
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
      saveColumnConfig({ pageCode: 'HJHZ_detail', columnConfig: JSON.stringify(orderedArr) })
    },
    handleResetColumns() {
      this.settingDataIndex = [...this.defDataIndex]
      resetColumnConfig({ pageCode: 'HJHZ_detail' })
    },
    // ─── 排序 ────────────────────────────────────────────
    handleTableChange(pagination, filters, sorter) {
      if (sorter && sorter.field) {
        this.sortField = sorter.field
        this.sortOrder = sorter.order || ''
      } else {
        this.sortField = ''
        this.sortOrder = ''
      }
    },
    // ─── 行选择 ────────────────────────────────────────
    onRowSelectChange(keys) {
      this.selectedRowKeys = keys
    },
    // ─── 拆分（按钮在表格上方） ──────────────────────────
    handleSplitSelected() {
      if (!this.selectedRowKeys.length) return
      let record = this.items.find(i => i.rowId === this.selectedRowKeys[0])
      if (record) this.$refs.splitModal.show(record)
    },
    handleRemoveSelectedSplitRow() {
      if (!this.selectedRowKeys.length) return
      let record = this.items.find(i => i.rowId === this.selectedRowKeys[0])
      if (!record) return
      let group = this.items.filter(i => i.depotItemId === record.depotItemId)
      if (group.length <= 1) {
        this.$message.warning('至少保留一行')
        return
      }
      let idx = this.items.findIndex(i => i.rowId === record.rowId)
      if (idx !== -1) {
        this.items.splice(idx, 1)
        this.selectedRowKeys = []
      }
    },
    handleSplitRow(record) {
      this.$refs.splitModal.show(record)
    },
    onSplitOk({ sourceRowId, splitRows }) {
      let idx = this.items.findIndex(i => i.rowId === sourceRowId)
      if (idx === -1) return
      let source = this.items[idx]
      let newItems = splitRows.map(sr => ({
        ...source,
        rowId: 'r' + (rowIdSeq++),
        id: null,
        weight: sr.weight,
        unitPrice: source.unitPrice,
        allPrice: source.unitPrice ? parseFloat((sr.weight * source.unitPrice).toFixed(2)) : null,
        remark: ''
      }))
      let first = newItems[0]
      first.rowId = source.rowId
      first.id = source.id
      first.remark = source.remark
      this.items.splice(idx, 1, first, ...newItems.slice(1))
    },
    isSplitRow(record) {
      let group = this.items.filter(i => i.depotItemId === record.depotItemId)
      return group.length > 1
    },
    handleRemoveSplitRow(record) {
      let group = this.items.filter(i => i.depotItemId === record.depotItemId)
      if (group.length <= 1) {
        this.$message.warning('至少保留一行')
        return
      }
      let idx = this.items.findIndex(i => i.rowId === record.rowId)
      if (idx !== -1) {
        this.items.splice(idx, 1)
      }
    },
    // ─── 保存 ──────────────────────────────────────────
    buildSaveParams() {
      let groupMap = {}
      this.items.forEach(item => {
        let key = item.depotItemId
        if (!groupMap[key]) groupMap[key] = { sum: 0, orig: item.originalWeight }
        groupMap[key].sum += Number(item.weight) || 0
      })
      for (let key in groupMap) {
        let g = groupMap[key]
        if (g.orig && Math.abs(g.sum - Number(g.orig)) > 0.001) {
          this.$message.error(`拆分组重量不匹配：原始=${Number(g.orig).toFixed(3)}，当前合计=${g.sum.toFixed(3)}`)
          return null
        }
      }
      return {
        id: this.approvalId,
        deliveryDate: this.deliveryDate ? this.deliveryDate.format('YYYY-MM-DD') : '',
        remark: this.remark,
        items: JSON.stringify(this.items.map(item => ({
          depotItemId: item.depotItemId,
          materialId: item.materialId,
          materialExtendId: item.materialExtendId,
          barCode: item.barCode,
          name: item.name,
          standard: item.standard,
          model: item.model,
          color: item.color,
          brand: item.brand,
          operNumber: item.operNumber,
          weight: item.weight,
          unitPrice: item.unitPrice,
          allPrice: item.allPrice,
          taxRate: item.taxRate,
          taxMoney: item.taxMoney,
          taxLastMoney: item.taxLastMoney,
          remark: item.remark,
          originalWeight: item.originalWeight
        })))
      }
    },
    handleSave() {
      let params = this.buildSaveParams()
      if (!params) return
      this.confirmLoading = true
      savePriceApprovalItems(params).then(res => {
        this.confirmLoading = false
        if (res && res.code === 200) {
          this.$message.success('保存成功')
          this.loadDetail()
          this.$emit('ok')
        } else {
          this.$message.error(res.data || '保存失败')
        }
      }).catch(() => { this.confirmLoading = false })
    },
    handleSaveAndConfirm() {
      let params = this.buildSaveParams()
      if (!params) return
      let noPrice = this.items.find(i => !i.unitPrice || Number(i.unitPrice) === 0)
      if (noPrice) {
        this.$message.error('请为所有明细行填写单价')
        return
      }
      this.confirmLoading = true
      savePriceApprovalItems(params).then(res => {
        if (res && res.code === 200) {
          confirmPriceApproval({ id: this.approvalId }).then(res2 => {
            this.confirmLoading = false
            if (res2 && res2.code === 200) {
              this.$message.success('保存并核准成功')
              this.visible = false
              this.$emit('ok')
            } else {
              this.$message.error(res2.data || '核准失败')
            }
          }).catch(() => { this.confirmLoading = false })
        } else {
          this.confirmLoading = false
          this.$message.error(res.data || '保存失败')
        }
      }).catch(() => { this.confirmLoading = false })
    },
    // ─── 导入出库单 ────────────────────────────────────────
    handleImportSaleOut() {
      this.$refs.selectModal.show()
    },
    onSaleOutImported(approvalId) {
      this.approvalId = approvalId
      this.isNew = false
      this.loadDetail()
      this.$emit('ok')
    },
    handleCancel() {
      this.visible = false
    }
  }
}
</script>
