/**
 * CLodop 本地打印服务工具模块
 * CLodop 是运行在用户本机的打印服务（监听 localhost:8000/18000）
 * 浏览器通过动态加载其 JS 脚本与本地服务通信
 */

let loadPromise = null

/**
 * 动态加载 CLodop JS 脚本
 * 依次尝试 8000(HTTP) 和 18000(HTTPS) 端口
 * @returns {Promise<boolean>} 是否加载成功
 */
export function loadCLodop() {
  if (loadPromise) return loadPromise
  loadPromise = new Promise((resolve) => {
    const ports = [8000, 18000]
    let loaded = false
    let tried = 0
    ports.forEach(port => {
      const script = document.createElement('script')
      script.src = `http://localhost:${port}/CLodopfuncs.js?priority=1`
      script.onload = () => {
        if (!loaded) {
          loaded = true
          resolve(true)
        }
      }
      script.onerror = () => {
        tried++
        if (tried >= ports.length && !loaded) {
          resolve(false)
        }
      }
      document.head.appendChild(script)
    })
    // 超时兜底 3 秒
    setTimeout(() => {
      if (!loaded) resolve(false)
    }, 3000)
  })
  return loadPromise
}

/**
 * 重置加载状态，允许重新加载（用于重试连接）
 */
export function resetCLodop() {
  loadPromise = null
}

/**
 * 获取 CLodop 实例
 * @returns {object|null} CLodop 控件实例，不可用时返回 null
 */
export function getCLodopInstance() {
  try {
    if (typeof CLODOP !== 'undefined') {
      return CLODOP
    }
    if (typeof window.getCLodop === 'function') {
      return window.getCLodop()
    }
  } catch (e) {
    // ignore
  }
  return null
}

/**
 * 检测 CLodop 是否可用
 */
export function isAvailable() {
  return getCLodopInstance() != null
}

/**
 * 获取本机打印机列表
 * @returns {string[]} 打印机名称数组
 */
export function getPrinterList() {
  const LODOP = getCLodopInstance()
  if (!LODOP) return []
  try {
    const count = LODOP.GET_PRINTER_COUNT()
    const list = []
    for (let i = 0; i < count; i++) {
      list.push(LODOP.GET_PRINTER_NAME(i))
    }
    return list
  } catch (e) {
    return []
  }
}

// ═══ 内部工具函数 ═══

/**
 * 包装渲染后的 HTML 为完整文档（含打印样式）
 */
function wrapHtml(body) {
  return `<!DOCTYPE html><html><head><meta charset="utf-8">
<style>
  body { font-family: SimSun, serif; font-size: 12px; color: #000; margin: 0; }
  table { border-collapse: collapse; width: 100%; }
  th, td { border: 1px solid #000; padding: 4px 8px; text-align: left; font-size: 12px; }
  th { background-color: #f0f0f0; font-weight: bold; }
  h2 { text-align: center; margin: 10px 0; }
  .header-row td { border: none; padding: 4px 8px; }
  .footer-info { margin-top: 10px; font-size: 11px; }
</style></head><body>${body}</body></html>`
}

/**
 * 匹配 CLodop 调用参数值（支持字符串和数值两种形式）
 * 例如: "20mm" 或 20 或 0
 */
const RE_PARAM = `(?:["']([^"']*)["']|(\\d+))`

/**
 * 从模板代码中解析 PRINT_INITA / PRINT_INIT 调用信息
 * @param {string} code - CLodop 模板代码
 * @returns {{ title: string, inita: string[]|null }}
 *   title — 打印任务标题
 *   inita — PRINT_INITA 的四个边距参数 [top, left, width, height]，无则 null
 */
function parseInitCall(code) {
  let title = ''
  let inita = null

  // 优先匹配 PRINT_INITA(top, left, width, height, title)
  // 参数可以是字符串 "20mm" 或数值 0
  const reA = new RegExp(
    `LODOP\\s*\\.\\s*PRINT_INITA\\s*\\(` +
    `\\s*${RE_PARAM}\\s*,` +  // top
    `\\s*${RE_PARAM}\\s*,` +  // left
    `\\s*${RE_PARAM}\\s*,` +  // width
    `\\s*${RE_PARAM}\\s*,` +  // height
    `\\s*["']([^"']*)["']` +  // title (始终为字符串)
    `\\s*\\)`
  )
  const mA = code.match(reA)
  if (mA) {
    // 每个 RE_PARAM 产生 2 个捕获组（字符串值 or 数值），取非空的那个
    inita = [
      mA[1] != null ? mA[1] : mA[2],
      mA[3] != null ? mA[3] : mA[4],
      mA[5] != null ? mA[5] : mA[6],
      mA[7] != null ? mA[7] : mA[8]
    ]
    title = mA[9]
    return { title, inita }
  }

  // 退回匹配 PRINT_INIT(title)
  const reI = /LODOP\s*\.\s*PRINT_INIT\s*\(\s*["']([^"']*)["']\s*\)/
  const mI = code.match(reI)
  if (mI) {
    title = mI[1]
  }
  return { title, inita }
}

/**
 * 清洗模板代码：
 *  1. 修复字符串内换行
 *  2. 移除 PRINT_INITA / PRINT_INIT / PREVIEW / PRINT 调用
 * @param {string} code
 * @returns {string}
 */
function cleanTemplateCode(code) {
  let cleaned = code
  // 移除控制类调用，避免与外部调用重复
  cleaned = cleaned
    .replace(/LODOP\s*\.\s*PRINT_INITA?\s*\([^)]*\)\s*;?/g, '')
    .replace(/LODOP\s*\.\s*PREVIEW\s*\([^)]*\)\s*;?/g, '')
    .replace(/LODOP\s*\.\s*PRINT\s*\([^)]*\)\s*;?/g, '')
  return cleaned
}

/**
 * 调用 LODOP 初始化（根据是否有 INITA 参数选择对应 API）
 * @param {object} LODOP - CLodop 实例
 * @param {string} title - 打印任务标题
 * @param {string[]|null} inita - PRINT_INITA 边距参数，null 时使用 PRINT_INIT
 */
function callInit(LODOP, title, inita) {
  if (inita) {
    LODOP.PRINT_INITA(inita[0], inita[1], inita[2], inita[3], title)
  } else {
    LODOP.PRINT_INIT(title)
  }
}

// ═══ 公共 API ═══

/**
 * 检测模板内容是否为 CLodop API 代码格式
 * 跳过注释和空行，判断首条有效语句是否以 LODOP. 开头
 * @param {string} template - 模板内容
 * @returns {boolean}
 */
export function isCLodopCode(template) {
  if (!template) return false
  // 逐行扫描，跳过空行和 // 注释行
  const lines = template.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line || line.startsWith('//')) continue
    return line.startsWith('LODOP.')
  }
  return false
}

/**
 * 打开 CLodop 内置可视化设计器
 * @param {string} initCode - 初始 CLodop API 代码（可为空）
 * @returns {Promise<string>} 设计后的完整代码字符串
 */
export function designTemplate(initCode) {
  return new Promise((resolve, reject) => {
    const LODOP = getCLodopInstance()
    if (!LODOP) {
      reject(new Error('CLodop 未连接'))
      return
    }
    try {
      // 解析初始代码中的 INIT 调用，保留边距信息用于设计器起点
      let designTitle = '模板设计'
      let designInita = null
      if (initCode && initCode.trim()) {
        const parsed = parseInitCall(initCode)
        if (parsed.title) designTitle = parsed.title
        designInita = parsed.inita
      }
      callInit(LODOP, designTitle, designInita)

      // 执行初始代码构建打印内容作为设计起点
      if (initCode && initCode.trim()) {
        try {
          const cleaned = cleanTemplateCode(initCode)
          new Function('LODOP', cleaned)(LODOP)
        } catch (e) {
          console.warn('初始模板代码执行异常', e)
        }
      }
      // 设置异步回调接收设计结果
      LODOP.On_Return = function (taskID, value) {
        resolve(value || '')
      }
      LODOP.PRINT_DESIGN()
    } catch (e) {
      reject(e)
    }
  })
}

/**
 * 执行 CLodop 代码字符串进行打印/预览
 * 支持 PRINT_INIT 和 PRINT_INITA（含打印区域边距）两种初始化方式
 * @param {string} code - CLodop API 代码字符串（已替换占位符的渲染结果）
 * @param {object} options - 选项
 * @param {boolean} [options.preview=true] - true=预览, false=直接打印
 * @param {string} [options.printer] - 指定打印机
 * @param {string} [options.title='打印'] - 任务名称（模板中有则覆盖）
 * @returns {boolean}
 */
export function execPrintCode(code, options = {}) {
  const LODOP = getCLodopInstance()
  if (!LODOP) return false
  const { preview = true, printer, title = '打印' } = options
  try {
    // 解析模板中的初始化调用
    const parsed = parseInitCall(code)
    const initTitle = parsed.title || title
    // 清洗代码（移除 INIT/PREVIEW/PRINT）
    const cleanCode = cleanTemplateCode(code)
    // 初始化打印任务
    callInit(LODOP, initTitle, parsed.inita)
    if (printer) {
      LODOP.SET_PRINTER_INDEX(printer)
    }
    // 执行模板业务逻辑（ADD_PRINT_TEXT 等）
    new Function('LODOP', cleanCode)(LODOP)
    if (preview) {
      LODOP.PREVIEW()
    } else {
      LODOP.PRINT()
    }
    return true
  } catch (e) {
    console.error('CLodop 代码执行异常', e)
    return false
  }
}

/**
 * 使用 CLodop 打印 HTML 内容
 * @param {string} renderedHtml - 渲染后的 HTML 片段
 * @param {object} options - 打印选项
 * @param {string} [options.title='打印'] - 打印任务名称
 * @param {string} [options.printer] - 指定打印机名称，不传则使用默认打印机
 * @param {number} [options.copies=1] - 打印份数
 * @param {string} [options.pageSize='A4'] - 纸张大小
 * @param {boolean} [options.preview=false] - true=预览, false=直接打印
 * @returns {boolean} 是否成功调用
 */
export function printHtml(renderedHtml, options = {}) {
  const LODOP = getCLodopInstance()
  if (!LODOP) return false

  const {
    title = '打印',
    printer,
    copies = 1,
    pageSize = 'A4',
    preview = false
  } = options

  try {
    LODOP.PRINT_INIT(title)

    // 纸张设置（1=纵向）
    LODOP.SET_PRINT_PAGESIZE(1, 0, 0, pageSize)

    // 指定打印机
    if (printer) {
      LODOP.SET_PRINTER_INDEX(printer)
    }

    // 打印份数
    if (copies > 1) {
      LODOP.SET_PRINT_COPIES(copies)
    }

    // 添加 HTML 内容
    const fullHtml = wrapHtml(renderedHtml)
    LODOP.ADD_PRINT_HTM(0, 0, '100%', '100%', fullHtml)

    if (preview) {
      LODOP.PREVIEW()
    } else {
      LODOP.PRINT()
    }
    return true
  } catch (e) {
    console.error('CLodop 打印异常', e)
    return false
  }
}
