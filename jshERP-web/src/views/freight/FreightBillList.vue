<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="单据编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入单据编号" v-model="queryParam.billNo"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="结算方" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择结算方" showSearch allow-clear optionFilterProp="children" v-model="queryParam.carrierId">
                    <a-select-option v-for="(item,index) in carrierList" :key="index" :value="item.id">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="单据状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择单据状态" allow-clear v-model="queryParam.status">
                    <a-select-option value="0">未审核</a-select-option>
                    <a-select-option value="1">已审核</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="送达状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="全部" allow-clear v-model="queryParam.deliveryStatus">
                    <a-select-option value="0">未送达</a-select-option>
                    <a-select-option value="1">已送达</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? '收起' : '展开' }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </a-col>
              </span>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item label="单据日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-range-picker
                      style="width:100%"
                      v-model="queryParam.createTimeRange"
                      format="YYYY-MM-DD"
                      :placeholder="['开始时间', '结束时间']"
                      @change="onDateChange"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">新增</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" icon="delete" @click="batchDel">删除</a-button>
          <a-button v-if="btnEnableList.indexOf(2)>-1" icon="check" @click="batchSetStatus(1)">审核</a-button>
          <a-button v-if="btnEnableList.indexOf(7)>-1" icon="stop" @click="batchSetStatus(0)">反审核</a-button>
          <a-button icon="car" style="color:#52c41a" @click="batchSetDeliveryStatus('1')">标记送达</a-button>
          <a-button icon="undo" @click="batchSetDeliveryStatus('0')">取消送达</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
        </div>
        <!-- table区域 -->
        <div>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :expandedRowKeys="expandedRowKeys"
            @expand="onExpandFreight"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="myHandleDetail(record)">查看</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="handleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => handleDeleteBill(record)">
                <a>删除</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderStatus" slot-scope="text, record">
              <a-tag v-if="record.status === '0' || record.status === 0" color="red">未审核</a-tag>
              <a-tag v-if="record.status === '1' || record.status === 1" color="green">已审核</a-tag>
              <a-tag v-if="record.deliveryStatus === '1'" color="blue">已送达</a-tag>
              <a-tag v-else color="orange">未送达</a-tag>
            </template>
            <a-table
              bordered
              size="small"
              slot="expandedRowRender"
              slot-scope="record"
              :loading="record.loading"
              :columns="expandDetailColumns"
              :dataSource="record.childrens"
              :row-key="(record, index) => record.itemId || index"
              :pagination="false">
            </a-table>
          </a-table>
        </div>
        <freight-bill-modal ref="modalForm" @ok="modalFormOk"></freight-bill-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import FreightBillModal from './modules/FreightBillModal'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier, deleteFreightBill, getColumnConfig, saveColumnConfig, resetColumnConfig, getFreightDetail } from '@/api/api'
  import { postAction } from '@/api/manage'
  import Vue from 'vue'
  export default {
    name: "FreightBillList",
    mixins: [JeecgListMixin],
    components: {
      FreightBillModal,
      ColumnSettingPopover
    },
    data() {
      return {
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        queryParam: {
          billNo: '',
          carrierId: undefined,
          status: undefined,
          deliveryStatus: '0',
          beginTime: '',
          endTime: ''
        },
        carrierList: [],
        urlPath: '/freight/bill',
        prefixNo: 'WLBILL',
        settingDataIndex: [],
        defDataIndex: ['action','billNo','billTimeStr','customerNames','projectNames','carrierName','totalWeight','unitPrice','totalFreight','remark','status'],
        columns: [],
        defColumns: [
          {
            title: '操作',
            dataIndex: 'action',
            width: 180,
            align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '单据编号', dataIndex: 'billNo', width: 180 },
          { title: '日期', dataIndex: 'billTimeStr', width: 120 },
          { title: '出库客户', dataIndex: 'customerNames', width: 150, ellipsis: true },
          { title: '项目名称', dataIndex: 'projectNames', width: 150, ellipsis: true },
          { title: '结算方', dataIndex: 'carrierName', width: 150 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110 },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 110 },
          { title: '总运费(元)', dataIndex: 'totalFreight', width: 110 },
          { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true },
          {
            title: '状态', dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        expandedRowKeys: [],
        expandDetailColumns: [
          { title: '出库单号', dataIndex: 'billNo', width: 160 },
          { title: '出库日期', dataIndex: 'billTimeStr', width: 100 },
          { title: '客户名称', dataIndex: 'customerName', width: 120 },
          { title: '名称', dataIndex: 'materialName', width: 150 },
          { title: '规格', dataIndex: 'standard', width: 100 },
          { title: '型号', dataIndex: 'model', width: 100 },
          { title: '批号', dataIndex: 'batchNumber', width: 100 },
          { title: '数量', dataIndex: 'operNumber', width: 80 },
          { title: '单位', dataIndex: 'materialUnit', width: 60 },
          { title: '重量(吨)', dataIndex: 'itemWeight', width: 90 },
          { title: '仓库', dataIndex: 'depotName', width: 100 },
          { title: '业务员', dataIndex: 'salesMan', width: 80 }
        ],
        url: {
          list: "/freightHead/list",
          delete: "/freightHead/deleteFreightBill",
          deleteBatch: "/freightHead/deleteBatchFreightBill",
          batchSetStatusUrl: "/freightHead/batchSetStatus"
        }
      }
    },
    watch: {
      dataSource() {
        this.expandedRowKeys = []
      }
    },
    created() {
      this.initCarrierList();
      this.initColumnsSetting();
    },
    methods: {
      initColumnsSetting() {
        this.settingDataIndex = [...this.defDataIndex]
        this.applyColumnsOrdered(this.settingDataIndex)
        getColumnConfig({ pageCode: this.prefixNo }).then((res) => {
          if(res && res.code === 200 && res.data && res.data.columnConfig) {
            try {
              let configArr = JSON.parse(res.data.columnConfig)
              if(configArr && configArr.length > 0) {
                this.settingDataIndex = configArr
                this.applyColumnsOrdered(configArr)
                Vue.ls.set(this.prefixNo, configArr.join(','))
                return
              }
            } catch(e) { /* ignore */ }
          }
          let columnsStr = Vue.ls.get(this.prefixNo)
          if(columnsStr && columnsStr.indexOf(',')>-1) {
            this.settingDataIndex = columnsStr.split(',')
            this.applyColumnsOrdered(this.settingDataIndex)
            this.saveColumnsToServer(this.settingDataIndex)
          }
        }).catch(() => {
          let columnsStr = Vue.ls.get(this.prefixNo)
          if(columnsStr && columnsStr.indexOf(',')>-1) {
            this.settingDataIndex = columnsStr.split(',')
            this.applyColumnsOrdered(this.settingDataIndex)
          }
        })
      },
      applyColumnsOrdered(orderedArr) {
        let colMap = {}
        let fixedKeys = ['rowIndex', 'action']
        this.defColumns.forEach(col => { colMap[col.dataIndex] = col })
        let result = []
        orderedArr.forEach(di => {
          if(colMap[di]) {
            let c = Object.assign({}, colMap[di], { align: 'center' })
            if (!fixedKeys.includes(c.dataIndex)) {
              delete c.width
            }
            result.push(c)
          }
        })
        this.columns = result
      },
      saveColumnsToServer(dataIndexArr) {
        saveColumnConfig({
          pageCode: this.prefixNo,
          columnConfig: JSON.stringify(dataIndexArr)
        }).then(() => {
          Vue.ls.set(this.prefixNo, dataIndexArr.join(','))
        })
      },
      onColChange(orderedArr) {
        this.settingDataIndex = orderedArr
        this.applyColumnsOrdered(orderedArr)
        this.saveColumnsToServer(orderedArr)
      },
      myHandleDetail(record) {
        if(this.btnEnableList.indexOf(7)===-1) {
          this.$refs.modalForm.isCanBackCheck = false
        }
        this.$refs.modalForm.detail(record);
      },
      handleRestDefault() {
        Vue.ls.remove(this.prefixNo)
        resetColumnConfig({ pageCode: this.prefixNo })
        this.settingDataIndex = [...this.defDataIndex]
        this.applyColumnsOrdered(this.settingDataIndex)
      },
      initCarrierList() {
        selectAllFreightCarrier({}).then((res) => {
          if (res.code === 200) {
            this.carrierList = res.data || [];
          }
        })
      },
      onExpandFreight(expanded, record) {
        if (expanded) {
          this.expandedRowKeys = [...new Set([...this.expandedRowKeys, record.id])]
          this.$set(record, 'loading', true)
          this.$set(record, 'childrens', [])
          getFreightDetail({ id: record.id }).then((res) => {
            if (res.code === 200 && res.data) {
              this.$set(record, 'childrens', res.data.detailList || [])
            }
          }).finally(() => {
            this.$set(record, 'loading', false)
          })
        } else {
          this.expandedRowKeys = this.expandedRowKeys.filter(key => key !== record.id)
        }
      },
      searchQuery() {
        this.expandedRowKeys = []
        this.loadData(1);
      },
      searchReset() {
        this.expandedRowKeys = []
        this.queryParam = {
          billNo: '',
          carrierId: undefined,
          status: undefined,
          deliveryStatus: '0',
          beginTime: '',
          endTime: ''
        }
        this.loadData(1);
      },
      batchSetDeliveryStatus(deliveryStatus) {
        if (this.selectedRowKeys.length <= 0) {
          this.$message.warning('请选择一条记录！')
          return
        }
        let ids = this.selectedRowKeys.join(',')
        let that = this
        let msg = deliveryStatus === '1' ? '标记送达' : '取消送达'
        this.$confirm({
          title: '确认操作',
          content: '确定对选中的单据进行' + msg + '吗?',
          onOk() {
            postAction('/freightHead/batchSetDeliveryStatus', { deliveryStatus: deliveryStatus, ids: ids }).then((res) => {
              if (res.code === 200) {
                that.$message.success(msg + '成功')
                that.loadData()
              } else {
                that.$message.warning(res.data.message)
              }
            })
          }
        })
      },
      onDateChange(dates, dateStrings) {
        if (dates && dates.length === 2) {
          this.queryParam.beginTime = dateStrings[0];
          this.queryParam.endTime = dateStrings[1];
        } else {
          this.queryParam.beginTime = '';
          this.queryParam.endTime = '';
        }
      },
      handleDeleteBill(record) {
        let that = this;
        deleteFreightBill({ id: record.id }).then((res) => {
          if (res.code === 200) {
            that.$message.success('删除成功');
            that.loadData();
          } else {
            that.$message.warning(res.data.message);
          }
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
