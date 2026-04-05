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
      <a-select
        v-model="selectedTemplateId"
        style="width:260px;"
        placeholder="选择模板"
        @change="handleTemplateChange">
        <a-select-option :value="0">系统默认模板</a-select-option>
        <a-select-opt-group v-if="fileTemplates.length" label="文件模板">
          <a-select-option v-for="t in fileTemplates" :key="t.id" :value="t.id">
            {{ t.templateName }}
          </a-select-option>
        </a-select-opt-group>
        <a-select-opt-group v-if="dbTemplates.length" label="自定义模板">
          <a-select-option v-for="t in dbTemplates" :key="t.id" :value="t.id">
            {{ t.templateName }}{{ t.isDefault === '1' ? ' (默认)' : '' }}
          </a-select-option>
        </a-select-opt-group>
      </a-select>
      <a-input v-model="userInputFields.carNo" placeholder="车号" style="width:100px;" size="small" />
      <a-date-picker v-model="userInputFields.sendDate" placeholder="送货日期" style="width:140px;" size="small" format="YYYYMMDD" valueFormat="YYYYMMDD" />
      <!-- CLodop 操作区 -->
      <div style="margin-left:auto;display:flex;align-items:center;gap:8px;">
        <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
        <a-tag v-else color="orange" @click="initCLodop" style="cursor:pointer;">CLodop未连接(点击重试)</a-tag>
        <a-select
          v-if="clodopReady && printerList.length"
          v-model="selectedPrinter"
          style="width:180px;"
          placeholder="默认打印机">
          <a-select-option value="">默认打印机</a-select-option>
          <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
        </a-select>
        <a-button type="primary" icon="eye" @click="handlePrint(true)" :disabled="!clodopReady">预览</a-button>
        <a-button v-if="clodopReady" type="primary" icon="printer" @click="handlePrint(false)">打印</a-button>
      </div>
    </div>
  </a-modal>
</template>
<script>
  import { savePrintTemplate, deletePrintTemplate, listPrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate } from '@/utils/printTemplateDefaults'
  import { render } from '@/utils/printTemplateEngine'
  import { isCLodopCode, execPrintCode, printHtml, designTemplate } from '@/utils/clodop'

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
        designing: false,
        clodopReady: false,
        printerList: [],
        selectedPrinter: '',
        selectedTemplateId: 0,
        templateName: '',
        templateHtml: '',
        templateList: [],
        headerFields: [],
        detailFields: [],
        isDefault: false,
        userInputFields: { carNo: '', extraText: '', sendDate: '' },
        currentSource: '', // 当前选中模板的来源: 'db'/'file'/''
        printPreview: '' // 实时预览内容
      }
    },
    computed: {
      /** 当前是否为数据库自定义模板 */
      isCustomTemplate() {
        return this.currentSource === 'db' && this.selectedTemplateId > 0
      },
      /** 当前是否为文件模板 */
      isFileTemplate() {
        return this.currentSource === 'file'
      },
      /** 保存按钮文本 */
      saveButtonText() {
        if (this.isFileTemplate) return '保存到文件'
        return this.isCustomTemplate ? '更新模板' : '保存模板'
      },
      /** 分离文件模板和数据库模板 */
      fileTemplates() {
        return this.templateList.filter(t => t.source === 'file')
      },
      dbTemplates() {
        return this.templateList.filter(t => t.source === 'db')
      }
    },
    watch: {
      /** 监听模板HTML变化，实时更新预览 */
      templateHtml() {
        this.updatePreview()
      },
      /** 监听附加信息变化，实时更新预览 */
      userInputFields: {
        handler() {
          this.updatePreview()
        },
        deep: true
      }
    },
    methods: {
      // ═══ 生命周期 ═══

      async show() {
        this.visible = true
        this.designing = false
        await this.loadTemplateList()
        this.restorePrintPreferences()
        this.loadFieldMeta()
        this.initCLodop()
        this.updatePreview()
      },
      /** 恢复上次打印的模板和打印机选择 */
      restorePrintPreferences() {
        const storageKey = `printPrefs_${this.billType}`
        const prefs = JSON.parse(localStorage.getItem(storageKey) || '{}')
        if (prefs.templateId) {
          const tpl = this.templateList.find(t => t.id === prefs.templateId)
          if (tpl) {
            this.handleTemplateChange(prefs.templateId)
          } else {
            this.selectDefault()
          }
        } else {
          this.selectDefault()
        }
        if (prefs.printer) {
          this.selectedPrinter = prefs.printer
        }
      },
      /** 保存打印偏好设置 */
      savePrintPreferences() {
        const storageKey = `printPrefs_${this.billType}`
        const prefs = {
          templateId: this.selectedTemplateId,
          printer: this.selectedPrinter
        }
        localStorage.setItem(storageKey, JSON.stringify(prefs))
      },
      handleCancel() {
        this.visible = false
        this.designing = false
      },

      // ═══ 模板状态管理（核心：所有模板切换统一经过 applyTemplate） ═══

      /** 原子更新模板四要素，防止状态不同步 */
      applyTemplate({ id = 0, name = '', html = '', isDefault = false } = {}) {
        this.selectedTemplateId = id
        this.templateName = name
        this.templateHtml = html
        this.isDefault = isDefault
        this.updatePreview()
      },
      /** 实时更新预览内容 */
      updatePreview() {
        try {
          this.printPreview = render(this.templateHtml, this.model, this.dataSource, this.userInputFields)
        } catch (e) {
          this.printPreview = `<p style="color:red;">预览出错: ${e.message}</p>`
        }
      },
      /** 选中默认模板，无则回退系统默认 */
      selectDefault() {
        const defaultTpl = this.templateList.find(t => t.isDefault === '1')
        if (defaultTpl) {
          this.applyTemplate({
            id: defaultTpl.id,
            name: defaultTpl.templateName,
            html: defaultTpl.templateHtml,
            isDefault: true
          })
        } else {
          this.applyTemplate({ html: getDefaultTemplate(this.billType) })
        }
      },
      /** 下拉切换模板 */
      handleTemplateChange(id) {
        if (id === 0) {
          this.currentSource = ''
          this.applyTemplate({ html: getDefaultTemplate(this.billType) })
        } else {
          const tpl = this.templateList.find(t => t.id === id)
          if (tpl) {
            this.currentSource = tpl.source || 'db'
            this.applyTemplate({
              id: tpl.id,
              name: tpl.templateName,
              html: tpl.templateHtml,
              isDefault: tpl.isDefault === '1'
            })
          }
        }
      },
      /** 新增模板 — 保留当前编辑器内容，重置为新建状态 */
      handleNewTemplate() {
        this.selectedTemplateId = 0
        this.templateName = ''
        this.isDefault = false
      },

      // ═══ 数据加载 ═══

      /** 加载模板列表（纯数据） */
      async loadTemplateList() {
        const res = await listPrintTemplate({ billType: this.billType })
        if (res && res.code === 200 && Array.isArray(res.data)) {
          this.templateList = res.data
        }
      },
      loadFieldMeta() {
        getPrintFieldMeta({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.headerFields = res.data.headerFields || []
            this.detailFields = res.data.detailFields || []
          }
        })
      },
      async initCLodop() {
        try {
          const { loadCLodop, isAvailable, getPrinterList, resetCLodop } = await import('@/utils/clodop')
          resetCLodop()
          await loadCLodop()
          this.clodopReady = isAvailable()
          if (this.clodopReady) {
            this.printerList = getPrinterList()
          }
        } catch (e) {
          this.clodopReady = false
        }
      },

      // ═══ CRUD 操作 ═══

      async handleSave() {
        if (!this.templateName || !this.templateName.trim()) {
          this.$message.warning('请输入模板名称')
          return
        }
        this.saving = true
        try {
          let params = {
            billType: this.billType,
            templateName: this.templateName,
            templateHtml: this.templateHtml,
            source: this.isFileTemplate ? 'file' : 'db'
          }
          if (!this.isFileTemplate) {
            params.id = this.isCustomTemplate ? this.selectedTemplateId : null
            params.isDefault = this.isDefault ? '1' : '0'
          }
          const res = await savePrintTemplate(params)
          if (res && res.code === 200) {
            this.$message.success('模板保存成功')
            const savedName = this.templateName
            await this.loadTemplateList()
            const saved = this.templateList.find(t => t.templateName === savedName)
            if (saved) {
              this.selectedTemplateId = saved.id
              this.currentSource = saved.source || 'db'
              this.isDefault = saved.isDefault === '1'
            }
          } else {
            this.$message.warning('保存失败')
          }
        } finally {
          this.saving = false
        }
      },
      async handleDelete() {
        let res
        if (this.isFileTemplate) {
          const tpl = this.templateList.find(t => t.id === this.selectedTemplateId)
          res = await deletePrintTemplate({
            source: 'file',
            billType: this.billType,
            fileName: tpl ? tpl.fileName : ''
          })
        } else {
          res = await deletePrintTemplate({ id: this.selectedTemplateId })
        }
        if (res && res.code === 200) {
          this.$message.success('模板已删除')
          await this.loadTemplateList()
          this.selectDefault()
        } else {
          this.$message.warning('删除失败')
        }
      },

      // ═══ 编辑器辅助 ═══

      insertField(key, isDetail) {
        this.insertRaw(isDetail ? `{{detail.${key}}}` : `{{${key}}}`)
      },
      insertRaw(text) {
        const ta = this.$el.querySelector('.template-textarea')
        if (!ta) {
          this.templateHtml += text
          return
        }
        const start = ta.selectionStart
        const end = ta.selectionEnd
        this.templateHtml = this.templateHtml.substring(0, start) + text + this.templateHtml.substring(end)
        this.$nextTick(() => {
          ta.focus()
          ta.selectionStart = ta.selectionEnd = start + text.length
        })
      },

      // ═══ 打开 CLodop 设计器 ═══

      async openDesigner() {
        if (!this.clodopReady) {
          this.$message.warning('CLodop 未连接')
          return
        }
        this.designing = true
        try {
          let initCode = ''
          if (isCLodopCode(this.templateHtml)) {
            initCode = render(this.templateHtml, this.model, this.dataSource, this.userInputFields)
          }
          const resultCode = await designTemplate(initCode)
          if (resultCode && resultCode.trim()) {
            this.templateHtml = resultCode
            this.$message.success('设计器代码已导入编辑器')
          }
        } catch (e) {
          this.$message.error('设计器打开失败：' + e.message)
        } finally {
          this.designing = false
        }
      },

      // ═══ 预览/打印 ═══

      handlePrint(preview) {
        if (!this.clodopReady) {
          this.$message.warning('CLodop 未连接')
          return
        }
        // 保存当前模板和打印机选择
        this.savePrintPreferences()
        const opts = {
          preview: preview !== false,
          printer: this.selectedPrinter || undefined,
          title: this.templateName || '打印'
        }
        let rendered
        try {
          rendered = render(this.templateHtml, this.model, this.dataSource, this.userInputFields)
        } catch (e) {
          this.$message.error('模板渲染出错：' + e.message)
          return
        }
        if (isCLodopCode(this.templateHtml)) {
          if (!execPrintCode(rendered, opts)) {
            this.$message.error('CLodop 代码执行失败')
          }
          return
        }
        printHtml(rendered, opts)
      }
    }
  }
</script>
<style scoped>
  .editor-pane {
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    overflow: hidden;
  }
  .template-textarea {
    width: 100%;
    height: 520px;
    font-family: 'Courier New', Consolas, monospace;
    font-size: 12px;
    line-height: 1.5;
    padding: 8px;
    border: none;
    resize: none;
    white-space: pre;
    overflow: auto;
    tab-size: 2;
    outline: none;
  }
  .template-textarea:focus {
    box-shadow: inset 0 0 0 1px #40a9ff;
  }
  .field-panel {
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    padding: 8px;
    height: 520px;
    overflow-y: auto;
  }
  .panel-title {
    margin: 0 0 6px;
    font-size: 13px;
    color: #333;
  }
  .panel-title small {
    color: #999;
    font-weight: normal;
  }
</style>
