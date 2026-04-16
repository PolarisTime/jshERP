<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="loadData(1)">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="物流方" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="全部" showSearch allow-clear optionFilterProp="children" v-model="queryParam.carrierId" @change="loadData(1)">
                    <a-select-option v-for="item in carrierList" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="审核状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="全部" allow-clear v-model="queryParam.status">
                    <a-select-option value="0">未审核</a-select-option>
                    <a-select-option value="1">已审核</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-button type="primary" @click="loadData(1)">查询</a-button>
                <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区 -->
        <div class="table-operator" style="margin-top:5px;display:flex;align-items:center;flex-wrap:wrap;gap:6px;">
          <a-button type="primary" icon="plus" @click="handleNew">新建物流对账单</a-button>
          <a-button icon="check" @click="handleAudit('1')" :disabled="!hasSingleSelected">审核</a-button>
          <a-button icon="rollback" @click="handleAudit('0')" :disabled="!hasSingleSelected">反审核</a-button>
          <a-button icon="edit" @click="handleSign('1')" :disabled="!hasSingleSelected">签署</a-button>
          <a-button icon="close-circle" @click="handleSign('0')" :disabled="!hasSingleSelected">取消签署</a-button>
          <a-button icon="printer" @click="handleCustomPrint(false)" :disabled="!hasSingleSelected">打印</a-button>
          <a-button icon="printer" @click="handleCustomPrint(true)" :disabled="!hasSingleSelected">打印(含明细)</a-button>
          <a-button icon="download" @click="handleExportCsv">导出</a-button>
          <a-button icon="download" @click="handleExportDetailCsv" :disabled="!hasSingleSelected">导出(含明细)</a-button>
        </div>
        <!-- 对账单列表 -->
        <a-table
          bordered size="middle" rowKey="id"
          :columns="columns" :dataSource="dataSource"
          :components="handleDrag(columns)"
          :loading="loading" :pagination="false" :scroll="scroll"
          :rowSelection="{ selectedRowKeys, onChange: onSelectChange, type: 'radio' }">
          <span slot="action" slot-scope="text, record">
            <a @click="handleDetail(record)">查看</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
              <a style="color:#ff4d4f">删除</a>
            </a-popconfirm>
            <a-divider type="vertical" />
            <a @click="$refs.attachModal.show(record, 'attachment')" style="white-space:nowrap">
              <a-icon type="paper-clip" /> 附件
              <a-badge v-if="record.attachment" :count="record.attachment.split(',').filter(f=>f).length" :numberStyle="{fontSize:'10px',minWidth:'16px',height:'16px',lineHeight:'16px'}" />
              <a-icon v-else type="close-circle" style="color:#ccc;font-size:12px" />
            </a>
          </span>
          <template slot="statusRender" slot-scope="text, record">
            <a-tag :color="String(record.status)==='1' ? 'green' : 'red'">
              {{ String(record.status)==='1' ? '已审核' : '未审核' }}
            </a-tag>
          </template>
          <template slot="signRender" slot-scope="text, record">
            <a-tag :color="String(record.signStatus)==='1' ? 'blue' : 'orange'">
              {{ String(record.signStatus)==='1' ? '已签署' : '未签署' }}
            </a-tag>
          </template>
        </a-table>
        <a-row style="margin-top:8px;text-align:right;">
          <a-pagination size="small" show-size-changer show-quick-jumper
            :current="ipagination.current" :pageSize="ipagination.pageSize"
            :total="ipagination.total" :pageSizeOptions="['10','20','50','100']"
            @change="paginationChange" @showSizeChange="paginationShowSizeChange"
            :show-total="t => `共 ${t} 条`" />
        </a-row>

        <freight-statement-generate-modal ref="generateModal" @ok="loadData(1)" />
        <freight-statement-detail-modal ref="detailModal" />
        <attachment-modal ref="attachModal" bizPath="freightStatement" @change="onAttachChange"></attachment-modal>
        <custom-print-modal ref="customPrintModal" billType="freightStatement" :model="printModel" :dataSource="printDataSource" />
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier, listFreightStatements, getFreightStatementDetail,
           auditFreightStatement, signFreightStatement, deleteFreightStatement,
           updateFreightStatementAttachment } from '@/api/api'
  import { putAction } from '@/api/manage'
  import FreightStatementGenerateModal from './modules/FreightStatementGenerateModal'
  import FreightStatementDetailModal from './modules/FreightStatementDetailModal'
  import AttachmentModal from '@/components/tools/AttachmentModal'
  import CustomPrintModal from '../bill/dialog/CustomPrintModal'

  export default {
    name: "FreightReport",
    mixins: [JeecgListMixin],
    components: { FreightStatementGenerateModal, FreightStatementDetailModal, AttachmentModal, CustomPrintModal },
    data() {
      return {
        labelCol: { span: 5 },
        wrapperCol: { span: 18, offset: 1 },
        queryParam: { carrierId: undefined, status: undefined, signStatus: undefined },
        carrierList: [],
        selectedRowKeys: [],
        selectedRows: [],
        columns: [
          { title: '操作', dataIndex: 'action', width: 180, align: 'center', scopedSlots: { customRender: 'action' } },
          { title: '对账单号', dataIndex: 'statementNo', width: 170 },
          { title: '物流方', dataIndex: 'carrierName', width: 150 },
          { title: '账期开始', dataIndex: 'beginTimeStr', width: 110 },
          { title: '账期结束', dataIndex: 'endTimeStr', width: 110 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110, customRender: t => t != null ? Number(t).toFixed(3) : '' },
          { title: '总运费(元)', dataIndex: 'totalFreight', width: 110, customRender: t => t != null ? Number(t).toFixed(2) : '' },
          { title: '已付金额', dataIndex: 'paidAmount', width: 100, customRender: t => t != null ? Number(t).toFixed(2) : '0.00' },
          { title: '审核状态', dataIndex: 'status', width: 90, align: 'center', scopedSlots: { customRender: 'statusRender' } },
          { title: '签署状态', dataIndex: 'signStatus', width: 90, align: 'center', scopedSlots: { customRender: 'signRender' } },
          { title: '创建时间', dataIndex: 'createTimeStr', width: 160 },
          { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true }
        ],
        printModel: {},
        printDataSource: [],
        url: { list: '/freightStatement/list' }
      }
    },
    computed: {
      hasSingleSelected() { return this.selectedRowKeys.length === 1 }
    },
    created() {
      this.initCarrier()
    },
    methods: {
      initCarrier() {
        selectAllFreightCarrier().then(res => {
          if (res && res.code === 200) this.carrierList = res.data || []
        })
      },
      onSelectChange(keys, rows) {
        this.selectedRowKeys = keys
        this.selectedRows = rows
      },
      searchReset() {
        this.queryParam = { carrierId: undefined, status: undefined, signStatus: undefined }
        this.loadData(1)
      },
      handleNew() { this.$refs.generateModal.show() },
      handleDetail(record) {
        this.$refs.detailModal.show(record)
      },
      handleDelete(record) {
        deleteFreightStatement({ id: record.id }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('删除成功')
            this.loadData()
          } else this.$message.error(res.data || '删除失败')
        })
      },
      handleAudit(status) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        auditFreightStatement({ id, status }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(status === '1' ? '审核成功' : '已反审核')
            this.loadData()
          } else this.$message.error(res.data || '操作失败')
        })
      },
      handleSign(signStatus) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        if (signStatus === '1' && String(row.status || '0') !== '1') {
          this.$message.warning('请先审核再签署')
          return
        }
        signFreightStatement({ id, signStatus }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(signStatus === '1' ? '签署成功' : '已取消签署')
            this.loadData()
          } else this.$message.error(res.data || '操作失败')
        })
      },
      // ─── 打印 ──────────────────────────────────────────────────
      async handleCustomPrint(withDetail) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        const res = await getFreightStatementDetail({ id })
        if (!res || res.code !== 200) { this.$message.error('获取详情失败'); return }
        const items = res.data.items || []
        const processedItems = items.map((it, idx) => {
          let subs = it.subItems || []
          // 从子明细中汇总项目名称（去重，兼容 projectName 和 project_name）
          let projects = [...new Set(subs.map(s => s.projectName || s.project_name).filter(p => p))]
          return {
            ...it,
            _index: idx + 1,
            projectName: projects.join('、'),
            _subItemsJson: JSON.stringify(subs)
          }
        })
        const model = {
          ...row,
          ...(res.data.header || {}),
          withDetail: withDetail,
          details: processedItems
        }
        this.printModel = model
        this.printDataSource = processedItems
        this.$nextTick(() => { this.$refs.customPrintModal.show(model, processedItems) })
      },
      // ─── 导出 ──────────────────────────────────────────────────
      handleExportCsv() {
        let rows = this.dataSource || []
        if (this.selectedRowKeys.length > 0) {
          const keySet = new Set(this.selectedRowKeys.map(String))
          rows = rows.filter(r => keySet.has(String(r.id)))
        }
        if (rows.length === 0) { this.$message.warning('无数据可导出'); return }
        const headers = ['对账单号','物流方','账期开始','账期结束','总重量(吨)','总运费(元)','已付金额','审核状态','签署状态','创建时间']
        const keys = ['statementNo','carrierName','beginTimeStr','endTimeStr','totalWeight','totalFreight','paidAmount','status','signStatus','createTimeStr']
        let csv = '\uFEFF' + headers.join(',') + '\n'
        rows.forEach(r => {
          csv += keys.map(k => {
            let v = r[k] != null ? String(r[k]) : ''
            if (k === 'status') v = v === '1' ? '已审核' : '未审核'
            if (k === 'signStatus') v = v === '1' ? '已签署' : '未签署'
            return '"' + v.replace(/"/g, '""') + '"'
          }).join(',') + '\n'
        })
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = '物流对账.csv'
        link.click()
      },
      async handleExportDetailCsv() {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        const res = await getFreightStatementDetail({ id })
        if (!res || res.code !== 200) { this.$message.error('获取详情失败'); return }
        const items = res.data.items || []
        const headers = ['对账单号','物流方','账期','物流单号','日期','总重量(吨)','单价(元/吨)','运费(元)','备注']
        let csv = '\uFEFF' + headers.join(',') + '\n'
        const sno = row.statementNo || ''
        const carrier = row.carrierName || ''
        const period = (row.beginTimeStr || '') + '~' + (row.endTimeStr || '')
        items.forEach(it => {
          let vals = [sno, carrier, period, it.billNo || '', it.billTime || '',
            it.totalWeight || '', it.unitPrice || '', it.totalFreight || '', it.remark || '']
          csv += vals.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',') + '\n'
        })
        csv += '"","","","","","' + (row.totalWeight || '') + '","","' + (row.totalFreight || '') + '",""\n'
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = (sno || '物流对账单') + '.csv'
        link.click()
      },
      onAttachChange({ id, attachments }) {
        updateFreightStatementAttachment({ id, attachment: attachments }).then(res => {
          if (res && res.code === 200) this.$message.success('附件已保存')
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
