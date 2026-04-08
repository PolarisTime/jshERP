/**
 * CLodop 打印 Mixin
 * 模板从后端文件目录加载（/home/sakura/jshERP/printTemplates），不使用数据库。
 *
 * 使用方式：
 *   1. mixins: [ClodopMixin]
 *   2. data 中声明 clodopBillType: 'purchaseIn'
 */
import { listPrintTemplate } from '@/api/api'
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
    async initClodop() {
      try {
        const { loadCLodop, isAvailable, getPrinterList, resetCLodop } = await import('@/utils/clodop')
        if (!isAvailable()) {
          resetCLodop()
          await loadCLodop()
        }
        this.clodopReady = isAvailable()
        if (this.clodopReady) {
          this.printerList = getPrinterList()
          this.$nextTick(() => {
            if (typeof this.initScroll === 'function') this.initScroll()
          })
        }
      } catch (e) {
        console.error('[CLodop] 初始化失败', e)
        this.clodopReady = false
      }
    },

    // 从后端文件目录加载模板（后端 listByBillType 同时返回 db+file，
    // 但数据库已清空，只有 file 来源的模板）
    loadPrintTemplate() {
      if (!this.clodopBillType) return
      listPrintTemplate({ billType: this.clodopBillType }).then(res => {
        if (res && res.code === 200 && Array.isArray(res.data)) {
          // 只使用文件来源的模板
          const fileTemplates = res.data.filter(t => t.source === 'file' || !t.source)
          const templates = fileTemplates.length > 0 ? fileTemplates : res.data
          this.printTemplateList = templates
          const def = templates.find(t => t.isDefault === '1') || templates[0]
          this.selectedTemplateId = def ? def.id : null
        }
      }).catch(e => {
        console.warn('[CLodop] 模板加载失败', e)
      })
    },

    async doPrint(preview) {
      if (!this.clodopReady) {
        this.$message.warning('CLodop 未连接，请先点击状态标签重试')
        return
      }
      const tpl = this.printTemplateList.find(t => t.id === this.selectedTemplateId)
      if (!tpl) {
        this.$message.warning('未找到打印模板，请在 printTemplates 目录放置 .lodop 文件')
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
