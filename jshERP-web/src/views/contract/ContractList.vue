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
                  <a-input placeholder="请输入合同编号" v-model="queryParam.contractNo"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="合同名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入合同名称" v-model="queryParam.contractName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="合同状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择" v-model="queryParam.status" allowClear>
                    <a-select-option value="草稿">草稿</a-select-option>
                    <a-select-option value="已签订">已签订</a-select-option>
                    <a-select-option value="履行中">履行中</a-select-option>
                    <a-select-option value="已完成">已完成</a-select-option>
                    <a-select-option value="已终止">已终止</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="border-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">新增</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">删除</a-button>
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
            bordered
            size="middle"
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical"/>
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => handleDelete(record.id)">
                <a>删除</a>
              </a-popconfirm>
            </span>
          </a-table>
        </div>
        <contract-modal ref="modalForm" @ok="modalFormOk"></contract-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import ContractModal from './modules/ContractModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  export default {
    name: "ContractList",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover,
      ContractModal
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
        queryParam: {},
        urlPath: '/contract/contract_list',
        pageName: 'contractList',
        defColumns: [
          {
            title: '#',
            dataIndex: 'rowIndex',
            key: 'rowIndex',
            width: 40,
            align: "center",
            customRender: function (t, r, index) {
              return parseInt(index) + 1;
            }
          },
          {
            title: '操作',
            dataIndex: 'action',
            scopedSlots: { customRender: 'action' },
            align: "center",
            width: 120
          },
          { title: '合同编号', dataIndex: 'contractNo', width: 140 },
          { title: '合同名称', dataIndex: 'contractName', width: 180 },
          { title: '项目名称', dataIndex: 'projectName', width: 150 },
          { title: '客户名称', dataIndex: 'organName', width: 150 },
          {
            title: '签订日期', dataIndex: 'signDate', width: 110,
            customRender: function (text) {
              return text ? text.substring(0, 10) : ''
            }
          },
          {
            title: '合同金额', dataIndex: 'amount', width: 120, align: 'right',
            customRender: function (text) {
              return text != null ? Number(text).toFixed(2) : ''
            }
          },
          { title: '状态', dataIndex: 'status', width: 80, align: 'center' },
          {
            title: '生效日期', dataIndex: 'effectDate', width: 110,
            customRender: function (text) {
              return text ? text.substring(0, 10) : ''
            }
          },
          {
            title: '到期日期', dataIndex: 'expireDate', width: 110,
            customRender: function (text) {
              return text ? text.substring(0, 10) : ''
            }
          },
          { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true }
        ],
        defDataIndex: ['rowIndex', 'action', 'contractNo', 'contractName', 'projectName',
          'organName', 'signDate', 'amount', 'status', 'effectDate', 'expireDate', 'remark'],
        url: {
          list: "/contract/list",
          delete: "/contract/delete",
          deleteBatch: "/contract/deleteBatch"
        }
      }
    },
    created() {
      this.initColumnsSetting();
    },
    methods: {
      searchQuery() {
        this.loadData(1);
      },
      searchReset() {
        this.queryParam = {}
        this.loadData(1);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
