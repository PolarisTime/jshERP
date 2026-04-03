/**
 * 打印模板渲染引擎
 * 将 HTML 模板中的占位符替换为实际数据
 * 打印优先使用 CLodop 本地打印服务，不可用时降级为浏览器 iframe 打印
 */
import { isAvailable, printHtml as clodopPrint } from './clodop'

/**
 * 推导长度：优先从条码第二个 "-" 后判断，失败则从规格(standard)判断
 * 规则：
 *   PL6E/PL8E/PL10E/PL6/PL8/PL10/XC6/XC8/XC10 → "-"
 *   含 *12（如 10E*12, 12*12, 18*12, 20*12）→ "12米"
 *   纯数字或数字+E（如 10E, 12, 14, 18, 20）→ "9米"
 */
function deriveLength(barCode, standard) {
  // 先尝试从条码第二个 "-" 后取值
  var seg = ''
  if (barCode) {
    var parts = barCode.split('-')
    if (parts.length >= 3) {
      seg = parts.slice(2).join('-').trim()
    }
  }
  // 条码无法解析时，用规格字段
  if (!seg && standard) {
    seg = String(standard).trim()
  }
  if (!seg) return ''
  // 盘螺/线材类 → 无长度
  if (/^(PL|XC)(6|8|10)E?$/i.test(seg)) return '-'
  // 含 *12 → 12米
  if (/\*12$/i.test(seg)) return '12米'
  // 纯数字或数字+E → 9米
  if (/^\d+E?$/i.test(seg)) return '9米'
  return ''
}

/**
 * 预处理：计算派生字段，合并额外输入
 * @param {object} model - 主表数据（会被原地修改）
 * @param {array} dataSource - 明细行数据（每项会被原地修改）
 * @param {object} [extraFields] - 用户输入的额外字段（carNo, sendDate 等）
 */
function preProcess(model, dataSource, extraFields) {
  // 合并用户输入字段到 model
  if (extraFields) {
    Object.keys(extraFields).forEach(k => {
      if (extraFields[k] != null && extraFields[k] !== '') {
        model[k] = extraFields[k]
      }
    })
  }
  // sendDate 格式化为 YYYY年MM月DD日
  if (model.sendDate) {
    var sd = model.sendDate
    if (typeof sd === 'object') {
      // moment 对象或 Date 对象
      var dt = sd._isAMomentObject ? sd.toDate() : (sd instanceof Date ? sd : new Date(sd))
      model.sendDate = dt.getFullYear() + '年' +
        String(dt.getMonth() + 1).padStart(2, '0') + '月' +
        String(dt.getDate()).padStart(2, '0') + '日'
    } else if (/^\d{8}$/.test(sd)) {
      model.sendDate = sd.substring(0,4) + '年' + sd.substring(4,6) + '月' + sd.substring(6,8) + '日'
    } else if (/^\d{4}-\d{2}-\d{2}/.test(sd)) {
      model.sendDate = sd.substring(0,4) + '年' + sd.substring(5,7) + '月' + sd.substring(8,10) + '日'
    }
    // 已经是 X年X月X日 格式则保持不变
  }
  // sendDate 默认取当天
  if (!model.sendDate) {
    var d = new Date()
    model.sendDate = d.getFullYear() + '年' +
      String(d.getMonth() + 1).padStart(2, '0') + '月' +
      String(d.getDate()).padStart(2, '0') + '日'
  }
  // 别名映射：supplierCompany = organName（客户名称）
  if (!model.supplierCompany && model.organName) {
    model.supplierCompany = model.organName
  }
  // 别名映射：customerName = organName
  if (!model.customerName && model.organName) {
    model.customerName = model.organName
  }
  if (dataSource && dataSource.length > 0) {
    // 计算 totalPiece（明细 operNumber 合计）
    let totalPiece = 0
    dataSource.forEach(item => {
      const num = parseFloat(item.operNumber)
      if (!isNaN(num)) totalPiece += num
      // 派生 length 字段（优先条码，其次规格，在去除*12之前）
      if (item.length == null || item.length === '') {
        item.length = deriveLength(item.barCode, item.standard)
      }
      // 规格去除 *12 后缀（如 18*12 → 18）
      if (item.standard) {
        item.standard = String(item.standard).replace(/\*12$/g, '')
      }
    })
    model.totalPiece = totalPiece
    // 计算 totalWeight（明细 weight 合计）
    if (!model.totalWeight) {
      let totalWeight = 0
      dataSource.forEach(item => {
        const w = parseFloat(item.weight)
        if (!isNaN(w)) totalWeight += w
      })
      if (totalWeight > 0) model.totalWeight = totalWeight
    }
  }
}

/**
 * 渲染打印模板
 * @param {string} templateHtml - HTML模板字符串
 * @param {object} model - 主表数据对象
 * @param {array} dataSource - 明细行数据数组
 * @param {object} [extraFields] - 用户输入的额外字段
 * @returns {string} 渲染后的HTML
 */
export function render(templateHtml, model, dataSource, extraFields) {
  if (!templateHtml) return ''
  let html = templateHtml

  // 0. 过滤空明细行（无商品名称的行不打印）+ 预处理
  if (dataSource) {
    dataSource = dataSource.filter(item => item.name || item.barCode || item.materialName)
  }
  preProcess(model || {}, dataSource || [], extraFields)

  // 1a. 处理 {{#each details}}...{{/each}} 语法（CLodop JS 数组模板）
  const eachRegex = /\{\{#each\s+details\}\}([\s\S]*?)\{\{\/each\}\}/g
  html = html.replace(eachRegex, (match, rowTemplate) => {
    if (!dataSource || dataSource.length === 0) return ''
    return dataSource.map((item, index) => {
      let row = rowTemplate
      row = row.replace(/\{\{_index\}\}/g, String(index + 1))
      // {{xxx}} 在 each 块内映射到 detail item 字段
      row = row.replace(/\{\{(\w+)\}\}/g, (m, key) => {
        if (key === '_index') return String(index + 1)
        return item[key] != null ? String(item[key]) : ''
      })
      return row
    }).join('')
  })

  // 1b. 处理 <!--DETAIL_ROW_START-->...<!--DETAIL_ROW_END--> 语法
  const loopRegex = /<!--DETAIL_ROW_START-->([\s\S]*?)<!--DETAIL_ROW_END-->/g
  html = html.replace(loopRegex, (match, rowTemplate) => {
    if (!dataSource || dataSource.length === 0) return ''
    // 检测是否为 CLodop 代码模板（包含 LODOP.ADD_PRINT_ 调用）
    const isCLodopBlock = /LODOP\.\s*ADD_PRINT_/i.test(rowTemplate)
    // CLodop 模板：计算行高用于 Y 坐标递增
    let baseY = 0
    let rowHeight = 25
    if (isCLodopBlock) {
      // 提取第一个 ADD_PRINT_TEXT/HTM 的 Y 坐标作为基准
      const yMatch = rowTemplate.match(/ADD_PRINT_\w+\s*\(\s*(\d+)/)
      if (yMatch) baseY = parseInt(yMatch[1])
      // 尝试从 SET_PRINT_STYLEA FontSize 推算行高，默认 25
      const fsMatch = rowTemplate.match(/FontSize['"]\s*,\s*(\d+)/)
      if (fsMatch) rowHeight = Math.max(parseInt(fsMatch[1]) * 2.5, 20)
    }
    return dataSource.map((item, index) => {
      let row = rowTemplate
      // CLodop 代码模板：偏移每行 Y 坐标
      if (isCLodopBlock && index > 0) {
        const offset = index * rowHeight
        // 替换所有 ADD_PRINT_XXX(Y, ...) 中的 Y 值
        row = row.replace(/(ADD_PRINT_\w+\s*\(\s*)(\d+)/g, (m, prefix, y) => {
          return prefix + String(parseInt(y) + offset)
        })
      }
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
