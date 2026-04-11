<template>
  <a-modal :title="title" :visible="visible" :footer="null" @cancel="visible=false" :width="900">
    <a-descriptions bordered size="small" :column="2" style="margin-bottom:12px;">
      <a-descriptions-item label="对账单号">{{ header.statementNo }}</a-descriptions-item>
      <a-descriptions-item label="物流方">{{ header.carrierName }}</a-descriptions-item>
      <a-descriptions-item label="账期">{{ header.beginTimeStr }} 至 {{ header.endTimeStr }}</a-descriptions-item>
      <a-descriptions-item label="总重量(吨)">{{ header.totalWeight }}</a-descriptions-item>
      <a-descriptions-item label="总运费(元)">{{ header.totalFreight }}</a-descriptions-item>
      <a-descriptions-item label="备注">{{ header.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <a-table size="small" bordered rowKey="id" :columns="columns" :dataSource="items" :pagination="false" :scroll="{ y: 350 }">
    </a-table>
  </a-modal>
</template>

<script>
  import { getFreightStatementDetail } from '@/api/api'

  export default {
    name: 'FreightStatementDetailModal',
    data() {
      return {
        visible: false,
        title: '物流对账单详情',
        header: {},
        items: [],
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
    methods: {
      show(record) {
        this.header = record || {}
        this.items = []
        this.visible = true
        getFreightStatementDetail({ id: record.id }).then(res => {
          if (res && res.code === 200 && res.data) {
            if (res.data.header) Object.assign(this.header, res.data.header)
            this.items = res.data.items || []
          }
        })
      }
    }
  }
</script>
