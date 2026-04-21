<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
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
              <a-col :md="6" :sm="24">
                <a-form-item label="供应商" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择" showSearch allow-clear optionFilterProp="children" v-model="queryParam.organId">
                    <a-select-option v-for="item in organList" :key="item.id" :value="item.id">{{ item.supplier }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="仓库" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择" allow-clear v-model="queryParam.depotId">
                    <a-select-option v-for="item in depotList" :key="item.id" :value="item.id">{{ item.depotName }}</a-select-option>
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
          <a-button v-print="'#weightDiffPrint'" icon="printer">打印</a-button>
          <a-button icon="download" @click="handleExport">导出</a-button>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
        </div>
        <!-- table区域 -->
        <section ref="print" id="weightDiffPrint">
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="rowId"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            @change="handleTableChange">
            <template slot="purchaseBillNoRender" slot-scope="text">
              <span v-for="(no, idx) in (text||'').split(',')" :key="idx">
                <a @click="handleViewBill(no.trim())">{{ no.trim() }}</a>
                <span v-if="idx < (text||'').split(',').length - 1">, </span>
              </span>
            </template>
            <template slot="saleBillNoRender" slot-scope="text">
              <span v-if="text">
                <span v-for="(no, idx) in text.split(',')" :key="idx">
                  <a @click="handleViewBill(no.trim())">{{ no.trim() }}</a>
                  <span v-if="idx < text.split(',').length - 1">, </span>
                </span>
              </span>
              <span v-else>-</span>
            </template>
            <template slot="weightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(3) }}
            </template>
            <template slot="weightDiffRender" slot-scope="text">
              <span :style="{color: parseFloat(text || 0) > 0 ? 'green' : parseFloat(text || 0) < 0 ? 'red' : ''}">
                {{ parseFloat(text || 0).toFixed(3) }}
              </span>
            </template>
            <template slot="priceRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="diffAmountRender" slot-scope="text">
              <span :style="{color: parseFloat(text || 0) > 0 ? 'green' : parseFloat(text || 0) < 0 ? 'red' : ''}">
                {{ parseFloat(text || 0).toFixed(2) }}
              </span>
            </template>
          </a-table>
        </section>
        <!-- 汇总统计栏 -->
        <div style="margin-top:10px;padding:8px 16px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
          <span>记录总数：<b>{{ summary.totalCount }}</b></span>
          <a-divider type="vertical" />
          <span>入库理论重量合计(吨)：<b>{{ summary.totalInWeight }}</b></span>
          <a-divider type="vertical" />
          <span>出库过磅重量合计(吨)：<b>{{ summary.totalOutWeight }}</b></span>
          <a-divider type="vertical" />
          <span>差额合计(吨)：<b :style="{color: parseFloat(summary.totalDiff) > 0 ? 'green' : parseFloat(summary.totalDiff) < 0 ? 'red' : ''}">{{ summary.totalDiff }}</b></span>
          <a-divider type="vertical" />
          <span>差额金额合计：<b :style="{color: parseFloat(summary.totalDiffAmount) > 0 ? 'green' : parseFloat(summary.totalDiffAmount) < 0 ? 'red' : ''}">{{ summary.totalDiffAmount }}</b></span>
        </div>
        <bill-detail ref="billDetailModal"></bill-detail>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import BillDetail from '../bill/dialog/BillDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getAction } from '@/api/manage'
  import { findBySelectSup } from '@/api/api'
  export default {
    name: "WeightDiffReport",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover,
      BillDetail
    },
    data() {
      return {
        labelCol: { span: 5 },
        wrapperCol: { span: 18, offset: 1 },
        queryParam: {
          beginTime: '',
          endTime: '',
          organId: undefined,
          depotId: undefined
        },
        dateRange: [],
        organList: [],
        depotList: [],
        urlPath: '/report/weight_diff_report',
        pageName: 'weightDiffReport',
        defColumns: [
          { title: '供应商', dataIndex: 'organName', width: 130, ellipsis: true },
          { title: '商品名称', dataIndex: 'materialName', width: 150, ellipsis: true },
          { title: '规格', dataIndex: 'standard', width: 100 },
          { title: '材质', dataIndex: 'model', width: 100 },
          { title: '条码', dataIndex: 'barCode', width: 130 },
          { title: '批号', dataIndex: 'batchNumber', width: 120 },
          { title: '数量', dataIndex: 'operNumber', width: 80, align: 'right' },
          { title: '入库单号', dataIndex: 'purchaseBillNo', width: 170, scopedSlots: { customRender: 'purchaseBillNoRender' } },
          { title: '入库理论重量(吨)', dataIndex: 'inTheoreticalWeight', width: 140, align: 'right', scopedSlots: { customRender: 'weightRender' } },
          { title: '出库单号', dataIndex: 'saleBillNo', width: 170, scopedSlots: { customRender: 'saleBillNoRender' } },
          { title: '出库过磅重量(吨)', dataIndex: 'outActualWeight', width: 140, align: 'right', scopedSlots: { customRender: 'weightRender' } },
          { title: '差额(吨)', dataIndex: 'weightDiff', width: 110, align: 'right', scopedSlots: { customRender: 'weightDiffRender' } },
          { title: '采购单价', dataIndex: 'purchaseUnitPrice', width: 100, align: 'right', scopedSlots: { customRender: 'priceRender' } },
          { title: '差额金额', dataIndex: 'diffAmount', width: 110, align: 'right', scopedSlots: { customRender: 'diffAmountRender' } },
          { title: '仓库', dataIndex: 'depotName', width: 100 }
        ],
        defDataIndex: ['organName','materialName','standard','model','barCode','batchNumber','operNumber','purchaseBillNo','inTheoreticalWeight','saleBillNo','outActualWeight','weightDiff','purchaseUnitPrice','diffAmount','depotName'],
        url: {
          list: "/depotItem/weightDifference"
        },
        summary: {
          totalCount: 0,
          totalInWeight: '0.000',
          totalOutWeight: '0.000',
          totalDiff: '0.000',
          totalDiffAmount: '0.00'
        }
      }
    },
    created() {
      this.initColumnsSetting();
      this.initOrganList();
      this.initDepotList();
    },
    watch: {
      dataSource(val) {
        // 生成唯一rowKey
        if(val && val.length) {
          val.forEach((row, idx) => { row.rowId = idx })
        }
        this.calcSummary();
      }
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam);
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      initOrganList() {
        findBySelectSup({}).then((res) => {
          if (res) {
            this.organList = res;
          }
        })
      },
      initDepotList() {
        getAction('/depot/findDepotByCurrentUser', {}).then((res) => {
          if (res && res.code === 200) {
            this.depotList = res.data || [];
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
          beginTime: '',
          endTime: '',
          organId: undefined,
          depotId: undefined
        }
        this.dateRange = [];
        this.loadData(1);
      },
      handleViewBill(billNo) {
        if (billNo) {
          this.$refs.billDetailModal.myHandleDetail(billNo.trim());
        }
      },
      calcSummary() {
        let totalIn = 0, totalOut = 0, totalDiffAmount = 0;
        (this.dataSource || []).forEach(row => {
          totalIn += parseFloat(row.inTheoreticalWeight || 0);
          totalOut += parseFloat(row.outActualWeight || 0);
          totalDiffAmount += parseFloat(row.diffAmount || 0);
        });
        this.summary = {
          totalCount: this.ipagination ? this.ipagination.total : (this.dataSource || []).length,
          totalInWeight: totalIn.toFixed(3),
          totalOutWeight: totalOut.toFixed(3),
          totalDiff: (totalIn - totalOut).toFixed(3),
          totalDiffAmount: totalDiffAmount.toFixed(2)
        };
      },
      handleExport() {
        if (!this.dataSource || this.dataSource.length === 0) {
          this.$message.warning('暂无数据可导出');
          return;
        }
        let headers = ['供应商','商品名称','规格','材质','条码','批号','数量','入库单号','入库理论重量(吨)','出库单号','出库过磅重量(吨)','差额(吨)','采购单价','差额金额','仓库'];
        let rows = this.dataSource.map(row => [
          row.organName || '',
          row.materialName || '',
          row.standard || '',
          row.model || '',
          row.barCode || '',
          row.batchNumber || '',
          row.operNumber || 0,
          row.purchaseBillNo || '',
          parseFloat(row.inTheoreticalWeight || 0).toFixed(3),
          row.saleBillNo || '',
          parseFloat(row.outActualWeight || 0).toFixed(3),
          parseFloat(row.weightDiff || 0).toFixed(3),
          parseFloat(row.purchaseUnitPrice || 0).toFixed(2),
          parseFloat(row.diffAmount || 0).toFixed(2),
          row.depotName || ''
        ]);
        let BOM = '\uFEFF';
        let csvContent = BOM + headers.join(',') + '\n' + rows.map(r => r.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');
        let blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        let link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = '长短款报表.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
