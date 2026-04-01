<template>
  <a-modal
    title="自定义格式打印"
    :width="1300"
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
      <!-- CLodop 打印操作区 -->
      <div style="margin-left:auto;display:flex;align-items:center;gap:8px;">
        <a-tag v-if="clodopReady" color="green">CLodop已连接</a-tag>
        <a-tag v-else color="orange" @click="initCLodop" style="cursor:pointer;">CLodop未连接(点击重试)</a-tag>
        <a-select
          v-if="clodopReady && printerList.length"
          v-model="selectedPrinter"
          style="width:200px;"
          placeholder="默认打印机">
          <a-select-option value="">默认打印机</a-select-option>
          <a-select-option v-for="p in printerList" :key="p" :value="p">{{ p }}</a-select-option>
        </a-select>
        <a-button type="primary" icon="eye" @click="handlePrint(true)" :disabled="loading">预览打印</a-button>
        <a-button v-if="clodopReady" type="primary" icon="printer" @click="handlePrint(false)" :disabled="loading">直接打印</a-button>
      </div>
    </div>
    <!-- 主体区域 -->
    <a-tabs v-model="activeTab" size="small" @change="onTabChange">
      <!-- 打印预览 -->
      <a-tab-pane key="preview" tab="打印预览">
        <a-row :gutter="16">
          <a-col :span="18">
            <div class="preview-pane" v-html="renderedPreview"></div>
          </a-col>
          <a-col :span="6">
            <div class="field-panel">
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
      </a-tab-pane>
      <!-- 模板源码 -->
      <a-tab-pane key="source" tab="模板源码">
        <textarea
          class="template-textarea"
          v-model="templateHtml"
          spellcheck="false"
        ></textarea>
      </a-tab-pane>
      <!-- 可视化设计器 -->
      <a-tab-pane key="designer" tab="可视化设计">
        <div class="designer-container">
          <!-- 左侧拖拽面板 -->
          <div class="designer-left">
            <h4 class="panel-title">主表字段</h4>
            <div v-for="f in headerFields" :key="'hp_h_'+f.key"
              class="ep-draggable-item" :tid="billType + '.' + f.key">
              {{ f.label }}
            </div>
            <h4 class="panel-title" style="margin-top:12px;">明细表格</h4>
            <div class="ep-draggable-item" :tid="billType + '.detailTable'">明细表</div>
            <h4 class="panel-title" style="margin-top:12px;">辅助元素</h4>
            <div class="ep-draggable-item" tid="defaultModule.hline">横线</div>
            <div class="ep-draggable-item" tid="defaultModule.vline">竖线</div>
            <div class="ep-draggable-item" tid="defaultModule.rect">矩形</div>
            <div class="ep-draggable-item" tid="defaultModule.oval">椭圆</div>
            <div class="ep-draggable-item" tid="defaultModule.text">自定义文本</div>
            <div class="ep-draggable-item" tid="defaultModule.image">图片</div>
          </div>
          <!-- 中间设计画布 -->
          <div class="designer-center">
            <div v-if="!designerInited" style="text-align:center;padding:40px;color:#999;">
              设计器加载中...
            </div>
            <div id="hiprint-printTemplate"></div>
          </div>
          <!-- 右侧属性面板 -->
          <div class="designer-right">
            <h4 class="panel-title">元素属性</h4>
            <div id="PrintElementOptionSetting"></div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>
<script>
  import { getPrintTemplate, savePrintTemplate, deletePrintTemplate, listPrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate } from '@/utils/printTemplateDefaults'
  import { render, doPrint } from '@/utils/printTemplateEngine'
  import { BillElementProvider, isHiprintJson } from '@/utils/hiprintConfig'
  import { getDefaultHiprintTemplate } from '@/utils/hiprintDefaultTemplates'

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
        clodopReady: false,
        printerList: [],
        selectedPrinter: '',
        activeTab: 'preview',
        selectedTemplateId: 0,
        templateName: '',
        templateHtml: '',
        templateList: [],
        headerFields: [],
        detailFields: [],
        // hiprint 设计器相关
        hiprintTemplate: null,
        designerInited: false,
        templateMode: 'html'  // 'html' 或 'hiprint'
      }
    },
    computed: {
      renderedPreview() {
        if (!this.templateHtml) return '<p style="color:#999;text-align:center;padding:40px;">暂无模板内容</p>'
        // hiprint JSON 模板在预览 tab 显示提示
        if (isHiprintJson(this.templateHtml)) {
          return '<p style="color:#1890ff;text-align:center;padding:40px;">此模板为可视化设计模板，请在「可视化设计」标签页中预览和打印</p>'
        }
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
        this.templateMode = 'html'
        this.designerInited = false
        this.hiprintTemplate = null
        this.loadTemplateList()
        this.loadFieldMeta()
        this.initCLodop()
      },
      /** 初始化 CLodop 连接 */
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
      /** 加载模板列表，自动选中默认模板 */
      loadTemplateList() {
        listPrintTemplate({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.templateList = res.data
            const defaultTpl = this.templateList.find(t => t.isDefault === '1')
            if (defaultTpl) {
              this.selectedTemplateId = defaultTpl.id
              this.templateName = defaultTpl.templateName
              this.templateHtml = defaultTpl.templateHtml
              this.detectTemplateMode(defaultTpl.templateHtml)
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
      /** 检测模板格式 */
      detectTemplateMode(content) {
        this.templateMode = isHiprintJson(content) ? 'hiprint' : 'html'
      },
      /** Tab 切换 */
      onTabChange(key) {
        this.activeTab = key
        if (key === 'designer') {
          this.$nextTick(() => this.initDesigner())
        }
      },
      /** 初始化 hiprint 设计器 */
      async initDesigner() {
        if (this.designerInited) {
          // 已初始化，如果切换了模板则更新
          if (this.templateMode === 'hiprint' && this.hiprintTemplate) {
            try {
              this.hiprintTemplate.update(JSON.parse(this.templateHtml))
            } catch (e) {
              // ignore
            }
          }
          return
        }
        try {
          // jQuery 必须先挂载到 window，hiprint 在加载时即依赖全局 jQuery
          const $ = (await import('jquery')).default
          window.jQuery = window.jQuery || $
          window.$ = window.$ || $

          const { hiprint, defaultElementTypeProvider } = await import('vue-plugin-hiprint')

          // 注册 provider：默认元素 + 自定义单据字段
          const billProvider = new BillElementProvider(this.billType, this.headerFields, this.detailFields)
          hiprint.init({ providers: [new defaultElementTypeProvider(), billProvider] })

          // 构建拖拽元素
          this.$nextTick(() => {
            hiprint.PrintElementTypeManager.buildByHtml($('.ep-draggable-item'))
          })

          // 解析已有模板 JSON 或加载默认 hiprint 模板
          let json = {}
          if (this.templateMode === 'hiprint') {
            try {
              json = JSON.parse(this.templateHtml)
            } catch (e) {
              json = getDefaultHiprintTemplate(this.billType)
            }
          } else {
            // HTML 模式下首次打开设计器，加载默认 hiprint 模板
            json = getDefaultHiprintTemplate(this.billType)
          }

          this.hiprintTemplate = new hiprint.PrintTemplate({
            template: json,
            settingContainer: '#PrintElementOptionSetting',
            dataMode: 1,
            history: true
          })
          this.hiprintTemplate.design('#hiprint-printTemplate')
          this.designerInited = true
        } catch (e) {
          console.error('hiprint 设计器初始化失败', e)
          this.$message.error('可视化设计器加载失败：' + e.message)
        }
      },
      /** 从设计器获取模板 JSON 字符串 */
      getDesignerJson() {
        if (this.hiprintTemplate) {
          return JSON.stringify(this.hiprintTemplate.getJson())
        }
        return this.templateHtml
      },
      /** 切换模板 */
      handleTemplateChange(id) {
        if (id === 0) {
          this.templateName = ''
          this.templateHtml = getDefaultTemplate(this.billType)
          this.templateMode = 'html'
        } else {
          const tpl = this.templateList.find(t => t.id === id)
          if (tpl) {
            this.templateName = tpl.templateName
            this.templateHtml = tpl.templateHtml
            this.detectTemplateMode(tpl.templateHtml)
            // 设计器已打开且是 hiprint 模板时更新
            if (this.designerInited && this.templateMode === 'hiprint' && this.hiprintTemplate) {
              try {
                this.hiprintTemplate.update(JSON.parse(tpl.templateHtml))
              } catch (e) {
                // ignore
              }
            }
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
          const ta = document.createElement('textarea')
          ta.value = text
          document.body.appendChild(ta)
          ta.select()
          document.execCommand('copy')
          document.body.removeChild(ta)
          this.$message.success('已复制：' + text)
        }
      },
      /** 获取当前模板内容（自动判断来源） */
      getCurrentTemplateContent() {
        if (this.activeTab === 'designer' && this.hiprintTemplate) {
          return this.getDesignerJson()
        }
        return this.templateHtml
      },
      /** 保存为新模板 */
      handleSave() {
        if (!this.templateName || !this.templateName.trim()) {
          this.$message.warning('请输入模板名称')
          return
        }
        const content = this.getCurrentTemplateContent()
        this.saving = true
        savePrintTemplate({
          id: null,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: content,
          isDefault: '0'
        }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板保存成功')
            this.templateHtml = content
            this.detectTemplateMode(content)
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
        const content = this.getCurrentTemplateContent()
        this.saving = true
        savePrintTemplate({
          id: this.selectedTemplateId,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: content,
          isDefault: '1'
        }).then((res) => {
          if (res && res.code === 200) {
            this.$message.success('模板更新成功')
            this.templateHtml = content
            this.detectTemplateMode(content)
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
            this.templateMode = 'html'
            this.loadTemplateList()
          } else {
            this.$message.warning('删除失败')
          }
        })
      },
      /** 打印（兼容 HTML 和 hiprint 两种模板） */
      handlePrint(preview) {
        // hiprint 模板：使用 hiprint 引擎打印
        if (this.activeTab === 'designer' && this.hiprintTemplate) {
          const printData = { ...this.model, details: this.dataSource }
          if (preview !== false) {
            this.hiprintTemplate.print(printData)
          } else {
            // 直接打印（浏览器方式）
            this.hiprintTemplate.print(printData)
          }
          return
        }
        // 已保存的 hiprint JSON 模板（非设计器 tab 但模板是 JSON）
        if (isHiprintJson(this.templateHtml) && this.hiprintTemplate) {
          const printData = { ...this.model, details: this.dataSource }
          this.hiprintTemplate.print(printData)
          return
        }
        // HTML 模板：使用现有引擎
        const html = render(this.templateHtml, this.model, this.dataSource)
        doPrint(html, {
          preview: preview !== false,
          printer: this.selectedPrinter || undefined,
          title: this.templateName || '打印'
        })
      },
      handleCancel() {
        this.visible = false
      }
    }
  }
</script>
<style scoped>
  /* 预览面板 */
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

  /* 字段面板 */
  .field-panel {
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    padding: 8px;
    max-height: 600px;
    overflow-y: auto;
  }

  /* 源码编辑 */
  .template-textarea {
    width: 100%;
    min-height: 520px;
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

  /* 可视化设计器布局 */
  .designer-container {
    display: flex;
    height: 560px;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    overflow: hidden;
  }
  .designer-left {
    width: 160px;
    padding: 8px;
    overflow-y: auto;
    border-right: 1px solid #e8e8e8;
    background: #fafafa;
  }
  .designer-center {
    flex: 1;
    overflow: auto;
    background: #f0f2f5;
    padding: 10px;
  }
  .designer-right {
    width: 240px;
    padding: 8px;
    overflow-y: auto;
    border-left: 1px solid #e8e8e8;
    background: #fafafa;
  }
  .panel-title {
    margin: 0 0 6px;
    font-size: 13px;
    color: #333;
  }

  /* 可拖拽元素 */
  .ep-draggable-item {
    padding: 4px 8px;
    margin: 2px 0;
    background: #fff;
    border: 1px solid #d9d9d9;
    border-radius: 3px;
    cursor: move;
    font-size: 12px;
    user-select: none;
    transition: all 0.2s;
  }
  .ep-draggable-item:hover {
    background: #e6f7ff;
    border-color: #1890ff;
    color: #1890ff;
  }
</style>
<!-- hiprint 全局样式（不加 scoped） -->
<style>
  /* hiprint 设计器画布容器 */
  #hiprint-printTemplate .hiprint-printPaper {
    background: #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    margin: 0 auto;
  }
  /* 属性面板样式调整 */
  #PrintElementOptionSetting .hiprint-option-item {
    margin-bottom: 6px;
  }
  #PrintElementOptionSetting .hiprint-option-item label {
    font-size: 12px;
    color: #666;
  }
  #PrintElementOptionSetting .hiprint-option-item input,
  #PrintElementOptionSetting .hiprint-option-item select {
    width: 100%;
    padding: 2px 4px;
    font-size: 12px;
    border: 1px solid #d9d9d9;
    border-radius: 2px;
  }
</style>
