<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :maskClosable="false"
    :style="modalStyle"
    fullscreen
    switchFullscreen
    @cancel="handleCancel"
    wrapClassName="ant-modal-cust-warp">
    <template slot="footer">
      <a-button @click="handleCancel">取消(ESC)</a-button>
      <template v-if="!isReadOnly">
        <a-button type="primary" :loading="confirmLoading" @click.prevent="handleOk">保存</a-button>
      </template>
      <template v-else>
        <a-button v-print="'#freightBillPrint'">普通打印</a-button>
        <a-button @click="handleCustomPrint">自定义格式打印</a-button>
        <a-button v-if="isCanBackCheck && model.status==='1'" @click="handleBackCheck">反审核</a-button>
      </template>
    </template>
    <a-form :form="form">
        <section id="freightBillPrint">
        <a-row class="form-row" :gutter="24">
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="单据编号">
              <span v-if="isReadOnly">{{ model.billNo }}</span>
              <a-input v-else placeholder="自动生成" v-decorator.trim="['billNo']" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="单据日期">
              <span v-if="isReadOnly">{{ model.billTimeStr }}</span>
              <a-date-picker v-else style="width:100%" v-decorator="['billTime', validatorRules.billTime]"
                format="YYYY-MM-DD" placeholder="请选择日期" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="结算方">
              <span v-if="isReadOnly">{{ model.carrierName }}</span>
              <a-select v-else placeholder="请选择结算方" v-decorator="['carrierId', validatorRules.carrierId]"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children">
                <a-select-option v-for="(item,index) in carrierList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="单价(元/吨)">
              <span v-if="isReadOnly">{{ model.unitPrice }}</span>
              <a-input-number v-else style="width:100%" placeholder="请输入单价" :min="0" :precision="2"
                v-decorator="['unitPrice', validatorRules.unitPrice]" @change="onUnitPriceChange" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="总重量(吨)">
              <span v-if="isReadOnly">{{ totalWeight }}</span>
              <a-input v-else :readOnly="true" v-model="totalWeight" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="总运费(元)">
              <span v-if="isReadOnly">{{ totalFreight }}</span>
              <a-input v-else :readOnly="true" v-model="totalFreight" />
            </a-form-item>
          </a-col>
          <a-col v-if="isReadOnly" :span="6">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="状态">
              <a-tag v-if="model.status === '0' || model.status === 0" color="red">未审核</a-tag>
              <a-tag v-if="model.status === '1' || model.status === 1" color="green">已审核</a-tag>
            </a-form-item>
          </a-col>
        </a-row>
        <!-- 已选出库单明细区域 -->
        <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom:8px;">
          <div>
            <a-button v-if="!isReadOnly" type="primary" icon="plus" @click="showSelectSaleOut">选择出库单</a-button>
          </div>
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />
        </div>
        <a-table
          size="middle"
          bordered
          :rowKey="(record, index) => record.itemId || index"
          :columns="currentDetailColumns"
          :dataSource="selectedSaleOutList"
          :pagination="false"
          :components="detailDragComponents">
          <span slot="action" slot-scope="text, record, index">
            <a-popconfirm title="确定移除吗?" @confirm="() => removeSaleOut(index)">
              <a style="color:red;">移除</a>
            </a-popconfirm>
          </span>
        </a-table>
        <!-- 备注（位于表格下方，与出入库单一致） -->
        <a-row class="form-row" :gutter="24">
          <a-col :lg="24" :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
              <span v-if="isReadOnly">备注：{{ model.remark }}</span>
              <a-textarea v-else :rows="1" placeholder="请输入备注" v-decorator="['remark']" style="margin-top:8px;"/>
            </a-form-item>
          </a-col>
        </a-row>
        </section>
      </a-form>
    <!-- 选择出库单弹窗 -->
    <a-modal
      title="选择销售出库单"
      :width="1300"
      :visible="selectModalVisible"
      :maskClosable="false"
      @ok="confirmSelectSaleOut"
      @cancel="selectModalVisible=false"
      okText="确认选择"
      cancelText="取消">
      <div style="margin-bottom:10px;">
        <a-input-search
          placeholder="请输入单据号搜索"
          style="width:300px"
          v-model="saleOutSearchKey"
          @search="loadAvailableSaleOut"
          enter-button="搜索" />
      </div>
      <a-table
        size="middle"
        bordered
        rowKey="id"
        :columns="saleOutColumns"
        :dataSource="availableSaleOutList"
        :loading="saleOutLoading"
        :pagination="saleOutPagination"
        :rowSelection="{selectedRowKeys: saleOutSelectedKeys, onChange: onSaleOutSelectChange}"
        :components="saleOutDragComponents"
        @change="handleSaleOutTableChange">
        <template slot="saleOutStatus" slot-scope="text">
          <a-tag v-if="text === '1'" color="green">已审核</a-tag>
          <a-tag v-else color="orange">未审核</a-tag>
        </template>
      </a-table>
    </a-modal>
  <custom-print-modal ref="customPrintModal" billType="freightBill" :model="model" :dataSource="selectedSaleOutList" />
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import moment from 'moment'
  import VueDraggableResizable from 'vue-draggable-resizable'
  import { selectAllFreightCarrier, addFreightBill, editFreightBill, getAvailableSaleOut, getFreightDetail, getFreightDepotItems, freightBatchSetStatus } from '@/api/api'
  import { getAction, postAction } from '@/api/manage'
  import CustomPrintModal from '@/views/bill/dialog/CustomPrintModal'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  export default {
    name: "FreightBillModal",
    mixins: [JeecgListMixin],
    components: { CustomPrintModal, VueDraggableResizable, ColumnSettingPopover },
    data() {
      return {
        title: "操作",
        width: document.body.clientWidth > 1800 ? '1600px' : '1200px',
        visible: false,
        confirmLoading: false,
        isReadOnly: false,
        isCanBackCheck: true,
        model: {},
        carrierList: [],
        totalWeight: '0.000',
        totalFreight: '0.00',
        currentUnitPrice: 0,
        selectedSaleOutList: [],
        modalStyle: {
          top: '20px',
          height: '95%'
        },
        // 选择出库单弹窗
        selectModalVisible: false,
        saleOutSearchKey: '',
        availableSaleOutList: [],
        saleOutLoading: false,
        saleOutSelectedKeys: [],
        saleOutSelectedRows: [],
        saleOutPagination: {
          current: 1,
          pageSize: 10,
          total: 0,
          showTotal: (total) => `共${total}条`,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50']
        },
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        validatorRules: {
          billTime: {
            rules: [{ required: true, message: '请选择单据日期!' }]
          },
          carrierId: {
            rules: [{ required: true, message: '请选择结算方!' }]
          },
          unitPrice: {
            rules: [{ required: true, message: '请输入单价!' }]
          }
        },
        disableMixinCreated: true,
        pageName: 'freightBillModal',
        defColumns: [
          {
            title: '操作', dataIndex: 'action', width: 80, align: 'center',
            scopedSlots: { customRender: 'action' }
          },
          { title: '出库单号', dataIndex: 'billNo', width: 160 },
          { title: '出库日期', dataIndex: 'billTimeStr', width: 100 },
          { title: '客户名称', dataIndex: 'customerName', width: 120 },
          { title: '项目名称', dataIndex: 'projectName', width: 100 },
          { title: '名称', dataIndex: 'materialName', width: 150 },
          { title: '规格', dataIndex: 'standard', width: 100 },
          { title: '型号', dataIndex: 'model', width: 100 },
          { title: '批号', dataIndex: 'batchNumber', width: 100 },
          { title: '数量', dataIndex: 'operNumber', width: 80 },
          { title: '单位', dataIndex: 'materialUnit', width: 60 },
          { title: '重量(吨)', dataIndex: 'itemWeight', width: 90 },
          { title: '仓库', dataIndex: 'depotName', width: 100 },
          { title: '业务员', dataIndex: 'salesMan', width: 80 }
        ],
        defDataIndex: ['action', 'billNo', 'billTimeStr', 'customerName', 'materialName', 'standard', 'model', 'batchNumber', 'operNumber', 'materialUnit', 'itemWeight', 'depotName', 'salesMan'],
        saleOutColumns: [
          { title: '出库单号', dataIndex: 'billNo', width: 180 },
          { title: '状态', dataIndex: 'status', width: 80, scopedSlots: { customRender: 'saleOutStatus' } },
          { title: '客户名称', dataIndex: 'customerName', width: 150 },
          { title: '项目名称', dataIndex: 'projectName', width: 120 },
          { title: '日期', dataIndex: 'billTimeStr', width: 110 },
          { title: '商品信息', dataIndex: 'materialNames', width: 300, ellipsis: true },
          { title: '金额(元)', dataIndex: 'totalAmount', width: 110 },
          { title: '总重量(吨)', dataIndex: 'totalWeight', width: 110 },
          { title: '仓库', dataIndex: 'depotName', width: 120 },
          { title: '业务员', dataIndex: 'salesMan', width: 100 },
          { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true }
        ],
        form: this.$form.createForm(this)
      }
    },
    created() {
      this.initColumnsSetting()
    },
    computed: {
      currentDetailColumns() {
        if (this.isReadOnly) {
          return this.columns.filter(col => col.dataIndex !== 'action')
        }
        return this.columns
      },
      detailDragComponents() {
        return this.buildDragComponents(this.columns)
      },
      saleOutDragComponents() {
        return this.buildDragComponents(this.saleOutColumns)
      }
    },
    methods: {
      buildDragComponents(columns) {
        const that = this
        return {
          header: {
            cell: (h, props, children) => {
              const { key, ...restProps } = props
              const col = columns.find(c => (c.dataIndex || c.key) === key)
              if (!col || !col.width) {
                return h('th', { ...restProps }, children)
              }
              const dragProps = {
                key: col.dataIndex || col.key,
                class: 'table-draggable-handle',
                attrs: { w: 10, x: col.width, z: 1, axis: 'x', draggable: true, resizable: false },
                on: {
                  dragging: (x) => { col.width = Math.max(x, 1) }
                }
              }
              return h('th', { ...restProps, class: 'resize-table-th' }, [children, h(VueDraggableResizable, { ...dragProps })])
            }
          }
        }
      },
      detail(record) {
        this.isReadOnly = true;
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.selectedSaleOutList = [];
        this.totalWeight = '0.00';
        this.totalFreight = '0.00';
        this.currentUnitPrice = 0;
        this.visible = true;
        this.title = '物流单-详情';
        if (record.id) {
          this.loadDetailForView(record.id);
        }
      },
      detailByBillNo(billNo) {
        this.isReadOnly = true;
        this.form.resetFields();
        this.model = {};
        this.selectedSaleOutList = [];
        this.totalWeight = '0.00';
        this.totalFreight = '0.00';
        this.currentUnitPrice = 0;
        this.visible = true;
        this.title = '物流单-详情';
        this.confirmLoading = true;
        getFreightDetail({ billNo: billNo }).then((res) => {
          if (res.code === 200 && res.data) {
            let data = res.data;
            this.model = {
              id: data.id,
              billNo: data.billNo,
              billTimeStr: data.billTimeStr,
              carrierName: data.carrierName,
              carrierId: data.carrierId,
              unitPrice: data.unitPrice,
              totalWeight: data.totalWeight,
              totalFreight: data.totalFreight,
              remark: data.remark,
              status: String(data.status)
            };
            this.currentUnitPrice = data.unitPrice || 0;
            this.selectedSaleOutList = data.detailList || [];
            this.calcTotal();
          }
        }).finally(() => {
          this.confirmLoading = false;
        })
      },
      loadDetailForView(id) {
        this.confirmLoading = true;
        getFreightDetail({ id: id }).then((res) => {
          if (res.code === 200 && res.data) {
            let data = res.data;
            this.model = Object.assign(this.model, {
              billNo: data.billNo,
              billTimeStr: data.billTimeStr,
              carrierName: data.carrierName,
              carrierId: data.carrierId,
              unitPrice: data.unitPrice,
              totalWeight: data.totalWeight,
              totalFreight: data.totalFreight,
              remark: data.remark,
              status: String(data.status)
            });
            this.currentUnitPrice = data.unitPrice || 0;
            this.selectedSaleOutList = data.detailList || [];
            this.calcTotal();
          }
        }).finally(() => {
          this.confirmLoading = false;
        })
      },
      handleBackCheck() {
        let that = this;
        this.$confirm({
          title: '确认反审核',
          content: '是否反审核该物流单？',
          onOk() {
            postAction('/freightHead/batchSetStatus', { status: '0', ids: that.model.id + '' }).then((res) => {
              if (res.code === 200) {
                that.$message.success('反审核成功');
                that.$emit('ok');
                that.close();
              } else {
                that.$message.warning(res.data.message || '操作失败');
              }
            })
          }
        });
      },
      add() {
        this.edit({});
        // 自动生成运费单编号，格式: yyyyW0001
        getAction('/freightHead/buildBillNo').then((res) => {
          if (res && res.code === 200) {
            this.$nextTick(() => {
              this.form.setFieldsValue({ billNo: res.data.billNo })
            })
          }
        })
      },
      edit(record) {
        this.isReadOnly = false;
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.selectedSaleOutList = [];
        this.totalWeight = '0.00';
        this.totalFreight = '0.00';
        this.currentUnitPrice = 0;
        this.visible = true;
        this.initCarrierList();
        if (this.model.id) {
          // 编辑模式，加载详情
          this.loadDetail(this.model.id);
        } else {
          this.$nextTick(() => {
            this.form.setFieldsValue({
              billTime: moment()
            })
          });
        }
      },
      loadDetail(id) {
        getFreightDetail({ id: id }).then((res) => {
          if (res.code === 200 && res.data) {
            let data = res.data;
            this.currentUnitPrice = data.unitPrice || 0;
            this.$nextTick(() => {
              this.form.setFieldsValue({
                billNo: data.billNo,
                billTime: data.billTimeStr ? moment(data.billTimeStr) : null,
                carrierId: data.carrierId,
                unitPrice: data.unitPrice,
                remark: data.remark
              })
            });
            this.selectedSaleOutList = data.detailList || [];
            this.calcTotal();
          }
        })
      },
      initCarrierList() {
        selectAllFreightCarrier({}).then((res) => {
          if (res.code === 200) {
            this.carrierList = res.data || [];
          }
        })
      },
      onUnitPriceChange(value) {
        this.currentUnitPrice = value || 0;
        this.calcTotal();
      },
      calcTotal() {
        let weight = 0;
        this.selectedSaleOutList.forEach(item => {
          weight += parseFloat(item.itemWeight || item.totalWeight || 0);
        });
        this.totalWeight = weight.toFixed(3);
        this.totalFreight = (weight * this.currentUnitPrice).toFixed(2);
      },
      removeSaleOut(index) {
        // 移除同一出库单的所有明细行
        let depotHeadId = this.selectedSaleOutList[index].depotHeadId
        this.selectedSaleOutList = this.selectedSaleOutList.filter(item => item.depotHeadId !== depotHeadId)
        this.calcTotal();
      },
      showSelectSaleOut() {
        this.saleOutSearchKey = '';
        this.saleOutSelectedKeys = [];
        this.saleOutSelectedRows = [];
        this.saleOutPagination.current = 1;
        this.selectModalVisible = true;
        this.loadAvailableSaleOut();
      },
      loadAvailableSaleOut() {
        this.saleOutLoading = true;
        let excludeIds = this.selectedSaleOutList.map(item => item.depotHeadId || item.id).join(',');
        let params = {
          billNo: this.saleOutSearchKey,
          excludeIds: excludeIds,
          currentPage: this.saleOutPagination.current,
          pageSize: this.saleOutPagination.pageSize
        };
        getAvailableSaleOut(params).then((res) => {
          if (res.code === 200) {
            this.availableSaleOutList = res.data.rows || [];
            this.saleOutPagination.total = res.data.total || 0;
          }
        }).finally(() => {
          this.saleOutLoading = false;
        })
      },
      handleSaleOutTableChange(pagination) {
        this.saleOutPagination.current = pagination.current;
        this.saleOutPagination.pageSize = pagination.pageSize;
        this.loadAvailableSaleOut();
      },
      onSaleOutSelectChange(selectedRowKeys, selectedRows) {
        this.saleOutSelectedKeys = selectedRowKeys;
        this.saleOutSelectedRows = selectedRows;
      },
      confirmSelectSaleOut() {
        if (this.saleOutSelectedRows.length === 0) {
          this.$message.warning('请至少选择一条出库单！');
          return;
        }
        // 收集新选的出库单ID（排除已有的）
        let existIds = this.selectedSaleOutList.map(item => item.depotHeadId)
        let newIds = this.saleOutSelectedRows
          .map(row => row.id)
          .filter(id => existIds.indexOf(id) === -1)
        if (newIds.length === 0) {
          this.$message.warning('所选出库单已全部添加');
          this.selectModalVisible = false;
          return;
        }
        // 调用后端获取商品明细行
        getFreightDepotItems({ headerIds: newIds.join(',') }).then((res) => {
          if (res.code === 200 && res.data) {
            res.data.forEach(item => {
              this.selectedSaleOutList.push(item)
            })
            this.calcTotal()
          }
        })
        this.selectModalVisible = false;
      },
      close() {
        this.$emit('close');
        this.visible = false;
      },
      handleOk() {
        const that = this;
        this.form.validateFields((err, values) => {
          if (!err) {
            if (that.selectedSaleOutList.length === 0) {
              that.$message.warning('请至少选择一条出库单！');
              return;
            }
            that.confirmLoading = true;
            let billMain = Object.assign({}, that.model);
            billMain.billNo = values.billNo;
            billMain.billTime = values.billTime ? values.billTime.format('YYYY-MM-DD') : '';
            billMain.carrierId = values.carrierId;
            billMain.unitPrice = values.unitPrice;
            //totalWeight 和 totalFreight 由后端 recalcHeadTotal 计算，不从前端传递
            delete billMain.totalWeight;
            delete billMain.totalFreight;
            billMain.remark = values.remark;
            // 从明细行提取唯一出库单，按 depotHeadId 汇总重量
            let weightMap = {}
            that.selectedSaleOutList.forEach(item => {
              let hid = item.depotHeadId || item.id
              if (!weightMap[hid]) weightMap[hid] = 0
              weightMap[hid] += parseFloat(item.itemWeight || item.totalWeight || 0)
            })
            let rows = Object.keys(weightMap).map(hid => {
              return { depotHeadId: parseInt(hid), totalWeight: weightMap[hid].toFixed(3) }
            });
            let formData = {
              info: JSON.stringify(billMain),
              rows: JSON.stringify(rows)
            };
            let obj;
            if (!that.model.id) {
              obj = addFreightBill(formData);
            } else {
              obj = editFreightBill(formData);
            }
            obj.then((res) => {
              if (res.code === 200) {
                if (res.data && res.data.billNo) {
                  that.$message.success('运费单 ' + res.data.billNo + ' 保存成功')
                }
                that.$emit('ok');
                that.close();
              } else {
                that.$message.warning(res.data.message);
              }
            }).finally(() => {
              that.confirmLoading = false;
            })
          }
        })
      },
      handleCustomPrint() {
        this.$refs.customPrintModal.show()
      },
      handleCancel() {
        this.close();
      }
    }
  }
</script>
<style scoped>
  .resize-table-th {
    position: relative;
  }
  .table-draggable-handle {
    height: 100% !important;
    bottom: 0;
    left: auto !important;
    right: -5px;
    cursor: col-resize;
    touch-action: none;
    position: absolute;
    transform: none !important;
  }
</style>
