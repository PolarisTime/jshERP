<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="margin-top: 5px">
          <column-setting-popover
            :defColumns="defColumns"
            :settingDataIndex.sync="settingDataIndex"
            @change="onColChange"
            @reset="handleRestDefault"
          />

                    <!-- CLodop -->
          <span style="margin-left:8px;display:flex;align-items:center;gap:6px;">
            <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
            <a-tag v-else color="orange" style="cursor:pointer;" @click="initClodop">CLodop未连接（点击重试）</a-tag>
            <a-select v-if="printTemplateList.length" v-model="selectedTemplateId"
              style="width:160px;" placeholder="选择打印模板">
              <a-select-option v-for="t in printTemplateList" :key="t.id" :value="t.id">{{ t.templateName }}</a-select-option>
            </a-select>
            <a-select v-if="clodopReady && printerList.length" v-model="selectedPrinter"
              style="width:180px;" placeholder="默认打印机">
              <a-select-option value="">默认打印机</a-select-option>
              <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
            </a-select>
            <a-button icon="eye" :disabled="!clodopReady || !selectedTemplateId || selectedRowKeys.length !== 1" @click="doPrint(true)">预览</a-button>
            <a-button type="primary" icon="printer" :disabled="!clodopReady || !selectedTemplateId || selectedRowKeys.length === 0" @click="doPrint(false)">打印</a-button>
          </span>
                </div>
        <!-- table区域-begin -->
        <div>
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
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">编辑</a>
            </span>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <platform-config-modal ref="modalForm" @ok="modalFormOk"></platform-config-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
<script>
  import PlatformConfigModal from './modules/PlatformConfigModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import { ClodopMixin } from '@/mixins/ClodopMixin'
  export default {
    name: "PlatformConfigList",
    mixins:[ClodopMixin, JeecgListMixin],
    components: {
      PlatformConfigModal,
      ColumnSettingPopover
    },
    data () {
      return {
        clodopBillType: 'platformConfig',
        currentRoleId: '',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 查询条件
        queryParam: {platformKey:'',},
        pageName: 'platformConfigList',
        defDataIndex:['rowIndex','action','platformKeyInfo','platformValue'],
        // 表头
        defColumns: [
          {
            title: '#',
            dataIndex: 'rowIndex',
            key:'rowIndex',
            width:40,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title: '操作',
            dataIndex: 'action',
            align:"center",
            width: 100,
            scopedSlots: { customRender: 'action' },
          },
          {
            title: '配置名称',
            dataIndex: 'platformKeyInfo',
            width: 100
          },
          {
            title: '配置值',
            dataIndex: 'platformValue',
            width: 500
          }
        ],
        url: {
          list: "/platformConfig/list",
          delete: "/platformConfig/delete",
          deleteBatch: "/platformConfig/deleteBatch"
        },
      }
    },
    created () {
      this.initColumnsSetting()
    },
    methods: {
      handleEdit: function (record) {
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = "编辑";
        this.$refs.modalForm.disableSubmit = false;
        if(this.btnEnableList.indexOf(1)===-1) {
          this.$refs.modalForm.isReadOnly = true
        }
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>