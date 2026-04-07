/**
 * CLodop / Lodop7 本地打印服务工具模块
 * 加载逻辑基于官方综合示例：WebSocket 优先，HTTP(S) 兜底
 */

// ═══ 官方加载逻辑 ═══════════════════════════════════════════

const MainJS = 'CLodopfuncs.js'
const URL_WS1   = 'ws://localhost:8000/'   + MainJS
const URL_WS2   = 'ws://localhost:18000/'  + MainJS
const URL_HTTP1 = 'http://localhost:8000/' + MainJS
const URL_HTTP2 = 'http://localhost:18000/' + MainJS
const URL_HTTP3 = 'https://localhost.lodop.net:8443/' + MainJS

let LoadJsState = ''   // '' | 'loadingA' | 'loadingB' | 'complete'
let loadPromise = null

/** 判断是否需要 CLodop（不支持插件的浏览器） */
function needCLodop() {
  try {
    const ua = navigator.userAgent
    if (/Windows\sPhone|iPhone|iPod|iPad|Android|Edge\D?\d+/i.test(ua)) return true
    const x64 = /x64/i.test(ua)
    if (!/Trident/i.test(ua) && !/MSIE/i.test(ua) && x64) return true
    const verFF = ua.match(/Firefox\D?(\d+)/i)
    if (verFF && (parseInt(verFF[1]) >= 41 || x64)) return true
    const verOPR = ua.match(/OPR\D?(\d+)/i)
    if (verOPR && parseInt(verOPR[1]) >= 32) return true
    if (!/Trident/i.test(ua) && !/MSIE/i.test(ua)) {
      const verCh = ua.match(/Chrome\D?(\d+)/i)
      if (verCh && parseInt(verCh[1]) >= 41) return true
    }
    return false
  } catch (e) { return true }
}

/** HTTP 兜底加载（低版本 CLodop 或 WS 失败时使用） */
function checkOrTryHttp(resolve) {
  if (window.getCLodop) {
    LoadJsState = 'complete'
    resolve(true)
    return
  }
  if (LoadJsState === 'loadingB' || LoadJsState === 'complete') return
  LoadJsState = 'loadingB'
  const head = document.head || document.documentElement
  const JS1 = document.createElement('script')
  const JS2 = document.createElement('script')
  const JS3 = document.createElement('script')
  JS1.src = URL_HTTP1
  JS2.src = URL_HTTP2
  JS3.src = URL_HTTP3

  const onOk = () => {
    LoadJsState = 'complete'
    resolve(!!window.getCLodop)
  }
  JS1.onload = JS2.onload = JS3.onload = onOk
  JS2.onerror = JS3.onerror = onOk  // 兜底：两个都失败也要 resolve

  JS1.onerror = () => {
    if (window.location.protocol !== 'https:')
      head.insertBefore(JS2, head.firstChild)
    else
      head.insertBefore(JS3, head.firstChild)
  }
  head.insertBefore(JS1, head.firstChild)

  // 最长等 5 秒
  setTimeout(() => {
    if (LoadJsState !== 'complete') { LoadJsState = 'complete'; resolve(false) }
  }, 5000)
}

/**
 * 加载 CLodop（WebSocket 优先，HTTP 降级）
 * @returns {Promise<boolean>}
 */
export function loadCLodop() {
  if (loadPromise) return loadPromise
  loadPromise = new Promise((resolve) => {
    if (!needCLodop()) {
      // 非 CLodop 环境（旧 IE 插件模式）：直接尝试获取
      resolve(!!window.getCLodop)
      return
    }
    if (!window.WebSocket && window.MozWebSocket) window.WebSocket = window.MozWebSocket
    LoadJsState = 'loadingA'
    try {
      const WSK1 = new WebSocket(URL_WS1)
      WSK1.onopen    = () => { setTimeout(() => checkOrTryHttp(resolve), 200) }
      WSK1.onmessage = (e) => { if (!window.getCLodop) { try { eval(e.data) } catch(ex){} } } // eslint-disable-line no-eval
      WSK1.onerror   = () => {
        try {
          const WSK2 = new WebSocket(URL_WS2)
          WSK2.onopen    = () => { setTimeout(() => checkOrTryHttp(resolve), 200) }
          WSK2.onmessage = (e) => { if (!window.getCLodop) { try { eval(e.data) } catch(ex){} } } // eslint-disable-line no-eval
          WSK2.onerror   = () => checkOrTryHttp(resolve)
        } catch (e2) { checkOrTryHttp(resolve) }
      }
    } catch (e) {
      checkOrTryHttp(resolve)
    }
  })
  return loadPromise
}

/**
 * 重置，允许重新连接（用于"点击重试"）
 */
export function resetCLodop() {
  loadPromise = null
  LoadJsState = ''
}

// ═══ 实例获取 ════════════════════════════════════════════════

/**
 * 获取 LODOP 实例
 */
export function getCLodopInstance() {
  try {
    if (typeof window.getCLodop === 'function') {
      const inst = window.getCLodop()
      if (inst) return inst
    }
    if (typeof CLODOP !== 'undefined') return CLODOP // eslint-disable-line no-undef
  } catch (e) { /* ignore */ }
  return null
}

/** 检测 CLodop 是否可用 */
export function isAvailable() {
  return getCLodopInstance() != null
}

/** 获取打印机列表 */
export function getPrinterList() {
  const LODOP = getCLodopInstance()
  if (!LODOP) return []
  try {
    const count = LODOP.GET_PRINTER_COUNT()
    const list = []
    for (let i = 0; i < count; i++) list.push(LODOP.GET_PRINTER_NAME(i))
    return list
  } catch (e) { return [] }
}

// ═══ 内部工具 ════════════════════════════════════════════════

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

const RE_PARAM = `(?:["']([^"']*)["']|(\\d+))`

function parseInitCall(code) {
  let title = '', inita = null
  const reA = new RegExp(
    `LODOP\\s*\\.\\s*PRINT_INITA\\s*\\(` +
    `\\s*${RE_PARAM}\\s*,` +
    `\\s*${RE_PARAM}\\s*,` +
    `\\s*${RE_PARAM}\\s*,` +
    `\\s*${RE_PARAM}\\s*,` +
    `\\s*["']([^"']*)["']\\s*\\)`
  )
  const mA = code.match(reA)
  if (mA) {
    inita = [
      mA[1] != null ? mA[1] : mA[2],
      mA[3] != null ? mA[3] : mA[4],
      mA[5] != null ? mA[5] : mA[6],
      mA[7] != null ? mA[7] : mA[8]
    ]
    title = mA[9]
    return { title, inita }
  }
  const mI = code.match(/LODOP\s*\.\s*PRINT_INIT\s*\(\s*["']([^"']*)["']\s*\)/)
  if (mI) title = mI[1]
  return { title, inita }
}

function cleanTemplateCode(code) {
  let c = code.replace(/"([^"]*)"/g, (m) => m.replace(/\n\s*/g, ''))
  c = c
    .replace(/LODOP\s*\.\s*PRINT_INITA?\s*\([^)]*\)\s*;?/g, '')
    .replace(/LODOP\s*\.\s*PREVIEW\s*\([^)]*\)\s*;?/g, '')
    .replace(/LODOP\s*\.\s*PRINT\s*\(\s*\)\s*;?/g, '')
  return c
}

function callInit(LODOP, title, inita) {
  if (inita) LODOP.PRINT_INITA(inita[0], inita[1], inita[2], inita[3], title)
  else        LODOP.PRINT_INIT(title)
}

// ═══ 公共 API ════════════════════════════════════════════════

/** 检测是否为 CLodop 代码格式 */
export function isCLodopCode(template) {
  if (!template) return false
  for (const line of template.split('\n')) {
    const t = line.trim()
    if (!t || t.startsWith('//')) continue
    return t.startsWith('LODOP.')
  }
  return false
}

/** 打开 CLodop 可视化设计器 */
export function designTemplate(initCode) {
  return new Promise((resolve, reject) => {
    const LODOP = getCLodopInstance()
    if (!LODOP) { reject(new Error('CLodop 未连接')); return }
    try {
      const { title: dt, inita: di } = initCode && initCode.trim() ? parseInitCall(initCode) : {}
      callInit(LODOP, dt || '模板设计', di || null)
      if (initCode && initCode.trim()) {
        try { new Function('LODOP', cleanTemplateCode(initCode))(LODOP) } catch (e) { console.warn(e) }
      }
      LODOP.On_Return = (taskID, value) => resolve(value || '')
      LODOP.PRINT_DESIGN()
    } catch (e) { reject(e) }
  })
}

/**
 * 执行 CLodop 代码字符串（预览 / 打印）
 */
export function execPrintCode(code, options = {}) {
  const LODOP = getCLodopInstance()
  if (!LODOP) return false
  const { preview = true, printer, title = '打印' } = options
  try {
    const { title: pt, inita } = parseInitCall(code)
    const cleanCode = cleanTemplateCode(code)
    callInit(LODOP, pt || title, inita)
    if (printer) LODOP.SET_PRINTER_INDEX(printer)
    new Function('LODOP', cleanCode)(LODOP)
    preview ? LODOP.PREVIEW() : LODOP.PRINT()
    return true
  } catch (e) {
    console.error('[CLodop] execPrintCode 异常', e)
    return false
  }
}

/**
 * 使用 CLodop 打印 HTML
 */
export function printHtml(renderedHtml, options = {}) {
  const LODOP = getCLodopInstance()
  if (!LODOP) return false
  const { title = '打印', printer, copies = 1, pageSize = 'A4', preview = false } = options
  try {
    LODOP.PRINT_INIT(title)
    LODOP.SET_PRINT_PAGESIZE(1, 0, 0, pageSize)
    if (printer) LODOP.SET_PRINTER_INDEX(printer)
    if (copies > 1) LODOP.SET_PRINT_COPIES(copies)
    LODOP.ADD_PRINT_HTM(0, 0, '100%', '100%', wrapHtml(renderedHtml))
    preview ? LODOP.PREVIEW() : LODOP.PRINT()
    return true
  } catch (e) {
    console.error('[CLodop] printHtml 异常', e)
    return false
  }
}
