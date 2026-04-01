<template>
  <a-modal
    title="自定义格式打印"
    :width="1200"
    :visible="visible"
    :maskClosable="false"
    :footer="null"
    @cancel="handleCancel"
    style="top:20px;">
    <!-- 操作栏 -->
    <div style="margin-bottom:10px;display:flex;align-items:center;flex-wrap:wrap;gap:8px;">
      <!-- 模板选择 -->
      <a-select
        v-model="selectedTemplateId"
        style="width:220px;"
        placeholder="选择模板"
        @change="handleTemplateChange">
        <a-select-option :value="0">系统默认模板</a-select-option>
        <a-select-option v-for="t in templateList" :key="t.id" :value="t.id">
          {{ t.templateName }}
        </a-select-option>
      </a-select>
      <a-input v-model="templateName" placeholder="模板名称" style="width:160px;" />
      <a-button type="primary" @click="handleSave" :loading="saving">保存为新模板</a-button>
      <a-button v-if="selectedTemplateId > 0" @click="handleUpdate" :loading="saving">更新当前模板</a-button>
      <a-popconfirm
        v-if="selectedTemplateId > 0"
        title="确定删除此模板？"
        @confirm="handleDelete">
        <a-button type="danger" ghost>删除模板</a-button>
      </a-popconfirm>
      <a-button type="primary" icon="printer" @click="handlePrint" :disabled="loading" style="margin-left:auto;">打印</a-button>
    </div>
    <a-row :gutter="16">
      <!-- 模板编辑区 -->
      <a-col :span="18">
        <a-tabs v-model="activeTab" size="small">
          <a-tab-pane key="preview" tab="打印预览">
            <div class="preview-pane" v-html="renderedPreview"></div>
          </a-tab-pane>
          <a-tab-pane key="source" tab="模板源码">
            <textarea
              class="template-textarea"
              v-model="templateHtml"
              spellcheck="false"
            ></textarea>
          </a-tab-pane>
        </a-tabs>
      </a-col>
      <!-- 字段面板 -->
      <a-col :span="6">
        <div style="border:1px solid #e8e8e8;border-radius:4px;padding:8px;max-height:600px;overflow-y:auto;">
          <h4 style="margin:0 0 8px;">主表字段 <small style="color:#999;">(点击复制)</small></h4>
          <div v-for="f in headerFields" :key="'h_'+f.key" style="margin-bottom:4px;">
            <a-tag color="blue" style="cursor:pointer;" @click="copyField(f.key, false)">{{ f.label }}</a-tag>
          </div>
          <a-divider style="margin:8px 0;" />
          <h4 style="margin:0 0 8px;">明细字段 <small style="color:#999;">(点击复制)</small></h4>
          <div v-for="f in detailFields" :key="'d_'+f.key" style="margin-bottom:4px;">
            <a-tag color="green" style="cursor:pointer;" @click="copyField(f.key, true)">{{ f.label }}</a-tag>
          </div>
          <a-divider style="margin:8px 0;" />
          <h4 style="margin:0 0 8px;">内置变量</h4>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="copyRaw('{{_index}}')">行序号</a-tag>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="copyRaw('{{_printDate}}')">打印日期</a-tag>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="copyRaw('{{_printTime}}')">打印时间</a-tag>
        </div>
      </a-col>
    </a-row>
  </a-modal>
</template>
<script>
  import { getPrintTemplate, savePrintTemplate, deletePrintTemplate, listPrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate } from '@/utils/printTemplateDefaults'
  import { render, doPrint } from '@/utils/printTemplateEngine'

  export default {
    name: 'CustomPrintModal',
    props: {
      billType: { type: String, required: true },
      model: { type: Object, default: () => ({}) },
      dataSource: { type: Array, default: () => [] }
    },
    data() {
      return {
        visible: false,
        saving: false,
        loading: false,
        activeTab: 'preview',
        selectedTemplateId: 0,
        templateName: '',
        templateHtml: '',
        templateList: [],
        headerFields: [],
        detailFields: []
      }
    },
    computed: {
      renderedPreview() {
        if (!this.templateHtml) return '<p style="color:#999;text-align:center;padding:40px;">暂无模板内容</p>'
        try {
          return render(this.templateHtml, this.model, this.dataSource)
        } catch (e) {
          return '<p style="color:red;padding:20px;">模板渲染出错：' + e.message + '</p>'
        }
      }
    },
    methods: {
      show() {
        this.visible = true
        this.loading = true
        this.activeTab = 'preview'
        this.selectedTemplateId = 0
        this.templateName = ''
        this.templateHtml = getDefaultTemplate(this.billType)
        this.loadTemplateList()
        this.loadFieldMeta()
      },
      /** 加载模板列表，自动选中默认模板 */
      loadTemplateList() {
        listPrintTemplate({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.templateList = res.data
            // 自动选中 isDefault='1' 的模板
            const defaultTpl = this.templateList.find(t => t.isDefault === '1')
            if (defaultTpl) {
              this.selectedTemplateId = defaultTpl.id
              this.templateName = defaultTpl.templateName
              this.templateHtml = defaultTpl.templateHtml
            }
          }
        }).finally(() => {
          this.loading = false
        })
      },
      loadFieldMeta() {
        getPrintFieldMeta({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.headerFields = res.data.headerFields || []
            this.detailFields = res.data.detailFields || []
          }
        })
      },
      /** 切换模板 */
      handleTemplateChange(id) {
        if (id === 0) {
          this.templateName = ''
          this.templateHtml = getDefaultTemplate(this.billType)
        } else {
          const tpl = this.templateList.find(t => t.id === id)
          if (tpl) {
            this.templateName = tpl.templateName
            this.templateHtml = tpl.templateHtml
          }
        }
      },
      /** 点击字段标签复制占位符到剪贴板 */
      copyField(key, isDetail) {
        const placeholder = isDetail ? `{{detail.${key}}}` : `{{${key}}}`
        this.copyRaw(placeholder)
      },
      copyRaw(text) {
        if (navigator.clipboard) {
          navigator.clipboard.writeText(text).then(() => {
            this.$message.success('已复制：' + text)
          })
        } else {
          // fallback
          const ta = document.createElement('textarea')
          ta.value = text
          document.body.appendChild(ta)
          ta.select()
          document.execCommand('copy')
          document.body.removeChild(ta)
          this.$message.success('已复制：' + text)
        }
      },
      /** 保存为新模板 */
      handleSave() {
        if (!this.templateName || !this.templateName.trim()) {
          this.$message.warning('请输入模板名称')
          return
        }
        this.saving = true
        savePrintTemplate({
          id: null,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: this.templateHtml,
          isDefault: '0'
        }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板保存成功')
            this.loadTemplateList()
          } else {
            this.$message.warning('保存失败')
          }
        }).finally(() => {
          this.saving = false
        })
      },
      /** 更新当前选中的模板 */
      handleUpdate() {
        this.saving = true
        savePrintTemplate({
          id: this.selectedTemplateId,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: this.templateHtml,
          isDefault: '1'
        }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板更新成功')
            this.loadTemplateList()
          } else {
            this.$message.warning('更新失败')
          }
        }).finally(() => {
          this.saving = false
        })
      },
      /** 删除当前选中的模板 */
      handleDelete() {
        deletePrintTemplate({ id: this.selectedTemplateId }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板已删除')
            this.selectedTemplateId = 0
            this.templateName = ''
            this.templateHtml = getDefaultTemplate(this.billType)
            this.loadTemplateList()
          } else {
            this.$message.warning('删除失败')
          }
        })
      },
      handlePrint() {
        const html = render(this.templateHtml, this.model, this.dataSource)
        doPrint(html)
      },
      handleCancel() {
        this.visible = false
      }
    }
  }
</script>
<style scoped>
  .preview-pane {
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    padding: 16px;
    min-height: 460px;
    max-height: 600px;
    overflow-y: auto;
    background: #fff;
  }
  .preview-pane table {
    border-collapse: collapse;
    width: 100%;
  }
  .preview-pane th,
  .preview-pane td {
    border: 1px solid #000;
    padding: 4px 8px;
    text-align: left;
    font-size: 12px;
  }
  .preview-pane th {
    background-color: #f0f0f0;
    font-weight: bold;
  }
  .preview-pane h2 {
    text-align: center;
    margin: 10px 0;
  }
  .preview-pane .header-row td {
    border: none;
    padding: 4px 8px;
  }
  .template-textarea {
    width: 100%;
    min-height: 460px;
    max-height: 600px;
    font-family: 'Courier New', Consolas, monospace;
    font-size: 12px;
    line-height: 1.5;
    padding: 8px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    resize: vertical;
    white-space: pre;
    overflow: auto;
    tab-size: 2;
  }
  .template-textarea:focus {
    border-color: #40a9ff;
    outline: none;
    box-shadow: 0 0 0 2px rgba(24,144,255,0.2);
  }
</style>
