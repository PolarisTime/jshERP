<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item label="字典名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入字典名称" v-model="queryParam.dictName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="字典类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input placeholder="请输入字典类型" v-model="queryParam.dictType"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model="queryParam.status" placeholder="请选择状态" allow-clear>
                    <a-select-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">
                      {{ dict.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">查询</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? '收起' : '展开' }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </span>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item label="创建时间" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-range-picker
                      v-model="queryParam.createTimeRange"
                      format="YYYY-MM-DD"
                      :placeholder="['开始日期', '结束日期']"
                      @change="onCreateDateChange"
                      @ok="onDateOk"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="border-top: 5px">
          <a-button @click="handleAdd" type="primary" icon="plus">新增</a-button>
          <a-button @click="batchDel" icon="delete">删除</a-button>
          <a-button @click="handleRefreshCache" icon="reload">刷新缓存</a-button>

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
            bordered
            size="middle"
            rowKey="dictId"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除吗?" @confirm="() => handleDelete(record.dictId)">
                <a>删除</a>
              </a-popconfirm>
            </span>
            <span slot="customRenderDictType" slot-scope="text, record">
              <a @click="handleShowData(record)">{{text}}</a>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderStatus" slot-scope="status">
              <dict-tag :options="dict.type.sys_normal_disable" :value="status"/>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <dict-type-modal ref="modalForm" @ok="modalFormOk"></dict-type-modal>
        <dict-data-list-modal ref="modalDataList"></dict-data-list-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- b y 7 5 2 7  1 8 9 2 0 -->
<script>
  import DictTypeModal from './modules/DictTypeModal'
  import DictDataListModal from './modules/DictDataListModal'
  import {JeecgListMixin} from '@/mixins/JeecgListMixin'
  import { deleteAction } from '@/api/manage'
  import moment from 'moment/moment'
import { ClodopMixin } from '@/mixins/ClodopMixin'
  export default {
    name: "DictList",
    dicts: ['sys_normal_disable'],
    mixins: [ClodopMixin, JeecgListMixin],
    components: {
      DictTypeModal,
      DictDataListModal,
    },
    data() {
      return {
        clodopBillType: \'dict\',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        queryParam: {
          dictName: '',
          dictType: '',
          status: undefined
        },
        columns: [
          {
            title: '#',
            dataIndex: '',
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
            scopedSlots: {customRender: 'action'},
            align: "center",
            width: 80
          },
          { title: '字典名称', dataIndex: 'dictName', width: 100},
          { title: '字典类型', dataIndex: 'dictType', width: 100,
            scopedSlots: { customRender: 'customRenderDictType' },
          },
          { title: '备注', dataIndex: 'remark', width: 200, ellipsis:true},
          { title: '状态',dataIndex: 'status',width: 60,align:"center",
            scopedSlots: { customRender: 'customRenderStatus' }
          },
          { title: '创建时间', dataIndex: 'createTime', width: 100},
          { title: '更新时间', dataIndex: 'updateTime', width: 100}
        ],
        url: {
          list: "/dict/type/list",
          delete: "/dict/type/delete",
          deleteBatch: "/dict/type/deleteBatch",
          refreshCacheUrl: "/dict/type/refreshCache"
        },
      }
    },
    created () {
    },
    methods: {
      handleRefreshCache() {
        deleteAction(this.url.refreshCacheUrl).then(() => {
          this.$message.success('刷新成功')
        })
      },
      handleShowData(record) {
        this.$refs.modalDataList.show(record)
      },
      onCreateDateChange: function (value, dateString) {
        this.queryParam.beginTime=dateString[0]
        this.queryParam.endTime=dateString[1]
        if(dateString[0] && dateString[1]) {
          this.queryParam.createTimeRange = [moment(dateString[0]), moment(dateString[1])]
        }
      },
      onDateOk(value) {
        console.log(value);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>