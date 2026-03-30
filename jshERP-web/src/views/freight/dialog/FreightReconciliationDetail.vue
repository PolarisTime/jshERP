<template>
  <a-modal
    title="对账明细"
    :width="1300"
    :visible="visible"
    :maskClosable="false"
    :footer="null"
    @cancel="visible=false">
    <!-- 信息栏 -->
    <div style="margin-bottom:10px;">
      <span style="font-weight:bold;font-size:15px;">{{ carrierName }}</span>
      <span v-if="beginTime || endTime" style="margin-left:16px;color:#999;">
        {{ beginTime || '...' }} ~ {{ endTime || '...' }}
      </span>
    </div>
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="24">
          <a-col :md="6" :sm="24">
            <a-form-item label="付款状态" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-select placeholder="全部" allow-clear v-model="paymentStatusFilter" @change="onFilterChange">
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="0">未付款</a-select-option>
                <a-select-option value="1">已付款</a-select-option>
                <a-select-option value="2">部分付款</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="onFilterChange">查询</a-button>
            </a-col>
          </span>
        </a-row>
      </a-form>
    </div>
    <!-- 操作按钮区域 -->
    <div class="table-operator" style="margin-top: 5px">
      <a-button type="primary" icon="check" @click="handleMarkPaid" :disabled="selectedRowKeys.length===0">标记已付款</a-button>
      <a-button icon="undo" @click="handleCancelPayment" :disabled="selectedRowKeys.length===0">取消付款</a-button>
      <a-button icon="download" @click="handleExport">导出CSV</a-button>
    </div>
    <!-- table区域 -->
    <div style="margin-bottom: 8px; text-align: right;">
      <column-setting-popover
        :defColumns="defColumns"
        :settingDataIndex.sync="settingDataIndex"
        @change="onColChange"
        @reset="handleRestDefault"
      />
    </div>
    <div>
      <a-table
        ref="table"
        size="middle"
        bordered
        rowKey="id"
        :columns="columns"
        :components="handleDrag(columns)"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange, getCheckboxProps: getCheckboxProps}"
        @change="handleTableChange">
        <span slot="action" slot-scope="text, record">
          <a @click="handleViewBill(record)">查看</a>
        </span>
        <template slot="paymentStatusRender" slot-scope="text">
          <a-tag v-if="text === '1'" color="green">已付款</a-tag>
          <a-tag v-else-if="text === '2'" color="orange">部分付款</a-tag>
          <a-tag v-else color="red">未付款</a-tag>
        </template>
        <template slot="totalFreightRender" slot-scope="text">
          {{ parseFloat(text || 0).toFixed(2) }}
        </template>
        <template slot="paidAmountRender" slot-scope="text">
          <span style="color:green;">{{ parseFloat(text || 0).toFixed(2) }}</span>
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
    <!-- 物流单详情弹窗 -->
    <freight-bill-modal ref="modalDetail" @ok="loadData"></freight-bill-modal>
  </a-modal>
</template>
<script>
  import FreightBillModal from '../modules/FreightBillModal'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getReconciliationDetail, batchSetPaymentStatus, cancelPayment, getFreightDetail } from '@/api/api'
  export default {
    name: "FreightReconciliationDetail",
    mixins: [JeecgListMixin],
    components: {
      FreightBillModal,
      ColumnSettingPopover
    },
    data() {
      return {
        visible: false,
        loading: false,
        carrierName: '',
        carrierId: null,
        beginTime: '',
        endTime: '',
        paymentStatusFilter: undefined,
        selectedRowKeys: [],
        dataSource: [],
        ipagination: {
          current: 1,
          pageSize: 10,
          total: 0,
          showTotal: (total) => `共${total}条`,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50']
        },
        disableMixinCreated: true,
        pageName: 'freightReconciliationDetail',
        defColumns: [
          {
            title: '操作', dataIndex: 'action', width: 80, align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '单据编号', dataIndex: 'billNo', width: 160 },
          { title: '日期', dataIndex: 'billTimeStr', width: 100 },
          { title: '结算方', dataIndex: 'carrierName', width: 130 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110, align: 'right' },
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
          { title: '付款时间', dataIndex: 'paymentTimeStr', width: 140 },
          { title: '操作人', dataIndex: 'paymentOperatorName', width: 80 },
          { title: '备注', dataIndex: 'remark', width: 120, ellipsis: true }
        ],
        defDataIndex: ['action', 'billNo', 'billTimeStr', 'carrierName', 'totalWeight', 'unitPrice', 'totalFreight', 'paymentStatus', 'paidAmount', 'paymentTimeStr', 'paymentOperatorName', 'remark'],
        summary: {
          totalCount: 0,
          totalFreight: '0.00',
          paidAmount: '0.00',
          unpaidAmount: '0.00'
        }
      }
    },
    created() {
      this.initColumnsSetting()
    },
    methods: {
      show(record, beginTime, endTime) {
        this.carrierId = record.carrierId;
        this.carrierName = record.carrierName || '';
        this.beginTime = beginTime || '';
        this.endTime = endTime || '';
        this.paymentStatusFilter = undefined;
        this.selectedRowKeys = [];
        this.visible = true;
        this.ipagination.current = 1;
        this.loadData();
      },
      loadData() {
        this.loading = true;
        let params = {
          search: JSON.stringify({
            carrierId: this.carrierId,
            beginTime: this.beginTime,
            endTime: this.endTime,
            paymentStatus: this.paymentStatusFilter || ''
          }),
          currentPage: this.ipagination.current,
          pageSize: this.ipagination.pageSize
        };
        getReconciliationDetail(params).then((res) => {
          if (res.code === 200) {
            this.dataSource = res.data.rows || [];
            this.ipagination.total = res.data.total || 0;
            this.calcSummary();
          }
        }).finally(() => {
          this.loading = false;
        })
      },
      calcSummary() {
        let totalFreight = 0, paidAmount = 0;
        this.dataSource.forEach(row => {
          totalFreight += parseFloat(row.totalFreight || 0);
          paidAmount += parseFloat(row.paidAmount || 0);
        });
        this.summary = {
          totalCount: this.ipagination.total,
          totalFreight: totalFreight.toFixed(2),
          paidAmount: paidAmount.toFixed(2),
          unpaidAmount: (totalFreight - paidAmount).toFixed(2)
        };
      },
      onFilterChange() {
        this.ipagination.current = 1;
        this.selectedRowKeys = [];
        this.loadData();
      },
      onSelectChange(selectedRowKeys) {
        this.selectedRowKeys = selectedRowKeys;
      },
      getCheckboxProps(record) {
        return {
          props: {}
        }
      },
      handleMarkPaid() {
        let that = this;
        // 过滤掉已付款的单据
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
          content: `即将标记 ${validIds.length} 条单据为已付款，是否继续？`,
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
        // 过滤出有付款记录的单据
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
          content: `即将取消 ${validIds.length} 条单据的付款标记，是否继续？`,
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
      handleExport() {
        // 有勾选则导出勾选的，否则导出当前页全部
        let exportData = this.dataSource;
        if (this.selectedRowKeys.length > 0) {
          let keySet = new Set(this.selectedRowKeys.map(String));
          exportData = this.dataSource.filter(r => keySet.has(String(r.id)));
        }
        if (!exportData || exportData.length === 0) {
          this.$message.warning('暂无数据可导出');
          return;
        }
        this.loading = true;
        // 并行获取每条运费单的出库单商品明细
        let promises = exportData.map(row => getFreightDetail({ id: row.id }));
        Promise.all(promises).then(results => {
          let paymentStatusMap = { '0': '未付款', '1': '已付款', '2': '部分付款' };
          let headers = [
            '运费单号', '日期', '结算方', '总重量(吨)', '单价(元/吨)', '总运费(元)',
            '付款状态', '已付金额', '付款时间', '操作人', '备注',
            '出库单号', '出库日期', '客户名称', '商品名称', '规格', '型号', '批号',
            '数量', '单位', '重量(吨)', '仓库'
          ];
          let rows = [];
          exportData.forEach((row, idx) => {
            let detail = (results[idx] && results[idx].code === 200) ? results[idx].data : {};
            let items = detail.detailList || [];
            let headCols = [
              row.billNo || '',
              row.billTimeStr || '',
              row.carrierName || '',
              row.totalWeight || 0,
              row.unitPrice || 0,
              parseFloat(row.totalFreight || 0).toFixed(2),
              paymentStatusMap[row.paymentStatus] || '未付款',
              parseFloat(row.paidAmount || 0).toFixed(2),
              row.paymentTimeStr || '',
              row.paymentOperatorName || '',
              row.remark || ''
            ];
            if (items.length === 0) {
              // 无明细时仅输出运费单行
              rows.push([...headCols, '', '', '', '', '', '', '', '', '', '', '']);
            } else {
              items.forEach((item, i) => {
                // 首行输出运费单信息，后续行运费单列留空
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
          let fileName = '运费对账明细' + (this.carrierName ? '-' + this.carrierName : '');
          this.downloadCsv(headers, rows, fileName);
        }).catch(() => {
          this.$message.error('获取明细数据失败');
        }).finally(() => {
          this.loading = false;
        });
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
      },
      handleTableChange(pagination) {
        this.ipagination.current = pagination.current;
        this.ipagination.pageSize = pagination.pageSize;
        this.loadData();
      },
      handleViewBill(record) {
        this.$refs.modalDetail.detail(record);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
