/**
 * 打印模板渲染引擎
 * 将 HTML 模板中的占位符替换为实际数据
 * 打印优先使用 CLodop 本地打印服务，不可用时降级为浏览器 iframe 打印
 */
import { isAvailable, printHtml as clodopPrint } from './clodop'

/**
 * 渲染打印模板
 * @param {string} templateHtml - HTML模板字符串
 * @param {object} model - 主表数据对象
 * @param {array} dataSource - 明细行数据数组
 * @returns {string} 渲染后的HTML
 */
export function render(templateHtml, model, dataSource) {
  if (!templateHtml) return ''
  let html = templateHtml

  // 1. 处理明细循环区域
  const loopRegex = /<!--DETAIL_ROW_START-->([\s\S]*?)<!--DETAIL_ROW_END-->/g
  html = html.replace(loopRegex, (match, rowTemplate) => {
    if (!dataSource || dataSource.length === 0) return ''
    return dataSource.map((item, index) => {
      let row = rowTemplate
      // 替换 {{_index}}
      row = row.replace(/\{\{_index\}\}/g, String(index + 1))
      // 替换 {{detail.xxx}}
      row = row.replace(/\{\{detail\.(\w+)\}\}/g, (m, key) => {
        return item[key] != null ? String(item[key]) : ''
      })
      return row
    }).join('')
  })

  // 2. 替换内置辅助变量
  const now = new Date()
  const printDate = now.getFullYear() + '-' +
    String(now.getMonth() + 1).padStart(2, '0') + '-' +
    String(now.getDate()).padStart(2, '0')
  const printTime = printDate + ' ' +
    String(now.getHours()).padStart(2, '0') + ':' +
    String(now.getMinutes()).padStart(2, '0')
  html = html.replace(/\{\{_printDate\}\}/g, printDate)
  html = html.replace(/\{\{_printTime\}\}/g, printTime)

  // 3. 替换主表字段 {{xxx}}（排除 detail. 前缀的，已在步骤1处理）
  html = html.replace(/\{\{(\w+)\}\}/g, (match, key) => {
    if (model && model[key] != null) {
      return String(model[key])
    }
    return ''
  })

  return html
}

/**
 * 执行打印：优先使用 CLodop，不可用时降级为 iframe + window.print()
 * @param {string} renderedHtml - 渲染后的HTML内容
 * @param {object} [options] - 打印选项（CLodop 模式有效）
 * @param {boolean} [options.preview=true] - 是否预览
 * @param {string} [options.printer] - 指定打印机
 * @param {string} [options.title] - 打印任务名称
 * @param {number} [options.copies] - 打印份数
 */
export function doPrint(renderedHtml, options = {}) {
  // 优先使用 CLodop 本地打印
  if (isAvailable()) {
    const success = clodopPrint(renderedHtml, { preview: true, ...options })
    if (success) return
    // CLodop 调用失败则降级
  }

  // 降级：iframe + window.print()
  const iframe = document.createElement('iframe')
  iframe.style.position = 'absolute'
  iframe.style.left = '-9999px'
  iframe.style.width = '0'
  iframe.style.height = '0'
  document.body.appendChild(iframe)

  const doc = iframe.contentDocument || iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @page { margin: 10mm; }
  body { font-family: SimSun, serif; font-size: 12px; color: #000; }
  table { border-collapse: collapse; width: 100%; }
  th, td { border: 1px solid #000; padding: 4px 8px; text-align: left; font-size: 12px; }
  th { background-color: #f0f0f0; font-weight: bold; }
  h2 { text-align: center; margin: 10px 0; }
  .header-row td { border: none; padding: 4px 8px; }
  .footer-info { margin-top: 10px; font-size: 11px; }
</style>
</head>
<body>${renderedHtml}</body>
</html>`)
  doc.close()

  const cleanup = () => {
    if (iframe.parentNode) {
      document.body.removeChild(iframe)
    }
  }

  iframe.contentWindow.focus()
  setTimeout(() => {
    // 监听打印完成事件，优先用 afterprint，保底 5s 延迟
    iframe.contentWindow.addEventListener('afterprint', cleanup)
    iframe.contentWindow.print()
    setTimeout(cleanup, 5000)
  }, 300)
}
