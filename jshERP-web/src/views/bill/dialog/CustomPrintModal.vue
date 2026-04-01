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
      <a-button type="primary" icon="printer" @click="handlePrint" :disabled="loading" style="margin-left:auto;">打印预览</a-button>
    </div>
    <a-row :gutter="16">
      <!-- 编辑器 -->
      <a-col :span="18">
        <div v-if="editorReady">
          <ckeditor
            :editor="editorClass"
            v-model="templateHtml"
            :config="editorConfig"
            @ready="onEditorReady"
          />
        </div>
      </a-col>
      <!-- 字段面板 -->
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
  </a-modal>
</template>
<script>
  import { getPrintTemplate, savePrintTemplate, deletePrintTemplate, listPrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate } from '@/utils/printTemplateDefaults'
  import { render, doPrint } from '@/utils/printTemplateEngine'
  import CKEditor from '@ckeditor/ckeditor5-vue2'
  import ClassicEditor from '@ckeditor/ckeditor5-build-classic'
  import '@ckeditor/ckeditor5-build-classic/build/translations/zh-cn'

  export default {
    name: 'CustomPrintModal',
    components: { ckeditor: CKEditor.component },
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
        editorReady: false,
        selectedTemplateId: 0,
        templateName: '',
        templateHtml: '',
        templateList: [],
        headerFields: [],
        detailFields: [],
        editorInstance: null,
        editorClass: ClassicEditor,
        editorConfig: {
          language: 'zh-cn',
          toolbar: [
            'undo', 'redo', '|',
            'heading', '|',
            'bold', 'italic', 'strikethrough', '|',
            'alignment', '|',
            'insertTable', '|',
            'bulletedList', 'numberedList', '|',
            'indent', 'outdent'
          ],
          table: {
            contentToolbar: [
              'tableColumn', 'tableRow', 'mergeTableCells',
              'tableProperties', 'tableCellProperties'
            ]
          },
          htmlSupport: {
            allow: [{ name: /.*/, attributes: true, classes: true, styles: true }]
          }
        }
      }
    },
    methods: {
      show() {
        this.visible = true
        this.loading = true
        this.selectedTemplateId = 0
        this.templateName = ''
        this.templateHtml = getDefaultTemplate(this.billType)
        this.editorReady = true
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
              if (this.editorInstance) {
                this.editorInstance.setData(this.templateHtml)
              }
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
          // 系统默认模板
          this.templateName = ''
          this.templateHtml = getDefaultTemplate(this.billType)
        } else {
          const tpl = this.templateList.find(t => t.id === id)
          if (tpl) {
            this.templateName = tpl.templateName
            this.templateHtml = tpl.templateHtml
          }
        }
        if (this.editorInstance) {
          this.editorInstance.setData(this.templateHtml)
        }
      },
      onEditorReady(editor) {
        this.editorInstance = editor
      },
      insertField(key, isDetail) {
        const placeholder = isDetail ? `{{detail.${key}}}` : `{{${key}}}`
        this.insertRaw(placeholder)
      },
      insertRaw(text) {
        if (this.editorInstance) {
          this.editorInstance.editing.view.focus()
          const viewFragment = this.editorInstance.data.processor.toView(text)
          const modelFragment = this.editorInstance.data.toModel(viewFragment)
          this.editorInstance.model.insertContent(modelFragment)
        }
      },
      /** 保存为新模板 */
      handleSave() {
        if (!this.templateName || !this.templateName.trim()) {
          this.$message.warning('请输入模板名称')
          return
        }
        this.syncEditorContent()
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
        this.syncEditorContent()
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
            if (this.editorInstance) {
              this.editorInstance.setData(this.templateHtml)
            }
            this.loadTemplateList()
          } else {
            this.$message.warning('删除失败')
          }
        })
      },
      syncEditorContent() {
        if (this.editorInstance) {
          this.templateHtml = this.editorInstance.getData()
        }
      },
      handlePrint() {
        this.syncEditorContent()
        const html = render(this.templateHtml, this.model, this.dataSource)
        doPrint(html)
      },
      handleCancel() {
        this.visible = false
        this.editorReady = false
        if (this.editorInstance) {
          this.editorInstance.destroy()
          this.editorInstance = null
        }
      }
    }
  }
</script>
<style scoped>
  ::v-deep .ck-editor__editable {
    min-height: 460px;
    max-height: 600px;
  }
</style>
