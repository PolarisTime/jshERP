/**
 * 各单据类型的默认打印模板
 * 未保存自定义模板时使用
 */

// 通用出入库单据模板（采购/销售/其他/调拨等）
function buildStandardTemplate(title, extraHeaderHtml, extraFooterHtml) {
  return `<div style="padding:10px;">
<h2>${title}</h2>
<table style="border:none;width:100%;margin-bottom:8px;">
<tr class="header-row">
  <td style="border:none;">往来单位：{{organName}}</td>
  <td style="border:none;">单据日期：{{operTimeStr}}</td>
  <td style="border:none;">单据编号：{{number}}</td>
  <td style="border:none;">关联订单：{{linkNumber}}</td>
</tr>
${extraHeaderHtml || ''}
</table>
<table>
<thead><tr>
  <th>序号</th><th>仓库</th><th>条码</th><th>名称</th><th>规格</th><th>型号</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th><th>备注</th>
</tr></thead>
<tbody>
<!--DETAIL_ROW_START-->
<tr>
  <td>{{_index}}</td><td>{{detail.depotName}}</td><td>{{detail.barCode}}</td><td>{{detail.name}}</td><td>{{detail.standard}}</td><td>{{detail.model}}</td><td>{{detail.unit}}</td><td>{{detail.operNumber}}</td><td>{{detail.unitPrice}}</td><td>{{detail.allPrice}}</td><td>{{detail.remark}}</td>
</tr>
<!--DETAIL_ROW_END-->
</tbody>
</table>
${extraFooterHtml || `<div class="footer-info">
<p>优惠率：{{discount}}%　收款优惠：{{discountMoney}}　优惠后金额：{{discountLastMoney}}</p>
<p>结算账户：{{accountName}}　本次付/收款：{{changeAmount}}　本次欠款：{{debt}}</p>
<p>备注：{{remark}}</p>
</div>`}
<p style="text-align:right;font-size:11px;margin-top:10px;">打印日期：{{_printDate}}</p>
</div>`
}

// 零售模板（无供应商/欠款，有付款/找零）
function buildRetailTemplate(title) {
  return `<div style="padding:10px;">
<h2>${title}</h2>
<table style="border:none;width:100%;margin-bottom:8px;">
<tr class="header-row">
  <td style="border:none;">会员卡号：{{organName}}</td>
  <td style="border:none;">单据日期：{{operTimeStr}}</td>
  <td style="border:none;">单据编号：{{number}}</td>
</tr>
</table>
<table>
<thead><tr>
  <th>序号</th><th>仓库</th><th>条码</th><th>名称</th><th>规格</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th>
</tr></thead>
<tbody>
<!--DETAIL_ROW_START-->
<tr>
  <td>{{_index}}</td><td>{{detail.depotName}}</td><td>{{detail.barCode}}</td><td>{{detail.name}}</td><td>{{detail.standard}}</td><td>{{detail.unit}}</td><td>{{detail.operNumber}}</td><td>{{detail.unitPrice}}</td><td>{{detail.allPrice}}</td>
</tr>
<!--DETAIL_ROW_END-->
</tbody>
</table>
<div class="footer-info">
<p>优惠率：{{discount}}%　付款：{{changeAmount}}　找零：{{backAmount}}</p>
<p>收款账户：{{accountName}}　备注：{{remark}}</p>
</div>
<p style="text-align:right;font-size:11px;margin-top:10px;">打印日期：{{_printDate}}</p>
</div>`
}

// 物流单模板
const freightBillTemplate = `<div style="padding:10px;">
<h2>物流单</h2>
<table style="border:none;width:100%;margin-bottom:8px;">
<tr class="header-row">
  <td style="border:none;">单据编号：{{billNo}}</td>
  <td style="border:none;">日期：{{billTimeStr}}</td>
  <td style="border:none;">结算方：{{carrierName}}</td>
</tr>
<tr class="header-row">
  <td style="border:none;">单价(元/吨)：{{unitPrice}}</td>
  <td style="border:none;">总重量(吨)：{{totalWeight}}</td>
  <td style="border:none;">总运费(元)：{{totalFreight}}</td>
</tr>
</table>
<table>
<thead><tr>
  <th>序号</th><th>出库单号</th><th>出库日期</th><th>客户名称</th><th>名称</th><th>规格</th><th>型号</th><th>批号</th><th>数量</th><th>单位</th><th>重量(吨)</th><th>仓库</th><th>业务员</th>
</tr></thead>
<tbody>
<!--DETAIL_ROW_START-->
<tr>
  <td>{{_index}}</td><td>{{detail.billNo}}</td><td>{{detail.billTimeStr}}</td><td>{{detail.customerName}}</td><td>{{detail.materialName}}</td><td>{{detail.standard}}</td><td>{{detail.model}}</td><td>{{detail.batchNumber}}</td><td>{{detail.operNumber}}</td><td>{{detail.materialUnit}}</td><td>{{detail.itemWeight}}</td><td>{{detail.depotName}}</td><td>{{detail.salesMan}}</td>
</tr>
<!--DETAIL_ROW_END-->
</tbody>
</table>
<div class="footer-info"><p>备注：{{remark}}</p></div>
<p style="text-align:right;font-size:11px;margin-top:10px;">打印日期：{{_printDate}}</p>
</div>`

// 含税列的标准模板
function buildTaxTemplate(title) {
  return `<div style="padding:10px;">
<h2>${title}</h2>
<table style="border:none;width:100%;margin-bottom:8px;">
<tr class="header-row">
  <td style="border:none;">往来单位：{{organName}}</td>
  <td style="border:none;">单据日期：{{operTimeStr}}</td>
  <td style="border:none;">单据编号：{{number}}</td>
  <td style="border:none;">关联订单：{{linkNumber}}</td>
</tr>
</table>
<table>
<thead><tr>
  <th>序号</th><th>仓库</th><th>条码</th><th>名称</th><th>规格</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th><th>税率(%)</th><th>税额</th><th>价税合计</th><th>备注</th>
</tr></thead>
<tbody>
<!--DETAIL_ROW_START-->
<tr>
  <td>{{_index}}</td><td>{{detail.depotName}}</td><td>{{detail.barCode}}</td><td>{{detail.name}}</td><td>{{detail.standard}}</td><td>{{detail.unit}}</td><td>{{detail.operNumber}}</td><td>{{detail.unitPrice}}</td><td>{{detail.allPrice}}</td><td>{{detail.taxRate}}</td><td>{{detail.taxMoney}}</td><td>{{detail.taxLastMoney}}</td><td>{{detail.remark}}</td>
</tr>
<!--DETAIL_ROW_END-->
</tbody>
</table>
<div class="footer-info">
<p>优惠率：{{discount}}%　收款优惠：{{discountMoney}}　优惠后金额：{{discountLastMoney}}</p>
<p>结算账户：{{accountName}}　本次付/收款：{{changeAmount}}　本次欠款：{{debt}}</p>
<p>备注：{{remark}}</p>
</div>
<p style="text-align:right;font-size:11px;margin-top:10px;">打印日期：{{_printDate}}</p>
</div>`
}

// 简单模板（其他入库/出库、调拨、组装、拆卸等无税率）
function buildSimpleTemplate(title) {
  return buildStandardTemplate(title, '', `<div class="footer-info"><p>备注：{{remark}}</p></div>`)
}

export const defaultTemplates = {
  retailOut: buildRetailTemplate('零售出库单'),
  retailBack: buildRetailTemplate('零售退货入库单'),
  purchaseApply: buildSimpleTemplate('请购单'),
  purchaseOrder: buildTaxTemplate('采购订单'),
  purchaseIn: buildTaxTemplate('采购入库单'),
  purchaseBack: buildTaxTemplate('采购退货出库单'),
  saleOrder: buildTaxTemplate('销售订单'),
  saleOut: buildTaxTemplate('销售出库单'),
  saleBack: buildTaxTemplate('销售退货入库单'),
  otherIn: buildSimpleTemplate('其它入库单'),
  otherOut: buildSimpleTemplate('其它出库单'),
  allocationOut: buildSimpleTemplate('调拨出库单'),
  assemble: buildSimpleTemplate('组装单'),
  disassemble: buildSimpleTemplate('拆卸单'),
  stockCheckReplay: buildSimpleTemplate('盘点复盘单'),
  freightBill: freightBillTemplate
}

export function getDefaultTemplate(billType) {
  return defaultTemplates[billType] || defaultTemplates['saleOut']
}
