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
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                </a-col>
              </span>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="margin-top: 5px">
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
        </div>
        <!-- table区域 -->
        <div style="margin-top:10px;">
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleDetail(record)">查看</a>
            </span>
            <template slot="customRenderStatus" slot-scope="status">
              <a-tag v-if="status === '0' || status === 0" color="red">未审核</a-tag>
              <a-tag v-if="status === '1' || status === 1" color="green">已审核</a-tag>
            </template>
          </a-table>
        </div>
        <freight-detail ref="modalDetail"></freight-detail>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import FreightDetail from '../freight/dialog/FreightDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier } from '@/api/api'
  export default {
    name: "FreightViewList",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover,
      FreightDetail
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
          status: undefined
        },
        carrierList: [],
        urlPath: '/financial/freight_view',
        pageName: 'freightViewList',
        defColumns: [
          {
            title: '操作',
            dataIndex: 'action',
            width: 80,
            align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '单据编号', dataIndex: 'billNo', width: 180 },
          { title: '日期', dataIndex: 'billTimeStr', width: 120 },
          { title: '结算方', dataIndex: 'carrierName', width: 150 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110 },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 110 },
          { title: '总运费(元)', dataIndex: 'totalFreight', width: 110 },
          {
            title: '状态', dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        defDataIndex: ['action', 'billNo', 'billTimeStr', 'carrierName', 'totalWeight', 'unitPrice', 'totalFreight', 'status'],
        url: {
          list: "/freightHead/list"
        }
      }
    },
    created() {
      this.initColumnsSetting();
      this.initCarrierList();
    },
    methods: {
      initCarrierList() {
        selectAllFreightCarrier({}).then((res) => {
          if (res.code === 200) {
            this.carrierList = res.data || [];
          }
        })
      },
      searchQuery() {
        this.loadData(1);
      },
      searchReset() {
        this.queryParam = {
          billNo: '',
          carrierId: undefined,
          status: undefined
        }
        this.loadData(1);
      },
      handleDetail(record) {
        this.$refs.modalDetail.show(record);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
