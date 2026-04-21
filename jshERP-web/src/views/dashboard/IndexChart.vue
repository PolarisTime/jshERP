<template>
  <div class="page-header-index-wide">
    <!-- 待处理单据列表 -->
    <a-row :gutter="24">
      <!-- 采购入库待处理 -->
      <a-col :sm="24" :xl="12" :style="{ marginBottom: '12px' }">
        <a-card :bordered="false" title="采购入库 - 待关联出库" :loading="pendingLoading">
          <a-table
            size="small"
            bordered
            rowKey="id"
            :columns="purchaseInColumns"
            :dataSource="purchaseInList"
            :pagination="false"
            :scroll="{ y: 300 }">
            <template slot="numberRender" slot-scope="text">
              <a @click="goToBill('purchase_in', text)">{{ text }}</a>
            </template>
            <template slot="weightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(3) }}
            </template>
            <template slot="amountRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
          </a-table>
          <div style="margin-top:8px;padding:4px 8px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;text-align:right;">
            <span>{{ purchaseInList.length }} 条</span>
            <a-divider type="vertical" />
            <span>合计吨位：<b>{{ purchaseInSummary.weight }}</b></span>
            <a-divider type="vertical" />
            <span>合计金额：<b>{{ purchaseInSummary.amount }}</b></span>
          </div>
        </a-card>
      </a-col>
      <!-- 销售出库待处理 -->
      <a-col :sm="24" :xl="12" :style="{ marginBottom: '12px' }">
        <a-card :bordered="false" title="销售出库 - 待审核/待核准" :loading="pendingLoading">
          <a-table
            size="small"
            bordered
            rowKey="id"
            :columns="saleOutColumns"
            :dataSource="saleOutList"
            :pagination="false"
            :scroll="{ y: 300 }">
            <template slot="numberRender" slot-scope="text">
              <a @click="goToBill('sale_out', text)">{{ text }}</a>
            </template>
            <template slot="weightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(3) }}
            </template>
            <template slot="amountRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="statusRender" slot-scope="text, record">
              <a-tag v-if="record.status === '0'" color="red">未审核</a-tag>
              <a-tag v-else-if="record.priceApproved !== '1'" color="orange">未核准</a-tag>
            </template>
          </a-table>
          <div style="margin-top:8px;padding:4px 8px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;text-align:right;">
            <span>{{ saleOutList.length }} 条</span>
            <a-divider type="vertical" />
            <span>合计吨位：<b>{{ saleOutSummary.weight }}</b></span>
            <a-divider type="vertical" />
            <span>合计金额：<b>{{ saleOutSummary.amount }}</b></span>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 物流待处理列表 -->
    <a-row :gutter="24">
      <a-col :sm="24" :xl="24" :style="{ marginBottom: '12px' }">
        <a-card :bordered="false" title="物流单 - 未审核/未送达" :loading="pendingLoading">
          <a-table
            size="small"
            bordered
            rowKey="id"
            :columns="freightColumns"
            :dataSource="freightList"
            :pagination="false"
            :scroll="{ y: 300 }">
            <template slot="billNoRender" slot-scope="text">
              <a @click="goToBill('freight', text)">{{ text }}</a>
            </template>
            <template slot="weightRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(3) }}
            </template>
            <template slot="amountRender" slot-scope="text">
              {{ parseFloat(text || 0).toFixed(2) }}
            </template>
            <template slot="freightStatusRender" slot-scope="text, record">
              <a-tag v-if="record.status === '0'" color="red">未审核</a-tag>
              <a-tag v-else color="green">已审核</a-tag>
              <a-tag v-if="record.deliveryStatus === '1'" color="blue">已送达</a-tag>
              <a-tag v-else color="orange">未送达</a-tag>
            </template>
          </a-table>
          <div style="margin-top:8px;padding:4px 8px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;text-align:right;">
            <span>{{ freightList.length }} 条</span>
            <a-divider type="vertical" />
            <span>合计吨位：<b>{{ freightSummary.weight }}</b></span>
            <a-divider type="vertical" />
            <span>合计运费：<b>{{ freightSummary.freight }}</b></span>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
<script>
  import ChartCard from '@/components/ChartCard'
  import ACol from "ant-design-vue/es/grid/Col"
  import ATooltip from "ant-design-vue/es/tooltip/Tooltip"
  import HeadInfo from '@/components/tools/HeadInfo.vue'
  import { getBuyAndSaleStatistics } from '@/api/api'
  import { getAction } from '@/api/manage'

  export default {
    name: "IndexChart",
    components: {
      ATooltip,
      ACol,
      ChartCard,
      HeadInfo
    },
    data() {
      return {
        loading: true,
        pendingLoading: true,
        statistics: {},
        purchaseInList: [],
        saleOutList: [],
        freightList: [],
        purchaseInColumns: [
          { title: '单据编号', dataIndex: 'number', width: 160, scopedSlots: { customRender: 'numberRender' } },
          { title: '日期', dataIndex: 'operTime', width: 100 },
          { title: '供应商', dataIndex: 'organName', width: 130, ellipsis: true },
          { title: '重量(吨)', dataIndex: 'totalWeight', width: 100, align: 'right', scopedSlots: { customRender: 'weightRender' } },
          { title: '金额', dataIndex: 'totalAmount', width: 100, align: 'right', scopedSlots: { customRender: 'amountRender' } }
        ],
        saleOutColumns: [
          { title: '单据编号', dataIndex: 'number', width: 160, scopedSlots: { customRender: 'numberRender' } },
          { title: '日期', dataIndex: 'operTime', width: 100 },
          { title: '客户', dataIndex: 'organName', width: 130, ellipsis: true },
          { title: '重量(吨)', dataIndex: 'totalWeight', width: 100, align: 'right', scopedSlots: { customRender: 'weightRender' } },
          { title: '金额', dataIndex: 'totalAmount', width: 100, align: 'right', scopedSlots: { customRender: 'amountRender' } },
          { title: '状态', dataIndex: 'status', width: 80, align: 'center', scopedSlots: { customRender: 'statusRender' } }
        ],
        freightColumns: [
          { title: '单据编号', dataIndex: 'billNo', width: 150, scopedSlots: { customRender: 'billNoRender' } },
          { title: '日期', dataIndex: 'billTime', width: 100 },
          { title: '结算方', dataIndex: 'carrierName', width: 130, ellipsis: true },
          { title: '重量(吨)', dataIndex: 'totalWeight', width: 100, align: 'right', scopedSlots: { customRender: 'weightRender' } },
          { title: '运费(元)', dataIndex: 'totalFreight', width: 100, align: 'right', scopedSlots: { customRender: 'amountRender' } },
          { title: '状态', dataIndex: 'status', width: 140, align: 'center', scopedSlots: { customRender: 'freightStatusRender' } }
        ]
      }
    },
    computed: {
      purchaseInSummary() {
        let w = 0, a = 0
        this.purchaseInList.forEach(r => { w += parseFloat(r.totalWeight || 0); a += parseFloat(r.totalAmount || 0) })
        return { weight: w.toFixed(3), amount: a.toFixed(2) }
      },
      saleOutSummary() {
        let w = 0, a = 0
        this.saleOutList.forEach(r => { w += parseFloat(r.totalWeight || 0); a += parseFloat(r.totalAmount || 0) })
        return { weight: w.toFixed(3), amount: a.toFixed(2) }
      },
      freightSummary() {
        let w = 0, f = 0
        this.freightList.forEach(r => { w += parseFloat(r.totalWeight || 0); f += parseFloat(r.totalFreight || 0) })
        return { weight: w.toFixed(3), freight: f.toFixed(2) }
      }
    },
    created() {
      setTimeout(() => { this.loading = false }, 1000)
      this.initStatistics()
      this.initPendingList()
    },
    methods: {
      initStatistics() {
        getBuyAndSaleStatistics().then((res) => {
          if (res.code === 200) {
            this.statistics = res.data
          }
        })
      },
      initPendingList() {
        getAction('/depotHead/dashboardPendingList', {}).then((res) => {
          if (res.code === 200) {
            this.purchaseInList = res.data.purchaseInList || []
            this.saleOutList = res.data.saleOutList || []
            this.freightList = res.data.freightList || []
          }
        }).finally(() => {
          this.pendingLoading = false
        })
      },
      goToBill(type, number) {
        if (type === 'purchase_in') {
          this.$router.push({ path: '/bill/purchase_in' })
        } else if (type === 'sale_out') {
          this.$router.push({ path: '/bill/sale_out' })
        } else if (type === 'freight') {
          this.$router.push({ path: '/freight/bill' })
        }
      }
    }
  }
</script>
<style lang="less" scoped>
  @import '~@assets/less/common.less';
  .head-info {
    position: relative;
    text-align: left;
    padding: 0 32px 0 0;
    min-width: 125px;
    span {
      color: rgba(0, 0, 0, .45);
      display: inline-block;
      font-size: .95rem;
      line-height: 42px;
      margin-bottom: 4px;
    }
    p {
      line-height: 42px;
      margin: 0;
      a {
        font-weight: 600;
        font-size: 1rem;
      }
    }
  }
</style>
