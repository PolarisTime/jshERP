/**
 * CLodop 打印 Mixin
 * 为所有列表页提供统一的打印功能：连接状态、模板选择、打印机选择、预览/打印
 *
 * 使用方式：
 *   1. mixins: [ClodopMixin]
 *   2. data 中声明 clodopBillType: 'purchaseIn'（对应打印模板的 billType）
 *   3. template 中引用 <clodop-bar> slot 或直接使用 clodopReady / printTemplateList 等数据
 *
 * doPrint(preview) 默认实现：将选中行数据作为 header、空数组作为 items 渲染模板。
 * 若需要加载明细数据，在页面中重写 doPrint 方法即可。
 */
import { listPrintTemplate } from '@/api/api'
import { render } from '@/utils/printTemplateEngine'
import { isCLodopCode, execPrintCode, printHtml } from '@/utils/clodop'

export const ClodopMixin = {
  data() {
    return {
      clodopBillType: '',     // 子组件覆盖：对应打印模板 billType
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
        await loadCLodop()
        this.clodopReady = isAvailable()
        if (this.clodopReady) {
          this.printerList = getPrinterList()
          if (this.clodopBillType && !this.printTemplateList.length) {
            this.loadPrintTemplate()
          }
        }
      } catch (e) {
        this.clodopReady = false
      }
    },

    // ─── 加载打印模板列表 ────────────────────────────────────
    loadPrintTemplate() {
      if (!this.clodopBillType) return
      listPrintTemplate({ billType: this.clodopBillType }).then(res => {
        if (res && res.code === 200 && Array.isArray(res.data)) {
          this.printTemplateList = res.data
          const def = res.data.find(t => t.isDefault === '1') || res.data[0]
          this.selectedTemplateId = def ? def.id : null
        }
      })
    },

    // ─── 通用打印（子页面可重写以加载明细数据）───────────────
    async doPrint(preview) {
      if (!this.clodopReady) {
        this.$message.warning('CLodop 未连接，请先点击状态标签重试')
        return
      }
      const tpl = this.printTemplateList.find(t => t.id === this.selectedTemplateId)
      if (!tpl) {
        this.$message.warning('未找到打印模板，请在【系统管理-打印模板管理】中配置')
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
      // 预览和打印都给出反馈
      if (preview) {
        if (fail > 0) this.$message.error('预览失败，请检查 CLodop 是否正常运行')
      } else {
        if (fail === 0) this.$message.success(`已发送 ${ok} 张到打印机`)
        else this.$message.warning(`打印完成：成功 ${ok} 张，失败 ${fail} 张`)
      }
    }
  }
}
