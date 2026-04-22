import { FormTypes, getListData } from '@/utils/JEditableTableUtil'
import { findBySelectCus, findBySelectRetail, findBySelectSup, findStockByDepotAndBarCode, getAccount,
  getBatchNumberList, getCurrentSystemConfig, getMaterialByBarCode, getPersonByNumType, getPlatformConfigByKey } from '@/api/api'
import { getAction } from '@/api/manage'
import { getCheckFlag, getMpListShort, getNowFormatDateTime } from '@/utils/util'
import { USER_INFO } from '@/store/mutation-types'
import Vue from 'vue'
import {
  bindColumnSettingForceSync,
  unbindColumnSettingForceSync,
  loadColumnSetting,
  saveColumnSetting,
  resetColumnSetting,
  forceSyncColumnSetting
} from '@/utils/columnSetting'

export const BillModalMixin = {
  data() {
    return {
      action: '',
      manyAccountBtnStatus: false,
      supList: [],
      cusList: [],
      retailList: [],
      personList: {
        options: [],
        value: ''
      },
      currentSelectDepotId: '',
      transferParam: {},
      defaultDepotId: '',
      depotList: [],
      accountList: [],
      accountIdList: [],
      accountMoneyList: [],
      billUnitPirce: '',
      scanBarCode: '',
      scanStatus: true,
      defaultTaxRate: 0, //系统默认税率(%)
      billStatus: '0',
      minWidth: 1100,
      isCanCheck: true,
      quickBtn: {
        vendor: false,
        customer: false,
        member: false,
        account: false,
        depot: false
      },
      billPrintFlag: false,
      /* 是否显示打印按钮 */
      isShowPrintBtn: true,
      /* 原始审核是否开启 */
      checkFlag: true,
      //零收付款的场景开关
      zeroChangeAmountFlag: false,
      //商品价格含税开关
      materialPriceTaxFlag: false,
      //按重量计价开关
      priceByWeightFlag: false,
      setTimeFlag: null,
      // 公司名称→项目名称两级联动
      selectedCustomerName: undefined,
      projectListForOrgan: [],
      // 商品单位重量映射表（barCode → 单位重量），用于数量变化时重新计算总重量
      unitWeightMap: {},
      detailColumnSettingEnabled: true,
      detailColumnSettingManagedBySelf: false,
      detailColumnStorageKey: '',
      detailColumnPageCode: '',
      detailAllColumns: [],
      detailBaseTypeMap: {},
      detailDefaultDataIndex: [],
      detailSettingDataIndex: [],
      detailExtraDefaultHiddenKeys: [],
      workflowEnabled: false,
      validatorRules:{
        price:{
          rules: [
            { pattern: /^(([0-9][0-9]*)|([0]\.\d{0,4}|[0-9][0-9]*\.\d{0,4}))$/, message: '金额格式不正确!' }
          ]
        }
      },
      spans: {
        labelCol1: {span: 2},
        wrapperCol1: {span: 22},
        //1_5: 分为1.5列（相当于占了2/3）
        labelCol1_5: { span: 3 },
        wrapperCol1_5: { span: 21 },
        labelCol2: {span: 4},
        wrapperCol2: {span: 20},
        labelCol3: {span: 6},
        wrapperCol3: {span: 18},
        labelCol6: {span: 12},
        wrapperCol6: {span: 12}
      },
    };
  },
  created () {
    let realScreenWidth = window.screen.width
    this.width = realScreenWidth<1500?'1200px':'1550px'
    this.minWidth = realScreenWidth<1500?1150:1500
    this.normalizeDetailColumnsDefinition()
    // 加载系统默认税率
    getCurrentSystemConfig().then((res) => {
      if (res && res.code === 200 && res.data) {
        this.defaultTaxRate = res.data.defaultTaxRate != null ? res.data.defaultTaxRate - 0 : 0
      }
    })
    if (!this.detailColumnSettingManagedBySelf) {
      this.setupDetailColumnSetting()
    }
  },
  mounted() {
    document.getElementById(this.prefixNo).addEventListener('keydown', this.handleOkKey)
    this._forceSyncColumnSettingsHandler = () => {
      if (typeof this.forceSyncColumnSettings === 'function') {
        this.forceSyncColumnSettings()
      }
    }
    bindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
  },
  beforeDestroy() {
    document.getElementById(this.prefixNo).removeEventListener('keydown', this.handleOkKey)
    if (this._forceSyncColumnSettingsHandler) {
      unbindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
      this._forceSyncColumnSettingsHandler = null
    }
  },
  computed: {
    readOnly: function() {
      return this.action !== "add" && this.action !== "edit";
    },
    uniqueCustomerNames: function() {
      let seen = new Set()
      return this.cusList
        .map(c => c.supplier)
        .filter(name => {
          if (!name || seen.has(name)) return false
          seen.add(name)
          return true
        })
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
  methods: {
    getManagedDetailColumnTitle(col) {
      if (!col) return ''
      if (col.key === 'model') return '材质'
      if (col.key === 'otherField1') return '长度'
      return col.title
    },
    isDetailColumnSettingEnabled() {
      return this.detailColumnSettingEnabled !== false && this.materialTable && Array.isArray(this.materialTable.columns)
    },
    getDetailAlwaysHiddenKeys() {
      return ['hiddenKey', 'isEdit']
    },
    getDetailFixedVisibleKeys() {
      return ['name', 'model', 'otherField1']
    },
    getDetailCommonDefaultHiddenKeys() {
      return ['color', 'brand', 'mfrs', 'otherField2', 'otherField3',
        'sku', 'snList', 'batchNumber', 'expirationDate', 'preNumber', 'finishNumber',
        'taxRate', 'taxMoney', 'taxLastMoney', 'linkId']
    },
    getDetailDefaultHiddenKeys() {
      return this.getDetailCommonDefaultHiddenKeys().concat(this.detailExtraDefaultHiddenKeys || [])
    },
    normalizeDetailColumnsDefinition() {
      if (!this.materialTable || !Array.isArray(this.materialTable.columns) || !this.materialTable.columns.length) return
      let columns = this.materialTable.columns
      columns.forEach(col => {
        col.align = 'center'
      })
      let nameIndex = columns.findIndex(col => col.key === 'name')
      let modelIndex = columns.findIndex(col => col.key === 'model')
      let standardIndex = columns.findIndex(col => col.key === 'standard')
      let otherField1Index = columns.findIndex(col => col.key === 'otherField1')
      if (modelIndex > -1) {
        columns[modelIndex].title = '材质'
        columns[modelIndex].type = FormTypes.normal
        if (nameIndex > -1 && modelIndex !== nameIndex + 1) {
          let modelCol = columns.splice(modelIndex, 1)[0]
          nameIndex = columns.findIndex(col => col.key === 'name')
          columns.splice(nameIndex + 1, 0, modelCol)
        }
      }
      if (otherField1Index > -1) {
        columns[otherField1Index].title = '长度'
        columns[otherField1Index].type = FormTypes.normal
        if (standardIndex > -1 && otherField1Index !== standardIndex + 1) {
          let targetCol = columns.splice(otherField1Index, 1)[0]
          standardIndex = columns.findIndex(col => col.key === 'standard')
          columns.splice(standardIndex + 1, 0, targetCol)
        }
      }
    },
    setupDetailColumnSetting() {
      if (!this.isDetailColumnSettingEnabled()) return
      this.detailColumnStorageKey = this.detailColumnStorageKey || (this.prefixNo ? this.prefixNo + '_MODAL_DETAIL_COLUMNS' : '')
      this.detailColumnPageCode = this.detailColumnPageCode || (this.prefixNo ? this.prefixNo + '_MODAL_DETAIL' : '')
      this.detailAllColumns = this.materialTable.columns.slice()
      this.detailAllColumns.forEach(col => {
        this.detailBaseTypeMap[col.key] = col.type
      })
      let defaultHiddenKeys = this.getDetailDefaultHiddenKeys()
      this.detailDefaultDataIndex = this.detailAllColumns
        .filter(col => this.getDetailAlwaysHiddenKeys().indexOf(col.key) === -1)
        .filter(col => defaultHiddenKeys.indexOf(col.key) === -1)
        .map(col => col.key)
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
    loadDetailColumnsFromLocal() {
      if (!this.detailColumnStorageKey) return
      let localConfig = Vue.ls.get(this.detailColumnStorageKey)
      if (localConfig && localConfig.indexOf(',') > -1) {
        let localArr = this.mergeDetailSettingKeys(localConfig.split(',').filter(Boolean))
        this.applyDetailColumnsOrdered(localArr)
        this.saveDetailColumnsToServer(localArr)
      }
    },
    mergeDetailSettingKeys(settingKeys) {
      let availableKeys = this.detailAllColumns
        .filter(col => this.getDetailAlwaysHiddenKeys().indexOf(col.key) === -1)
        .map(col => col.key)
      let visibleArr = []
      settingKeys.forEach(key => {
        if (availableKeys.indexOf(key) > -1 && visibleArr.indexOf(key) === -1) {
          visibleArr.push(key)
        }
      })
      this.detailDefaultDataIndex.forEach(key => {
        if (visibleArr.indexOf(key) === -1) {
          visibleArr.push(key)
        }
      })
      return this.normalizeDetailVisibleArr(visibleArr)
    },
    ensureDetailKeyPosition(result, key, anchorKeys = [], fallback = 'end') {
      if (result.indexOf(key) > -1) {
        result.splice(result.indexOf(key), 1)
      }
      for (let i = 0; i < anchorKeys.length; i++) {
        let anchorIndex = result.indexOf(anchorKeys[i])
        if (anchorIndex > -1) {
          result.splice(anchorIndex + 1, 0, key)
          return result
        }
      }
      if (fallback === 'start') {
        result.unshift(key)
      } else {
        result.push(key)
      }
      return result
    },
    normalizeDetailVisibleArr(visibleArr) {
      let availableKeys = this.detailAllColumns
        .filter(col => this.getDetailAlwaysHiddenKeys().indexOf(col.key) === -1)
        .map(col => col.key)
      let result = []
      visibleArr.forEach(key => {
        if (availableKeys.indexOf(key) > -1 && result.indexOf(key) === -1) {
          result.push(key)
        }
      })
      let fixedVisibleKeys = this.getDetailFixedVisibleKeys()
      fixedVisibleKeys.forEach(key => {
        if (availableKeys.indexOf(key) > -1 && result.indexOf(key) === -1) {
          result.push(key)
        }
      })
      if (availableKeys.indexOf('name') > -1) {
        this.ensureDetailKeyPosition(result, 'name', ['barCode', 'depotId'], 'start')
      }
      if (availableKeys.indexOf('model') > -1) {
        this.ensureDetailKeyPosition(result, 'model', ['name'], 'end')
      }
      if (availableKeys.indexOf('otherField1') > -1) {
        this.ensureDetailKeyPosition(result, 'otherField1', ['standard'], 'end')
      }
      return result
    },
    applyDetailColumnsOrdered(dataIndexArr) {
      if (!this.isDetailColumnSettingEnabled()) return
      let visibleArr = []
      let availableKeys = this.detailAllColumns
        .filter(col => this.getDetailAlwaysHiddenKeys().indexOf(col.key) === -1)
        .map(col => col.key)
      dataIndexArr.forEach(key => {
        if (availableKeys.indexOf(key) > -1 && visibleArr.indexOf(key) === -1) {
          visibleArr.push(key)
        }
      })
      visibleArr = this.normalizeDetailVisibleArr(visibleArr)
      let colMap = {}
      this.detailAllColumns.forEach(col => {
        colMap[col.key] = col
      })
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
      if (!this.isDetailColumnSettingEnabled()) return
      this.detailAllColumns.forEach(col => {
        if (this.detailBaseTypeMap[col.key]) {
          col.type = this.detailBaseTypeMap[col.key]
        }
      })
      this.applyDetailColumnsOrdered(this.detailSettingDataIndex.length ? this.detailSettingDataIndex : this.detailDefaultDataIndex)
    },
    isDetailManagedColumnKey(key) {
      return this.detailDefColumns.some(col => col.dataIndex === key)
    },
    restoreManagedDetailColumnType(columns, key) {
      for (let i = 0; i < columns.length; i++) {
        if (columns[i].key === key && this.detailBaseTypeMap[key]) {
          columns[i].type = this.detailBaseTypeMap[key]
        }
      }
    },
    forceSyncColumnSettings() {
      if (!this.isDetailColumnSettingEnabled()) return Promise.resolve([])
      if (this.visible === false) return Promise.resolve([])
      return forceSyncColumnSetting({
        pageCode: this.detailColumnPageCode,
        storageKey: this.detailColumnStorageKey,
        defaultDataIndex: this.detailDefaultDataIndex,
        mergeSetting: (settingKeys) => this.mergeDetailSettingKeys(settingKeys),
        applySetting: (dataIndexArr) => {
          this.applyDetailColumnsOrdered(dataIndexArr)
        }
      })
    },
    // 快捷键
    handleOkKey(e) {
      const key = window.event.keyCode ? window.event.keyCode : window.event.which
      if (key === 83 && e.ctrlKey) {
        //保存 CTRL+S
        this.handleOkOnly()
        e.preventDefault()
      }
    },
    addInit(amountNum) {
      //清空上一次的明细数据，防止连续新建时残留上一张单据的内容
      if(this.materialTable) {
        this.materialTable.dataSource = []
      }
      getAction('/sequence/buildNumber').then((res) => {
        if (res && res.code === 200) {
          let year = new Date().getFullYear()
          this.model.defaultNumber = year + amountNum + res.data.defaultNumber
          this.form.setFieldsValue({'number': year + amountNum + res.data.defaultNumber})
        }
      })
      this.$nextTick(() => {
        this.form.setFieldsValue({'operTime':getNowFormatDateTime(), 'discount': 0,
          'discountMoney': 0, 'discountLastMoney': 0, 'otherMoney': 0, 'changeAmount': 0, 'debt': 0})
      })
      this.$nextTick(() => {
        getAccount({}).then((res)=>{
          if(res && res.code === 200) {
            for (const item of res.data.accountList) {
              if(item.isDefault){
                this.form.setFieldsValue({'accountId': item.id})
              }
            }
            //数据从前一个页面带过来的情况
            if(this.transferParam && this.transferParam.accountId) {
              this.form.setFieldsValue({'accountId': this.transferParam.accountId})
            }
          }
        })
      })
      this.accountIdList = []
      this.accountMoneyList = []
      this.manyAccountBtnStatus = false
    },
    copyAddInit(amountNum) {
      getAction('/sequence/buildNumber').then((res) => {
        if (res && res.code === 200) {
          let year = new Date().getFullYear()
          this.form.setFieldsValue({'number': year + amountNum + res.data.defaultNumber})
        }
      })
      this.$nextTick(() => {
        this.form.setFieldsValue({'operTime':getNowFormatDateTime()})
      })
    },
    /** 查询某个tab的数据 */
    requestSubTableData(url, params, tab, success) {
      tab.loading = true
      getAction(url, params).then(res => {
        if(res && res.code === 200){
          tab.dataSource = res.data.rows
          for(let i=0; i<tab.dataSource.length; i++){
            let info = tab.dataSource[i]
            info.isEdit = this.model.id?1:0
            this.changeColumnShow(info)
            //填充单位重量映射：单位重量 = 总重量 ÷ 数量
            if(info.barCode) {
              this.unitWeightMap[info.barCode] = (info.weight-0) / ((info.operNumber-0) || 1)
            }
          }
          typeof success === 'function' ? success(res) : ''
        }
      }).finally(() => {
        tab.loading = false
      })
    },
    //改变字段的状态，1-显示 0-隐藏
    changeFormTypes(columns, key, type) {
      if (this.isDetailColumnSettingEnabled() && this.isDetailManagedColumnKey(key)) {
        this.restoreManagedDetailColumnType(columns, key)
        return
      }
      for(let i=0; i<columns.length; i++){
        if(columns[i].key === key) {
          if(type){
            if(key === 'snList' || key === 'batchNumber') {
              if(this.prefixNo === 'LSCK' || this.prefixNo === 'CGTH'  || this.prefixNo === 'XSCK' || this.prefixNo === 'QTCK' || this.prefixNo === 'DBCK') {
                columns[i].type = FormTypes.popupJsh //显示
              } else {
                if(key === 'snList') {
                  columns[i].type = FormTypes.popupJsh //显示
                } else {
                  columns[i].type = FormTypes.input //显示
                }
              }
            } else if(key === 'expirationDate') {
              if(this.prefixNo === 'LSTH' || this.prefixNo === 'CGRK' || this.prefixNo === 'XSTH' || this.prefixNo === 'QTRK') {
                columns[i].type = FormTypes.date //显示
              } else {
                columns[i].type = FormTypes.input //显示
              }
            } else {
              columns[i].type = FormTypes.normal //显示
            }
          } else {
            columns[i].type = FormTypes.hidden //隐藏
          }
        }
      }
    },
    setColumnTypeByKey(columns, key, type) {
      if (!Array.isArray(columns)) {
        return
      }
      for (let i = 0; i < columns.length; i++) {
        if (columns[i].key === key) {
          columns[i].type = type
          return
        }
      }
    },
    initSystemConfig() {
      getCurrentSystemConfig().then((res) => {
        if(res.code === 200 && res.data){
          let multiBillType = res.data.multiBillType
          let multiLevelApprovalFlag = res.data.multiLevelApprovalFlag
          this.checkFlag = getCheckFlag(multiBillType, multiLevelApprovalFlag, this.prefixNo)
          this.purchaseBySaleFlag = res.data.purchaseBySaleFlag==='1'?true:false
          this.inOutManageFlag = res.data.inOutManageFlag==='1'?true:false
          this.zeroChangeAmountFlag = res.data.zeroChangeAmountFlag==='1'?true:false
          this.materialPriceTaxFlag = res.data.materialPriceTaxFlag==='1'?true:false
          this.priceByWeightFlag = res.data.priceByWeightFlag==='1'?true:false
          if(res.data.auditPrintFlag==='1') {
            if(this.model.status === '0' || this.model.status === '9') {
              this.isShowPrintBtn = false
            } else {
              this.isShowPrintBtn = true
            }
          } else {
            this.isShowPrintBtn = true
          }
        }
      })
    },
    initSupplier(isChecked) {
      let that = this;
      findBySelectSup({organId: this.model.organId, limit:1}).then((res)=>{
        if(res) {
          that.supList = res
          if(isChecked && res.length>0) {
            that.form.setFieldsValue({'organId': res[0].id})
          }
        }
      });
    },
    initCustomer(isChecked) {
      let that = this;
      findBySelectCus({organId: this.model.organId, limit:1}).then((res)=>{
        if(res) {
          that.cusList = res
          if(isChecked && res.length>0) {
            that.form.setFieldsValue({'organId': res[0].id})
          }
        }
      });
    },
    initRetail(isChecked) {
      let that = this;
      findBySelectRetail({organId: this.model.organId, limit:1}).then((res)=>{
        if(res) {
          that.retailList = res
          if(isChecked && res.length>0) {
            that.form.setFieldsValue({'organId': res[0].id})
          }
        }
      });
    },
    initSalesman() {
      let that = this;
      getPersonByNumType({type:1}).then((res)=>{
        if(res) {
          that.personList.options = res;
        }
      });
    },
    initDepot() {
      let that = this;
      getAction('/depot/findDepotByCurrentUser').then((res) => {
        if(res.code === 200){
          let arr = res.data
          for(let item of that.materialTable.columns){
            if(item.key == 'depotId' || item.key == 'anotherDepotId') {
              item.options = []
              for(let i=0; i<arr.length; i++) {
                let depotInfo = {};
                depotInfo.value = arr[i].id + '' //注意-此处value必须为字符串格式
                depotInfo.text = arr[i].depotName
                depotInfo.title = arr[i].depotName
                item.options.push(depotInfo)
              }
            }
          }
        }
      })
    },
    initAccount(isChecked){
      let that = this;
      getAccount({}).then((res)=>{
        if(res && res.code === 200) {
          let list = res.data.accountList
          let lastId = list.length>0?list[0].id:''
          getCurrentSystemConfig().then((res) => {
            if (res.code === 200 && res.data) {
              let multiAccountFlag = res.data.multiAccountFlag
              if(multiAccountFlag==='1') {
                list.splice(0,0,{id: 0, name: '多账户'})
              }
            }
            that.accountList = list
            if(isChecked) {
              that.form.setFieldsValue({'accountId': lastId})
            }
          })
        }
      })
    },
    handleSearchSupplier(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectSup({key: value, limit:1}).then((res) => {
          if(res) {
            that.supList = res;
          }
        })
      },500)
    },
    handleSearchCustomer(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectCus({key: value, limit:1}).then((res) => {
          if(res) {
            that.cusList = res;
          }
        })
      },500)
    },
    //公司名称→项目名称联动
    handleCustomerNameChange(supplierName) {
      this.form.setFieldsValue({ organId: undefined })
      if (supplierName) {
        this.projectListForOrgan = this.cusList.filter(c => c.supplier === supplierName)
        if (this.projectListForOrgan.length === 1) {
          this.$nextTick(() => {
            this.form.setFieldsValue({ organId: this.projectListForOrgan[0].id })
          })
        }
      } else {
        this.projectListForOrgan = []
      }
    },
    handleSearchRetail(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectRetail({key: value, limit:1}).then((res) => {
          if(res) {
            that.retailList = res;
          }
        })
      },500)
    },
    handleManyAccount(){
      this.selectAccount(0)
    },
    selectAccount(value){
      if(value === 0) { //多账户
        this.$refs.manyAccountModalForm.edit(this.accountIdList, this.accountMoneyList)
        this.$refs.manyAccountModalForm.title = "多账户结算"
        this.manyAccountBtnStatus = true
      } else {
        this.accountIdList = []
        this.accountMoneyList = []
        this.manyAccountBtnStatus = false
      }
    },
    manyAccountModalFormOk(idList, moneyList, allPrice) {
      this.accountIdList = idList
      this.accountMoneyList = moneyList
      let discountLastMoney = this.form.getFieldValue('discountLastMoney')-0
      let otherMoney = this.form.getFieldValue('otherMoney')?this.form.getFieldValue('otherMoney')-0:0
      let debt = (discountLastMoney + otherMoney - allPrice).toFixed(2)
      this.$nextTick(() => {
        this.form.setFieldsValue({'changeAmount':allPrice, 'debt':debt})
      });
    },
    addSupplier() {
      this.$refs.vendorModalForm.add();
      this.$refs.vendorModalForm.title = "新增供应商";
      this.$refs.vendorModalForm.disableSubmit = false;
    },
    addCustomer() {
      this.$refs.customerModalForm.add();
      this.$refs.customerModalForm.title = "新增客户（提醒：如果找不到新添加的客户，请到用户管理检查是否分配了该客户权限）";
      this.$refs.customerModalForm.disableSubmit = false;
    },
    addMember() {
      this.$refs.memberModalForm.add();
      this.$refs.memberModalForm.title = "新增会员";
      this.$refs.memberModalForm.disableSubmit = false;
    },
    handleBatchSetDepot() {
      this.$refs.batchSetDepotModalForm.add();
      this.$refs.batchSetDepotModalForm.title = "批量切换仓库";
      this.$refs.batchSetDepotModalForm.disableSubmit = false;
    },
    addDepot() {
      this.$refs.depotModalForm.add();
      this.$refs.depotModalForm.title = "新增仓库";
      this.$refs.depotModalForm.disableSubmit = false;
    },
    addAccount() {
      this.$refs.accountModalForm.add();
      this.$refs.accountModalForm.title = "新增结算账户";
      this.$refs.accountModalForm.disableSubmit = false;
    },
    vendorModalFormOk() {
      this.initSupplier(1)
    },
    customerModalFormOk() {
      this.initCustomer(1)
    },
    memberModalFormOk() {
      this.initRetail(1)
    },
    batchSetDepotModalFormOk(depotId) {
      this.getAllTable().then(tables => {
        return getListData(this.form, tables)
      }).then(allValues => {
        //获取单据明细列表信息
        let detailArr = allValues.tablesValue[0].values
        let barCodes = ''
        for(let detail of detailArr){
          barCodes += detail.barCode + ','
        }
        if(barCodes) {
          barCodes = barCodes.substring(0, barCodes.length-1)
        }
        let param = {
          barCode: barCodes,
          organId: this.form.getFieldValue('organId'),
          depotId: depotId,
          mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
          prefixNo: this.prefixNo
        }
        getMaterialByBarCode(param).then((res) => {
          if (res && res.code === 200) {
            let mList = res.data
            //构造新的列表数组，用于存放单据明细信息
            let newDetailArr = []
            if(mList && mList.length) {
              for (let i = 0; i < detailArr.length; i++) {
                let item = detailArr[i]
                item.depotId = depotId
                for (let j = 0; j < mList.length; j++) {
                  if(mList[j].mBarCode === item.barCode) {
                    item.stock = mList[j].stock
                  }
                }
                newDetailArr.push(item)
              }
            } else {
              for (let i = 0; i < detailArr.length; i++) {
                let item = detailArr[i]
                item.depotId = depotId
                newDetailArr.push(item)
              }
            }
            this.materialTable.dataSource = newDetailArr
          }
        })
      })
    },
    depotModalFormOk() {
      this.initDepot()
    },
    accountModalFormOk() {
      this.initAccount(1)
    },
    workflowModalFormOk() {
      this.close()
    },
    onAdded(event) {
      let that = this
      const { row, target } = event
      target.setValues([{rowKey: row.id, values: {operNumber:0}}])
      //自动下滑到最后一行
      setTimeout(function(){
        that.$refs.materialDataTable.resetScrollTop((target.rows.length+1)*that.$refs.materialDataTable.rowHeight)
      },1000)
      if(this.currentSelectDepotId) {
        //如果单据选择过仓库，则直接从当前选择的仓库加载
        target.setValues([{rowKey: row.id, values: {depotId: this.currentSelectDepotId}}])
      } else {
        getAction('/depot/findDepotByCurrentUser').then((res) => {
          if (res.code === 200) {
            let arr = res.data
            if(arr.length===1) {
              target.setValues([{rowKey: row.id, values: {depotId: arr[0].id+''}}])
            } else {
              for (let i = 0; i < arr.length; i++) {
                if(arr[i].isDefault){
                  target.setValues([{rowKey: row.id, values: {depotId: arr[i].id+''}}])
                }
              }
            }
          }
        })
      }
    },
    //单元值改变一个字符就触发一次
    onValueChange(event) {
      let that = this
      const { type, row, column, value, target } = event
      let param,snList,batchNumber,operNumber,unitPrice,allPrice,taxRate,taxMoney,taxLastMoney
      switch(column.key) {
        case "depotId":
          that.currentSelectDepotId = row.depotId
          if(row.barCode){
            that.getStockByDepotBarCode(row, target)
          }
          break;
        case "barCode":
          param = {
            barCode: value,
            organId: this.form.getFieldValue('organId'),
            mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
            prefixNo: this.prefixNo
          }
          getMaterialByBarCode(param).then((res) => {
            if (res && res.code === 200) {
              let mList = res.data
              if (value.indexOf(',') > -1) {
                //多个条码
                this.$refs.materialDataTable.getValues((error, values) => {
                  values.pop()  //移除最后一行数据
                  let mArr = values
                  for (let i = 0; i < mList.length; i++) {
                    let mInfo = mList[i]
                    this.changeColumnShow(mInfo)
                    let mObj = this.parseInfoToObj(mInfo)
                    mObj.depotId = mInfo.depotId
                    mObj.stock = mInfo.stock
                    mObj.snList = ''
                    mObj.batchNumber = that.prefixNo==='CGRK' ? that.buildAutoBatchNumber() : ''
                    mObj.expirationDate = that.prefixNo==='CGRK' ? that.buildTodayDate() : ''
                    mArr.push(mObj)
                  }
                  let allPriceTotal = 0
                  let taxLastMoneyTotal = 0
                  for (let j = 0; j < mArr.length; j++) {
                    allPriceTotal += mArr[j].allPrice-0
                    taxLastMoneyTotal += mArr[j].taxLastMoney-0
                    //组合和拆分单据给商品类型进行重新赋值
                    if(j===0) {
                      mArr[0].mType = '组合件'
                    } else {
                      mArr[j].mType = '普通子件'
                    }
                  }
                  this.materialTable.dataSource = mArr
                  if(this.prefixNo ==='LSCK' || this.prefixNo ==='LSTH') {
                    target.statisticsColumns.allPrice = allPriceTotal
                  } else {
                    target.statisticsColumns.taxLastMoney = taxLastMoneyTotal
                  }
                  that.autoChangePrice(target)
                  //强制渲染
                  target.$forceUpdate()
                })
              } else {
                //单个条码
                let depotIdSelected = this.prefixNo !== 'CGDD' && this.prefixNo !== 'XSDD'? row.depotId: ''
                findStockByDepotAndBarCode({ depotId: depotIdSelected, barCode: row.barCode }).then((res) => {
                  if (res && res.code === 200) {
                    let mArr = []
                    let mInfo = mList[0]
                    this.changeColumnShow(mInfo)
                    let mInfoEx = this.parseInfoToObj(mInfo)
                    mInfoEx.stock = res.data.stock
                    mInfoEx.snList = ''
                    mInfoEx.batchNumber = that.prefixNo==='CGRK' ? that.buildAutoBatchNumber() : ''
                    mInfoEx.expirationDate = that.prefixNo==='CGRK' ? that.buildTodayDate() : ''
                    let mObj = {
                      rowKey: row.id,
                      values: mInfoEx
                    }
                    mArr.push(mObj)
                    target.setValues(mArr);
                    target.recalcAllStatisticsColumns()
                    that.autoChangePrice(target)
                    target.autoSelectBySpecialKey('operNumber', row.orderNum)
                    //强制渲染
                    target.$forceUpdate()
                  }
                })
              }
            }
          });
          break;
        case "snList":
          snList = value
          if(snList) {
            snList = snList.replaceAll('，',',')
            let snArr = snList.split(',')
            operNumber = snArr.length
            taxRate = row.taxRate-0 //税率
            unitPrice = row.unitPrice-0 //单价
            //重量联动：从单位重量映射表取单位重量 × 新数量
            let snUnitWeight = that.unitWeightMap[row.barCode] || 0
            let snNewWeight = parseFloat((snUnitWeight * operNumber).toFixed(4))
            let snIsEditable = row.weightEditable === '1' || row.weightEditable === 1
            let snFactor = (that.priceByWeightFlag || snIsEditable) ? (snNewWeight || operNumber) : operNumber
            allPrice = (unitPrice*snFactor).toFixed(2)-0
            if(this.materialPriceTaxFlag) {
              let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
              taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
              taxLastMoney = allPrice
            } else {
              taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
              taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
            }
            target.setValues([{rowKey: row.id, values: {operNumber: operNumber, allPrice: allPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney, weight: snNewWeight}}])
            target.recalcAllStatisticsColumns()
            that.autoChangePrice(target)
          }
          break;
        case "batchNumber":
          //只针对零售出库、销售出库、采购退货、其它出库
          if(this.prefixNo === 'LSCK' || this.prefixNo === 'XSCK' || this.prefixNo === 'CGTH' || this.prefixNo === 'QTCK') {
            batchNumber = value
            let depotItemId = ''
            if(this.model.id) {
              //只有在保存之后的编辑页面下才获取明细id
              let rowId = row.id
              if(rowId.length<=19) {
                depotItemId = rowId-0
              }
            }
            getBatchNumberList({name:'', depotItemId: depotItemId, depotId: row.depotId, barCode: row.barCode, batchNumber: batchNumber}).then((res) => {
              if (res && res.code === 200) {
                if(res.data && res.data.rows) {
                  let info = res.data.rows[0]
                  let preNumber = row.preNumber-0 //原数量
                  let finishNumber = row.finishNumber-0 //已出库
                  let totalNum = info.totalNum-0 //批次数量
                  if(preNumber > 0) {
                    if(totalNum > preNumber - finishNumber) {
                      operNumber = preNumber - finishNumber
                    } else {
                      operNumber = totalNum
                    }
                  } else {
                    operNumber = totalNum
                  }
                  taxRate = row.taxRate-0 //税率
                  unitPrice = row.unitPrice-0 //单价
                  //重量联动：weight_editable类别优先使用入库时的过磅重量
                  let bnUnitWeight = that.unitWeightMap[row.barCode] || 0
                  let bnTheoreticalWeight = parseFloat((bnUnitWeight * operNumber).toFixed(4))
                  let bnIsEditable = info.weightEditable === '1' || info.weightEditable === 1
                  let bnActualWeight = (bnIsEditable && info.inWeight) ? (info.inWeight-0) : bnTheoreticalWeight
                  let bnFactor = (that.priceByWeightFlag || bnIsEditable) ? bnActualWeight : operNumber
                  allPrice = (unitPrice*bnFactor).toFixed(2)-0
                  if(this.materialPriceTaxFlag) {
                    let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
                    taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
                    taxLastMoney = allPrice
                  } else {
                    taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
                    taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
                  }
                  let bnSetValues = {expirationDate: info.expirationDateStr, operNumber: operNumber,
                      allPrice: allPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney, weight: bnActualWeight}
                  if (bnIsEditable) {
                    bnSetValues.weightEditable = info.weightEditable
                  }
                  target.setValues([{rowKey: row.id, values: bnSetValues}])
                  target.recalcAllStatisticsColumns()
                  that.autoChangePrice(target)
                }
              }
            })
          }
          break;
        case "operNumber":
          operNumber = value-0
          taxRate = row.taxRate-0 //税率
          unitPrice = row.unitPrice-0 //单价
          //重量联动：从单位重量映射表取单位重量 × 新数量
          let unitWeightVal = that.unitWeightMap[row.barCode] || 0
          let newWeight = parseFloat((unitWeightVal * operNumber).toFixed(4))
          let isEditableWeight = row.weightEditable === '1' || row.weightEditable === 1
          let opFactor
          if (isEditableWeight || that.priceByWeightFlag) {
            // 可编辑重量类别或按重量计价：使用新理论重量作为金额因子
            opFactor = newWeight || operNumber
          } else {
            // 默认：按数量计价
            opFactor = operNumber
          }
          allPrice = (unitPrice*opFactor).toFixed(2)-0
          if(this.materialPriceTaxFlag) {
            let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
            taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
            taxLastMoney = allPrice
          } else {
            taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
            taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
          }
          // 数量变更时始终同步更新重量
          // 销售出库不再计算价格（价格由独立的价格核准模块处理）
          let opSetValues = that.prefixNo === 'XSCK'
            ? { weight: newWeight }
            : { allPrice: allPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney, weight: newWeight }
          //采购入库：确保批号和有效期已赋值（防止barCode异步回调未完成时缺失）
          if(that.prefixNo === 'CGRK') {
            if(!row.batchNumber) {
              opSetValues.batchNumber = that.buildAutoBatchNumber()
            }
            if(!row.expirationDate) {
              opSetValues.expirationDate = that.buildTodayDate()
            }
          }
          target.setValues([{rowKey: row.id, values: opSetValues}])
          target.recalcAllStatisticsColumns()
          if(that.prefixNo !== 'XSCK') {
            that.autoChangePrice(target)
          }
          //强制刷新确保JDate组件重新渲染
          target.$forceUpdate()
          break;
        case "unitPrice":
          operNumber = row.operNumber-0 //数量
          unitPrice = value-0 //单价
          taxRate = row.taxRate-0 //税率
          let upIsEditable = row.weightEditable === '1' || row.weightEditable === 1
          let upFactor = (that.priceByWeightFlag || upIsEditable) ? (row.weight-0||operNumber) : operNumber
          allPrice = (unitPrice*upFactor).toFixed(2)-0
          if(this.materialPriceTaxFlag) {
            let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
            taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
            taxLastMoney = allPrice
          } else {
            taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
            taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
          }
          target.setValues([{rowKey: row.id, values: {allPrice: allPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney}}])
          target.recalcAllStatisticsColumns()
          that.autoChangePrice(target)
          break;
        case "allPrice":
          operNumber = row.operNumber-0 //数量
          taxRate = row.taxRate-0 //税率
          allPrice = value-0
          let apIsEditable = row.weightEditable === '1' || row.weightEditable === 1
          let apFactor = (that.priceByWeightFlag || apIsEditable) ? (row.weight-0||operNumber) : operNumber
          unitPrice = apFactor!==0 ? (allPrice/apFactor).toFixed(4)-0 : 0 //单价
          if(this.materialPriceTaxFlag) {
            let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
            taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
            taxLastMoney = allPrice
          } else {
            taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
            taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
          }
          target.setValues([{rowKey: row.id, values: {unitPrice: unitPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney}}])
          target.recalcAllStatisticsColumns()
          that.autoChangePrice(target)
          break;
        case "taxRate":
          operNumber = row.operNumber-0 //数量
          allPrice = row.allPrice-0
          unitPrice = row.unitPrice-0
          taxRate = value-0 //税率
          if(this.materialPriceTaxFlag) {
            let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
            taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
            taxLastMoney = allPrice
          } else {
            taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
            taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
          }
          target.setValues([{rowKey: row.id, values: {taxMoney: taxMoney, taxLastMoney: taxLastMoney}}])
          target.recalcAllStatisticsColumns()
          that.autoChangePrice(target)
          break;
        case "taxLastMoney":
          operNumber = row.operNumber-0 //数量
          taxLastMoney = value-0
          taxRate = row.taxRate-0 //税率
          let tlIsEditable = row.weightEditable === '1' || row.weightEditable === 1
          let tlFactor = (that.priceByWeightFlag || tlIsEditable) ? (row.weight-0||operNumber) : operNumber
          if(this.materialPriceTaxFlag) {
            allPrice = taxLastMoney
            unitPrice = tlFactor!==0 ? (taxLastMoney/tlFactor).toFixed(4)-0 : 0
            if(taxRate) {
              let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
              taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
            } else {
              taxMoney = 0
            }
          } else {
            if(taxRate) {
              unitPrice = tlFactor!==0 ? (taxLastMoney/tlFactor/(1+taxRate*0.01)).toFixed(4)-0 : 0
              allPrice = (unitPrice*tlFactor).toFixed(2)-0
              taxMoney =(taxLastMoney-allPrice).toFixed(2)-0
            } else {
              allPrice = taxLastMoney
              unitPrice = tlFactor!==0 ? (allPrice/tlFactor).toFixed(4)-0 : 0
              taxMoney = 0
            }
          }
          target.setValues([{rowKey: row.id, values: {unitPrice: unitPrice, allPrice: allPrice, taxMoney: taxMoney}}])
          target.recalcAllStatisticsColumns()
          that.autoChangePrice(target)
          break;
        case "weight":
          // 销售出库不再由重量联动价格
          if(that.prefixNo === 'XSCK') break;
          // weightEditable=1 或 priceByWeightFlag 时，按重量计价
          let editableWeightCategory = row.weightEditable === '1' || row.weightEditable === 1
          if(that.priceByWeightFlag || editableWeightCategory) {
            let weightVal = value-0
            unitPrice = row.unitPrice-0
            taxRate = row.taxRate-0
            allPrice = (unitPrice*weightVal).toFixed(2)-0
            if(this.materialPriceTaxFlag) {
              let realAllPrice = (allPrice/(1+taxRate*0.01)).toFixed(2)-0
              taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
              taxLastMoney = allPrice
            } else {
              taxMoney =((taxRate*0.01)*allPrice).toFixed(2)-0
              taxLastMoney = (allPrice + taxMoney).toFixed(2)-0
            }
            target.setValues([{rowKey: row.id, values: {allPrice: allPrice, taxMoney: taxMoney, taxLastMoney: taxLastMoney}}])
            target.recalcAllStatisticsColumns()
            that.autoChangePrice(target)
          }
          break;
      }
    },
    //转为商品对象
    parseInfoToObj(mInfo) {
      //同步单位重量到映射表，数量默认为1，单位重量即商品档案重量
      if(mInfo.mBarCode) {
        this.unitWeightMap[mInfo.mBarCode] = mInfo.weight-0
      }
      let initWeight = mInfo.weight-0 || 0
      let initIsEditable = mInfo.weightEditable === '1' || mInfo.weightEditable === 1
      let initFactor = (this.priceByWeightFlag || initIsEditable) ? (initWeight || 1) : 1
      let initAllPrice = (mInfo.billPrice * initFactor).toFixed(2)-0
      let initTaxRate = mInfo.taxRate-0 || (this.defaultTaxRate || 0)
      let initTaxMoney = 0
      let initTaxLastMoney = initAllPrice
      if(this.materialPriceTaxFlag) {
        if(initTaxRate) {
          let realAllPrice = (initAllPrice/(1+initTaxRate*0.01)).toFixed(2)-0
          initTaxMoney = (realAllPrice*initTaxRate*0.01).toFixed(2)-0
        }
        initTaxLastMoney = initAllPrice
      } else {
        initTaxMoney = ((initTaxRate*0.01)*initAllPrice).toFixed(2)-0
        initTaxLastMoney = (initAllPrice + initTaxMoney).toFixed(2)-0
      }
      // 所有单据类型均显示理论重量作为初始值
      let result = {
        barCode: mInfo.mBarCode,
        name: mInfo.name,
        standard: mInfo.standard,
        model: mInfo.model,
        color: mInfo.color,
        brand: mInfo.brand,
        mfrs: mInfo.mfrs,
        otherField1: mInfo.otherField1,
        otherField2: mInfo.otherField2,
        otherField3: mInfo.otherField3,
        unit: mInfo.commodityUnit,
        sku: mInfo.sku,
        operNumber: 1,
        unitPrice: mInfo.billPrice,
        allPrice: initAllPrice,
        taxRate: mInfo.taxRate,
        taxMoney: initTaxMoney,
        taxLastMoney: initTaxLastMoney,
        weight: initWeight,
        categoryId: mInfo.categoryId,
        weightEditable: mInfo.weightEditable
      }
      //采购入库：自动填充批号和有效期
      if(this.prefixNo === 'CGRK') {
        result.batchNumber = this.buildAutoBatchNumber()
        result.expirationDate = this.buildTodayDate()
      }
      return result
    },
    //使得材质、颜色、扩展信息、sku等为隐藏
    changeColumnHide() {
      this.changeFormTypes(this.materialTable.columns, 'model', 0)
      this.changeFormTypes(this.materialTable.columns, 'color', 0)
      this.changeFormTypes(this.materialTable.columns, 'brand', 0)
      this.changeFormTypes(this.materialTable.columns, 'mfrs', 0)
      this.changeFormTypes(this.materialTable.columns, 'otherField1', 0)
      this.changeFormTypes(this.materialTable.columns, 'otherField2', 0)
      this.changeFormTypes(this.materialTable.columns, 'otherField3', 0)
      this.changeFormTypes(this.materialTable.columns, 'sku', 0)
    },
    //使得sku、序列号、批号、到期日等为显示
    changeColumnShow(info) {
      if(info.model) {
        this.changeFormTypes(this.materialTable.columns, 'model', 1)
      }
      if(info.color) {
        this.changeFormTypes(this.materialTable.columns, 'color', 1)
      }
      if(info.brand) {
        this.changeFormTypes(this.materialTable.columns, 'brand', 1)
      }
      if(info.mfrs) {
        this.changeFormTypes(this.materialTable.columns, 'mfrs', 1)
      }
      if(info.otherField1) {
        this.changeFormTypes(this.materialTable.columns, 'otherField1', 1)
      }
      if(info.otherField2) {
        this.changeFormTypes(this.materialTable.columns, 'otherField2', 1)
      }
      if(info.otherField3) {
        this.changeFormTypes(this.materialTable.columns, 'otherField3', 1)
      }
      if(info.sku) {
        this.changeFormTypes(this.materialTable.columns, 'sku', 1)
      }
      if(info.enableSerialNumber === "1") {
        //如果开启出入库管理，并且类型等于采购、采购退货、销售、销售退货，则跳过
        if(this.inOutManageFlag && (this.prefixNo === 'CGRK' || this.prefixNo === 'CGTH' || this.prefixNo === 'XSCK' || this.prefixNo === 'XSTH')) {
          //跳过
        } else {
          this.changeFormTypes(this.materialTable.columns, 'snList', 1)
        }
      }
      if(info.enableBatchNumber === "1") {
        //如果开启出入库管理，并且类型等于采购、采购退货、销售、销售退货，则跳过
        if(this.inOutManageFlag && (this.prefixNo === 'CGRK' || this.prefixNo === 'CGTH' || this.prefixNo === 'XSCK' || this.prefixNo === 'XSTH')) {
          //跳过
        } else {
          this.changeFormTypes(this.materialTable.columns, 'batchNumber', 1)
          this.changeFormTypes(this.materialTable.columns, 'expirationDate', 1)
        }
      }
    },
    //删除一行或多行的时候触发
    onDeleted(ids, target) {
      target.recalcAllStatisticsColumns()
      this.autoChangePrice(target)
    },
    //根据仓库和条码查询库存
    getStockByDepotBarCode(row, target){
      findStockByDepotAndBarCode({ depotId: row.depotId, barCode: row.barCode }).then((res) => {
        if (res && res.code === 200) {
          target.setValues([{rowKey: row.id, values: {stock: res.data.stock}}])
          target.recalcAllStatisticsColumns()
        }
      })
    },
    //生成自动批号：年份+MMdd+S+HHmmss，确保不同时间入库的同一商品批号唯一
    buildAutoBatchNumber() {
      let now = new Date()
      let yyyy = now.getFullYear()
      let mm = String(now.getMonth() + 1).padStart(2, '0')
      let dd = String(now.getDate()).padStart(2, '0')
      let HH = String(now.getHours()).padStart(2, '0')
      let mi = String(now.getMinutes()).padStart(2, '0')
      let ss = String(now.getSeconds()).padStart(2, '0')
      return yyyy + mm + dd + 'S' + HH + mi + ss
    },
    //生成当天日期字符串：YYYY-MM-DD
    buildTodayDate() {
      let now = new Date()
      let y = now.getFullYear()
      let m = String(now.getMonth() + 1).padStart(2, '0')
      let d = String(now.getDate()).padStart(2, '0')
      return y + '-' + m + '-' + d
    },
    //改变优惠、本次付款、欠款的值
    autoChangePrice(target) {
      this.$nextTick(() => {
        // 在 $nextTick 内重新计算统计列，确保 setValues 的响应式更新已传播
        target.recalcAllStatisticsColumns()
        let allTaxLastMoney = parseFloat(target.statisticsColumns.taxLastMoney) || 0
        // 统计列未同步时（如通过 linkBillListOk 直接 push 行），从表单读取已有值
        if(allTaxLastMoney === 0) {
          let formDiscountLastMoney = parseFloat(this.form.getFieldValue('discountLastMoney')) || 0
          let formDiscountMoney = parseFloat(this.form.getFieldValue('discountMoney')) || 0
          if(formDiscountLastMoney > 0) {
            allTaxLastMoney = formDiscountLastMoney + formDiscountMoney
          }
        }
        let discount = parseFloat(this.form.getFieldValue('discount')) || 0
        let otherMoney = parseFloat(this.form.getFieldValue('otherMoney')) || 0
        let deposit = parseFloat(this.form.getFieldValue('deposit')) || 0
        let discountMoney = (discount*0.01*allTaxLastMoney).toFixed(2)-0
        let discountLastMoney = (allTaxLastMoney-discountMoney).toFixed(2)-0
        let changeAmountNew = (discountLastMoney + otherMoney - deposit).toFixed(2)-0
        changeAmountNew = this.prefixNo === 'CGDD' || this.prefixNo === 'XSDD'?0:changeAmountNew
        // 销售出库：本次收款默认置为0，欠款为总金额
        if(this.prefixNo === 'XSCK') {
          this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
            'changeAmount':0,'debt':changeAmountNew})
        } else {
          this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
            'changeAmount':changeAmountNew,'debt':0})
        }
        this.setZeroChangeAmount()
      });
    },
    //改变优惠率
    onChangeDiscount(e) {
      const value = e.target.value-0
      let otherMoney = this.form.getFieldValue('otherMoney')?(parseFloat(this.form.getFieldValue('otherMoney')) || 0):0
      let deposit = this.form.getFieldValue('deposit')
      let allTaxLastMoney = parseFloat(this.$refs.materialDataTable.statisticsColumns.taxLastMoney) || 0
      let discountMoneyNew = (allTaxLastMoney*value*0.01).toFixed(2)-0
      let discountLastMoneyNew = (allTaxLastMoney - discountMoneyNew).toFixed(2)-0
      let changeAmountNew = (discountLastMoneyNew + otherMoney).toFixed(2)-0
      if(deposit) {
        changeAmountNew = (changeAmountNew - deposit).toFixed(2)-0
      }
      this.$nextTick(() => {
        changeAmountNew = this.prefixNo === 'CGDD' || this.prefixNo === 'XSDD'?0:changeAmountNew
        // 销售出库：本次收款默认置为0，欠款为总金额
        if(this.prefixNo === 'XSCK') {
          this.form.setFieldsValue({'discountMoney':discountMoneyNew,'discountLastMoney':discountLastMoneyNew,
            'changeAmount':0,'debt':changeAmountNew})
        } else {
          this.form.setFieldsValue({'discountMoney':discountMoneyNew,'discountLastMoney':discountLastMoneyNew,
            'changeAmount':changeAmountNew,'debt':0})
        }
        this.setZeroChangeAmount()
      });
    },
    //改变付款优惠
    onChangeDiscountMoney(e) {
      const value = e.target.value-0
      let otherMoney = this.form.getFieldValue('otherMoney')?(parseFloat(this.form.getFieldValue('otherMoney')) || 0):0
      let deposit = this.form.getFieldValue('deposit')
      let allTaxLastMoney = parseFloat(this.$refs.materialDataTable.statisticsColumns.taxLastMoney) || 0
      let discountNew = (value/allTaxLastMoney*100).toFixed(2)-0
      let discountLastMoneyNew = (allTaxLastMoney - value).toFixed(2)-0
      let changeAmountNew = (discountLastMoneyNew + otherMoney).toFixed(2)-0
      if(deposit) {
        changeAmountNew = (changeAmountNew - deposit).toFixed(2)-0
      }
      this.$nextTick(() => {
        changeAmountNew = this.prefixNo === 'CGDD' || this.prefixNo === 'XSDD'?0:changeAmountNew
        // 销售出库：本次收款默认置为0，欠款为总金额
        if(this.prefixNo === 'XSCK') {
          this.form.setFieldsValue({'discount':discountNew,'discountLastMoney':discountLastMoneyNew,
            'changeAmount':0,'debt':changeAmountNew})
        } else {
          this.form.setFieldsValue({'discount':discountNew,'discountLastMoney':discountLastMoneyNew,
            'changeAmount':changeAmountNew,'debt':0})
        }
        this.setZeroChangeAmount()
      });
    },
    //其它费用
    onChangeOtherMoney(e) {
      const value = e.target.value-0
      let discountLastMoney = this.form.getFieldValue('discountLastMoney')-0
      let deposit = this.form.getFieldValue('deposit')
      let changeAmountNew = (discountLastMoney + value).toFixed(2)-0
      if(deposit) {
        changeAmountNew = (changeAmountNew - deposit).toFixed(2)-0
      }
      this.$nextTick(() => {
        // 销售出库：本次收款默认置为0，欠款为总金额
        if(this.prefixNo === 'XSCK') {
          this.form.setFieldsValue({'changeAmount':0, 'debt':changeAmountNew})
        } else {
          this.form.setFieldsValue({'changeAmount':changeAmountNew, 'debt':0})
        }
        this.setZeroChangeAmount()
      });
    },
    //改变扣除订金
    onChangeDeposit(e){
      const value = e.target.value-0
      let discountLastMoney = this.form.getFieldValue('discountLastMoney')-0
      let otherMoney = this.form.getFieldValue('otherMoney')?this.form.getFieldValue('otherMoney')-0:0
      let changeAmountNew = (discountLastMoney + otherMoney).toFixed(2)-0
      if(value) {
        changeAmountNew = (changeAmountNew - value).toFixed(2)-0
      }
      this.$nextTick(() => {
        // 销售出库：本次收款默认置为0，欠款为总金额
        if(this.prefixNo === 'XSCK') {
          this.form.setFieldsValue({'changeAmount':0, 'debt':changeAmountNew})
        } else {
          this.form.setFieldsValue({'changeAmount':changeAmountNew, 'debt':0})
        }
        this.setZeroChangeAmount()
      });
    },
    //改变本次付款
    onChangeChangeAmount(e) {
      const value = parseFloat(e.target.value) || 0
      let discountLastMoney = parseFloat(this.form.getFieldValue('discountLastMoney')) || 0
      let otherMoney = parseFloat(this.form.getFieldValue('otherMoney')) || 0
      let deposit = parseFloat(this.form.getFieldValue('deposit')) || 0
      let debtNew = (discountLastMoney + otherMoney - value - deposit).toFixed(2)-0
      this.form.setFieldsValue({'debt':debtNew})
      this.$forceUpdate()
    },
    //切换客户信息改变商品单价
    handleOrganChange(value) {
      let organId = value
      this.getAllTable().then(tables => {
        return getListData(this.form, tables)
      }).then(allValues => {
        let detailArr = allValues.tablesValue[0].values
        let barCodeStr = ''
        for(let detail of detailArr){
          if(detail.barCode) {
            barCodeStr += detail.barCode + ','
          }
        }
        if(barCodeStr) {
          let param = {
            barCode: barCodeStr,
            organId: organId,
            mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
            prefixNo: this.prefixNo
          }
          getMaterialByBarCode(param).then((res) => {
            if (res && res.code === 200) {
              let allLastMoney = 0
              let allTaxLastMoney = 0
              //获取单据明细列表信息
              let detailArr = allValues.tablesValue[0].values
              //构造新的列表数组，用于存放单据明细信息
              let newDetailArr = []
              for(let detail of detailArr){
                if(detail.barCode) {
                  //如果条码重复，就在给原来的数量加1
                  let mList = res.data
                  for (let i = 0; i < mList.length; i++) {
                    if(detail.barCode === mList[i].mBarCode) {
                      //由于改变了商品单价，需要同时更新相关金额和价税合计
                      let taxRate = detail.taxRate-0 //税率
                      detail.unitPrice = mList[i].billPrice-0 //单价
                      let scIsEditable = detail.weightEditable === '1' || detail.weightEditable === 1
                      let scFactor = (this.priceByWeightFlag || scIsEditable) ? (detail.weight-0||detail.operNumber-0) : detail.operNumber-0
                      detail.allPrice = (detail.unitPrice*scFactor).toFixed(2)-0
                      if(this.materialPriceTaxFlag) {
                        let realAllPrice = (detail.allPrice/(1+taxRate*0.01)).toFixed(2)-0
                        detail.taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
                        detail.taxLastMoney = detail.allPrice
                      } else {
                        detail.taxMoney = ((taxRate*0.01)*detail.allPrice).toFixed(2)-0
                        detail.taxLastMoney = (detail.allPrice + detail.taxMoney).toFixed(2)-0
                      }
                    }
                  }
                  newDetailArr.push(detail)
                }
              }
              this.materialTable.dataSource = newDetailArr
              //更新优惠后金额、本次付款等信息
              for(let newDetail of newDetailArr){
                allLastMoney = allLastMoney + (newDetail.allPrice-0)
                allTaxLastMoney = allTaxLastMoney + (newDetail.taxLastMoney-0)
              }
              let discount = this.form.getFieldValue('discount')-0
              let otherMoney = this.form.getFieldValue('otherMoney')?this.form.getFieldValue('otherMoney')-0:0
              let deposit = this.form.getFieldValue('deposit')
              let discountMoney = (discount*0.01*allTaxLastMoney).toFixed(2)-0
              let discountLastMoney = (allTaxLastMoney-discountMoney).toFixed(2)-0
              let changeAmountNew = (discountLastMoney + otherMoney).toFixed(2)-0
              if(deposit) {
                changeAmountNew = (changeAmountNew - deposit).toFixed(2)-0
              }
              this.$nextTick(() => {
                changeAmountNew = this.prefixNo === 'XSDD'?0:changeAmountNew
                // 销售出库：本次收款默认置为0，欠款为总金额
                if(this.prefixNo === 'XSCK') {
                  this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
                    'changeAmount':0,'debt':changeAmountNew})
                } else {
                  this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
                    'changeAmount':changeAmountNew,'debt':0})
                }
                this.setZeroChangeAmount()
              });
            }
          })
        }
      })
    },
    //切换收付款的金额为0
    setZeroChangeAmount() {
      // 销售出库/销售退货：本次收款始终置为0
      if(this.prefixNo === 'XSCK'||this.prefixNo === 'XSTH') {
        let oldChangeAmount = this.form.getFieldValue('changeAmount')-0
        if(oldChangeAmount !== 0) {
          this.form.setFieldsValue({'changeAmount':0, 'debt':oldChangeAmount})
        }
      }
      // 采购退货：仍遵循原有 zeroChangeAmountFlag 配置
      if(this.prefixNo === 'CGTH') {
        if(this.zeroChangeAmountFlag) {
          let oldChangeAmount = this.form.getFieldValue('changeAmount')-0
          this.form.setFieldsValue({'changeAmount':0, 'debt':oldChangeAmount})
        }
      }
      // 采购入库(CGRK)：本次付款默认为单据总金额，不再置0
    },
    scanEnter() {
      this.scanStatus = false
      this.$nextTick(() => {
        this.$refs.scanBarCode.focus()
      })
    },
    //扫码之后回车
    scanPressEnter() {
      let that = this
      if(this.scanBarCode) {
        this.getAllTable().then(tables => {
          return getListData(this.form, tables)
        }).then(allValues => {
          let param = {
            barCode: this.scanBarCode.trim(),
            organId: this.form.getFieldValue('organId'),
            mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
            prefixNo: this.prefixNo
          }
          getMaterialByBarCode(param).then((res) => {
            if (res && res.code === 200) {
              let hasFinished = false
              let allLastMoney = 0
              let allTaxLastMoney = 0
              //获取单据明细列表信息
              let detailArr = allValues.tablesValue[0].values
              //构造新的列表数组，用于存放单据明细信息
              let newDetailArr = []
              let hasAdd = false
              for(let detail of detailArr){
                if(detail.barCode) {
                  //如果扫码结果和条码重复，就在给原来的数量加1
                  if(detail.barCode === this.scanBarCode.trim() && !hasAdd) {
                    detail.operNumber = (detail.operNumber-0)+1
                    //由于改变了商品数量，需要同时更新重量和相关金额、价税合计
                    let scanUnitWeight = this.unitWeightMap[detail.barCode] || 0
                    detail.weight = parseFloat((scanUnitWeight * detail.operNumber).toFixed(4))
                    let taxRate = detail.taxRate-0 //税率
                    let unitPrice = detail.unitPrice-0 //单价
                    let scanIsEditable = detail.weightEditable === '1' || detail.weightEditable === 1
                    let scanFactor = (this.priceByWeightFlag || scanIsEditable) ? (detail.weight||detail.operNumber) : detail.operNumber
                    detail.allPrice = (unitPrice*scanFactor).toFixed(2)-0
                    if(this.materialPriceTaxFlag) {
                      let realAllPrice = (detail.allPrice/(1+taxRate*0.01)).toFixed(2)-0
                      detail.taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
                      detail.taxLastMoney = detail.allPrice
                    } else {
                      detail.taxMoney = ((taxRate*0.01)*detail.allPrice).toFixed(2)-0
                      detail.taxLastMoney = (detail.allPrice + detail.taxMoney).toFixed(2)-0
                    }
                    hasFinished = true
                    hasAdd = true
                  }
                  //如果扫码结果和序列号重复，就直接跳过
                  if(detail.snList === this.scanBarCode.trim()) {
                    this.$message.warning('抱歉，已经扫描过该序列号！');
                    hasFinished = true
                  }
                  newDetailArr.push(detail)
                }
              }
              if(!hasFinished) {
                //将扫码的条码对应的商品加入列表
                let item = {}
                let mList = res.data
                if(mList && mList.length>0) {
                  let mInfo = mList[0]
                  this.changeColumnShow(mInfo)
                  item.depotId = mInfo.depotId
                  item.barCode = mInfo.mBarCode
                  item.name = mInfo.name
                  item.standard = mInfo.standard
                  item.model = mInfo.model
                  item.color = mInfo.color
                  item.materialOther = mInfo.materialOther
                  item.stock = mInfo.stock
                  item.unit = mInfo.commodityUnit
                  item.sku = mInfo.sku
                  if(mInfo.mBarCode !== this.scanBarCode.trim()) {
                    if(this.prefixNo ==='LSCK' || this.prefixNo ==='CGTH' || this.prefixNo ==='XSCK' || this.prefixNo ==='QTCK') {
                      //此时给序列号赋值
                      item.snList = this.scanBarCode.trim()
                    }
                  }
                  item.operNumber = 1
                  item.unitPrice = mInfo.billPrice
                  //重量和金额联动：weightEditable类别按重量计价
                  let newItemWeight = mInfo.weight-0 || 0
                  let newItemIsEditable = mInfo.weightEditable === '1' || mInfo.weightEditable === 1
                  let newItemFactor = (this.priceByWeightFlag || newItemIsEditable) ? (newItemWeight || 1) : 1
                  item.allPrice = (mInfo.billPrice * newItemFactor).toFixed(2)-0
                  item.weight = newItemWeight
                  item.weightEditable = mInfo.weightEditable
                  let newItemTaxRate = mInfo.taxRate-0 || 0
                  if(this.materialPriceTaxFlag) {
                    let realAllPrice = (item.allPrice/(1+newItemTaxRate*0.01)).toFixed(2)-0
                    item.taxMoney = (realAllPrice*newItemTaxRate*0.01).toFixed(2)-0
                    item.taxLastMoney = item.allPrice
                  } else {
                    item.taxMoney = ((newItemTaxRate*0.01)*item.allPrice).toFixed(2)-0
                    item.taxLastMoney = (item.allPrice + item.taxMoney).toFixed(2)-0
                  }
                  item.taxRate = mInfo.taxRate
                  newDetailArr.push(item)
                } else {
                  this.$message.warning('抱歉，此条码不存在商品信息！');
                }
              }
              //组合和拆分单据给商品类型进行重新赋值
              for(let i=0; i< newDetailArr.length; i++) {
                if(i===0) {
                  newDetailArr[0].mType = '组合件'
                } else {
                  newDetailArr[i].mType = '普通子件'
                }
              }
              this.materialTable.dataSource = newDetailArr
              //更新优惠后金额、本次付款等信息
              for(let newDetail of newDetailArr){
                allLastMoney = allLastMoney + (newDetail.allPrice-0)
                allTaxLastMoney = allTaxLastMoney + (newDetail.taxLastMoney-0)
              }
              let discount = this.form.getFieldValue('discount')-0
              let otherMoney = this.form.getFieldValue('otherMoney')?this.form.getFieldValue('otherMoney')-0:0
              let deposit = this.form.getFieldValue('deposit')
              let discountMoney = (discount*0.01*allTaxLastMoney).toFixed(2)-0
              let discountLastMoney = (allTaxLastMoney-discountMoney).toFixed(2)-0
              let changeAmountNew = (discountLastMoney + otherMoney).toFixed(2)-0
              if(deposit) {
                changeAmountNew = (changeAmountNew - deposit).toFixed(2)-0
              }
              if(this.prefixNo === 'LSCK' || this.prefixNo === 'LSTH') {
                this.$nextTick(() => {
                  this.form.setFieldsValue({'changeAmount':allLastMoney,'getAmount':allLastMoney,'backAmount':0})
                });
              } else {
                this.$nextTick(() => {
                  changeAmountNew = this.prefixNo === 'CGDD' || this.prefixNo === 'XSDD'?0:changeAmountNew
                  // 销售出库：本次收款默认置为0，欠款为总金额
                  if(this.prefixNo === 'XSCK') {
                    this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
                      'changeAmount':0,'debt':changeAmountNew})
                  } else {
                    this.form.setFieldsValue({'discount':discount,'discountMoney':discountMoney,'discountLastMoney':discountLastMoney,
                      'changeAmount':changeAmountNew,'debt':0})
                  }
                });
              }
              //置空扫码的内容
              this.scanBarCode = ''
              this.$refs.scanBarCode.focus()
              //自动下滑到最后一行
              setTimeout(function(){
                that.$refs.materialDataTable.resetScrollTop((newDetailArr.length+1)*that.$refs.materialDataTable.rowHeight)
              },1000)
            }
          })
        })
      }
    },
    stopScan() {
      this.scanStatus = true
      this.scanBarCode = ''
    },
    onImport(prefixNo) {
      this.$refs.importItemModalForm.add(prefixNo);
    },
    importItemModalFormOk(data) {
      this.materialTable.dataSource = data
      this.$nextTick(() => {
        let discountLastMoney = 0
        for (let i = 0; i < data.length; i++) {
          discountLastMoney += data[i].taxLastMoney
          this.changeColumnShow(data[i])
        }
        let changeAmount = 0
        let debt = 0
        if(this.prefixNo === 'XSCK') {
          // 销售出库：本次收款默认置为0
          changeAmount = 0
          debt = discountLastMoney
        } else if(this.prefixNo === 'CGRK') {
          // 采购入库：本次付款默认为单据总金额
          changeAmount = discountLastMoney
          debt = 0
        }
        this.form.setFieldsValue({'discountLastMoney':discountLastMoney, 'changeAmount':changeAmount, 'debt': debt})
      });
    },
    //保存并审核
    handleOkAndCheck() {
      this.billStatus = '1'
      this.handleOk()
    },
    //仅保存
    handleOkOnly() {
      this.billStatus = '0'
      this.handleOk()
    },
    //发起流程
    handleWorkflow() {
      this.$message.warning('插件系统已移除，流程提交入口已关闭');
    },
    //三联打印新版
    handlePrintPro(billType) {
      if(this.model.id) {
        getPlatformConfigByKey({"platformKey": "bill_print_pro_url"}).then((res)=> {
          if (res && res.code === 200) {
            let billPrintUrl = res.data.platformValue + '&no=' + this.model.number
            let billPrintHeight = document.documentElement.clientHeight - 260
            this.$refs.modalPrintPro.show(this.model, billPrintUrl, billPrintHeight)
            this.$refs.modalPrintPro.title = billType + "-三联打印-新版"
          }
        })
      } else {
        this.$message.warning('请先保存单据后再打印！');
      }
    },
    //三联打印
    handlePrint(billType) {
      if(this.model.id) {
        getPlatformConfigByKey({"platformKey": "bill_print_url"}).then((res)=> {
          if (res && res.code === 200) {
            let billPrintUrl = res.data.platformValue + '&no=' + this.model.number
            let billPrintHeight = this.materialTable.dataSource.length*50 + 600
            this.$refs.modalPrint.show(this.model, billPrintUrl, billPrintHeight)
            this.$refs.modalPrint.title = billType + "-三联打印"
          }
        })
      } else {
        this.$message.warning('请先保存单据后再打印！');
      }
    },
    //加载平台配置信息
    initPlatform() {
      getPlatformConfigByKey({"platformKey": "bill_print_flag"}).then((res)=> {
        if (res && res.code === 200) {
          this.billPrintFlag = res.data.platformValue==='1'?true:false
        }
      })
    },
    //加载快捷按钮：供应商、客户、会员、结算账户、仓库
    initQuickBtn() {
      let btnStrList = Vue.ls.get('winBtnStrList') //按钮功能列表 JSON字符串
      if (btnStrList) {
        for (let i = 0; i < btnStrList.length; i++) {
          if (btnStrList[i].btnStr) {
            this.quickBtn.vendor = btnStrList[i].url === '/system/vendor'?btnStrList[i].btnStr.indexOf(1)>-1:this.quickBtn.vendor
            this.quickBtn.customer = btnStrList[i].url === '/system/customer'?btnStrList[i].btnStr.indexOf(1)>-1:this.quickBtn.customer
            this.quickBtn.member = btnStrList[i].url === '/system/member'?btnStrList[i].btnStr.indexOf(1)>-1:this.quickBtn.member
            this.quickBtn.account = btnStrList[i].url === '/system/account'?btnStrList[i].btnStr.indexOf(1)>-1:this.quickBtn.account
            this.quickBtn.depot = btnStrList[i].url === '/system/depot'?btnStrList[i].btnStr.indexOf(1)>-1:this.quickBtn.depot
          }
        }
      }
    },
    //动态替换扩展字段
    handleChangeOtherField() {
      let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
      this.normalizeDetailColumnsDefinition()
      if(mpStr) {
        let mpArr = mpStr.split(',')
        if(mpArr.length ===3) {
          for (let i = 0; i < this.materialTable.columns.length; i++) {
            if(this.materialTable.columns[i].key === 'otherField1') {
              this.materialTable.columns[i].title = '长度'
            }
            if(this.materialTable.columns[i].key === 'otherField2') {
              this.materialTable.columns[i].title = mpArr[1]
            }
            if(this.materialTable.columns[i].key === 'otherField3') {
              this.materialTable.columns[i].title = mpArr[2]
            }
          }
        }
      }
    }
  }
}
