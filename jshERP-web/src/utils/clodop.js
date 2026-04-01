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

    // 纸张设置（1=纵向, 2=横向）
    if (pageSize === 'A4') {
      LODOP.SET_PRINT_PAGESIZE(1, 0, 0, 'A4')
    } else if (pageSize === 'A5') {
      LODOP.SET_PRINT_PAGESIZE(1, 0, 0, 'A5')
    } else {
      LODOP.SET_PRINT_PAGESIZE(1, 0, 0, pageSize)
    }

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
