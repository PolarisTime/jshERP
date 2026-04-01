/**
 * Hiprint 可视化打印设计器配置
 * 定义各单据类型的可拖拽元素 provider
 */

/**
 * 通用单据字段 Provider
 * 根据 headerFields 和 detailFields 动态生成可拖拽元素
 */
export class BillElementProvider {
  constructor(billType, headerFields, detailFields) {
    this.billType = billType || 'bill'
    this.headerFields = headerFields || []
    this.detailFields = detailFields || []
  }

  addElementTypes(context) {
    // 主表字段 → 文本元素
    context.addPrintElementTypes(this.billType + '_header',
      this.headerFields.map(f => ({
        tid: `${this.billType}.${f.key}`,
        title: f.label,
        data: f.key,
        type: 'text',
        options: {
          testData: f.label,
          field: f.key,
          width: 120,
          height: 16,
          fontSize: 12,
          textAlign: 'left'
        }
      }))
    )

    // 明细表格
    if (this.detailFields.length > 0) {
      context.addPrintElementTypes(this.billType + '_detail', [
        {
          tid: `${this.billType}.detailTable`,
          title: '明细表格',
          type: 'table',
          options: {
            field: 'details',
            fields: this.detailFields.map(f => ({
              text: f.label,
              field: f.key,
              width: 80
            }))
          }
        }
      ])
    }
  }
}

/**
 * 判断是否为 hiprint JSON 模板
 * @param {string} template - 模板内容字符串
 * @returns {boolean}
 */
export function isHiprintJson(template) {
  if (!template) return false
  try {
    const obj = JSON.parse(template)
    return obj && obj.panels && Array.isArray(obj.panels)
  } catch {
    return false
  }
}
