<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入名称模糊查询" v-model="queryParam.name"></a-input>
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
        <freight-carrier-modal ref="modalForm" @ok="modalFormOk"></freight-carrier-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import FreightCarrierModal from './modules/FreightCarrierModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  export default {
    name: "FreightCarrierList",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover,
      FreightCarrierModal
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
        urlPath: '/freight/carrier',
        pageName: 'freightCarrierList',
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
            width: 150
          },
          { title: '名称', dataIndex: 'name', width: 150 },
          { title: '联系人', dataIndex: 'contacts', width: 100 },
          { title: '联系电话', dataIndex: 'phoneNum', width: 120 },
          { title: '开户行', dataIndex: 'bankName', width: 120 },
          { title: '银行账号', dataIndex: 'accountNumber', width: 160 },
          { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true }
        ],
        defDataIndex: ['rowIndex', 'action', 'name', 'contacts', 'phoneNum', 'bankName', 'accountNumber', 'remark'],
        url: {
          list: "/freightCarrier/list",
          delete: "/freightCarrier/delete",
          deleteBatch: "/freightCarrier/deleteBatch"
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
