<template>
  <j-modal
    title="新建物流对账单"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    fullscreen
    okText="生成对账单"
    cancelText="取消"
    :okButtonProps="{ props: { disabled: selectedRowKeys.length === 0 } }"
    @ok="handleGenerate"
    @cancel="handleCancel">

    <a-row :gutter="16" style="margin-bottom:12px;">
      <a-col :span="8">
        <a-form-item label="物流方" :labelCol="{span:5}" :wrapperCol="{span:19}">
          <a-select v-model="queryParam.carrierId" placeholder="请选择物流方"
            showSearch allow-clear optionFilterProp="children" style="width:100%" @change="handleCarrierChange">
            <a-select-option v-for="item in carrierList" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="10">
        <a-form-item label="单据日期" :labelCol="{span:4}" :wrapperCol="{span:20}">
          <a-range-picker v-model="queryParam.dateRange" format="YYYY-MM-DD"
            :placeholder="['开始日期','结束日期']" @change="onDateChange" style="width:100%" />
        </a-form-item>
      </a-col>
      <a-col :span="4">
        <a-button type="primary" @click="loadItems(1)">查询</a-button>
        <a-button style="margin-left:8px" @click="resetQuery">重置</a-button>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-bottom:10px;">
      <a-col :span="16">
        <a-form-item label="备注" :labelCol="{span:3}" :wrapperCol="{span:21}">
          <a-input v-model="remark" placeholder="对账单备注（可选）" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="8" style="text-align:right;padding-top:4px;">
        <span v-if="selectedRowKeys.length > 0" style="color:#1890ff;">
          已选 {{ selectedRowKeys.length }} 单 |
          重量：{{ selectedWeight }} 吨 |
          运费：{{ selectedFreight }} 元
        </span>
      </a-col>
    </a-row>

    <a-table
      size="small" bordered rowKey="id"
      :columns="columns" :dataSource="dataSource" :loading="loading"
      :rowSelection="rowSelection"
      :pagination="false" :scroll="{ x: 900, y: 400 }">
    </a-table>

    <div style="margin-top:8px;text-align:right;">
      <a-pagination size="small" show-size-changer show-quick-jumper
        :current="currentPage" :pageSize="pageSize"
        :total="total" :pageSizeOptions="['20','50','100','200']"
        @change="onPageChange" @showSizeChange="onPageSizeChange"
        :show-total="t => `共 ${t} 条`" />
    </div>
  </j-modal>
</template>

<script>
  import { selectAllFreightCarrier, listUnreconciledFreight, generateFreightStatement } from '@/api/api'
  import JModal from '@/components/jeecg/JModal'
  import moment from 'moment'

  export default {
    name: 'FreightStatementGenerateModal',
    components: { JModal },
    data() {
      return {
        visible: false,
        confirmLoading: false,
        loading: false,
        remark: '',
        queryParam: {
          carrierId: undefined,
          dateRange: [moment().subtract(3, 'months'), moment()],
          beginTime: moment().subtract(3, 'months').format('YYYY-MM-DD'),
          endTime: moment().format('YYYY-MM-DD')
        },
        carrierList: [],
        dataSource: [],
        selectedRowKeys: [],
        selectedRows: [],
        lockedCarrierId: '',
        currentPage: 1,
        pageSize: 50,
        total: 0,
        columns: [
          { title: '单据编号', dataIndex: 'billNo', width: 160 },
          { title: '日期', dataIndex: 'billTime', width: 100 },
          { title: '物流方', dataIndex: 'carrierName', width: 130 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 100, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 100 },
          { title: '总运费(元)', dataIndex: 'totalFreight', width: 110, customRender: t => t != null ? Number(t).toFixed(2) : '' },
          { title: '备注', dataIndex: 'remark', ellipsis: true }
        ]
      }
    },
    computed: {
      rowSelection() {
        return {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange,
          getCheckboxProps: (record) => ({
            disabled: !!this.lockedCarrierId && this.normalizeCarrierId(record.carrierId) !== this.lockedCarrierId
          })
        }
      },
      selectedWeight() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.totalWeight) || 0), 0).toFixed(3)
      },
      selectedFreight() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.totalFreight) || 0), 0).toFixed(2)
      }
    },
    created() {
      this.initCarrier()
    },
    methods: {
      normalizeCarrierId(carrierId) {
        return carrierId === null || carrierId === undefined || carrierId === '' ? '' : String(carrierId)
      },
      resetSelection(carrierId) {
        this.selectedRowKeys = []
        this.selectedRows = []
        this.lockedCarrierId = this.normalizeCarrierId(carrierId)
      },
      show() {
        this.visible = true
        this.resetSelection(this.queryParam.carrierId)
        this.remark = ''
        this.loadItems(1)
      },
      handleCancel() { this.visible = false },
      initCarrier() {
        selectAllFreightCarrier().then(res => {
          if (res && res.code === 200) this.carrierList = res.data || []
        })
      },
      loadItems(page) {
        if (page) this.currentPage = page
        this.loading = true
        listUnreconciledFreight({
          carrierId: this.queryParam.carrierId,
          beginTime: this.queryParam.beginTime,
          endTime: this.queryParam.endTime,
          currentPage: this.currentPage,
          pageSize: this.pageSize
        }).then(res => {
          if (res && res.code === 200) {
            this.dataSource = res.data.rows || []
            this.total = res.data.total || 0
          }
        }).finally(() => { this.loading = false })
      },
      handleCarrierChange(value) {
        this.resetSelection(value)
        this.loadItems(1)
      },
      resetQuery() {
        this.queryParam = {
          carrierId: undefined,
          dateRange: [moment().subtract(3, 'months'), moment()],
          beginTime: moment().subtract(3, 'months').format('YYYY-MM-DD'),
          endTime: moment().format('YYYY-MM-DD')
        }
        this.resetSelection('')
        this.loadItems(1)
      },
      onDateChange(dates, dateStrings) {
        this.queryParam.beginTime = dateStrings[0] || ''
        this.queryParam.endTime = dateStrings[1] || ''
        this.resetSelection(this.queryParam.carrierId)
      },
      onSelectChange(keys, rows) {
        if (!keys || keys.length === 0) {
          this.resetSelection(this.queryParam.carrierId)
          return
        }
        let baseCarrierId = this.lockedCarrierId || this.normalizeCarrierId(this.queryParam.carrierId) || this.normalizeCarrierId(rows[0] && rows[0].carrierId)
        let filteredRows = rows.filter(row => this.normalizeCarrierId(row.carrierId) === baseCarrierId)
        let filteredKeys = filteredRows.map(row => row.id)
        if (filteredRows.length !== rows.length) {
          this.$message.warning('一次只能选择同一物流方的单据')
        }
        this.lockedCarrierId = baseCarrierId
        this.selectedRowKeys = filteredKeys
        this.selectedRows = filteredRows
      },
      onPageChange(page) { this.currentPage = page; this.loadItems() },
      onPageSizeChange(cur, size) { this.pageSize = size; this.loadItems(1) },
      handleGenerate() {
        if (this.selectedRowKeys.length === 0) {
          this.$message.warning('请至少勾选一条物流单')
          return
        }
        let dates = this.selectedRows.map(r => r.billTime || '').filter(d => d).sort()
        let actualBegin = dates.length > 0 ? dates[0] : this.queryParam.beginTime
        let actualEnd = dates.length > 0 ? dates[dates.length - 1] : this.queryParam.endTime
        // 确定物流方（取第一条的carrierId，或从查询条件取）
        let carrierId = this.queryParam.carrierId
        if (!carrierId && this.selectedRows.length > 0) {
          carrierId = this.selectedRows[0].carrierId
        }
        this.confirmLoading = true
        generateFreightStatement({
          carrierId: carrierId,
          itemIds: this.selectedRowKeys,
          remark: this.remark,
          beginTime: actualBegin,
          endTime: actualEnd
        }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('物流对账单生成成功')
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
