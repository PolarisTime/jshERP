<template>
  <a-modal
    title="新建客户对账单"
    :width="1300"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    okText="生成对账单"
    cancelText="取消"
    :okButtonProps="{ props: { disabled: selectedRowKeys.length === 0 } }"
    @ok="handleGenerate"
    @cancel="handleCancel"
    style="top: 20px;">

    <!-- 查询条件 -->
    <a-row :gutter="16" style="margin-bottom:12px;">
      <a-col :span="7">
        <a-form-item label="客户" :labelCol="{span:5}" :wrapperCol="{span:19}">
          <a-select v-model="queryParam.organId" placeholder="请选择客户"
            showSearch allow-clear optionFilterProp="children"
            @search="handleSearchCustomer" style="width:100%">
            <a-select-option v-for="item in cusList" :key="item.id" :value="item.id">{{ item.supplier }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :span="10">
        <a-form-item label="出库日期" :labelCol="{span:6}" :wrapperCol="{span:18}">
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
      <a-col :span="8" style="text-align:right;padding-top:4px;">
        <span v-if="selectedRowKeys.length > 0" style="color:#1890ff;">
          已选 {{ selectedRowKeys.length }} 行 |
          重量：{{ selectedWeight }} 吨 |
          金额：{{ selectedAmount }} 元
        </span>
      </a-col>
    </a-row>

    <!-- 未对账明细表格 -->
    <a-table
      size="small"
      bordered
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :loading="loading"
      :rowSelection="{ selectedRowKeys, onChange: onSelectChange }"
      :pagination="false"
      :scroll="{ x: 1100, y: 380 }">
    </a-table>

    <!-- 分页 -->
    <div style="margin-top:8px;text-align:right;">
      <a-pagination size="small" show-size-changer show-quick-jumper
        :current="currentPage" :pageSize="pageSize"
        :total="total" :pageSizeOptions="['20','50','100','200']"
        @change="onPageChange" @showSizeChange="onPageSizeChange"
        :show-total="t => `共 ${t} 条`" />
    </div>
  </a-modal>
</template>

<script>
  import { findBySelectCus, listUnreconciledStatementItems, generateCustomerStatement } from '@/api/api'
  import moment from 'moment'

  export default {
    name: 'CustomerStatementGenerateModal',
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
        dataSource: [],
        selectedRowKeys: [],
        selectedRows: [],
        currentPage: 1,
        pageSize: 50,
        total: 0,
        searchTimer: null,
        columns: [
          { title: '日期', dataIndex: 'billTimeStr', width: 90, customRender: (t, r) => r.billTime ? r.billTime.substring(0, 10) : '' },
          { title: '出库单号', dataIndex: 'billNo', width: 140 },
          { title: '客户', dataIndex: 'customerName', width: 110, ellipsis: true },
          { title: '商品类别', dataIndex: 'categoryName', width: 90, ellipsis: true },
          { title: '品牌', dataIndex: 'brand', width: 80 },
          { title: '材质', dataIndex: 'model', width: 80 },
          { title: '规格', dataIndex: 'standard', width: 90 },
          { title: '件重', dataIndex: 'unitWeight', width: 75, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '件数', dataIndex: 'operNumber', width: 65 },
          { title: '重量小计(吨)', dataIndex: 'itemWeight', width: 100, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 100 },
          { title: '总金额(元)', dataIndex: 'allPrice', width: 100 },
          { title: '销售单备注', dataIndex: 'billRemark', ellipsis: true }
        ]
      }
    },
    computed: {
      selectedWeight() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.itemWeight) || 0), 0).toFixed(3)
      },
      selectedAmount() {
        return this.selectedRows.reduce((sum, r) => sum + (parseFloat(r.allPrice) || 0), 0).toFixed(2)
      }
    },
    created() {
      this.initCustomer()
    },
    methods: {
      show() {
        this.visible = true
        this.selectedRowKeys = []
        this.selectedRows = []
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
      },
      resetQuery() {
        this.queryParam = {
          organId: undefined,
          dateRange: [moment().subtract(3, 'months'), moment()],
          beginTime: moment().subtract(3, 'months').format('YYYY-MM-DD'),
          endTime: moment().format('YYYY-MM-DD')
        }
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
        this.selectedRowKeys = keys
        this.selectedRows = rows
      },
      onPageChange(page) {
        this.currentPage = page
        this.loadItems()
      },
      onPageSizeChange(current, size) {
        this.pageSize = size
        this.loadItems(1)
      },
      handleGenerate() {
        if (this.selectedRowKeys.length === 0) {
          this.$message.warning('请至少勾选一条明细')
          return
        }
        this.confirmLoading = true
        const params = {
          organId: this.queryParam.organId,
          itemIds: this.selectedRowKeys,
          remark: this.remark,
          beginTime: this.queryParam.beginTime,
          endTime: this.queryParam.endTime
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
