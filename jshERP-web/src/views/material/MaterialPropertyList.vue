<!-- from 7 5 2 7 1 8 9 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 操作按钮区域 -->
        <div class="table-operator">
          <column-setting-popover :defColumns="defColumns" :settingDataIndex.sync="settingDataIndex" @change="onColChange" @reset="handleRestDefault" />

          <!-- CLodop -->
          <span style="margin-left:8px;display:flex;align-items:center;gap:6px;">
            <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
            <a-tag v-else color="orange" style="cursor:pointer;" @click="initClodop">CLodop未连接（点击重试）</a-tag>
            <a-select v-if="clodopReady && printTemplateList.length" v-model="selectedTemplateId"
              style="width:160px;" placeholder="选择打印模板">
              <a-select-option v-for="t in printTemplateList" :key="t.id" :value="t.id">{{ t.templateName }}</a-select-option>
            </a-select>
            <a-select v-if="clodopReady && printerList.length" v-model="selectedPrinter"
              style="width:180px;" placeholder="默认打印机">
              <a-select-option value="">默认打印机</a-select-option>
              <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
            </a-select>
            <a-button icon="eye" :disabled="!clodopReady || selectedRowKeys.length !== 1" @click="doPrint(true)">预览</a-button>
            <a-button type="primary" icon="printer" :disabled="!clodopReady || selectedRowKeys.length === 0" @click="doPrint(false)">打印</a-button>
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
            :loading="loading"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">编辑</a>
            </span>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <material-property-modal ref="modalForm" @ok="modalFormOk"></material-property-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import MaterialPropertyModal from './modules/MaterialPropertyModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  import { getAction } from '@/api/manage'
  import Vue from 'vue'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import { ClodopMixin } from '@/mixins/ClodopMixin'
  export default {
    name: "MaterialPropertyList",
    mixins:[ClodopMixin, JeecgListMixin],
    components: {
      MaterialPropertyModal,
      JDate,
      ColumnSettingPopover
    },
    data () {
      return {
        clodopBillType: 'materialProperty',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 查询条件
        queryParam: {name:'',type:''},
        urlPath: '/material/material_property',
        pageName: 'materialPropertyList',
        defDataIndex:['rowIndex','action','nativeName','anotherName'],
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
            width: 100,
            align:"center",
            scopedSlots: { customRender: 'action' },
          },
          {title: '名称', dataIndex: 'nativeName', width: 100},
          {title: '别名', dataIndex: 'anotherName', width: 100}
        ],
        url: {
          list: "/materialProperty/list",
          delete: "/materialProperty/delete",
          deleteBatch: "/materialProperty/deleteBatch"
        }
      }
    },
    created () {
      this.initColumnsSetting()
    },
    computed: {

    },
    methods: {
      loadData(arg) {
        let params = this.getQueryParams() //查询条件
        this.loading = true
        getAction(this.url.list, params).then((res) => {
          if (res.code===200) {
            this.dataSource = res.data.rows
            this.ipagination.total = res.data.total
            Vue.ls.set('materialPropertyList', res.data.rows, 7 * 24 * 60 * 60 * 1000);
          } else if(res.code===510){
            this.$message.warning(res.data)
          } else {
            this.$message.warning(res.data.message)
          }
          this.loading = false
          this.onClearSelected()
        })
      },
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>