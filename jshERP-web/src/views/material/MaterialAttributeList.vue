<!-- by 7527_18920 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <!-- 搜索区域 -->
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="8">
                <a-form-item label="属性名" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                  <a-input placeholder="请输入属性名查询" v-model="queryParam.attributeName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="8">
                <a-form-item label="属性值" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                  <a-input placeholder="请输入属性值查询" v-model="queryParam.attributeValue"></a-input>
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
        <div class="table-operator"  style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">新增</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">删除</a-button>
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
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">编辑</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => handleDelete(record.id)">
                <a>删除</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderAttributeValue" slot-scope="attributeValue">
              <a-tag  v-for="(item,index) in getTagArr(attributeValue)" color="blue">{{item}}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <material-attribute-modal ref="modalForm" @ok="modalFormOk"></material-attribute-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import MaterialAttributeModal from './modules/MaterialAttributeModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  import ColumnSettingPopover from '@/components/tools/ColumnSettingPopover'
import { ClodopMixin } from '@/mixins/ClodopMixin'
  export default {
    name: "MaterialAttributeList",
    mixins:[ClodopMixin, JeecgListMixin],
    components: {
      MaterialAttributeModal,
      JDate,
      ColumnSettingPopover
    },
    data () {
      return {
        clodopBillType: \'materialAttribute\',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 查询条件
        queryParam: {
          attributeName:'',
          attributeValue:''
        },
        urlPath: '/material/material_attribute',
        pageName: 'materialAttributeList',
        defDataIndex:['rowIndex','action','attributeName','attributeValue'],
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
          {title: '属性名', dataIndex: 'attributeName', width: 150},
          {title: '属性值', dataIndex: 'attributeValue', width: 750,
            scopedSlots: { customRender: 'customRenderAttributeValue' }
          }
        ],
        url: {
          list: "/materialAttribute/list",
          delete: "/materialAttribute/delete",
          deleteBatch: "/materialAttribute/deleteBatch"
        }
      }
    },
    created () {
      this.initColumnsSetting()
    },
    computed: {

    },
    methods: {
      getTagArr(attributeValue) {
        return attributeValue.split('|')
      },
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