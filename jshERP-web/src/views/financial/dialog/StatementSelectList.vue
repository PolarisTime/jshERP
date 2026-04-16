<template>
  <a-modal
    title="请选择对账单"
    :width="900"
    :visible="visible"
    :maskClosable="false"
    @ok="handleOk"
    @cancel="handleCancel"
    style="top: 30px;">
    <a-table
      size="small"
      bordered
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :loading="loading"
      :rowSelection="{ selectedRowKeys, onChange: onSelectChange }"
      :pagination="false"
      :scroll="{ y: 400 }">
    </a-table>
  </a-modal>
</template>

<script>
  import { listUnpaidStatements } from '@/api/api'

  export default {
    name: 'StatementSelectList',
    data() {
      return {
        visible: false,
        loading: false,
        organId: null,
        dataSource: [],
        selectedRowKeys: [],
        selectRows: [],
        columns: [
          { title: '对账单号', dataIndex: 'statementNo', width: 140 },
          { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true },
          { title: '项目名称', dataIndex: 'projectName', width: 180, ellipsis: true },
          { title: '账期', dataIndex: 'period', width: 180,
            customRender: (t, r) => (r.beginTime || '') + ' ~ ' + (r.endTime || '')
          },
          { title: '总金额', dataIndex: 'totalAmount', width: 100, align: 'right',
            customRender: t => t != null ? Number(t).toFixed(2) : ''
          },
          { title: '已收金额', dataIndex: 'receivedAmount', width: 100, align: 'right',
            customRender: t => t != null ? Number(t).toFixed(2) : '0.00'
          },
          { title: '待收金额', dataIndex: 'unpaidAmount', width: 100, align: 'right',
            customRender: t => t != null ? Number(t).toFixed(2) : ''
          }
        ]
      }
    },
    methods: {
      show(organId) {
        this.organId = organId
        this.visible = true
        this.selectedRowKeys = []
        this.selectRows = []
        this.loadData()
      },
      loadData() {
        this.loading = true
        let params = {}
        if (this.organId) params.organId = this.organId
        listUnpaidStatements(params).then(res => {
          if (res && res.code === 200) {
            this.dataSource = res.data || []
          }
        }).finally(() => { this.loading = false })
      },
      onSelectChange(keys, rows) {
        this.selectedRowKeys = keys
        this.selectRows = rows
      },
      handleOk() {
        if (this.selectRows.length === 0) {
          this.$message.warning('请选择对账单')
          return
        }
        this.$emit('ok', this.selectRows)
        this.visible = false
      },
      handleCancel() {
        this.visible = false
      }
    }
  }
</script>
