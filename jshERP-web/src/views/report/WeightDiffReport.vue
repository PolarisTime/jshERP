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
                <a-form-item label="单据类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="全部" allow-clear v-model="queryParam.subType">
                    <a-select-option value="采购">采购入库</a-select-option>
                    <a-select-option value="采购退货">采购退货</a-select-option>
                    <a-select-option value="销售">销售出库</a-select-option>
                    <a-select-option value="销售退货">销售退货</a-select-option>
                    <a-select-option value="其它">其它</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="供应商/客户" :labelCol="labelCol" :wrapperCol="wrapperCol">
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
            rowKey="itemId"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            @change="handleTableChange">
            <template slot="billNoRender" slot-scope="text">
              <a @click="handleViewBill(text)">{{ text }}</a>
            </template>
            <template slot="theoreticalWeightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="actualWeightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="weightDiffRender" slot-scope="text">
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
          <span>理论重量合计(吨)：<b>{{ summary.totalTheoreticalWeight }}</b></span>
          <a-divider type="vertical" />
          <span>过磅重量合计(吨)：<b>{{ summary.totalActualWeight }}</b></span>
          <a-divider type="vertical" />
          <span>差额合计(吨)：<b :style="{color: parseFloat(summary.totalDiff) > 0 ? 'green' : parseFloat(summary.totalDiff) < 0 ? 'red' : ''}">{{ summary.totalDiff }}</b></span>
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
          subType: undefined,
          organId: undefined,
          depotId: undefined
        },
        dateRange: [],
        organList: [],
        depotList: [],
        urlPath: '/report/weight_diff_report',
        pageName: 'weightDiffReport',
        defColumns: [
          { title: '单据编号', dataIndex: 'billNo', width: 160, scopedSlots: { customRender: 'billNoRender' } },
          { title: '单据类型', dataIndex: 'subType', width: 90 },
          { title: '日期', dataIndex: 'billTimeStr', width: 100 },
          { title: '商品名称', dataIndex: 'materialName', width: 150, ellipsis: true },
          { title: '规格', dataIndex: 'standard', width: 100 },
          { title: '型号', dataIndex: 'model', width: 100 },
          { title: '条码', dataIndex: 'barCode', width: 130 },
          { title: '数量', dataIndex: 'basicNumber', width: 80, align: 'right' },
          {
            title: '理论重量(吨)', dataIndex: 'theoreticalWeight', width: 120, align: 'right',
            scopedSlots: { customRender: 'theoreticalWeightRender' }
          },
          {
            title: '过磅重量(吨)', dataIndex: 'actualWeight', width: 120, align: 'right',
            scopedSlots: { customRender: 'actualWeightRender' }
          },
          {
            title: '差额(吨)', dataIndex: 'weightDiff', width: 100, align: 'right',
            scopedSlots: { customRender: 'weightDiffRender' }
          },
          { title: '供应商/客户', dataIndex: 'organName', width: 130, ellipsis: true },
          { title: '仓库', dataIndex: 'depotName', width: 100 }
        ],
        defDataIndex: ['billNo', 'subType', 'billTimeStr', 'materialName', 'standard', 'model', 'barCode', 'basicNumber', 'theoreticalWeight', 'actualWeight', 'weightDiff', 'organName', 'depotName'],
        url: {
          list: "/depotItem/weightDifference"
        },
        summary: {
          totalCount: 0,
          totalTheoreticalWeight: '0.00',
          totalActualWeight: '0.00',
          totalDiff: '0.00'
        }
      }
    },
    created() {
      this.initColumnsSetting();
      this.initOrganList();
      this.initDepotList();
    },
    watch: {
      dataSource() {
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
        getAction('/supplier/findBySelectSup', {}).then((res) => {
          if (res && res.code === 200) {
            this.organList = res.data || [];
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
          subType: undefined,
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
        let totalTheoreticalWeight = 0, totalActualWeight = 0;
        (this.dataSource || []).forEach(row => {
          totalTheoreticalWeight += parseFloat(row.theoreticalWeight || 0);
          totalActualWeight += parseFloat(row.actualWeight || 0);
        });
        this.summary = {
          totalCount: this.ipagination ? this.ipagination.total : (this.dataSource || []).length,
          totalTheoreticalWeight: totalTheoreticalWeight.toFixed(2),
          totalActualWeight: totalActualWeight.toFixed(2),
          totalDiff: (totalActualWeight - totalTheoreticalWeight).toFixed(2)
        };
      },
      handleExport() {
        if (!this.dataSource || this.dataSource.length === 0) {
          this.$message.warning('暂无数据可导出');
          return;
        }
        let headers = ['单据编号', '单据类型', '日期', '商品名称', '规格', '型号', '条码', '数量', '理论重量(吨)', '过磅重量(吨)', '差额(吨)', '供应商/客户', '仓库'];
        let rows = this.dataSource.map(row => [
          row.billNo || '',
          row.subType || '',
          row.billTimeStr || '',
          row.materialName || '',
          row.standard || '',
          row.model || '',
          row.barCode || '',
          row.basicNumber || 0,
          parseFloat(row.theoreticalWeight || 0).toFixed(2),
          parseFloat(row.actualWeight || 0).toFixed(2),
          parseFloat(row.weightDiff || 0).toFixed(2),
          row.organName || '',
          row.depotName || ''
        ]);
        let BOM = '\uFEFF';
        let csvContent = BOM + headers.join(',') + '\n' + rows.map(r => r.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');
        let blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        let link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = '过磅重量差异报表.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
