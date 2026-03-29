<template>
  <a-modal
    title="自定义格式打印"
    :width="1200"
    :visible="visible"
    :maskClosable="false"
    :footer="null"
    @cancel="handleCancel"
    style="top:20px;">
    <a-tabs v-model="activeTab" @change="onTabChange">
      <!-- 模板编辑 Tab -->
      <a-tab-pane key="edit" tab="模板编辑" forceRender>
        <a-row :gutter="16">
          <a-col :span="18">
            <div style="margin-bottom:8px;">
              <a-input v-model="templateName" placeholder="模板名称" style="width:200px;margin-right:8px;" />
              <a-button type="primary" @click="handleSave" :loading="saving">保存模板</a-button>
              <a-button style="margin-left:8px;" @click="handleResetDefault">恢复默认</a-button>
            </div>
            <editor
              v-if="editorReady"
              v-model="templateHtml"
              :init="editorInit"
            />
          </a-col>
          <a-col :span="6">
            <div style="border:1px solid #e8e8e8;border-radius:4px;padding:8px;max-height:600px;overflow-y:auto;">
              <h4 style="margin:0 0 8px;">主表字段</h4>
              <div v-for="f in headerFields" :key="'h_'+f.key" style="margin-bottom:4px;">
                <a-tag color="blue" style="cursor:pointer;" @click="insertField(f.key, false)">{{ f.label }}</a-tag>
              </div>
              <a-divider style="margin:8px 0;" />
              <h4 style="margin:0 0 8px;">明细字段</h4>
              <div v-for="f in detailFields" :key="'d_'+f.key" style="margin-bottom:4px;">
                <a-tag color="green" style="cursor:pointer;" @click="insertField(f.key, true)">{{ f.label }}</a-tag>
              </div>
              <a-divider style="margin:8px 0;" />
              <h4 style="margin:0 0 8px;">内置变量</h4>
              <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="insertRaw('{{_index}}')">行序号</a-tag>
              <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="insertRaw('{{_printDate}}')">打印日期</a-tag>
              <a-tag color="orange" style="cursor:pointer;margin-bottom:4px;" @click="insertRaw('{{_printTime}}')">打印时间</a-tag>
            </div>
          </a-col>
        </a-row>
      </a-tab-pane>
      <!-- 打印预览 Tab -->
      <a-tab-pane key="preview" tab="打印预览" forceRender>
        <div style="margin-bottom:10px;">
          <a-button type="primary" icon="printer" @click="handlePrint">打印</a-button>
        </div>
        <div
          ref="previewArea"
          style="border:1px solid #d9d9d9;padding:20px;min-height:400px;background:#fff;"
          v-html="renderedHtml">
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>
<script>
  import { getPrintTemplate, savePrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate } from '@/utils/printTemplateDefaults'
  import { render, doPrint } from '@/utils/printTemplateEngine'
  import Editor from '@tinymce/tinymce-vue'
  import 'tinymce/tinymce'
  import 'tinymce/themes/silver'
  import 'tinymce/icons/default'
  import 'tinymce/plugins/table'
  import 'tinymce/plugins/code'
  import 'tinymce/plugins/fullscreen'
  import 'tinymce/plugins/preview'

  export default {
    name: 'CustomPrintModal',
    components: { Editor },
    props: {
      billType: { type: String, required: true },
      model: { type: Object, default: () => ({}) },
      dataSource: { type: Array, default: () => [] }
    },
    data() {
      return {
        visible: false,
        activeTab: 'edit',
        saving: false,
        editorReady: false,
        templateId: null,
        templateName: '默认模板',
        templateHtml: '',
        headerFields: [],
        detailFields: [],
        editorInstance: null,
        editorInit: {
          language: 'zh_CN',
          language_url: '/tinymce/langs/zh_CN.js',
          skin_url: '/tinymce/skins/ui/oxide',
          content_css: '/tinymce/skins/content/default/content.min.css',
          height: 500,
          menubar: 'file edit view insert format table',
          plugins: 'table code fullscreen preview',
          toolbar: 'undo redo | formatselect | bold italic | alignleft aligncenter alignright | table | code fullscreen',
          valid_elements: '*[*]',
          extended_valid_elements: 'style',
          branding: false,
          setup: (editor) => {
            this.editorInstance = editor
          }
        }
      }
    },
    computed: {
      renderedHtml() {
        return render(this.templateHtml, this.model, this.dataSource)
      }
    },
    methods: {
      show() {
        this.visible = true
        this.activeTab = 'edit'
        this.templateId = null
        this.templateName = '默认模板'
        this.templateHtml = getDefaultTemplate(this.billType)
        this.editorReady = true
        this.loadTemplate()
        this.loadFieldMeta()
      },
      loadTemplate() {
        getPrintTemplate({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.templateId = res.data.id
            this.templateName = res.data.templateName
            this.templateHtml = res.data.templateHtml
          }
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
      insertField(key, isDetail) {
        const placeholder = isDetail ? `{{detail.${key}}}` : `{{${key}}}`
        this.insertRaw(placeholder)
      },
      insertRaw(text) {
        if (this.editorInstance) {
          this.editorInstance.insertContent(text)
        }
      },
      handleSave() {
        this.syncEditorContent()
        this.saving = true
        savePrintTemplate({
          id: this.templateId,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: this.templateHtml,
          isDefault: '1'
        }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板保存成功')
            // 重新加载获取id
            this.loadTemplate()
          } else {
            this.$message.warning('保存失败')
          }
        }).finally(() => {
          this.saving = false
        })
      },
      syncEditorContent() {
        if (this.editorInstance && this.editorInstance.getContent) {
          this.templateHtml = this.editorInstance.getContent()
        }
      },
      onTabChange(key) {
        if (key === 'preview') {
          // 切到预览前，从编辑器实例同步最新内容
          this.syncEditorContent()
        }
      },
      handleResetDefault() {
        this.templateHtml = getDefaultTemplate(this.billType)
        this.templateId = null
        this.templateName = '默认模板'
      },
      handlePrint() {
        doPrint(this.renderedHtml)
      },
      handleCancel() {
        this.visible = false
        this.editorReady = false
        this.editorInstance = null
      }
    }
  }
</script>
<style scoped>
</style>
