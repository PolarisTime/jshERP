/**
 * 新增修改完成调用 modalFormOk方法 编辑弹框组件ref定义为modalForm
 * 高级查询按钮调用 superQuery方法  高级查询组件ref定义为superQueryModal
 * data中url定义 list为查询列表  delete为删除单条记录  deleteBatch为批量删除
 */
import { filterObj, getMpListShort, getNowFormatStr } from '@/utils/util'
import { deleteAction, getAction, postAction, downFile, downFilePost, getFileAccessHttpUrl } from '@/api/manage'
import Vue from 'vue'
import VueDraggableResizable from 'vue-draggable-resizable'
import { ACCESS_TOKEN } from "@/store/mutation-types"
import {mixinDevice} from '@/utils/mixin.js'
import {
  bindColumnSettingForceSync,
  unbindColumnSettingForceSync,
  loadColumnSetting,
  saveColumnSetting,
  resetColumnSetting,
  forceSyncColumnSetting
} from '@/utils/columnSetting'

export const JeecgListMixin = {
  mixins: [mixinDevice],
  components: {
    VueDraggableResizable
  },
  data(){
    return {
      //token header
      tokenHeader: {'X-Access-Token': Vue.ls.get(ACCESS_TOKEN)},
      /*卡片样式 */
      cardStyle: '',
      /* 查询条件-请不要在queryParam中声明非字符串值的属性 */
      queryParam: {},
      /* 数据源 */
      dataSource:[],
      // 实际列
      columns:[],
      // 实际索引
      settingDataIndex:[],
      /* 分页参数 */
      ipagination:{
        current: 1,
        pageSize: 50,
        pageSizeOptions: ['10', '20', '30', '50', '100'],
        showTotal: (total, range) => {
          return range[0] + "-" + range[1] + " 共" + total + "条"
        },
        showQuickJumper: true,
        showSizeChanger: true,
        total: 0
      },
      /* 控制table高度 */
      scroll: {},
      /* 排序参数 */
      isorter:{
        column: 'createTime',
        order: 'desc',
      },
      /* 筛选参数 */
      filters: {},
      /* table加载状态 */
      loading:false,
      /* 查询请求序号，避免旧响应覆盖新结果 */
      loadDataRequestSeq: 0,
      /* table选中keys*/
      selectedRowKeys: [],
      /* table选中records*/
      selectionRows: [],
      /* 查询折叠 */
      toggleSearchStatus:false,
      /* 高级查询条件生效状态 */
      superQueryFlag:false,
      /* 高级查询条件 */
      superQueryParams: '',
      /** 高级查询拼接方式 */
      superQueryMatchType: 'and',
      /** 是否加载时就执行 */
      disableMixinCreated: false,
      /* 按钮权限 */
      btnEnableList: '',
    }
  },
  created() {
    if(this.isDesktop()) {
      this.cardStyle = 'height:' + (document.documentElement.clientHeight-100) + 'px'
    }
    this._forceSyncColumnSettingsHandler = () => {
      if (typeof this.forceSyncColumnSettings === 'function') {
        this.forceSyncColumnSettings()
      }
    }
    bindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
    if(!this.disableMixinCreated){
      //console.log(' -- mixin created -- ')
      this.loadData();
      //初始化字典配置 在自己页面定义
      this.initDictConfig();
      //初始化按钮权限
      this.initActiveBtnStr();
    }
  },
  mounted () {
    this.initScroll()
    // 监听窗口缩放，动态重算卡片高度和表格滚动区域
    this._resizeHandler = this._debounce(() => {
      if (this.isDesktop()) {
        this.cardStyle = 'height:' + (document.documentElement.clientHeight - 100) + 'px'
      }
      this.initScroll()
    }, 200)
    window.addEventListener('resize', this._resizeHandler)
  },
  beforeDestroy () {
    if (this._resizeHandler) {
      window.removeEventListener('resize', this._resizeHandler)
      this._resizeHandler = null
    }
    if (this._forceSyncColumnSettingsHandler) {
      unbindColumnSettingForceSync(this, this._forceSyncColumnSettingsHandler)
      this._forceSyncColumnSettingsHandler = null
    }
  },
  methods:{
    getColumnSettingPageCode() {
      return this.pageName || this.prefixNo || ''
    },
    getColumnSettingStorageKey() {
      return this.pageName || this.prefixNo || ''
    },
    // 简易 debounce，避免 resize 频繁触发
    _debounce(fn, delay) {
      let timer = null
      return function (...args) {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => { fn.apply(this, args) }, delay)
      }
    },
    loadData(arg) {
      if(!this.url.list){
        this.$message.error("请设置url.list属性!")
        return
      }
      //加载数据 若传入参数1则加载第一页的内容
      if (arg === 1) {
        this.ipagination.current = 1
      }
      let params = this.getQueryParams() //查询条件
      const requestSeq = ++this.loadDataRequestSeq
      this.loading = true
      getAction(this.url.list, params).then((res) => {
        if (requestSeq !== this.loadDataRequestSeq) {
          return
        }
        if (res.code===200) {
          this.dataSource = res.data.rows
          this.ipagination.total = res.data.total
          this.tableAddTotalRow(this.columns, this.dataSource)
          if (typeof this.afterLoadDataSuccess === 'function') {
            this.afterLoadDataSuccess(this.dataSource, this.ipagination.total)
          }
        } else if(res.code===510){
          this.$message.warning(res.data)
        } else {
          this.$message.warning(res.data.message)
        }
        this.onClearSelected()
      }).finally(() => {
        if (requestSeq === this.loadDataRequestSeq) {
          this.loading = false
        }
      })
    },
    initDictConfig(){
      //console.log("--这是一个假的方法!")
    },
    handleSuperQuery(params, matchType) {
      //高级查询方法
      if(!params){
        this.superQueryParams=''
        this.superQueryFlag = false
      }else{
        this.superQueryFlag = true
        this.superQueryParams=JSON.stringify(params)
        this.superQueryMatchType = matchType
      }
      this.loadData(1)
    },
    getQueryParams() {
      //获取查询条件
      let sqp = {}
      if(this.superQueryParams){
        sqp['superQueryParams']=encodeURI(this.superQueryParams)
        sqp['superQueryMatchType'] = this.superQueryMatchType
      }
      let searchObj = {}
      searchObj.search = JSON.stringify(this.queryParam);
      var param = Object.assign(sqp, searchObj, this.isorter ,this.filters);
      param.field = this.getQueryField();
      param.currentPage = this.ipagination.current;
      param.pageSize = this.ipagination.pageSize;
      return filterObj(param);
    },
    getQueryField() {
      var str = "id,";
      this.columns.forEach(function (value) {
        str += "," + value.dataIndex;
      });
      return str;
    },
    onSelectChange(selectedRowKeys, selectionRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectionRows = selectionRows;
    },
    onClearSelected() {
      this.selectedRowKeys = []
      this.selectionRows = []
    },
    searchQuery() {
      this.loadData(1);
    },
    superQuery() {
      this.$refs.superQueryModal.show();
    },
    searchReset() {
      this.queryParam = {}
      this.loadData(1);
    },
    batchSetStatus: function (status) {
      if(!this.url.batchSetStatusUrl){
        this.$message.error("请设置url.batchSetStatusUrl属性!")
        return
      }
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning('请选择一条记录！');
        return;
      }
      var ids = "";
      for (var a = 0; a < this.selectedRowKeys.length; a++) {
        ids += this.selectedRowKeys[a] + ",";
      }
      var that = this;
      var doAction = function() {
        that.loading = true;
        postAction(that.url.batchSetStatusUrl, {status: status, ids: ids}).then((res) => {
          if(res.code === 200){
            that.loadData()
          } else {
            that.$message.warning(res.data.message);
          }
        }).finally(() => {
          that.loading = false;
        });
      };
      // 审核需确认，反审核/反核准直接执行
      if(status === 1 || status === '1') {
        this.$confirm({ title: "确认操作", content: "是否审核选中数据?", onOk: doAction });
      } else {
        doAction();
      }
    },
    batchSetPriceApproved: function (priceApproved) {
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning('请选择一条记录！');
        return;
      }
      var ids = this.selectedRowKeys.join(',');
      var that = this;
      var actionText = priceApproved === '1' ? '单据核准' : '取消核准';
      var doAction = function() {
        that.loading = true;
        postAction('/depotHead/batchSetPriceApproved', {priceApproved: priceApproved, ids: ids}).then((res) => {
          if(res.code === 200){
            that.$message.success(actionText + '成功');
            that.loadData()
          } else {
            that.$message.warning(res.data.message);
          }
        }).finally(() => {
          that.loading = false;
        });
      };
      // 核准需确认，取消核准直接执行
      if(priceApproved === '1') {
        this.$confirm({ title: "确认操作", content: "是否对选中单据进行" + actionText + "?", onOk: doAction });
      } else {
        doAction();
      }
    },
    batchDel: function () {
      if(!this.url.deleteBatch){
        this.$message.error("请设置url.deleteBatch属性!")
        return
      }
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning('请选择一条记录！');
        return;
      } else {
        var ids = "";
        for (var a = 0; a < this.selectedRowKeys.length; a++) {
          ids += this.selectedRowKeys[a] + ",";
        }
        var that = this;
        this.$confirm({
          title: "确认删除",
          content: "是否删除选中数据?",
          onOk: function () {
            that.loading = true;
            deleteAction(that.url.deleteBatch, {ids: ids}).then((res) => {
              if(res.code === 200){
                that.loadData()
              } else {
                that.$message.warning(res.data.message);
              }
            }).finally(() => {
              that.loading = false;
            });
          }
        });
      }
    },
    handleDelete: function (id) {
      if(!this.url.delete){
        this.$message.error("请设置url.delete属性!")
        return
      }
      var that = this;
      deleteAction(that.url.delete, {id: id}).then((res) => {
        if(res.code === 200){
          that.loadData();
        } else {
          that.$message.warning(res.data.message);
        }
      });
    },
    handleEdit: function (record) {
      this.$refs.modalForm.edit(record);
      this.$refs.modalForm.title = "编辑";
      this.$refs.modalForm.disableSubmit = false;
    },
    handleAdd: function () {
      this.$refs.modalForm.add();
      this.$refs.modalForm.title = "新增";
      this.$refs.modalForm.disableSubmit = false;
    },
    handleTableChange(pagination, filters, sorter) {
      //分页、排序、筛选变化时触发
      if (Object.keys(sorter).length > 0) {
        if(sorter.order) {
          this.isorter.column = sorter.field
          this.isorter.order = 'ascend' === sorter.order ? 'asc' : 'desc'
        } else {
          this.isorter.column = 'createTime'
          this.isorter.order = 'desc'
        }
      }
      if(pagination && pagination.current) {
        this.ipagination = pagination;
      }
      this.loadData();
    },
    handleToggleSearch(){
      this.toggleSearchStatus = !this.toggleSearchStatus;
    },
    // 给popup查询使用(查询区域不支持回填多个字段，限制只返回一个字段)
    getPopupField(fields){
      return fields.split(',')[0]
    },
    modalFormOk() {
      // 新增/修改 成功时，刷新列表数据
      this.loadData()
    },
    modalFormClose() {
      // 关闭弹窗时，刷新列表数据
      this.loadData()
    },
    handleDetail:function(record, type, prefixNo){
      this.$refs.modalDetail.show(record, type, prefixNo);
      this.$refs.modalDetail.title=type+"-详情";
    },
    //加载初始化列
    initColumnsSetting(){
      return loadColumnSetting({
        pageCode: this.getColumnSettingPageCode(),
        storageKey: this.getColumnSettingStorageKey(),
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
          this.columns = this.buildOrderedColumns(this.settingDataIndex)
        }
      })
    },
    //列设置更改事件
    onColChange (checkedValues) {
      this.settingDataIndex = [...checkedValues]
      this.columns = this.buildOrderedColumns(this.settingDataIndex)
      return saveColumnSetting({
        pageCode: this.getColumnSettingPageCode(),
        storageKey: this.getColumnSettingStorageKey(),
        dataIndexArr: this.settingDataIndex
      })
    },
    //按指定顺序构建列数组，统一居中对齐、列宽自适应内容
    buildOrderedColumns(orderedIndex) {
      let colMap = {}
      this.defColumns.forEach(col => { colMap[col.dataIndex] = col })
      //固定宽度的列（序号、操作）
      let fixedKeys = ['rowIndex', 'action']
      return orderedIndex.map(di => colMap[di]).filter(Boolean).map(col => {
        let c = Object.assign({}, col, { align: 'center' })
        if (!fixedKeys.includes(c.dataIndex)) {
          delete c.width
        }
        return c
      })
    },
    //恢复默认
    handleRestDefault() {
      return resetColumnSetting({
        pageCode: this.getColumnSettingPageCode(),
        storageKey: this.getColumnSettingStorageKey(),
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
          this.columns = this.buildOrderedColumns(this.settingDataIndex)
        }
      })
    },
    forceSyncColumnSettings() {
      return forceSyncColumnSetting({
        pageCode: this.getColumnSettingPageCode(),
        storageKey: this.getColumnSettingStorageKey(),
        defaultDataIndex: this.defDataIndex || [],
        applySetting: (dataIndexArr) => {
          this.settingDataIndex = [...dataIndexArr]
          this.columns = this.buildOrderedColumns(this.settingDataIndex)
        }
      })
    },
    /* 导出 */
    handleExportXls2(){
      let paramsStr = encodeURI(JSON.stringify(this.getQueryParams()));
      let url = `${window._CONFIG['domianURL']}/${this.url.exportXlsUrl}?paramsStr=${paramsStr}`;
      window.location.href = url;
    },
    //通过get方式导出CSV
    handleExportXls(fileName){
      if(!fileName || typeof fileName != "string"){
        fileName = "导出文件"
      }
      let param = {...this.queryParam};
      if(this.selectedRowKeys && this.selectedRowKeys.length>0){
        param['selections'] = this.selectedRowKeys.join(",")
      }
      console.log("导出参数",param)
      downFile(this.url.exportXlsUrl,param).then((data)=>{
        if (!data) {
          this.$message.warning("文件下载失败")
          return
        }
        if (typeof window.navigator.msSaveBlob !== 'undefined') {
          window.navigator.msSaveBlob(new Blob([data],{type: 'text/csv;charset=utf-8;'}), fileName+'.csv')
        }else{
          let url = window.URL.createObjectURL(new Blob([data],{type: 'text/csv;charset=utf-8;'}))
          let link = document.createElement('a')
          link.style.display = 'none'
          link.href = url
          link.setAttribute('download', fileName + '_' + getNowFormatStr()+'.csv')
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        }
      })
    },
    //通过CSV方式导出数据
    handleExportXlsPost(fileName, title, head, tip, list) {
      if(!fileName || typeof fileName != "string"){
        fileName = "导出文件"
      }
      let BOM = '\uFEFF';
      let headers = head.split(',');
      let csvRows = [headers.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')];
      if (list && list.length) {
        list.forEach(row => {
          if (Array.isArray(row)) {
            csvRows.push(row.map(v => '"' + String(v != null ? v : '').replace(/"/g, '""') + '"').join(','));
          }
        });
      }
      let csvContent = BOM + csvRows.join('\n');
      let blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      let link = document.createElement('a');
      link.style.display = 'none';
      link.href = URL.createObjectURL(blob);
      link.setAttribute('download', fileName + '_' + getNowFormatStr() + '.csv');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(link.href);
    },
    /* 导入 */
    handleImportExcel(info){
      this.confirmLoading = true
      if (info.file.status !== 'uploading') {
        console.log(info.file, info.fileList);
      }
      if (info.file.status === 'done') {
        if (info.file.response) {
          // this.$message.success(`${info.file.name} 文件上传成功`);
          if (info.file.response.code === 200) {
            this.$message.success(info.file.response.data || `${info.file.name} 文件上传成功`)
          } else {
            this.$message.warning(info.file.response.data, 8)
          }
          this.confirmLoading = false
          this.visible = false
          this.$emit('ok')
        } else {
          this.$message.error(`${info.file.name} ${info.file.response.data}.`);
          this.confirmLoading = false
        }
      } else if (info.file.status === 'error') {
        this.$message.error(`文件上传失败: ${info.file.msg} `)
        this.confirmLoading = false
      }
    },
    /* 图片预览 */
    getImgView(text){
      if(text && text.indexOf(",")>0){
        text = text.substring(0,text.indexOf(","))
      }
      return getFileAccessHttpUrl(text)
    },
    /* 文件下载 */
    uploadFile(text){
      if(!text){
        this.$message.warning("未知的文件")
        return;
      }
      if(text.indexOf(",")>0){
        text = text.substring(0,text.indexOf(","))
      }
      let url = getFileAccessHttpUrl(text)
      window.open(url);
    },
    /* 按钮权限 */
    initActiveBtnStr() {
      let btnStrList = Vue.ls.get('winBtnStrList'); //按钮功能列表 JSON字符串
      this.btnEnableList = ""; //按钮列表
      if (this.urlPath && btnStrList) {
        for (let i = 0; i < btnStrList.length; i++) {
          if (btnStrList[i].url === this.urlPath) {
            if (btnStrList[i].btnStr) {
              this.btnEnableList = btnStrList[i].btnStr
            }
          }
        }
      }
    },
    /* 初始化表格横向或纵向滚动 */
    initScroll() {
      if (this.isMobile()) {
        this.scroll.y = ''
      } else {
        let basicWidth = 150
        let basicLength = 274
        let searchWrapperDomLen=0, operatorDomLen=0
        //搜索区域
        let searchWrapperDom = document.getElementsByClassName('table-page-search-wrapper')
        //操作按钮区域
        let operatorDom = document.getElementsByClassName('table-operator')
        if(searchWrapperDom && searchWrapperDom[0]) {
          searchWrapperDomLen = searchWrapperDom[0].offsetHeight
        }
        if(operatorDom && operatorDom[0]) {
          operatorDomLen = operatorDom[0].offsetHeight+10
        }
        this.scroll.x = document.documentElement.clientWidth-basicWidth-64
        this.scroll.y = document.documentElement.clientHeight-searchWrapperDomLen-operatorDomLen-basicLength
      }
    },
    //列宽拖拽——直接操作DOM宽度，避免Vue重渲染导致事件丢失
    handleDrag(column){
      return {
        header: {
          cell: (h, props, children) => {
            const { key, ...restProps } = props
            const col = column.find((col) => {
              const k = col.dataIndex || col.key
              return k === key
            })
            if (!col || !col.width) {
              return h('th', { ...restProps }, children)
            }
            const handle = h('div', {
              class: 'resize-handle',
              on: {
                mousedown: function(e) {
                  e.stopPropagation()
                  e.preventDefault()
                  var th = e.target.parentNode
                  var startX = e.clientX
                  var startWidth = th.offsetWidth
                  var colIndex = Array.prototype.indexOf.call(th.parentNode.children, th)
                  // 找到表格所有同列的 col 元素
                  var table = th.closest('table')
                  var colgroup = table ? table.querySelector('colgroup') : null
                  var colEl = colgroup ? colgroup.children[colIndex] : null
                  var onMove = function(ev) {
                    var newWidth = Math.max(startWidth + (ev.clientX - startX), 50)
                    // ant-design-vue bordered 表格：header 和 body 是两个独立的 table
                    // 需要同时修改两个 table 的 colgroup
                    var wrapper = th.closest('.ant-table-scroll') || th.closest('.ant-table-content')
                    if (wrapper) {
                      var allColgroups = wrapper.querySelectorAll('colgroup')
                      allColgroups.forEach(function(cg) {
                        var targetCol = cg.children[colIndex]
                        if (targetCol) {
                          targetCol.style.width = newWidth + 'px'
                          targetCol.style.minWidth = newWidth + 'px'
                        }
                      })
                    }
                    th.style.width = newWidth + 'px'
                    th.style.minWidth = newWidth + 'px'
                    col.width = newWidth
                  }
                  var onUp = function() {
                    document.removeEventListener('mousemove', onMove)
                    document.removeEventListener('mouseup', onUp)
                    document.body.style.cursor = ''
                    document.body.style.userSelect = ''
                  }
                  document.body.style.cursor = 'col-resize'
                  document.body.style.userSelect = 'none'
                  document.addEventListener('mousemove', onMove)
                  document.addEventListener('mouseup', onUp)
                }
              }
            })
            return h('th', { ...restProps, class: 'resize-table-th' }, [children, handle])
          },
        }
      }
    },
    /** 表格增加合计行 */
    tableAddTotalRow(columns, dataSource) {
      if(dataSource.length>0 && this.ipagination.pageSize%10===1) {
        //分页条数为11、21、31等的时候增加合计行
        let numKey = 'rowIndex'
        let totalRow = { [numKey]: '合计' }
        //需要合计的列，记住最后的逗号不要删除
        let parseCols = 'initialStock,currentStock,currentStockPrice,currentWeight,initialAmount,thisMonthAmount,currentAmount,inSum,inSumPrice,' +
          'inOutSumPrice,outSum,outSumPrice,outInSumPrice,operNumber,allPrice,numSum,priceSum,prevSum,thisSum,thisAllPrice,changeAmount,' +
          'allPrice,taxMoney,currentNumber,lowCritical,highCritical,preNeed,debtMoney,backMoney,allNeed,' +
          'needDebt,realNeedDebt,finishDebt,debt,'
        columns.forEach(column => {
          let { key, dataIndex } = column
          if (![key, dataIndex].includes(numKey)) {
            let total = 0
            dataSource.forEach(data => {
              if(parseCols.indexOf(dataIndex+',')>-1) {
                if(data[dataIndex]) {
                  total += Number.parseFloat(data[dataIndex])
                } else {
                  total += 0
                }
              } else {
                total = '-'
              }
            })
            if (total !== '-') {
              total = total.toFixed(2)
            }
            totalRow[dataIndex] = total
          }
        })
        dataSource.push(totalRow)
        //总数要增加合计的行数，每页都有一行合计，所以总数要加上
        let size = Math.ceil(this.ipagination.total/(this.ipagination.pageSize-1))
        this.ipagination.total = this.ipagination.total + size
      }
    },
    //动态替换扩展字段
    handleChangeOtherField(showQuery) {
      let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
      if(mpStr) {
        let mpArr = mpStr.split(',')
        if(mpArr.length ===3) {
          if(showQuery) {
            this.queryTitle.mp1 = '长度'
            this.queryTitle.mp2 = mpArr[1]
            this.queryTitle.mp3 = mpArr[2]
          }
          for (let i = 0; i < this.defColumns.length; i++) {
            if(this.defColumns[i].dataIndex === 'otherField1') {
              this.defColumns[i].title = '长度'
            }
            if(this.defColumns[i].dataIndex === 'otherField2') {
              this.defColumns[i].title = mpArr[1]
            }
            if(this.defColumns[i].dataIndex === 'otherField3') {
              this.defColumns[i].title = mpArr[2]
            }
          }
        }
      }
    },
    paginationChange(page, pageSize) {
      this.ipagination.current = page
      this.ipagination.pageSize = pageSize
      this.loadData(this.ipagination.current);
    },
    paginationShowSizeChange(current, size) {
      this.ipagination.current = current
      this.ipagination.pageSize = size
      this.loadData(this.ipagination.current);
    }
  }

}
