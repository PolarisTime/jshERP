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
        style="width:200px;"
        placeholder="选择模板"
        @change="handleTemplateChange">
        <a-select-option :value="0">系统默认模板</a-select-option>
        <a-select-option v-for="bt in builtinTemplates" :key="bt.key" :value="'builtin_'+bt.key">
          {{ bt.name }}
        </a-select-option>
        <a-select-option v-for="t in templateList" :key="t.id" :value="t.id">
          {{ t.templateName }}
        </a-select-option>
      </a-select>
      <a-input v-model="templateName" placeholder="模板名称" style="width:140px;" />
      <a-button type="primary" @click="handleSave" :loading="saving">
        {{ selectedTemplateId > 0 ? '更新模板' : '保存模板' }}
      </a-button>
      <a-popconfirm v-if="selectedTemplateId > 0" title="确定删除此模板？" @confirm="handleDelete">
        <a-button type="danger" ghost>删除</a-button>
      </a-popconfirm>
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
        <a-button v-if="clodopReady" icon="tool" @click="openDesigner" :loading="designing">设计器</a-button>
        <a-button type="primary" icon="eye" @click="handlePrint(true)" :disabled="!clodopReady">预览</a-button>
        <a-button v-if="clodopReady" type="primary" icon="printer" @click="handlePrint(false)">打印</a-button>
      </div>
    </div>
    <!-- 打印附加信息 -->
    <div style="margin-bottom:10px;display:flex;align-items:center;gap:12px;padding:6px 10px;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;">
      <span style="font-size:12px;color:#666;white-space:nowrap;">附加信息：</span>
      <a-input v-model="userInputFields.carNo" placeholder="车号" style="width:140px;" size="small" />
      <a-input v-model="userInputFields.extraText" placeholder="自定义文本" style="width:200px;" size="small" />
      <a-date-picker v-model="userInputFields.sendDate" placeholder="送货日期" style="width:160px;" size="small" format="YYYYMMDD" valueFormat="YYYYMMDD" />
    </div>
    <!-- 主体：左侧编辑器 + 右侧字段面板 -->
    <a-row :gutter="12">
      <a-col :span="18">
        <div class="editor-pane">
          <textarea
            class="template-textarea"
            v-model="templateHtml"
            spellcheck="false"
            :placeholder="'在此编辑 HTML 模板，使用 {{字段名}} 引用数据'"
          ></textarea>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="field-panel">
          <h4 class="panel-title">主表字段 <small>(点击插入)</small></h4>
          <div v-for="f in headerFields" :key="'h_'+f.key" style="margin-bottom:3px;">
            <a-tag color="blue" style="cursor:pointer;" @click="insertField(f.key, false)">{{ f.label }}</a-tag>
          </div>
          <a-divider style="margin:8px 0;" />
          <h4 class="panel-title">明细字段 <small>(点击插入)</small></h4>
          <div v-for="f in detailFields" :key="'d_'+f.key" style="margin-bottom:3px;">
            <a-tag color="green" style="cursor:pointer;" @click="insertField(f.key, true)">{{ f.label }}</a-tag>
          </div>
          <a-divider style="margin:8px 0;" />
          <h4 class="panel-title">内置变量</h4>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:3px;" @click="insertRaw('{{_index}}')">行序号</a-tag>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:3px;" @click="insertRaw('{{_printDate}}')">打印日期</a-tag>
          <a-tag color="orange" style="cursor:pointer;margin-bottom:3px;" @click="insertRaw('{{_printTime}}')">打印时间</a-tag>
          <a-divider style="margin:8px 0;" />
          <h4 class="panel-title">循环标记</h4>
          <a-tag color="red" style="cursor:pointer;margin-bottom:3px;" @click="insertRaw('<!--DETAIL_ROW_START-->')">循环开始</a-tag>
          <a-tag color="red" style="cursor:pointer;margin-bottom:3px;" @click="insertRaw('<!--DETAIL_ROW_END-->')">循环结束</a-tag>
          <a-divider style="margin:8px 0;" />
          <div style="font-size:11px;color:#999;line-height:1.6;">
            <p style="margin:0 0 4px;"><b>用法说明：</b></p>
            <p style="margin:0;">主表：<code>{'{{'}字段名{'}}'}</code></p>
            <p style="margin:0;">明细：<code>{'{{'}detail.字段名{'}}'}</code></p>
            <p style="margin:0;">明细行需放在循环标记之间</p>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-modal>
</template>
<script>
  import { savePrintTemplate, deletePrintTemplate, listPrintTemplate, getPrintFieldMeta } from '@/api/api'
  import { getDefaultTemplate, defaultTemplates } from '@/utils/printTemplateDefaults'
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
        userInputFields: { carNo: '', extraText: '', sendDate: '' },
        builtinTemplates: []
      }
    },
    methods: {
      show() {
        this.visible = true
        this.designing = false
        this.selectedTemplateId = 0
        this.templateName = ''
        this.templateHtml = getDefaultTemplate(this.billType)
        this.loadTemplateList()
        this.loadFieldMeta()
        this.initCLodop()
        // 初始化内置模板（根据单据类型显示可用的内置变体）
        this.builtinTemplates = []
        if (this.billType === 'saleOut') {
          if (defaultTemplates.saleOutA) {
            this.builtinTemplates.push({ key: 'saleOutA', name: '[内置] 销售出库单A版' })
          }
          if (defaultTemplates.saleOutTax) {
            this.builtinTemplates.push({ key: 'saleOutTax', name: '[内置] 销售出库单B版' })
          }
        }
        // 物流单：仅保留A版（CLodop）作为默认，不再提供HTML表格内置选项
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
      loadTemplateList() {
        listPrintTemplate({ billType: this.billType }).then((res) => {
          if (res && res.code === 200 && res.data) {
            this.templateList = res.data
            const defaultTpl = this.templateList.find(t => t.isDefault === '1')
            if (defaultTpl) {
              this.selectedTemplateId = defaultTpl.id
              this.templateName = defaultTpl.templateName
              this.templateHtml = defaultTpl.templateHtml
            }
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
      /** 打开 CLodop 设计器（用于检查/导入代码） */
      async openDesigner() {
        if (!this.clodopReady) {
          this.$message.warning('CLodop 未连接')
          return
        }
        this.designing = true
        try {
          // 渲染当前模板数据后传入设计器
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
      /** 点击字段标签 → 插入到 textarea 光标位置 */
      insertField(key, isDetail) {
        const placeholder = isDetail ? `{{detail.${key}}}` : `{{${key}}}`
        this.insertRaw(placeholder)
      },
      insertRaw(text) {
        const ta = this.$el.querySelector('.template-textarea')
        if (!ta) {
          this.templateHtml += text
          return
        }
        const start = ta.selectionStart
        const end = ta.selectionEnd
        const before = this.templateHtml.substring(0, start)
        const after = this.templateHtml.substring(end)
        this.templateHtml = before + text + after
        // 恢复光标到插入文本之后
        this.$nextTick(() => {
          ta.focus()
          ta.selectionStart = ta.selectionEnd = start + text.length
        })
      },
      handleTemplateChange(id) {
        if (id === 0) {
          this.templateName = ''
          this.templateHtml = getDefaultTemplate(this.billType)
        } else if (typeof id === 'string' && id.startsWith('builtin_')) {
          // 内置模板
          const key = id.replace('builtin_', '')
          this.templateName = ''
          this.templateHtml = defaultTemplates[key] || ''
        } else {
          const tpl = this.templateList.find(t => t.id === id)
          if (tpl) {
            this.templateName = tpl.templateName
            this.templateHtml = tpl.templateHtml
          }
        }
      },
      handleSave() {
        if (!this.templateName || !this.templateName.trim()) {
          this.$message.warning('请输入模板名称')
          return
        }
        this.saving = true
        savePrintTemplate({
          id: this.selectedTemplateId > 0 ? this.selectedTemplateId : null,
          billType: this.billType,
          templateName: this.templateName,
          templateHtml: this.templateHtml,
          isDefault: this.selectedTemplateId > 0 ? '1' : '0'
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
      /** 预览/打印 — 渲染后通过 CLodop 输出 */
      handlePrint(preview) {
        if (!this.clodopReady) {
          this.$message.warning('CLodop 未连接')
          return
        }
        const opts = {
          preview: preview !== false,
          printer: this.selectedPrinter || undefined,
          title: this.templateName || '打印'
        }
        // render() 替换占位符（HTML 和 CLodop 代码通用）
        let rendered
        try {
          rendered = render(this.templateHtml, this.model, this.dataSource, this.userInputFields)
        } catch (e) {
          this.$message.error('模板渲染出错：' + e.message)
          return
        }
        // CLodop 代码模板
        if (isCLodopCode(this.templateHtml)) {
          if (!execPrintCode(rendered, opts)) {
            this.$message.error('CLodop 代码执行失败')
          }
          return
        }
        // HTML 模板
        printHtml(rendered, opts)
      },
      handleCancel() {
        this.visible = false
        this.designing = false
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
