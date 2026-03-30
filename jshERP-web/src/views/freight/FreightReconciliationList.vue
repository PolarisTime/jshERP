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
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="margin-top: 5px">
          <a-button icon="download" @click="handleExportAll">导出汇总</a-button>
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
            rowKey="carrierId"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
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
            <template slot="paidAmountRender" slot-scope="text">
              <span style="color:green;">{{ parseFloat(text || 0).toFixed(2) }}</span>
            </template>
            <template slot="unpaidAmountRender" slot-scope="text">
              <span :style="{color: parseFloat(text || 0) > 0 ? 'red' : ''}">{{ parseFloat(text || 0).toFixed(2) }}</span>
            </template>
          </a-table>
        </div>
        <!-- 汇总统计栏 -->
        <div style="margin-top:10px;padding:8px 16px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
          <span>物流方数量：<b>{{ summary.carrierCount }}</b></span>
          <a-divider type="vertical" />
          <span>总运费：<b>{{ summary.totalFreight }}</b></span>
          <a-divider type="vertical" />
          <span>已付运费：<b style="color:green;">{{ summary.paidAmount }}</b></span>
          <a-divider type="vertical" />
          <span>未付运费：<b style="color:red;">{{ summary.unpaidAmount }}</b></span>
        </div>
        <!-- 明细弹窗 -->
        <freight-reconciliation-detail ref="reconciliationDetail"></freight-reconciliation-detail>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import FreightReconciliationDetail from './dialog/FreightReconciliationDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier } from '@/api/api'
  export default {
    name: "FreightReconciliationList",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover,
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
        pageName: 'freightReconciliationList',
        defColumns: [
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
          },
          {
            title: '已付运费(元)', dataIndex: 'paidAmount', width: 140, align: 'right',
            scopedSlots: { customRender: 'paidAmountRender' }
          },
          {
            title: '未付运费(元)', dataIndex: 'unpaidAmount', width: 140, align: 'right',
            scopedSlots: { customRender: 'unpaidAmountRender' }
          }
        ],
        defDataIndex: ['action', 'carrierName', 'billCount', 'totalWeight', 'totalFreight', 'paidAmount', 'unpaidAmount'],
        url: {
          list: "/freightHead/reconciliation"
        },
        summary: {
          carrierCount: 0,
          totalFreight: '0.00',
          paidAmount: '0.00',
          unpaidAmount: '0.00'
        }
      }
    },
    created() {
      this.initColumnsSetting();
      this.initCarrierList();
    },
    watch: {
      dataSource() {
        this.calcSummary();
      }
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
      calcSummary() {
        let totalFreight = 0, paidAmount = 0;
        (this.dataSource || []).forEach(row => {
          totalFreight += parseFloat(row.totalFreight || 0);
          paidAmount += parseFloat(row.paidAmount || 0);
        });
        this.summary = {
          carrierCount: (this.dataSource || []).length,
          totalFreight: totalFreight.toFixed(2),
          paidAmount: paidAmount.toFixed(2),
          unpaidAmount: (totalFreight - paidAmount).toFixed(2)
        };
      },
      handleViewDetail(record) {
        this.$refs.reconciliationDetail.show(record, this.queryParam.beginTime, this.queryParam.endTime);
      },
      handleExportAll() {
        if (!this.dataSource || this.dataSource.length === 0) {
          this.$message.warning('暂无数据可导出');
          return;
        }
        let headers = ['物流方', '物流单数量', '总重量(吨)', '总运费(元)', '已付运费(元)', '未付运费(元)'];
        let rows = this.dataSource.map(row => [
          row.carrierName || '',
          row.billCount || 0,
          parseFloat(row.totalWeight || 0).toFixed(2),
          parseFloat(row.totalFreight || 0).toFixed(2),
          parseFloat(row.paidAmount || 0).toFixed(2),
          parseFloat(row.unpaidAmount || 0).toFixed(2)
        ]);
        this.downloadCsv(headers, rows, '运费对账汇总');
      },
      downloadCsv(headers, rows, fileName) {
        let BOM = '\uFEFF';
        let csvContent = BOM + headers.join(',') + '\n' + rows.map(r => r.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');
        let blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        let link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName + '.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
