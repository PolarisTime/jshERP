<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :keyboard="false"
    :forceRender="true"
    v-bind:prefixNo="prefixNo"
    fullscreen
    switchHelp
    switchFullscreen
    @cancel="handleCancel"
    :id="prefixNo"
    style="top:20px;height: 95%;">
    <template slot="footer">
      <a-button @click="handleCancel">取消</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro('销售出库')">三联打印-新版</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint('销售出库')">三联打印</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">保存并审核</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">保存（Ctrl+S）</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">提交流程</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="客户名称" data-step="1" data-title="客户名称"
                         data-intro="先选择客户公司名称，再在项目名称中选择具体项目">
              <a-select placeholder="请选择客户名称" v-model="selectedCustomerName" :disabled="!rowCanEdit"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" @change="handleCustomerNameChange" @search="handleSearchCustomer">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.customer" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addCustomer"><a-icon type="plus" /> 新增客户</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer(0)"><a-icon type="reload" /> 刷新列表</div>
                </div>
                <a-select-option v-for="name in uniqueCustomerNames" :key="name" :value="name">
                  {{ name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="单据日期">
              <j-date v-decorator="['operTime', validatorRules.operTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="单据编号" data-step="2" data-title="单据编号"
                         data-intro="单据编号自动生成、自动累加、开头是单据类型的首字母缩写，累加的规则是每次打开页面会自动占用一个新的编号">
              <a-input placeholder="请输入单据编号" v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="12" :md="24" :sm="24">
            <a-form-item :labelCol="{span:4}" :wrapperCol="{span:20}" label="项目名称">
              <a-select placeholder="请先选择客户名称" v-decorator="[ 'organId', validatorRules.organId ]" :disabled="!rowCanEdit || projectListForOrgan.length === 0"
                style="width:100%"
                :dropdownMatchSelectWidth="false"
                :dropdownStyle="{ minWidth: '520px' }"
                dropdownClassName="sale-out-project-dropdown"
                showSearch optionFilterProp="children" @change="handleOrganChange">
                <a-select-option v-for="item in projectListForOrgan" :key="item.id" :value="item.id">
                  <span class="project-option-label" :title="item.projectName || '(无项目名称)'">
                    {{ item.projectName || '(无项目名称)' }}
                  </span>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="12" :md="24" :sm="24">
            <a-form-item :labelCol="{span:4}" :wrapperCol="{span:20}" label="合同状态">
              <div style="border:1px solid #d9d9d9;border-radius:4px;padding:4px 11px;min-height:32px;line-height:24px;font-size:14px;background:#fff;">
                <template v-if="contractBalance">
                  <template v-if="!contractBalance.hasContract">
                    <span style="color:red">金额超限：{{ Math.abs(contractBalance.remainAmount) }} 元&nbsp;</span>
                    <span style="color:red">吨位超限：{{ Math.abs(contractBalance.remainTonnage) }} 吨&nbsp;</span>
                    <span style="color:#333">未签署合同</span>
                  </template>
                  <template v-else-if="contractBalance.remainAmount < 0 || contractBalance.remainTonnage < 0">
                    <span v-if="contractBalance.remainAmount < 0" style="color:red">金额超限：{{ Math.abs(contractBalance.remainAmount) }} 元&nbsp;</span>
                    <span v-else>剩余金额：<b style="color:#1890ff">{{ contractBalance.remainAmount }}</b> 元&nbsp;</span>
                    <span v-if="contractBalance.remainTonnage < 0" style="color:red">吨位超限：{{ Math.abs(contractBalance.remainTonnage) }} 吨&nbsp;</span>
                    <span v-else>剩余吨位：<b style="color:#1890ff">{{ contractBalance.remainTonnage }}</b> 吨&nbsp;</span>
                    <a-tag color="orange" style="margin-left:4px">已签署合同</a-tag>
                  </template>
                  <template v-else>
                    剩余金额：<b style="color:#1890ff">{{ contractBalance.remainAmount }}</b> 元&nbsp;
                    剩余吨位：<b style="color:#1890ff">{{ contractBalance.remainTonnage }}</b> 吨&nbsp;
                    <a-tag color="green" style="margin-left:4px">已签署合同</a-tag>
                  </template>
                </template>
                <span v-else style="color:#999">请先选择客户</span>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <div class="sale-out-detail-operator">
          <column-setting-popover
            :defColumns="detailDefColumns"
            :settingDataIndex.sync="detailSettingDataIndex"
            @change="onDetailColChange"
            @reset="handleDetailRestDefault"
          />
        </div>
        <j-editable-table id="billModal"
          :ref="refKeys[0]"
          :loading="materialTable.loading"
          :columns="materialTable.columns"
          :dataSource="materialTable.dataSource"
          :minWidth="minWidth"
          :maxHeight="300"
          :rowNumber="false"
          :rowSelection="true"
          :actionButton="rowCanEdit"
          :actionDeleteButton="!rowCanEdit"
          :dragSortAndNumber="true"
          @valueChange="onValueChange"
          @added="onAdded"
          @deleted="onDeleted">
          <template #buttonAfter>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom:5px;padding-right:8px" data-step="4" data-title="扫码录入" data-intro="此功能支持扫码枪扫描商品条码进行录入">
              <a-col v-if="scanStatus" :md="6" :sm="24">
                <a-button @click="scanEnter">扫码录入</a-button>
              </a-col>
              <a-col v-if="!scanStatus" :md="16" :sm="24" style="padding: 0 8px 0 12px">
                <a-input placeholder="请扫条码或序列号并回车" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
              </a-col>
              <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px 12px 0 0">
                <a-button @click="stopScan">收起扫码</a-button>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="float:left;padding-bottom: 5px;">
              <a-col :md="24" :sm="24">
                <a-button @click="handleHistoryBillList"><a-icon type="history" />历史单据</a-button>
              </a-col>
            </a-row>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom: 5px;padding-left:20px;">
              <a-button icon="link" @click="onSearchLinkNumber">关联采购入库</a-button>
            </a-row>
          </template>
          <template #depotBatchSet>
            <a-icon type="down" @click="handleBatchSetDepot" />
          </template>
          <template #depotAdd>
            <a-divider v-if="quickBtn.depot" style="margin: 4px 0;" />
            <div v-if="quickBtn.depot" class="dropdown-btn" @click="addDepot"><a-icon type="plus" /> 新增</div>
            <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initDepot"><a-icon type="reload" /> 刷新</div>
          </template>
        </j-editable-table>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="24" :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="">
              <a-textarea :rows="1" placeholder="请输入备注" v-decorator="[ 'remark' ]" style="margin-top:8px;"/>
            </a-form-item>
          </a-col>
        </a-row>
        <div style="display:none;">
          <a-input v-decorator.trim="[ 'linkNumber' ]" />
          <a-input v-decorator.trim="[ 'discount' ]" />
          <a-input v-decorator.trim="[ 'discountMoney' ]" />
          <a-input v-decorator.trim="[ 'discountLastMoney' ]" />
          <a-input v-decorator.trim="[ 'otherMoney' ]" />
        </div>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="结算账户" data-step="5" data-title="结算账户"
                         data-intro="如果在下拉框中选择多账户，则可以通过多个结算账户进行结算">
              <a-select style="width:80%;" placeholder="请选择结算账户" v-decorator="[ 'accountId', validatorRules.accountId ]"
                        :dropdownMatchSelectWidth="false" allowClear @select="selectAccount">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> 新增</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount(0)"><a-icon type="reload" /> 刷新</div>
                </div>
                <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
              <a-tooltip title="多账户明细">
                <a-button type="default" icon="folder" style="margin-left: 8px;" size="small" v-show="manyAccountBtnStatus" @click="handleManyAccount"/>
              </a-tooltip>
            </a-form-item>
          </a-col>
          <a-col v-if="depositStatus" :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="扣除订金">
              <a-input v-decorator.trim="[ 'deposit' ]" @change="onChangeDeposit"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="本次收款">
              <a-input placeholder="请输入本次收款" v-decorator.trim="[ 'changeAmount', validatorRules.changeAmount ]" @change="onChangeChangeAmount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="本次欠款" data-step="6" data-title="本次欠款"
                         data-intro="欠款产生的费用，后续可以在收款单进行收取">
              <a-input placeholder="请输入本次欠款" v-decorator.trim="[ 'debt', validatorRules.price ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="附件" data-step="7" data-title="附件"
                         data-intro="可以上传与单据相关的图片、文档，支持多个文件">
              <j-upload v-model="fileList" bizPath="bill" :billId="model.id || ''"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <many-account-modal ref="manyAccountModalForm" @ok="manyAccountModalFormOk"></many-account-modal>
    <link-bill-list ref="linkBillList" @ok="linkBillListOk"></link-bill-list>
    <customer-modal ref="customerModalForm" @ok="customerModalFormOk"></customer-modal>
    <depot-modal ref="depotModalForm" @ok="depotModalFormOk"></depot-modal>
    <account-modal ref="accountModalForm" @ok="accountModalFormOk"></account-modal>
    <batch-set-depot ref="batchSetDepotModalForm" @ok="batchSetDepotModalFormOk"></batch-set-depot>
    <history-bill-list ref="historyBillListModalForm"></history-bill-list>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
    <bill-print-iframe ref="modalPrint"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalPrintPro"></bill-print-pro-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import ManyAccountModal from '../dialog/ManyAccountModal'
  import LinkBillList from '../dialog/LinkBillList'
  import CustomerModal from '../../system/modules/CustomerModal'
  import DepotModal from '../../system/modules/DepotModal'
  import AccountModal from '../../system/modules/AccountModal'
  import BatchSetDepot from '../dialog/BatchSetDepot'
  import HistoryBillList from '../dialog/HistoryBillList'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort,handleIntroJs } from "@/utils/util"
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  import { findBySelectCus, getContractBalance } from '@/api/api'
  import { loadColumnSetting, saveColumnSetting, resetColumnSetting } from '@/utils/columnSetting'
  export default {
    name: "SaleOutModal",
    mixins: [JEditableTableMixin, BillModalMixin],
    components: {
      ManyAccountModal,
      LinkBillList,
      CustomerModal,
      DepotModal,
      AccountModal,
      BatchSetDepot,
      HistoryBillList,
      WorkflowIframe,
      BillPrintIframe,
      BillPrintProIframe,
      ColumnSettingPopover,
      JUpload,
      JDate,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title:"操作",
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        operTimeStr: '',
        prefixNo: 'XSCK',
        detailColumnSettingManagedBySelf: true,
        detailColumnStorageKey: 'XSCK_MODAL_DETAIL_COLUMNS',
        detailColumnPageCode: 'XSCK_MODAL_DETAIL',
        depositStatus: false,
        contractBalance: null,  // 当前客户的合同余额信息
        fileList:[],
        detailAllColumns: [],
        detailBaseTypeMap: {},
        detailDefaultDataIndex: [],
        detailSettingDataIndex: [],
        rowCanEdit: true,
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        refKeys: ['materialDataTable', ],
        activeKey: 'materialDataTable',
        materialTable: {
          loading: false,
          dataSource: [],
          columns: [
            { title: '仓库名称', key: 'depotId', width: '6%', type: FormTypes.select, placeholder: '请选择${title}', options: [],
              allowSearch:true, validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: '条码', key: 'barCode', width: '6%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: '名称', key: 'name', width: '6%', type: FormTypes.normal },
            { title: '规格', key: 'standard', width: '6%', type: FormTypes.normal },
            { title: '材质', key: 'model', width: '6%', type: FormTypes.normal },
            { title: '颜色', key: 'color', width: '5%', type: FormTypes.normal },
            { title: '品牌', key: 'brand', width: '6%', type: FormTypes.normal },
            { title: '制造商', key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: '长度', key: 'otherField1', width: '6%', type: FormTypes.normal },
            { title: '扩展2', key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: '扩展3', key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: '单位', key: 'unit', width: '4%', type: FormTypes.normal },
            { title: '序列号', key: 'snList', width: '12%', type: FormTypes.popupJsh, kind: 'sn', multi: true },
            { title: '批号', key: 'batchNumber', width: '7%', type: FormTypes.popupJsh, kind: 'batch', multi: false },
            { title: '有效期', key: 'expirationDate',width: '7%', type: FormTypes.input, readonly: true },
            { title: '多属性', key: 'sku', width: '9%', type: FormTypes.normal },
            { title: '原数量', key: 'preNumber', width: '4%', type: FormTypes.normal },
            { title: '已出库', key: 'finishNumber', width: '4%', type: FormTypes.normal },
            { title: '数量', key: 'operNumber', width: '4%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: '重量', key: 'weight', width: '4%', type: FormTypes.inputNumber, statistics: true, statisticsDecimals: 3,
              readonly: (row) => { return row.weightEditable !== '1' && row.weightEditable !== 1 }
            },
            { title: '单价', key: 'unitPrice', width: '4%', type: FormTypes.inputNumber},
            { title: '金额', key: 'allPrice', width: '5%', type: FormTypes.inputNumber },
            { title: '税率', key: 'taxRate', width: '4%', type: FormTypes.input,placeholder: '%'},
            { title: '税额', key: 'taxMoney', width: '5%', type: FormTypes.normal },
            { title: '价税合计', key: 'taxLastMoney', width: '7%', type: FormTypes.inputNumber },
            { title: '库存', key: 'stock', width: '5%', type: FormTypes.normal },
            { title: '备注', key: 'remark', width: '6%', type: FormTypes.input },
            { title: '关联id', key: 'linkId', width: '5%', type: FormTypes.normal },
          ]
        },
        confirmLoading: false,
        validatorRules:{
          operTime:{
            rules: [
              { required: true, message: '请输入单据日期！' }
            ]
          },
          number:{
            rules: [
              { required: true, message: '请输入单据编号!' }
            ]
          },
          organId:{
            rules: [
              { required: true, message: '请选择客户！' }
            ]
          },
          accountId:{
            rules: [
              { required: true, message: '请选择结算账户！' }
            ]
          },
          changeAmount:{
            rules: [
              { required: true, message: '请输入金额，如果为空请填0！' },
              { pattern: /^(([0-9][0-9]*)|([0]\.\d{0,4}|[0-9][0-9]*\.\d{0,4}))$/, message: '金额格式不正确!' }
            ]
          }
        },
        url: {
          add: '/depotHead/addDepotHeadAndDetail',
          edit: '/depotHead/updateDepotHeadAndDetail',
          detailList: '/depotItem/getDetailList'
        }
      }
    },
    computed: {
      currentProjectName() {
        if (!this.model.organId || !this.cusList || !this.cusList.length) return ''
        const cus = this.cusList.find(c => c.id === this.model.organId)
        return cus ? cus.projectName || '' : ''
      },
      detailDefColumns() {
        return this.detailAllColumns
          .filter(col => this.getDetailAlwaysHiddenKeys().indexOf(col.key) === -1)
          .filter(col => this.getDetailFixedVisibleKeys().indexOf(col.key) === -1)
          .map(col => ({
            title: this.getManagedDetailColumnTitle(col),
            dataIndex: col.key
          }))
      }
    },
    created() {
      this.normalizeDetailColumnsDefinition()
      this.detailAllColumns = this.materialTable.columns.slice()
      this.detailAllColumns.forEach(col => {
        this.detailBaseTypeMap[col.key] = col.type
      })
      const detailDefaultHiddenKeys = ['color', 'brand', 'mfrs', 'otherField2', 'otherField3',
        'sku', 'snList', 'batchNumber', 'expirationDate', 'preNumber', 'finishNumber',
        'unitPrice', 'allPrice', 'taxRate', 'taxMoney', 'taxLastMoney', 'remark', 'linkId']
      this.detailDefaultDataIndex = this.detailAllColumns
        .filter(col => detailDefaultHiddenKeys.indexOf(col.key) === -1)
        .map(col => col.key)
      this.initDetailColumnSetting()
    },
    methods: {
      initDetailColumnSetting() {
        return loadColumnSetting({
          pageCode: this.detailColumnPageCode,
          storageKey: this.detailColumnStorageKey,
          defaultDataIndex: this.detailDefaultDataIndex,
          mergeSetting: (dataIndexArr) => this.mergeDetailSettingKeys(dataIndexArr),
          applySetting: (dataIndexArr) => {
            this.applyDetailColumnsOrdered(dataIndexArr)
          }
        })
      },
      applyDetailColumnsOrdered(dataIndexArr) {
        let colMap = {}
        this.detailAllColumns.forEach(col => {
          colMap[col.key] = col
        })
        let visibleArr = []
        dataIndexArr.forEach(key => {
          if (colMap[key] && visibleArr.indexOf(key) === -1) {
            visibleArr.push(key)
          }
        })
        visibleArr = this.normalizeDetailVisibleArr(visibleArr)
        let orderedVisible = visibleArr.map(key => colMap[key])
        let hiddenCols = this.detailAllColumns.filter(col => visibleArr.indexOf(col.key) === -1)
        this.detailAllColumns.forEach(col => {
          col.hiddenBySetting = visibleArr.indexOf(col.key) === -1
        })
        this.materialTable.columns = orderedVisible.concat(hiddenCols)
        this.detailSettingDataIndex = visibleArr
      },
      saveDetailColumnsToServer(dataIndexArr) {
        return saveColumnSetting({
          pageCode: this.detailColumnPageCode,
          storageKey: this.detailColumnStorageKey,
          dataIndexArr,
          mergeSetting: (settingKeys) => this.mergeDetailSettingKeys(settingKeys)
        })
      },
      onDetailColChange(dataIndexArr) {
        this.applyDetailColumnsOrdered(dataIndexArr)
        this.saveDetailColumnsToServer(this.detailSettingDataIndex)
      },
      handleDetailRestDefault() {
        return resetColumnSetting({
          pageCode: this.detailColumnPageCode,
          storageKey: this.detailColumnStorageKey,
          defaultDataIndex: this.detailDefaultDataIndex,
          mergeSetting: (settingKeys) => this.mergeDetailSettingKeys(settingKeys),
          applySetting: (dataIndexArr) => {
            this.applyDetailColumnsOrdered(dataIndexArr)
          }
        })
      },
      resetDetailColumnTypes() {
        this.detailAllColumns.forEach(col => {
          if (this.detailBaseTypeMap[col.key]) {
            col.type = this.detailBaseTypeMap[col.key]
          }
        })
        this.applyDetailColumnsOrdered(this.detailSettingDataIndex.length ? this.detailSettingDataIndex : this.detailDefaultDataIndex)
      },
      setBarCodeColumnType(type) {
        const barCodeCol = this.detailAllColumns.find(col => col.key === 'barCode')
        if (barCodeCol) {
          barCodeCol.type = type
        }
      },
      flushActiveEditor() {
        const activeElement = document.activeElement
        if (activeElement && typeof activeElement.blur === 'function') {
          activeElement.blur()
        }
        return new Promise((resolve) => {
          this.$nextTick(() => {
            setTimeout(resolve, 0)
          })
        })
      },
      handleOk() {
        this.flushActiveEditor().then(() => {
          JEditableTableMixin.methods.handleOk.call(this)
        })
      },
      // ─── 覆盖保存方法：超额提示（不阻拦）─────────────────────────
      handleOkOnly() {
        if (this.contractBalance) {
          const cb = this.contractBalance
          const warnings = []
          if (parseFloat(cb.remainAmount) < 0) {
            warnings.push(`已超出合同金额额度 ${Math.abs(cb.remainAmount)} 元`)
          }
          if (parseFloat(cb.remainTonnage) < 0) {
            warnings.push(`已超出合同吨位额度 ${Math.abs(cb.remainTonnage)} 吨`)
          }
          if (warnings.length > 0) {
            this.$notification.warning({
              message: '超出合同限额提示',
              description: warnings.join('；'),
              duration: 6
            })
          }
        }
        this.billStatus = '0'
        this.handleOk()
      },
      // ─── 覆盖父类 handleCustomerNameChange，切换客户时清除合同状态 ──
      handleCustomerNameChange(supplierName) {
        this.contractBalance = null
        this.form.setFieldsValue({ organId: undefined })
        if (supplierName) {
          this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
          if (this.projectListForOrgan.length === 1) {
            this.$nextTick(() => {
              this.form.setFieldsValue({ organId: this.projectListForOrgan[0].id })
              // 自动触发合同查询
              this.handleOrganChange(this.projectListForOrgan[0].id)
            })
          }
        } else {
          this.projectListForOrgan = []
        }
      },
      // ─── 覆盖父类 handleOrganChange，追加合同余额查询 ──────────
      handleOrganChange(value) {
        // 调用父类逻辑（更新商品单价）
        this.$options.mixins.forEach(mixin => {
          if (mixin.methods && mixin.methods.handleOrganChange) {
            mixin.methods.handleOrganChange.call(this, value)
          }
        })
        // 查询合同余额
        this.contractBalance = null
        if (value) {
          getContractBalance({ organId: value }).then(res => {
            if (res && res.code === 200 && res.data) {
              const d = res.data
              let totalAmt = Number(d.totalAmount || 0)
              let totalTon = Number(d.totalTonnage || 0)
              let hasContract = totalAmt > 0 || totalTon > 0
              this.contractBalance = {
                hasContract: hasContract,
                totalAmount:    totalAmt.toFixed(2),
                totalTonnage:   totalTon.toFixed(3),
                remainAmount:   Number(d.remainAmount   || 0).toFixed(2),
                remainTonnage:  Number(d.remainTonnage  || 0).toFixed(3)
              }
            }
          }).catch(() => {})
        }
      },
      //调用完edit()方法之后会自动调用此方法
      editAfter() {
        this.billStatus = '0'
        this.currentSelectDepotId = ''
        this.rowCanEdit = true
        this.resetDetailColumnTypes()
        this.setBarCodeColumnType(FormTypes.popupJsh)
        if (this.action === 'add') {
          this.selectedCustomerName = undefined
          this.projectListForOrgan = []
          this.contractBalance = null
          this.depositStatus = false
          this.addInit(this.prefixNo)
          this.personList.value = ''
          this.fileList = []
          this.$nextTick(() => {
            handleIntroJs(this.prefixNo, 1)
            if(this.transferParam && this.transferParam.number) {
              let tp = this.transferParam
              this.linkBillListOk(tp.list, tp.number, tp.organId, tp.discountMoney, tp.deposit, tp.remark, this.defaultDepotId, tp.accountId, tp.salesMan)
            }
          })
        } else {
          if(this.model.linkNumber) {
            this.rowCanEdit = false
            this.setBarCodeColumnType(FormTypes.normal)
          }
          // 初始化编辑时的公司名称、项目列表和合同状态
          // 注意：客户名称和项目列表的解析延迟到 initCustomer 回调中执行，
          // 避免使用旧的 cusList 导致概率性显示不正确
          this.contractBalance = null
          if(this.model.organId) {
            // 加载合同余额
            getContractBalance({ organId: this.model.organId }).then(res => {
              if (res && res.code === 200 && res.data) {
                let d = res.data
                let totalAmt = Number(d.totalAmount || 0)
                let totalTon = Number(d.totalTonnage || 0)
                let hasContract = totalAmt > 0 || totalTon > 0
                this.contractBalance = {
                  hasContract: hasContract,
                  totalAmount: totalAmt.toFixed(2),
                  totalTonnage: totalTon.toFixed(3),
                  remainAmount: Number(d.remainAmount || 0).toFixed(2),
                  remainTonnage: Number(d.remainTonnage || 0).toFixed(3)
                }
              }
            }).catch(() => {})
          }
          this.model.operTime = this.model.operTimeStr
          if(this.model.deposit) {
            this.depositStatus = true
          } else {
            this.depositStatus = false
            this.model.deposit = 0
          }
          this.model.debt = (this.model.discountLastMoney + this.model.otherMoney - this.model.deposit - this.model.changeAmount).toFixed(2)
          if(this.model.accountId == null) {
            this.model.accountId = 0
            this.manyAccountBtnStatus = true
            this.accountIdList = this.model.accountIdList
            this.accountMoneyList = this.model.accountMoneyList
          } else {
            this.manyAccountBtnStatus = false
          }
          this.personList.value = this.model.salesMan
          this.fileList = this.model.fileName
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'linkNumber', 'remark',
              'discount','discountMoney','discountLastMoney','otherMoney','accountId','deposit','changeAmount','debt','salesMan'))
          });
          // 加载子表数据
          let params = {
            headerId: this.model.id,
            mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
            linkType: 'basic'
          }
          let url = this.readOnly ? this.url.detailList : this.url.detailList;
          this.requestSubTableData(url, params, this.materialTable);
        }
        //复制新增单据-初始化单号和日期
        if(this.action === 'copyAdd') {
          this.model.id = ''
          this.model.tenantId = ''
          this.copyAddInit(this.prefixNo)
        }
        this.initSystemConfig()
        // 覆盖 initCustomer：加载完客户列表后，再解析编辑单据的客户名称和项目列表
        findBySelectCus({organId: this.model.organId, limit:1}).then((res) => {
          if(res) {
            this.cusList = res
            // 编辑/查看模式下，从新加载的列表中匹配客户
            if(this.action !== 'add' && this.model.organId) {
              let cus = res.find(c => c.id === this.model.organId)
              if(cus) {
                this.selectedCustomerName = cus.supplier
                this.projectListForOrgan = res.filter(c => c.supplier === cus.supplier)
              }
            }
          }
        })
        this.initSalesman()
        this.initDepot()
        this.initAccount(0)
        this.initPlatform()
        this.initQuickBtn()
        this.handleChangeOtherField()
      },
      //提交单据时整理成formData
      classifyIntoFormData(allValues) {
        this.url.edit = '/depotHead/updateDepotHeadAndDetail'
        let totalPrice = 0
        let billMain = Object.assign(this.model, allValues.formValue)
        let detailArr = allValues.tablesValue[0].values
        billMain.type = '出库'
        billMain.subType = '销售'
        for(let item of detailArr){
          totalPrice += item.allPrice-0
        }
        billMain.totalPrice = totalPrice
        if(billMain.accountId === 0) {
          billMain.accountId = ''
        }
        billMain.accountIdList = this.accountIdList.length>0 ? JSON.stringify(this.accountIdList) : ""
        billMain.accountMoneyList = this.accountMoneyList.length>0 ? JSON.stringify(this.accountMoneyList) : ""
        if(this.fileList && this.fileList.length > 0) {
          billMain.fileName = this.fileList
        } else {
          billMain.fileName = ''
        }
        if(this.model.id){
          billMain.id = this.model.id
        }
        billMain.salesMan = this.personList.value
        billMain.status = this.billStatus
        return {
          info: JSON.stringify(billMain),
          rows: JSON.stringify(detailArr),
        }
      },
      handleHistoryBillList() {
        let organId = this.form.getFieldValue('organId')
        this.$refs.historyBillListModalForm.show('出库', '销售', '客户', organId);
        this.$refs.historyBillListModalForm.disableSubmit = false;
      },
      onSearchLinkNumber() {
        //status: 0=未审核, 1=已审核, 2=完成入库, 3=部分入库，linkedFlag=0 只显示未被关联的
        this.$refs.linkBillList.show('入库', '采购', '供应商', "0,1,2,3", '0')
        this.$refs.linkBillList.title = "请选择采购入库单"
      },
      linkBillListOk(selectBillDetailRows, linkNumber, organId, discountMoney, deposit, remark, depotId, accountId, salesMan) {
        let that = this
        // 销售出库关联采购入库：采购入库的 changeAmount 不是订金，organId 是供应商不是客户
        deposit = 0
        organId = null
        this.setBarCodeColumnType(FormTypes.normal)
        this.changeFormTypes(this.materialTable.columns, 'preNumber', 1)
        this.changeFormTypes(this.materialTable.columns, 'finishNumber', 1)
        if(!selectBillDetailRows || selectBillDetailRows.length === 0) return
        //同步读取 dataSource 构建已有 linkId 集合（物流单模式：不走异步 getValues）
        let existLinkIdSet = new Set()
        this.materialTable.dataSource.forEach(row => {
          if(row.linkId) existLinkIdSet.add(String(row.linkId))
        })
        let newRows = []
        for(let j=0; j<selectBillDetailRows.length; j++) {
          let info = JSON.parse(JSON.stringify(selectBillDetailRows[j]))
          if(existLinkIdSet.has(String(info.id))) continue
          if(info.finishNumber>0) {
            info.operNumber = info.preNumber - info.finishNumber
            info.allPrice = info.operNumber * info.unitPrice-0
            let taxRate = info.taxRate-0
            if(this.materialPriceTaxFlag) {
              let realAllPrice = (info.allPrice/(1+taxRate*0.01)).toFixed(2)-0
              info.taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
              info.taxLastMoney = info.allPrice
            } else {
              info.taxMoney = (info.allPrice*taxRate/100).toFixed(2)-0
              info.taxLastMoney = (info.allPrice + info.taxMoney).toFixed(2)-0
            }
          }
          info.linkId = info.id
          if(info.operNumber>0) {
            newRows.push(info)
            this.changeColumnShow(info)
          }
        }
        if(newRows.length === 0) {
          this.$message.warning('所选单据的明细已全部添加')
          return
        }
        //直接 push 追加（物流单模式），不覆盖已有行
        newRows.forEach(row => this.materialTable.dataSource.push(row))
        //重新累计全部明细行的金额
        let allTaxLastMoney = 0
        this.materialTable.dataSource.forEach(row => {
          allTaxLastMoney += (row.taxLastMoney-0) || 0
        })
        let discount = 0
        discountMoney = 0
        let discountLastMoney = allTaxLastMoney.toFixed(2)-0
        let changeAmount = discountLastMoney
        if(deposit) {
          this.depositStatus = true
          changeAmount = (discountLastMoney - deposit).toFixed(2)-0
        }
        //追加 linkNumber（多单逗号分隔）
        let oldLinkNumber = this.form.getFieldValue('linkNumber') || ''
        let mergedLinkNumber = oldLinkNumber ? oldLinkNumber + ',' + linkNumber : linkNumber
        let currentOrganId = this.form.getFieldValue('organId')
        this.$nextTick(() => {
          let formValues = {
            'linkNumber': mergedLinkNumber,
            'discount': discount,
            'discountMoney': discountMoney,
            'discountLastMoney': discountLastMoney,
            'deposit': deposit,
            'changeAmount': 0,
            'debt': changeAmount,
            'accountId': accountId,
            'remark': remark
          }
          //已有客户时不覆盖，仅在首次关联时填入
          if(!currentOrganId && organId) {
            formValues['organId'] = organId
          }
          this.form.setFieldsValue(formValues)
          if(!currentOrganId && organId) {
            findBySelectCus({organId: organId, limit:1}).then((res)=> {
              that.cusList = res && Array.isArray(res) ? res : [];
            })
          }
          that.personList.value = salesMan
        })
        if(depotId) {
          setTimeout(function () {
            that.batchSetDepotModalFormOk(depotId)
          },1000)
        }
      },
    }
  }
</script>
<style scoped>
  .sale-out-detail-operator {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 8px;
  }

  .project-option-label {
    display: block;
    white-space: normal;
    word-break: break-all;
    line-height: 22px;
  }

  /deep/ .sale-out-project-dropdown .ant-select-dropdown-menu-item {
    white-space: normal;
    height: auto;
    line-height: 22px;
    padding-top: 8px;
    padding-bottom: 8px;
  }
</style>
