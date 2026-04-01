/**
 * Hiprint 可视化设计器默认模板（JSON 格式）
 * 每种单据类型提供一个预配置的 hiprint JSON 模板
 * 用户首次打开设计器时加载，可在设计器中自由修改
 *
 * JSON 结构说明：
 * - panels[]: 面板数组（通常一个）
 *   - width/height: 纸张尺寸（mm）
 *   - paperHeader/paperFooter: 页眉/页脚位置
 *   - printElements[]: 打印元素数组
 *     - type: text|table|hline|vline|rect|oval|image
 *     - options: 元素属性（top/left/width/height/field/title 等）
 */

/**
 * 构建通用出入库单据的 hiprint 模板
 * @param {string} title - 单据标题
 * @param {Array} headerRows - 主表字段行 [{field, title, left, top, width}]
 * @param {Array} tableColumns - 明细表列 [{title, field, width, align}]
 * @param {Array} footerRows - 页脚字段行
 * @param {object} opts - 额外选项
 */
function buildPanel(title, headerRows, tableColumns, footerRows, opts = {}) {
  const elements = []
  let uid = 1

  // 标题
  elements.push({
    uid: 'e' + uid++,
    type: 'text',
    options: {
      left: 5, top: 5, width: 200, height: 20,
      title: title,
      fontSize: 18,
      fontWeight: 'bold',
      textAlign: 'center',
      lineHeight: 20
    }
  })

  // 分隔线
  elements.push({
    uid: 'e' + uid++,
    type: 'hline',
    options: { left: 5, top: 28, width: 200, height: 1, borderWidth: 1 }
  })

  // 主表字段
  let headerTop = 32
  for (let i = 0; i < headerRows.length; i++) {
    const row = headerRows[i]
    for (let j = 0; j < row.length; j++) {
      const f = row[j]
      // 标签
      elements.push({
        uid: 'e' + uid++,
        type: 'text',
        options: {
          left: f.left || (j * 68),
          top: headerTop,
          width: f.labelWidth || 28,
          height: 10,
          title: f.label + '：',
          fontSize: 9,
          textAlign: 'right',
          color: '#666'
        }
      })
      // 值
      elements.push({
        uid: 'e' + uid++,
        type: 'text',
        options: {
          left: (f.left || (j * 68)) + (f.labelWidth || 28),
          top: headerTop,
          width: f.valueWidth || 38,
          height: 10,
          title: f.label,
          field: f.field,
          fontSize: 9,
          textAlign: 'left'
        }
      })
    }
    headerTop += 12
  }

  // 明细表格
  const tableTop = headerTop + 4
  elements.push({
    uid: 'e' + uid++,
    type: 'table',
    options: {
      left: 5,
      top: tableTop,
      width: 200,
      height: opts.tableHeight || 120,
      field: 'details',
      tableHeaderFontSize: 8,
      tableBodyFontSize: 8,
      tableHeaderBackground: '#f0f0f0',
      tableHeaderBorderColor: '#000',
      tableBorderColor: '#000',
      columns: [
        tableColumns.map(c => ({
          title: c.title,
          field: c.field,
          width: c.width || 20,
          align: c.align || 'center',
          halign: 'center'
        }))
      ]
    }
  })

  // 页脚字段
  const footerTop = tableTop + (opts.tableHeight || 120) + 6
  if (footerRows && footerRows.length > 0) {
    for (let i = 0; i < footerRows.length; i++) {
      const row = footerRows[i]
      for (let j = 0; j < row.length; j++) {
        const f = row[j]
        elements.push({
          uid: 'e' + uid++,
          type: 'text',
          options: {
            left: f.left || (j * 68),
            top: footerTop + i * 12,
            width: f.width || 66,
            height: 10,
            title: f.label + '：' + (f.field ? '' : ''),
            field: f.field,
            fontSize: 9,
            textAlign: 'left'
          }
        })
      }
    }
  }

  // 打印日期（右下角）
  elements.push({
    uid: 'e' + uid++,
    type: 'text',
    options: {
      left: 140, top: footerTop + (footerRows ? footerRows.length * 12 : 0) + 4,
      width: 65, height: 10,
      title: '打印日期',
      field: '_printDate',
      fontSize: 8,
      textAlign: 'right',
      color: '#999'
    }
  })

  return {
    panels: [{
      width: opts.paperWidth || 210,
      height: opts.paperHeight || 297,
      paperHeader: 0,
      paperFooter: (opts.paperHeight || 297) - 20,
      printElements: elements
    }]
  }
}

// --- 标准明细列 ---
const standardColumns = [
  { title: '序号', field: '_index', width: 10 },
  { title: '仓库', field: 'depotName', width: 22 },
  { title: '条码', field: 'barCode', width: 26 },
  { title: '名称', field: 'name', width: 26 },
  { title: '规格', field: 'standard', width: 18 },
  { title: '型号', field: 'model', width: 16 },
  { title: '单位', field: 'unit', width: 10 },
  { title: '数量', field: 'operNumber', width: 14, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 16, align: 'right' },
  { title: '金额', field: 'allPrice', width: 18, align: 'right' },
  { title: '备注', field: 'remark', width: 24 }
]

const taxColumns = [
  { title: '序号', field: '_index', width: 8 },
  { title: '仓库', field: 'depotName', width: 18 },
  { title: '条码', field: 'barCode', width: 22 },
  { title: '名称', field: 'name', width: 22 },
  { title: '规格', field: 'standard', width: 14 },
  { title: '单位', field: 'unit', width: 10 },
  { title: '数量', field: 'operNumber', width: 12, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 14, align: 'right' },
  { title: '金额', field: 'allPrice', width: 16, align: 'right' },
  { title: '税率(%)', field: 'taxRate', width: 14, align: 'right' },
  { title: '税额', field: 'taxMoney', width: 14, align: 'right' },
  { title: '价税合计', field: 'taxLastMoney', width: 18, align: 'right' },
  { title: '备注', field: 'remark', width: 18 }
]

const retailColumns = [
  { title: '序号', field: '_index', width: 10 },
  { title: '仓库', field: 'depotName', width: 24 },
  { title: '条码', field: 'barCode', width: 28 },
  { title: '名称', field: 'name', width: 28 },
  { title: '规格', field: 'standard', width: 20 },
  { title: '单位', field: 'unit', width: 12 },
  { title: '数量', field: 'operNumber', width: 16, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 18, align: 'right' },
  { title: '金额', field: 'allPrice', width: 22, align: 'right' }
]

const freightColumns = [
  { title: '序号', field: '_index', width: 8 },
  { title: '出库单号', field: 'billNo', width: 22 },
  { title: '出库日期', field: 'billTimeStr', width: 16 },
  { title: '客户', field: 'customerName', width: 18 },
  { title: '名称', field: 'materialName', width: 18 },
  { title: '规格', field: 'standard', width: 14 },
  { title: '型号', field: 'model', width: 12 },
  { title: '批号', field: 'batchNumber', width: 14 },
  { title: '数量', field: 'operNumber', width: 10, align: 'right' },
  { title: '单位', field: 'materialUnit', width: 10 },
  { title: '重量(吨)', field: 'itemWeight', width: 14, align: 'right' },
  { title: '仓库', field: 'depotName', width: 14 },
  { title: '业务员', field: 'salesMan', width: 14 }
]

// --- 主表字段行 ---
const standardHeaderRows = [
  [
    { label: '往来单位', field: 'organName', left: 0, labelWidth: 28, valueWidth: 40 },
    { label: '单据日期', field: 'operTimeStr', left: 70, labelWidth: 28, valueWidth: 32 },
    { label: '单据编号', field: 'number', left: 132, labelWidth: 28, valueWidth: 42 }
  ],
  [
    { label: '关联订单', field: 'linkNumber', left: 0, labelWidth: 28, valueWidth: 40 },
    { label: '销售人员', field: 'salesMan', left: 70, labelWidth: 28, valueWidth: 32 },
    { label: '付款类型', field: 'payType', left: 132, labelWidth: 28, valueWidth: 42 }
  ]
]

const retailHeaderRows = [
  [
    { label: '会员卡号', field: 'organName', left: 0, labelWidth: 28, valueWidth: 40 },
    { label: '单据日期', field: 'operTimeStr', left: 70, labelWidth: 28, valueWidth: 32 },
    { label: '单据编号', field: 'number', left: 132, labelWidth: 28, valueWidth: 42 }
  ]
]

const freightHeaderRows = [
  [
    { label: '单据编号', field: 'billNo', left: 0, labelWidth: 28, valueWidth: 40 },
    { label: '日期', field: 'billTimeStr', left: 70, labelWidth: 18, valueWidth: 42 },
    { label: '结算方', field: 'carrierName', left: 132, labelWidth: 22, valueWidth: 48 }
  ],
  [
    { label: '单价(元/吨)', field: 'unitPrice', left: 0, labelWidth: 36, valueWidth: 32 },
    { label: '总重量(吨)', field: 'totalWeight', left: 70, labelWidth: 36, valueWidth: 24 },
    { label: '总运费(元)', field: 'totalFreight', left: 132, labelWidth: 36, valueWidth: 34 }
  ]
]

// --- 页脚字段行 ---
const standardFooterRows = [
  [
    { label: '优惠率', field: 'discount', width: 40 },
    { label: '收款优惠', field: 'discountMoney', left: 42, width: 46 },
    { label: '优惠后金额', field: 'discountLastMoney', left: 90, width: 50 }
  ],
  [
    { label: '结算账户', field: 'accountName', width: 40 },
    { label: '本次付/收款', field: 'changeAmount', left: 42, width: 46 },
    { label: '本次欠款', field: 'debt', left: 90, width: 50 }
  ],
  [
    { label: '备注', field: 'remark', width: 200 }
  ]
]

const retailFooterRows = [
  [
    { label: '优惠率', field: 'discount', width: 40 },
    { label: '付款', field: 'changeAmount', left: 42, width: 46 },
    { label: '找零', field: 'backAmount', left: 90, width: 50 }
  ],
  [
    { label: '收款账户', field: 'accountName', width: 40 },
    { label: '备注', field: 'remark', left: 42, width: 158 }
  ]
]

const simpleFooterRows = [
  [
    { label: '备注', field: 'remark', width: 200 }
  ]
]

const freightFooterRows = [
  [
    { label: '备注', field: 'remark', width: 200 }
  ]
]

// --- 导出默认模板 ---
export const hiprintDefaultTemplates = {
  // 零售
  retailOut: buildPanel('零售出库单', retailHeaderRows, retailColumns, retailFooterRows),
  retailBack: buildPanel('零售退货入库单', retailHeaderRows, retailColumns, retailFooterRows),

  // 采购（含税）
  purchaseApply: buildPanel('请购单', standardHeaderRows, standardColumns, simpleFooterRows),
  purchaseOrder: buildPanel('采购订单', standardHeaderRows, taxColumns, standardFooterRows),
  purchaseIn: buildPanel('采购入库单', standardHeaderRows, taxColumns, standardFooterRows),
  purchaseBack: buildPanel('采购退货出库单', standardHeaderRows, taxColumns, standardFooterRows),

  // 销售（含税）
  saleOrder: buildPanel('销售订单', standardHeaderRows, taxColumns, standardFooterRows),
  saleOut: buildPanel('销售出库单', standardHeaderRows, taxColumns, standardFooterRows),
  saleBack: buildPanel('销售退货入库单', standardHeaderRows, taxColumns, standardFooterRows),

  // 其他（无税率）
  otherIn: buildPanel('其它入库单', standardHeaderRows, standardColumns, simpleFooterRows),
  otherOut: buildPanel('其它出库单', standardHeaderRows, standardColumns, simpleFooterRows),
  allocationOut: buildPanel('调拨出库单', standardHeaderRows, standardColumns, simpleFooterRows),
  assemble: buildPanel('组装单', standardHeaderRows, standardColumns, simpleFooterRows),
  disassemble: buildPanel('拆卸单', standardHeaderRows, standardColumns, simpleFooterRows),
  stockCheckReplay: buildPanel('盘点复盘单', standardHeaderRows, standardColumns, simpleFooterRows),

  // 物流单
  freightBill: buildPanel('物流单', freightHeaderRows, freightColumns, freightFooterRows)
}

/**
 * 获取指定单据类型的默认 hiprint JSON 模板
 * @param {string} billType - 单据类型编码
 * @returns {object} hiprint JSON 模板对象
 */
export function getDefaultHiprintTemplate(billType) {
  return hiprintDefaultTemplates[billType] || hiprintDefaultTemplates['saleOut']
}
