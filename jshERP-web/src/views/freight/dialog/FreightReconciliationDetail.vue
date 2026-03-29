<template>
  <a-modal
    title="对账明细"
    :width="1100"
    :visible="visible"
    :maskClosable="false"
    :footer="null"
    @cancel="visible=false">
    <div style="margin-bottom:10px;">
      <span style="font-weight:bold;font-size:15px;">{{ carrierName }}</span>
      <span v-if="beginTime || endTime" style="margin-left:16px;color:#999;">
        {{ beginTime || '...' }} ~ {{ endTime || '...' }}
      </span>
    </div>
    <a-table
      size="middle"
      bordered
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="ipagination"
      :loading="loading"
      @change="handleTableChange">
      <span slot="action" slot-scope="text, record">
        <a @click="handleViewBill(record)">查看</a>
      </span>
      <template slot="customRenderStatus" slot-scope="status">
        <a-tag v-if="status === '0' || status === 0" color="red">未审核</a-tag>
        <a-tag v-if="status === '1' || status === 1" color="green">已审核</a-tag>
      </template>
    </a-table>
    <freight-detail ref="modalDetail"></freight-detail>
  </a-modal>
</template>
<script>
  import FreightDetail from './FreightDetail'
  import { getAction } from '@/api/manage'
  export default {
    name: "FreightReconciliationDetail",
    components: {
      FreightDetail
    },
    data() {
      return {
        visible: false,
        loading: false,
        carrierName: '',
        carrierId: null,
        beginTime: '',
        endTime: '',
        dataSource: [],
        ipagination: {
          current: 1,
          pageSize: 10,
          total: 0,
          showTotal: (total) => `共${total}条`,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50']
        },
        columns: [
          {
            title: '操作', dataIndex: 'action', width: 80, align: "center",
            scopedSlots: { customRender: 'action' }
          },
          { title: '单据编号', dataIndex: 'billNo', width: 200 },
          { title: '日期', dataIndex: 'billTimeStr', width: 120 },
          { title: '结算方', dataIndex: 'carrierName', width: 150 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 120 },
          { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 120 },
          { title: '总运费(元)', dataIndex: 'totalFreight', width: 120 },
          {
            title: '状态', dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ]
      }
    },
    methods: {
      show(record, beginTime, endTime) {
        this.carrierId = record.carrierId;
        this.carrierName = record.carrierName || '';
        this.beginTime = beginTime || '';
        this.endTime = endTime || '';
        this.visible = true;
        this.ipagination.current = 1;
        this.loadData();
      },
      loadData() {
        this.loading = true;
        let params = {
          search: JSON.stringify({
            carrierId: this.carrierId,
            status: '1',
            beginTime: this.beginTime,
            endTime: this.endTime
          }),
          currentPage: this.ipagination.current,
          pageSize: this.ipagination.pageSize
        };
        getAction('/freightHead/list', params).then((res) => {
          if (res.code === 200) {
            this.dataSource = res.data.rows || [];
            this.ipagination.total = res.data.total || 0;
          }
        }).finally(() => {
          this.loading = false;
        })
      },
      handleTableChange(pagination) {
        this.ipagination.current = pagination.current;
        this.ipagination.pageSize = pagination.pageSize;
        this.loadData();
      },
      handleViewBill(record) {
        this.$refs.modalDetail.show(record);
      }
    }
  }
</script>
