<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="合同编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入合同编号" v-model="queryParam.contractNo" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="合同名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入合同名称" v-model="queryParam.contractName" />
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item label="审核" :labelCol="{span:6}" :wrapperCol="{span:16}">
                  <a-select v-model="queryParam.auditStatus" allow-clear placeholder="全部">
                    <a-select-option value="0">未审核</a-select-option>
                    <a-select-option value="1">已审核</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item label="签署" :labelCol="{span:6}" :wrapperCol="{span:16}">
                  <a-select v-model="queryParam.signStatus" allow-clear placeholder="全部">
                    <a-select-option value="0">未签署</a-select-option>
                    <a-select-option value="1">已签署</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="4" :sm="24">
                <span class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left:8px" @click="searchReset">重置</a-button>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <!-- 操作按钮区域（参考销售出库样式）-->
        <div class="table-operator" style="margin-top:5px;display:flex;align-items:center;flex-wrap:wrap;gap:6px;">
          <a-button v-if="btnEnableList.indexOf(1)>-1" type="primary" icon="plus" @click="handleAdd">新增</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" icon="delete" @click="batchDel">删除</a-button>
          <a-button icon="check" :disabled="!hasSingleSelected" @click="handleAudit('1')">审核</a-button>
          <a-button icon="rollback" :disabled="!hasSingleSelected" @click="handleAudit('0')">反审核</a-button>
          <a-button icon="edit" :disabled="!hasSingleSelected" @click="handleSign('1')">签署</a-button>
          <a-button icon="close-circle" :disabled="!hasSingleSelected" @click="handleSign('0')">取消签署</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />

          <!-- CLodop -->
          <span style="margin-left:8px;display:flex;align-items:center;gap:6px;">
            <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
            <a-tag v-else color="orange" style="cursor:pointer;" @click="initClodop">CLodop未连接（点击重试）</a-tag>
            <a-select v-if="clodopReady && printTemplateList.length" v-model="selectedTemplateId"
              style="width:160px;" placeholder="选择打印模板">
              <a-select-option v-for="t in printTemplateList" :key="t.id" :value="t.id">{{ t.templateName }}</a-select-option>
            </a-select>
            <a-select v-if="clodopReady && printerList.length" v-model="selectedPrinter"
              style="width:180px;" placeholder="默认打印机">
              <a-select-option value="">默认打印机</a-select-option>
              <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
            </a-select>
            <a-button icon="eye" :disabled="!clodopReady || selectedRowKeys.length !== 1" @click="doPrint(true)">预览</a-button>
            <a-button type="primary" icon="printer" :disabled="!clodopReady || selectedRowKeys.length === 0" @click="doPrint(false)">打印</a-button>
          </span>
                </div>

        <!-- table区域 -->
        <a-table
          ref="table"
          bordered
          size="middle"
          rowKey="id"
          :columns="columns"
          :dataSource="dataSource"
          :components="handleDrag(columns)"
          :pagination="ipagination"
          :scroll="scroll"
          :loading="loading"
          :rowSelection="{selectedRowKeys, onChange: onSelectChange, type:'radio'}"
          @change="handleTableChange">
          <span slot="action" slot-scope="text, record">
            <a @click="handleEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a @click="handleUpload(record)">附件</a>
            <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical"/>
            <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => handleDelete(record.id)">
              <a>删除</a>
            </a-popconfirm>
          </span>
          <template slot="auditRender" slot-scope="text, record">
            <a-tag :color="record.auditStatus === '1' ? 'green' : 'red'">
              {{ record.auditStatus === '1' ? '已审核' : '未审核' }}
            </a-tag>
          </template>
          <template slot="signRender" slot-scope="text, record">
            <a-tag :color="record.signStatus === '1' ? 'blue' : 'orange'">
              {{ record.signStatus === '1' ? '已签署' : '未签署' }}
            </a-tag>
          </template>
        </a-table>

        <!-- 汇总统计 -->
        <div style="margin-top:10px;padding:8px 16px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
          <span>合同总价：<b style="color:red">{{ summary.totalAmount }}</b> 元</span>
          <a-divider type="vertical" />
          <span>合同吨位：<b>{{ summary.totalTonnage }}</b> 吨</span>
          <a-divider type="vertical" />
          <span>已送货金额：<b>{{ summary.deliveredAmount }}</b> 元</span>
          <a-divider type="vertical" />
          <span>剩余额度：<b style="color:#1890ff">{{ summary.remainAmount }}</b> 元</span>
          <a-divider type="vertical" />
          <span>剩余吨位：<b style="color:#1890ff">{{ summary.remainTonnage }}</b> 吨</span>
        </div>

        <contract-modal ref="modalForm" @ok="modalFormOk"></contract-modal>

        <!-- 附件上传弹窗 -->
        <a-modal title="合同附件上传" :visible="uploadVisible" :footer="null" @cancel="uploadVisible=false">
          <j-upload v-model="uploadAttachments" bizPath="contract" :billId="String(uploadContractId||'')"
            fileType="all" @change="onAttachmentChange" />
        </a-modal>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import ContractModal from './modules/ContractModal'
  import JUpload from '@/components/jeecg/JUpload'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { auditContract, signContract, updateContractAttachments } from '@/api/api'
import { ClodopMixin } from '@/mixins/ClodopMixin'

  export default {
    name: 'ContractList',
    mixins: [ClodopMixin, JeecgListMixin],
    components: { ColumnSettingPopover, ContractModal, JUpload },
    data() {
      return {
        clodopBillType: \'contract\',
        labelCol: { span: 5 },
        wrapperCol: { span: 18, offset: 1 },
        queryParam: { contractNo: '', contractName: '', auditStatus: undefined, signStatus: undefined },
        urlPath: '/contract/contract_list',
        pageName: 'contractList',
        selectedRowKeys: [],
        selectedRows: [],
        // 附件上传
        uploadVisible: false,
        uploadContractId: null,
        uploadAttachments: '',
        defDataIndex: ['rowIndex','action','contractNo','contractName','projectName','organName',
          'signDate','amount','tonnage','auditStatus','signStatus',
          'deliveredAmount','deliveredTonnage','remark'],
        defColumns: [
          { title:'#', dataIndex:'rowIndex', width:40, align:'center', customRender:(t,r,i)=>i+1 },
          { title:'操作', dataIndex:'action', width:140, align:'center', scopedSlots:{customRender:'action'} },
          { title:'合同编号', dataIndex:'contractNo', width:150 },
          { title:'合同名称', dataIndex:'contractName', width:180, ellipsis:true },
          { title:'项目名称', dataIndex:'projectName', width:140, ellipsis:true },
          { title:'客户名称', dataIndex:'organName', width:140, ellipsis:true },
          { title:'签订日期', dataIndex:'signDate', width:110, customRender:t=>t?t.substring(0,10):'' },
          { title:'合同总价(元)', dataIndex:'amount', width:120, align:'right', customRender:t=>t!=null?Number(t).toFixed(2):'' },
          { title:'合同吨位(吨)', dataIndex:'tonnage', width:110, align:'right', customRender:t=>t!=null?Number(t).toFixed(3):'' },
          { title:'审核状态', dataIndex:'auditStatus', width:90, align:'center', scopedSlots:{customRender:'auditRender'} },
          { title:'签署状态', dataIndex:'signStatus', width:90, align:'center', scopedSlots:{customRender:'signRender'} },
          { title:'已送货(元)', dataIndex:'deliveredAmount', width:110, align:'right', customRender:t=>t!=null?Number(t).toFixed(2):'' },
          { title:'已送货(吨)', dataIndex:'deliveredTonnage', width:110, align:'right', customRender:t=>t!=null?Number(t).toFixed(3):'' },
          { title:'备注', dataIndex:'remark', ellipsis:true }
        ],
        columns: [],
        summary: { totalAmount: '0.00', totalTonnage: '0.000', deliveredAmount: '0.00', remainAmount: '0.00', remainTonnage: '0.000' },
        url: { list:'/contract/list', delete:'/contract/delete', deleteBatch:'/contract/deleteBatch' }
      }
    },
    computed: {
      hasSingleSelected() { return this.selectedRowKeys.length === 1 }
    },
    watch: {
      dataSource() { this.calcSummary() }
    },
    created() {
      this.initColumnsSetting()
    },
    methods: {
      searchQuery() { this.loadData(1) },
      searchReset() {
        this.queryParam = { contractNo:'', contractName:'', auditStatus:undefined, signStatus:undefined }
        this.loadData(1)
      },
      onSelectChange(keys, rows) {
        this.selectedRowKeys = keys
        this.selectedRows = rows
        this.calcSummary()
      },
      calcSummary() {
        let rows = this.selectedRows.length > 0 ? this.selectedRows : (this.dataSource || [])
        let totalAmount = 0, totalTonnage = 0, deliveredAmount = 0, deliveredTonnage = 0
        rows.forEach(r => {
          totalAmount += parseFloat(r.amount || 0)
          totalTonnage += parseFloat(r.tonnage || 0)
          deliveredAmount += parseFloat(r.deliveredAmount || 0)
          deliveredTonnage += parseFloat(r.deliveredTonnage || 0)
        })
        this.summary = {
          totalAmount: totalAmount.toFixed(2),
          totalTonnage: totalTonnage.toFixed(3),
          deliveredAmount: deliveredAmount.toFixed(2),
          remainAmount: (totalAmount - deliveredAmount).toFixed(2),
          remainTonnage: (totalTonnage - deliveredTonnage).toFixed(3)
        }
      },
      handleAdd() { this.$refs.modalForm.add() },
      handleEdit(record) { this.$refs.modalForm.edit(record) },
      handleDelete(id) { this.handleDeleteBySingle(id) },
      handleAudit(auditStatus) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        if (auditStatus === '0' && row.signStatus === '1') {
          this.$message.warning('已签署的合同不能反审核'); return
        }
        auditContract({ id, auditStatus }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(auditStatus === '1' ? '审核成功' : '反审核成功')
            this.loadData()
          } else this.$message.error(res.data || '操作失败')
        })
      },
      handleSign(signStatus) {
        if (!this.hasSingleSelected) return
        const id = this.selectedRowKeys[0]
        const row = this.selectedRows[0]
        if (signStatus === '1' && row.auditStatus !== '1') {
          this.$message.warning('请先审核再签署'); return
        }
        signContract({ id, signStatus }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(signStatus === '1' ? '签署成功' : '已取消签署')
            this.loadData()
          } else this.$message.error(res.data || '操作失败')
        })
      },
      handleUpload(record) {
        this.uploadContractId = record.id
        this.uploadAttachments = record.attachments || ''
        this.uploadVisible = true
      },
      onAttachmentChange(val) {
        updateContractAttachments({ id: this.uploadContractId, attachments: val }).then(res => {
          if (res && res.code === 200) this.$message.success('附件已保存')
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
