/**
 * CLodop 打印 Mixin
 * 模板从本地文件加载（printTemplateDefaults.js），不依赖数据库。
 *
 * 使用方式：
 *   1. mixins: [ClodopMixin]
 *   2. data 中声明 clodopBillType: 'purchaseIn'（对应 billTypeTemplateMap 的 key）
 */
import { getTemplatesByBillType } from '@/utils/printTemplateDefaults'
import { render } from '@/utils/printTemplateEngine'
import { isCLodopCode, execPrintCode, printHtml } from '@/utils/clodop'

export const ClodopMixin = {
  data() {
    return {
      clodopBillType: '',
      clodopReady: false,
      printerList: [],
      selectedPrinter: '',
      printTemplateList: [],
      selectedTemplateId: null
    }
  },
  mounted() {
    this.initClodop()
    if (this.clodopBillType) {
      this.loadPrintTemplate()
    }
  },
  methods: {
    // ─── 初始化 CLodop ──────────────────────────────────────
    async initClodop() {
      try {
        const { loadCLodop, isAvailable, getPrinterList, resetCLodop } = await import('@/utils/clodop')
        resetCLodop()
        const ok = await loadCLodop()
        this.clodopReady = ok && isAvailable()
        if (this.clodopReady) {
          this.printerList = getPrinterList()
        }
      } catch (e) {
        console.error('[CLodop] 初始化失败', e)
        this.clodopReady = false
      }
    },

    // ─── 从本地文件加载模板（不调用后端 API）────────────────
    loadPrintTemplate() {
      if (!this.clodopBillType) return
      const templates = getTemplatesByBillType(this.clodopBillType)
      this.printTemplateList = templates
      const def = templates.find(t => t.isDefault === '1') || templates[0]
      this.selectedTemplateId = def ? def.id : null
    },

    // ─── 通用打印（子页面可重写以加载明细数据）───────────────
    async doPrint(preview) {
      if (!this.clodopReady) {
        this.$message.warning('CLodop 未连接，请先点击状态标签重试')
        return
      }
      const tpl = this.printTemplateList.find(t => t.id === this.selectedTemplateId)
      if (!tpl) {
        this.$message.warning('未配置打印模板')
        return
      }
      const keys = this.selectedRowKeys || []
      if (keys.length === 0) { this.$message.warning('请先勾选数据'); return }
      const ids = preview ? [keys[0]] : keys
      const rowMap = {}
      ;(this.selectedRows || this.dataSource || []).forEach(r => { rowMap[r.id] = r })
      let ok = 0, fail = 0
      for (const id of ids) {
        try {
          const header = rowMap[id] || {}
          const rendered = render(tpl.templateHtml, header, [], {})
          const opts = {
            preview,
            printer: this.selectedPrinter || undefined,
            title: header.number || header.billNo || String(id)
          }
          const success = isCLodopCode(tpl.templateHtml)
            ? execPrintCode(rendered, opts)
            : printHtml(rendered, opts)
          success ? ok++ : fail++
        } catch (e) {
          console.error('[CLodop] 打印异常', e)
          fail++
        }
      }
      if (preview) {
        if (fail > 0) this.$message.error('预览失败，请检查 CLodop 是否正常运行')
      } else {
        if (fail === 0) this.$message.success(`已发送 ${ok} 张到打印机`)
        else this.$message.warning(`打印完成：成功 ${ok} 张，失败 ${fail} 张`)
      }
    }
  }
}
