<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
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
                <a-form-item label="付款状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select placeholder="请选择付款状态" allow-clear v-model="queryParam.paymentStatus">
                    <a-select-option value="0">未付款</a-select-option>
                    <a-select-option value="1">已付款</a-select-option>
                    <a-select-option value="2">部分付款</a-select-option>
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
          <a-button type="primary" icon="check" @click="handleMarkPaid" :disabled="selectedRowKeys.length===0">标记已付款</a-button>
          <a-button icon="undo" @click="handleCancelPayment" :disabled="selectedRowKeys.length===0">取消付款</a-button>
          <a-button icon="export" @click="handleExportSelected" :disabled="selectedRowKeys.length===0">导出选中</a-button>
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
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleDetail(record)">查看</a>
            </span>
            <template slot="paymentStatusRender" slot-scope="text">
              <a-tag v-if="text === '1'" color="green">已付款</a-tag>
              <a-tag v-else-if="text === '2'" color="orange">部分付款</a-tag>
              <a-tag v-else color="red">未付款</a-tag>
            </template>
            <template slot="totalWeightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="totalFreightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="paidAmountRender" slot-scope="text">
              <span style="color:green;">{{ parseFloat(text || 0).toFixed(2) }}</span>
            </template>
            <template slot="unpaidAmountRender" slot-scope="text, record">
              <span :style="{color: calcUnpaid(record) > 0 ? 'red' : ''}">{{ calcUnpaid(record).toFixed(2) }}</span>
            </template>
          </a-table>
        </div>
        <!-- 汇总统计栏 -->
        <div style="margin-top:10px;padding:8px 16px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
          <span>单据总数：<b>{{ summary.totalCount }}</b></span>
          <a-divider type="vertical" />
          <span>应付总额：<b>{{ summary.totalFreight }}</b></span>
          <a-divider type="vertical" />
          <span>已付总额：<b style="color:green;">{{ summary.paidAmount }}</b></span>
          <a-divider type="vertical" />
          <span>未付总额：<b style="color:red;">{{ summary.unpaidAmount }}</b></span>
        </div>
        <!-- 详情弹窗 -->
        <freight-detail ref="modalDetail"></freight-detail>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import FreightDetail from '../freight/dialog/FreightDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { selectAllFreightCarrier, batchSetPaymentStatus, cancelPayment, getFreightDetail } from '@/api/api'
  export default {
    name: "FreightTransactionList",
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
          carrierId: undefined,
          status: '1',
          paymentStatus: undefined,
          beginTime: '',
          endTime: ''
        },
        dateRange: [],
        carrierList: [],
        selectedRowKeys: [],
        urlPath: '/financial/freight_transaction',
        pageName: 'freightTransactionList',
        defColumns: [
          {
            title: '操作',
            dataIndex: 'action',
            width: 80,
            align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '单据编号', dataIndex: 'billNo', width: 160 },
          { title: '日期', dataIndex: 'billTimeStr', width: 100 },
          { title: '结算方', dataIndex: 'carrierName', width: 130 },
          {
            title: '总重量(吨)', dataIndex: 'totalWeight', width: 110, align: 'right',
            scopedSlots: { customRender: 'totalWeightRender' }
          },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 110, align: 'right' },
          {
            title: '总运费(元)', dataIndex: 'totalFreight', width: 110, align: 'right',
            scopedSlots: { customRender: 'totalFreightRender' }
          },
          {
            title: '付款状态', dataIndex: 'paymentStatus', width: 100, align: 'center',
            scopedSlots: { customRender: 'paymentStatusRender' }
          },
          {
            title: '已付金额', dataIndex: 'paidAmount', width: 100, align: 'right',
            scopedSlots: { customRender: 'paidAmountRender' }
          },
          {
            title: '未付金额', dataIndex: 'unpaidAmount', width: 100, align: 'right',
            scopedSlots: { customRender: 'unpaidAmountRender' }
          },
          { title: '付款时间', dataIndex: 'paymentTimeStr', width: 140 },
          { title: '操作人', dataIndex: 'paymentOperatorName', width: 80 },
          { title: '备注', dataIndex: 'remark', width: 120, ellipsis: true }
        ],
        defDataIndex: ['action', 'billNo', 'billTimeStr', 'carrierName', 'totalWeight', 'unitPrice', 'totalFreight', 'paymentStatus', 'paidAmount', 'unpaidAmount', 'paymentTimeStr', 'paymentOperatorName', 'remark'],
        url: {
          list: "/freightHead/list"
        },
        summary: {
          totalCount: 0,
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
        this.selectedRowKeys = [];
        this.loadData(1);
      },
      searchReset() {
        this.queryParam = {
          carrierId: undefined,
          status: '1',
          paymentStatus: undefined,
          beginTime: '',
          endTime: ''
        }
        this.dateRange = [];
        this.selectedRowKeys = [];
        this.loadData(1);
      },
      onSelectChange(selectedRowKeys) {
        this.selectedRowKeys = selectedRowKeys;
      },
      handleDetail(record) {
        this.$refs.modalDetail.show(record);
      },
      calcUnpaid(record) {
        let totalFreight = parseFloat(record.totalFreight || 0);
        let paidAmount = parseFloat(record.paidAmount || 0);
        return totalFreight - paidAmount;
      },
      calcSummary() {
        let totalFreight = 0, paidAmount = 0;
        (this.dataSource || []).forEach(row => {
          totalFreight += parseFloat(row.totalFreight || 0);
          paidAmount += parseFloat(row.paidAmount || 0);
        });
        this.summary = {
          totalCount: this.ipagination ? this.ipagination.total : (this.dataSource || []).length,
          totalFreight: totalFreight.toFixed(2),
          paidAmount: paidAmount.toFixed(2),
          unpaidAmount: (totalFreight - paidAmount).toFixed(2)
        };
      },
      handleMarkPaid() {
        let that = this;
        let validIds = this.selectedRowKeys.filter(id => {
          let row = this.dataSource.find(r => r.id === id);
          return row && row.paymentStatus !== '1';
        });
        if (validIds.length === 0) {
          this.$message.warning('所选单据均已付款，无需重复标记');
          return;
        }
        this.$confirm({
          title: '确认标记已付款',
          content: '即将标记 ' + validIds.length + ' 条单据为已付款，是否继续？',
          onOk() {
            let ids = validIds.join(',');
            batchSetPaymentStatus({ paymentStatus: '1', paidAmount: 0, ids: ids }).then((res) => {
              if (res.code === 200) {
                that.$message.success('标记成功');
                that.selectedRowKeys = [];
                that.loadData();
              } else {
                that.$message.warning(res.data || '操作失败');
              }
            })
          }
        });
      },
      handleCancelPayment() {
        let that = this;
        let validIds = this.selectedRowKeys.filter(id => {
          let row = this.dataSource.find(r => r.id === id);
          return row && row.paymentStatus !== '0';
        });
        if (validIds.length === 0) {
          this.$message.warning('所选单据均未付款，无需取消');
          return;
        }
        this.$confirm({
          title: '确认取消付款标记',
          content: '即将取消 ' + validIds.length + ' 条单据的付款标记，是否继续？',
          onOk() {
            cancelPayment({ ids: validIds.join(',') }).then((res) => {
              if (res.code === 200) {
                that.$message.success('取消成功');
                that.selectedRowKeys = [];
                that.loadData();
              } else {
                that.$message.warning(res.data || '操作失败');
              }
            })
          }
        });
      },
      handleExportSelected() {
        let keySet = new Set(this.selectedRowKeys.map(String));
        let exportData = this.dataSource.filter(r => keySet.has(String(r.id)));
        if (!exportData || exportData.length === 0) {
          this.$message.warning('暂无选中数据可导出');
          return;
        }
        this.loading = true;
        let promises = exportData.map(row => getFreightDetail({ id: row.id }));
        Promise.all(promises).then(results => {
          let paymentStatusMap = { '0': '未付款', '1': '已付款', '2': '部分付款' };
          let headers = [
            '运费单号', '日期', '结算方', '总重量(吨)', '单价(元/吨)', '总运费(元)',
            '付款状态', '已付金额', '未付金额', '付款时间', '操作人', '备注',
            '出库单号', '出库日期', '客户名称', '商品名称', '规格', '材质', '批号',
            '数量', '单位', '重量(吨)', '仓库'
          ];
          let rows = [];
          exportData.forEach((row, idx) => {
            let detail = (results[idx] && results[idx].code === 200) ? results[idx].data : {};
            let items = detail.detailList || [];
            let totalFreight = parseFloat(row.totalFreight || 0);
            let paidAmount = parseFloat(row.paidAmount || 0);
            let ps = row.paymentStatus != null ? row.paymentStatus : '0';
            let headCols = [
              row.billNo || '',
              row.billTimeStr || '',
              row.carrierName || '',
              parseFloat(row.totalWeight || 0).toFixed(2),
              row.unitPrice || 0,
              totalFreight.toFixed(2),
              paymentStatusMap[ps] || '未付款',
              paidAmount.toFixed(2),
              (totalFreight - paidAmount).toFixed(2),
              row.paymentTimeStr || '',
              row.paymentOperatorName || '',
              row.remark || ''
            ];
            if (items.length === 0) {
              rows.push([...headCols, '', '', '', '', '', '', '', '', '', '', '']);
            } else {
              items.forEach((item, i) => {
                let prefix = i === 0 ? headCols : headCols.map(() => '');
                rows.push([
                  ...prefix,
                  item.billNo || '',
                  item.billTimeStr || '',
                  item.customerName || '',
                  item.materialName || '',
                  item.standard || '',
                  item.model || '',
                  item.batchNumber || '',
                  item.operNumber || '',
                  item.materialUnit || '',
                  item.itemWeight || '',
                  item.depotName || ''
                ]);
              });
            }
          });
          let BOM = '\uFEFF';
          let csvContent = BOM + headers.join(',') + '\n' + rows.map(r => r.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');
          let blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
          let link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.download = '运费往来_选中明细.csv';
          link.click();
          URL.revokeObjectURL(link.href);
          this.$message.success('导出成功，共 ' + exportData.length + ' 张单据');
        }).catch(() => {
          this.$message.error('获取明细数据失败');
        }).finally(() => {
          this.loading = false;
        });
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
