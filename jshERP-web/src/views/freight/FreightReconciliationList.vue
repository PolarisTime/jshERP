<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="物流方" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择物流方" showSearch allow-clear optionFilterProp="children" v-model="queryParam.carrierId">
                    <a-select-option v-for="(item,index) in carrierList" :key="index" :value="item.id">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="日期范围" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width:100%"
                    v-model="dateRange"
                    format="YYYY-MM-DD"
                    :placeholder="['开始时间', '结束时间']"
                    @change="onDateChange"
                  />
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
        <!-- table区域 -->
        <div style="margin-top:10px;">
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="carrierId"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleViewDetail(record)">查看明细</a>
            </span>
            <template slot="totalWeightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="totalFreightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
          </a-table>
        </div>
        <freight-reconciliation-detail ref="reconciliationDetail"></freight-reconciliation-detail>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import FreightReconciliationDetail from './dialog/FreightReconciliationDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier } from '@/api/api'
  export default {
    name: "FreightReconciliationList",
    mixins: [JeecgListMixin],
    components: {
      FreightReconciliationDetail
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
          carrierId: undefined,
          beginTime: '',
          endTime: ''
        },
        dateRange: [],
        carrierList: [],
        urlPath: '/freight/reconciliation',
        columns: [
          {
            title: '操作',
            dataIndex: 'action',
            width: 100,
            align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '物流方', dataIndex: 'carrierName', width: 200 },
          { title: '物流单数量', dataIndex: 'billCount', width: 120, align: 'center' },
          {
            title: '总重量(吨)', dataIndex: 'totalWeight', width: 140, align: 'right',
            scopedSlots: { customRender: 'totalWeightRender' }
          },
          {
            title: '总运费(元)', dataIndex: 'totalFreight', width: 140, align: 'right',
            scopedSlots: { customRender: 'totalFreightRender' }
          }
        ],
        url: {
          list: "/freightHead/reconciliation"
        }
      }
    },
    created() {
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
      onDateChange(dates, dateStrings) {
        if (dates && dates.length === 2) {
          this.queryParam.beginTime = dateStrings[0];
          this.queryParam.endTime = dateStrings[1];
        } else {
          this.queryParam.beginTime = '';
          this.queryParam.endTime = '';
        }
      },
      searchQuery() {
        this.loadData(1);
      },
      searchReset() {
        this.queryParam = {
          carrierId: undefined,
          beginTime: '',
          endTime: ''
        }
        this.dateRange = [];
        this.loadData(1);
      },
      handleViewDetail(record) {
        this.$refs.reconciliationDetail.show(record, this.queryParam.beginTime, this.queryParam.endTime);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
