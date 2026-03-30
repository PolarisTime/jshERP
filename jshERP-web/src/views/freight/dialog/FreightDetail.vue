<template>
  <div ref="container">
    <a-modal
      title="运费单详情"
      :width="900"
      :visible="visible"
      :maskClosable="true"
      :footer="null"
      @cancel="handleCancel"
      style="top:5%;height:90%;">
      <a-spin :spinning="loading">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom:16px;">
          <a-descriptions-item label="单据编号">{{ detail.billNo }}</a-descriptions-item>
          <a-descriptions-item label="日期">{{ detail.billTimeStr }}</a-descriptions-item>
          <a-descriptions-item label="结算方">{{ detail.carrierName }}</a-descriptions-item>
          <a-descriptions-item label="单价(元/吨)">{{ detail.unitPrice }}</a-descriptions-item>
          <a-descriptions-item label="总重量(吨)">{{ detail.totalWeight }}</a-descriptions-item>
          <a-descriptions-item label="总运费(元)">{{ detail.totalFreight }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag v-if="detail.status === '0' || detail.status === 0" color="red">未审核</a-tag>
            <a-tag v-if="detail.status === '1' || detail.status === 1" color="green">已审核</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div style="display: flex; justify-content: space-between; align-items: center; margin: 10px 0;">
          <h4 style="margin: 0;">出库单明细</h4>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
        </div>
        <a-table
          size="small"
          bordered
          rowKey="id"
          :columns="columns"
          :components="handleDrag(columns)"
          :dataSource="detailList"
          :pagination="false">
        </a-table>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import { getFreightDetail } from '@/api/api'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  export default {
    name: "FreightDetail",
    mixins: [JeecgListMixin],
    components: {
      ColumnSettingPopover
    },
    data() {
      return {
        visible: false,
        loading: false,
        detail: {},
        detailList: [],
        disableMixinCreated: true,
        pageName: 'freightDetail',
        defColumns: [
          { title: '出库单号', dataIndex: 'billNo', width: 180 },
          { title: '客户名称', dataIndex: 'customerName', width: 150 },
          { title: '出库日期', dataIndex: 'billTimeStr', width: 120 },
          { title: '重量(吨)', dataIndex: 'totalWeight', width: 100 }
        ],
        defDataIndex: ['billNo', 'customerName', 'billTimeStr', 'totalWeight']
      }
    },
    created() {
      this.initColumnsSetting()
    },
    methods: {
      show(record) {
        this.visible = true;
        this.detail = {};
        this.detailList = [];
        this.loadDetail({ id: record.id });
      },
      showByBillNo(billNo) {
        this.visible = true;
        this.detail = {};
        this.detailList = [];
        this.loadDetail({ billNo: billNo });
      },
      loadDetail(params) {
        this.loading = true;
        getFreightDetail(params).then((res) => {
          if (res.code === 200 && res.data) {
            this.detail = res.data;
            this.detailList = res.data.detailList || [];
          }
        }).finally(() => {
          this.loading = false;
        })
      },
      handleCancel() {
        this.visible = false;
      }
    }
  }
</script>
<style scoped>
</style>
