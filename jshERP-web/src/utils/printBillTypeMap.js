/**
 * 中文单据类型 → 英文编码映射
 */
export const billTypeMap = {
  '零售出库': 'retailOut',
  '零售退货入库': 'retailBack',
  '请购单': 'purchaseApply',
  '采购订单': 'purchaseOrder',
  '采购入库': 'purchaseIn',
  '采购退货出库': 'purchaseBack',
  '销售订单': 'saleOrder',
  '销售出库': 'saleOut',
  '销售退货入库': 'saleBack',
  '其它入库': 'otherIn',
  '其它出库': 'otherOut',
  '调拨出库': 'allocationOut',
  '组装单': 'assemble',
  '拆卸单': 'disassemble',
  '盘点复盘': 'stockCheckReplay'
}

/**
 * 单据类型中文名
 */
export const billTypeNameMap = {
  'retailOut': '零售出库',
  'retailBack': '零售退货入库',
  'purchaseApply': '请购单',
  'purchaseOrder': '采购订单',
  'purchaseIn': '采购入库',
  'purchaseBack': '采购退货出库',
  'saleOrder': '销售订单',
  'saleOut': '销售出库',
  'saleBack': '销售退货入库',
  'otherIn': '其它入库',
  'otherOut': '其它出库',
  'allocationOut': '调拨出库',
  'assemble': '组装单',
  'disassemble': '拆卸单',
  'stockCheckReplay': '盘点复盘',
  'freightBill': '物流单'
}

export function getBillTypeCode(chineseName) {
  return billTypeMap[chineseName] || chineseName
}
