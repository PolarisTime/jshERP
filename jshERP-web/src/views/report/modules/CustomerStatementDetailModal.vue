<template>
  <a-modal
    :title="'对账单详情 — ' + (model.statementNo || '')"
    :width="1300"
    :visible="visible"
    :footer="null"
    :maskClosable="false"
    @cancel="handleCancel"
    style="top:20px;">

    <!-- 头部信息 -->
    <a-descriptions bordered size="small" :column="4" style="margin-bottom:12px;">
      <a-descriptions-item label="对账单号">{{ model.statementNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ model.customerName }}</a-descriptions-item>
      <a-descriptions-item label="账期">{{ model.beginTimeStr }} ~ {{ model.endTimeStr }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ model.createTimeStr }}</a-descriptions-item>
      <a-descriptions-item label="总重量(吨)">{{ model.totalWeight }}</a-descriptions-item>
      <a-descriptions-item label="总金额(元)">{{ model.totalAmount }}</a-descriptions-item>
      <a-descriptions-item label="审核状态">
        <a-tag :color="model.status === '1' ? 'green' : 'red'">
          {{ model.status === '1' ? '已审核' : '未审核' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="签署状态">
        <a-tag :color="model.signStatus === '1' ? 'blue' : 'orange'">
          {{ model.signStatus === '1' ? '已签署' : '未签署' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="备注" :span="4">{{ model.remark }}</a-descriptions-item>
    </a-descriptions>

    <!-- 明细表格 -->
    <a-table
      size="small"
      bordered
      rowKey="id"
      :columns="columns"
      :dataSource="items"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 1200, y: 400 }">
    </a-table>

    <!-- 附件上传 -->
    <div style="margin-top:14px;">
      <span style="font-weight:500;margin-right:8px;">附件：</span>
      <j-upload v-model="attachmentValue" bizPath="bill" :billId="model.id || ''"
        @change="onAttachmentChange" />
    </div>
  </a-modal>
</template>

<script>
  import { getCustomerStatementDetail, updateStatementAttachment } from '@/api/api'
  import JUpload from '@/components/jeecg/JUpload'

  export default {
    name: 'CustomerStatementDetailModal',
    components: { JUpload },
    data() {
      return {
        visible: false,
        loading: false,
        model: {},
        items: [],
        attachmentValue: '',
        columns: [
          { title: '序号', width: 50, customRender: (t, r, i) => i + 1 },
          { title: '日期', dataIndex: 'billTime', width: 90, customRender: t => t ? t.substring(0, 10) : '' },
          { title: '出库单号', dataIndex: 'billNo', width: 150 },
          { title: '商品类别', dataIndex: 'categoryName', width: 90 },
          { title: '名称', dataIndex: 'brand', width: 120, ellipsis: true },
          { title: '材质', dataIndex: 'model', width: 80 },
          { title: '规格', dataIndex: 'standard', width: 90 },
          { title: '件重', dataIndex: 'unitWeight', width: 75, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '件数', dataIndex: 'operNumber', width: 65 },
          { title: '重量小计(吨)', dataIndex: 'itemWeight', width: 110, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 100 },
          { title: '总金额(元)', dataIndex: 'allPrice', width: 100 },
          { title: '销售单备注', dataIndex: 'billRemark', ellipsis: true }
        ]
      }
    },
    methods: {
      async show(record) {
        this.model = { ...record }
        this.visible = true
        this.loading = true
        try {
          const res = await getCustomerStatementDetail({ id: record.id })
          if (res && res.code === 200) {
            this.model = { ...record, ...(res.data.header || {}) }
            this.items = res.data.items || []
            this.attachmentValue = this.model.attachment || ''
          }
        } finally {
          this.loading = false
        }
      },
      handleCancel() {
        this.visible = false
      },
      onAttachmentChange(val) {
        updateStatementAttachment({ id: this.model.id, attachment: val }).then(res => {
          if (res && res.code === 200) this.model.attachment = val
        })
      }
    }
  }
</script>
