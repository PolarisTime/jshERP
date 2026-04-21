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
      :rowSelection="rowSelection"
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
        lockedOrganId: '',
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
    computed: {
      rowSelection() {
        return {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange,
          getCheckboxProps: (record) => ({
            disabled: !!this.lockedOrganId && this.normalizeOrganId(record.organId) !== this.lockedOrganId
          })
        }
      }
    },
    methods: {
      normalizeOrganId(organId) {
        return organId === null || organId === undefined || organId === '' ? '' : String(organId)
      },
      show(organId) {
        this.organId = organId
        this.lockedOrganId = this.normalizeOrganId(organId)
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
        if (!keys || keys.length === 0) {
          this.selectedRowKeys = []
          this.selectRows = []
          this.lockedOrganId = this.normalizeOrganId(this.organId)
          return
        }
        let baseOrganId = this.lockedOrganId || this.normalizeOrganId(this.organId) || this.normalizeOrganId(rows[0] && rows[0].organId)
        let filteredRows = rows.filter(row => this.normalizeOrganId(row.organId) === baseOrganId)
        let filteredKeys = filteredRows.map(row => row.id)
        if (filteredRows.length !== rows.length) {
          this.$message.warning('一次只能选择同一项目名称的对账单')
        }
        this.lockedOrganId = baseOrganId
        this.selectedRowKeys = filteredKeys
        this.selectRows = filteredRows
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
        this.lockedOrganId = ''
        this.visible = false
      }
    }
  }
</script>
