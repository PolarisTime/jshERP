/**
 * 各单据类型的默认打印 HTML 模板
 * 未保存自定义模板时使用
 *
 * 占位符：
 *   {{fieldName}}         — 主表字段
 *   {{detail.fieldName}}  — 明细行字段
 *   {{_index}}            — 行序号（从1开始）
 *   {{_printDate}}        — 打印日期
 *   {{_printTime}}        — 打印时间
 *
 * 明细行循环：
 *   <!--DETAIL_ROW_START--> ... <!--DETAIL_ROW_END-->
 */

// ─── 通用出入库单据 ───

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

// ─── 含税列 ───

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

// ─── 零售 ───

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

// ─── 简单（仅备注页脚） ───

function buildSimpleTemplate(title) {
  return buildStandardTemplate(title, '', '<div class="footer-info"><p>备注：{{remark}}</p></div>')
}

// ─── 建材供货单（CLodop 格式, 800×600 画布） ───

const saleOutJiancaiTemplate = `LODOP.PRINT_INIT("建材供货单");
LODOP.SET_PRINT_PAGESIZE(1,2120,1590,"");
LODOP.SET_PRINT_STYLE("FontName","微软雅黑");
LODOP.SET_PRINT_STYLE("FontSize",9);

// ═══ 标题 ═══
LODOP.ADD_PRINT_TEXT(8,10,780,28,"嘉兴熠祺建材有限公司（供货单）");
LODOP.SET_PRINT_STYLEA(0,"FontSize",16);
LODOP.SET_PRINT_STYLEA(0,"Bold",1);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);

// ═══ 主表信息（带边框） ═══
var hL=10,hW=780,hSplit=480,hRowH=22;
var hTop=40;
LODOP.ADD_PRINT_RECT(hTop,hL,hW,hRowH,0,1);
LODOP.ADD_PRINT_LINE(hTop,hL+hSplit,hTop+hRowH,hL+hSplit,0,1);
LODOP.ADD_PRINT_TEXT(hTop+4,hL+8,hSplit-16,16,"需方公司：{{organName}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
var billNo="{{freightBillNo}}";
LODOP.ADD_PRINT_TEXT(hTop+4,hL+hSplit+8,hW-hSplit-16,16,billNo?"单据号：No."+billNo:"单据号：");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);

hTop+=hRowH;
LODOP.ADD_PRINT_RECT(hTop,hL,hW,hRowH,0,1);
LODOP.ADD_PRINT_LINE(hTop,hL+hSplit,hTop+hRowH,hL+hSplit,0,1);
LODOP.ADD_PRINT_TEXT(hTop+4,hL+8,hSplit-16,16,"工程名称：{{projectName}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
LODOP.ADD_PRINT_TEXT(hTop+4,hL+hSplit+8,hW-hSplit-16,16,"日期：{{sendDate}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);

hTop+=hRowH;
LODOP.ADD_PRINT_RECT(hTop,hL,hW,hRowH,0,1);
LODOP.ADD_PRINT_LINE(hTop,hL+hSplit,hTop+hRowH,hL+hSplit,0,1);
LODOP.ADD_PRINT_TEXT(hTop+4,hL+8,hSplit-16,16,"地址：{{projectAddress}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
LODOP.ADD_PRINT_TEXT(hTop+4,hL+hSplit+8,hW-hSplit-16,16,"车号：{{carNo}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);

// ═══ 明细表格（7列：去掉单价、金额） ═══
var tTop=hTop+hRowH+4;
var thH=28;
var rowH=24;
var col=[75,80,75,65,50,60,60];
var remarkW=315;
var colName=["品牌","品名","材质","规格","件数","件重/吨","总重/吨"];

// 表头行 — 列0-6
var left=10;
for(var i=0;i<col.length;i++){
  LODOP.ADD_PRINT_RECT(tTop,left,col[i],thH,0,1);
  LODOP.ADD_PRINT_TEXT(tTop+7,left+2,col[i]-4,16,colName[i]);
  LODOP.SET_PRINT_STYLEA(0,"Bold",1);
  LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
  LODOP.SET_PRINT_STYLEA(0,"FontSize",9);
  left+=col[i];
}
// 表头行 — 备注列
LODOP.ADD_PRINT_RECT(tTop,left,remarkW,thH,0,1);
LODOP.ADD_PRINT_TEXT(tTop+7,left+2,remarkW-4,16,"备  注");
LODOP.SET_PRINT_STYLEA(0,"Bold",1);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
LODOP.SET_PRINT_STYLEA(0,"FontSize",9);

// ═══ 明细数据 ═══
var DetailList = [
{{#each details}}
  {brand:"{{name}}",pname:"{{categoryName}}",material:"{{model}}",spec:"{{standard}}",piece:"{{operNumber}}",weight:"{{weight}}"},
{{/each}}
];

var maxRows=12;
var dataTop=tTop+thH;

// 绘制固定12行网格（列0-6）
for(var r=0;r<maxRows;r++){
  var l=10;
  for(var i=0;i<col.length;i++){
    LODOP.ADD_PRINT_RECT(dataTop+r*rowH,l,col[i],rowH,0,1);
    l+=col[i];
  }
}

// 填入数据
var totalPiece=0,totalWeight=0;
for(var k=0;k<DetailList.length&&k<maxRows;k++){
  var d=DetailList[k];
  var pw="";
  var w=parseFloat(d.weight),n=parseFloat(d.piece);
  if(!isNaN(w)&&!isNaN(n)&&n>0) pw=(w/n).toFixed(3);
  if(!isNaN(n)) totalPiece+=n;
  if(!isNaN(w)) totalWeight+=w;
  var arr=[d.brand,d.pname,d.material,d.spec,d.piece,pw,d.weight];
  var l=10;
  for(var i=0;i<arr.length;i++){
    LODOP.ADD_PRINT_TEXT(dataTop+k*rowH+5,l+2,col[i]-4,16,arr[i]||"");
    LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
    LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
    l+=col[i];
  }
}

// ═══ "以下无内容"标记（紧接最后一条数据行之后） ═══
var noContentRow=DetailList.length<maxRows?DetailList.length:maxRows;
if(noContentRow<maxRows){
  var ncLeft=10;
  var ncW=0;
  for(var i=0;i<col.length;i++) ncW+=col[i];
  LODOP.ADD_PRINT_TEXT(dataTop+noContentRow*rowH+5,ncLeft+2,ncW-4,16,"以下无内容");
  LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
  LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
  LODOP.SET_PRINT_STYLEA(0,"FontColor","#666666");
}

// ═══ 合计行 ═══
var sumTop=dataTop+maxRows*rowH;
var sumArr=["合计","","","",totalPiece||"","",totalWeight?totalWeight.toFixed(3):""];
var l=10;
for(var i=0;i<col.length;i++){
  LODOP.ADD_PRINT_RECT(sumTop,l,col[i],rowH,0,1);
  LODOP.ADD_PRINT_TEXT(sumTop+5,l+2,col[i]-4,16,sumArr[i]||"");
  LODOP.SET_PRINT_STYLEA(0,"Bold",1);
  LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
  LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
  l+=col[i];
}

// ═══ 备注合并单元格（数据区+合计行，确保右边框完整） ═══
var remarkLeft=10;
for(var i=0;i<col.length;i++) remarkLeft+=col[i];
var remarkH=maxRows*rowH+rowH;
LODOP.ADD_PRINT_RECT(dataTop,remarkLeft,remarkW,remarkH,0,1);

// 条款文字（字号9，比数据行大）
var cY=dataTop+6;
var cX=remarkLeft+6;
var cW=remarkW-12;
LODOP.ADD_PRINT_TEXT(cY,cX,cW,38,"1.货物规格、型号、数量及价格在收货时当即点清，并签字生效。");
LODOP.SET_PRINT_STYLEA(0,"FontSize",9);
cY+=40;
LODOP.ADD_PRINT_TEXT(cY,cX,cW,110,"2.对货物必须先行检测合格后使用，如有质量问题需方需在五日内提出书面异议，逾期视为认可，供方负责调换或协助向厂方索赔，否则供方不予处理。需方不得以质量异议为由拒付或少付货款，否则视需方违约且需方向供方支付日息万分之五付违约金。");
LODOP.SET_PRINT_STYLEA(0,"FontSize",9);
cY+=112;
LODOP.ADD_PRINT_TEXT(cY,cX,cW,65,"3.需方收货后，应当即时或合同约定时间全部付款，否则需按日息万分之五支付违约金，同时承担供方实现债权支出的一切费用。");
LODOP.SET_PRINT_STYLEA(0,"FontSize",9);

// ═══ 底部签收 ═══
var footTop=sumTop+rowH+10;
LODOP.ADD_PRINT_TEXT(footTop,10,300,18,"需方签收人：________________");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);

LODOP.PREVIEW();`

// ─── 销售出库单A版（CLodop 格式, 800×600 画布, 无边框表头） ───

const saleOutATemplate = `LODOP.PRINT_INIT("销售出库单A版");
LODOP.SET_PRINT_PAGESIZE(1,2120,1590,"");
LODOP.SET_PRINT_STYLE("FontName","微软雅黑");
LODOP.SET_PRINT_STYLE("FontSize",9);

// ═══ 附加信息（左上角空白处，第二框文本） ═══
LODOP.ADD_PRINT_TEXT(2,10,300,14,"{{extraText}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",8);

// ═══ 主表数据（填入预印标签右侧空白处） ═══
// 需方公司值
LODOP.ADD_PRINT_TEXT(48,80,380,16,"{{organName}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
// 单据号值（物流单编号）
LODOP.ADD_PRINT_TEXT(48,540,240,16,"{{freightBillNo}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",12);

// 工程名称值（取项目名称）
LODOP.ADD_PRINT_TEXT(70,80,380,16,"{{projectName}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
// 日期值（拆分年/月/日数字，不打印"年月日"文字）
var sd="{{sendDate}}";
var sdY="",sdM="",sdD="";
var sdMatch=sd.match(/(\\d{4})\\D*(\\d{1,2})\\D*(\\d{1,2})/);
if(sdMatch){sdY=sdMatch[1];sdM=sdMatch[2];sdD=sdMatch[3];}
LODOP.ADD_PRINT_TEXT(70,520,40,16,sdY);
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
LODOP.ADD_PRINT_TEXT(70,580,25,16,sdM);
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
LODOP.ADD_PRINT_TEXT(70,620,25,16,sdD);
LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);

// ═══ 明细数据（填入预印表格单元格内） ═══
var DetailList = [
{{#each details}}
  {brand:"{{name}}",pname:"{{categoryName}}",material:"{{model}}",spec:"{{standard}}",piece:"{{operNumber}}",weight:"{{weight}}",uprice:"{{unitPrice}}"},
{{/each}}
];

// 列左边距与宽度（对齐预印格线，不含金额列）
var col=[60,65,60,60,45,55,55,65];
var rowH=24;
var dataTop=124;
var maxRows=12;

// 填入明细行数据（不打印金额列）
var totalPiece=0,totalWeight=0;
for(var k=0;k<DetailList.length&&k<maxRows;k++){
  var d=DetailList[k];
  var pw="";
  var w=parseFloat(d.weight),n=parseFloat(d.piece);
  if(!isNaN(w)&&!isNaN(n)&&n>0) pw=(w/n).toFixed(3);
  if(!isNaN(n)) totalPiece+=n;
  if(!isNaN(w)) totalWeight+=w;
  var arr=[d.brand,d.pname,d.material,d.spec,d.piece,pw,d.weight,d.uprice];
  var l=10;
  for(var i=0;i<arr.length;i++){
    LODOP.ADD_PRINT_TEXT(dataTop+k*rowH+5,l+2,col[i]-4,16,arr[i]||"");
    LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
    LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
    l+=col[i];
  }
}

// ═══ 合计行数据（不打印金额） ═══
var sumTop=dataTop+maxRows*rowH;
var sumArr=["","","","",totalPiece||"","",totalWeight?totalWeight.toFixed(3):"",""];
var l=10;
for(var i=0;i<col.length;i++){
  if(sumArr[i]){
    LODOP.ADD_PRINT_TEXT(sumTop+5,l+2,col[i]-4,16,sumArr[i]);
    LODOP.SET_PRINT_STYLEA(0,"Bold",1);
    LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
    LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
  }
  l+=col[i];
}

LODOP.PREVIEW();`

// ─── 物流运费单（CLodop 格式） ───

const freightBillClodopTemplate = `LODOP.PRINT_INIT("物流单A版");
LODOP.SET_PRINT_PAGESIZE(1,2100,2970,"A4");

// ═══ 标题 ═══
LODOP.ADD_PRINT_TEXT(18,15,770,22,"物流运费单");
LODOP.SET_PRINT_STYLEA(0,"FontSize",16);
LODOP.SET_PRINT_STYLEA(0,"Alignment",2);
LODOP.SET_PRINT_STYLEA(0,"Bold",1);

// ═══ 单据编号（右侧） ═══
LODOP.ADD_PRINT_TEXT(10,500,265,18,"No.{{billNo}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",12);
LODOP.SET_PRINT_STYLEA(0,"FontColor","#333333");
LODOP.SET_PRINT_STYLEA(0,"Alignment",3);
LODOP.SET_PRINT_STYLEA(0,"Bold",1);

// ═══ 打印日期（左上角） ═══
LODOP.ADD_PRINT_TEXT(30,20,300,12,"打印日期：{{_printDate}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",7);
LODOP.SET_PRINT_STYLEA(0,"FontColor","#999999");

LODOP.ADD_PRINT_LINE(42,15,42,785,0,2);

// ═══ 主表信息（左列） ═══
LODOP.ADD_PRINT_TEXT(50,20,350,14,"单据日期：{{billTimeStr}}");
LODOP.ADD_PRINT_TEXT(68,20,350,14,"客户名称：{{customerName}}");
LODOP.ADD_PRINT_TEXT(86,20,350,14,"项目名称：{{projectName}}");

// ═══ 主表信息（右列） ═══
LODOP.ADD_PRINT_TEXT(50,450,335,14,"结算方：{{carrierName}}");
LODOP.ADD_PRINT_TEXT(68,450,335,14,"单价(元/吨)：{{unitPrice}}");
LODOP.ADD_PRINT_TEXT(86,450,335,14,"总重量(吨)：{{totalWeight}}");
LODOP.ADD_PRINT_TEXT(104,450,335,14,"总运费(元)：{{totalFreight}}");
LODOP.SET_PRINT_STYLEA(0,"Bold",1);

// ═══ 备注 ═══
LODOP.ADD_PRINT_TEXT(122,20,760,14,"备注：{{remark}}");
LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
LODOP.SET_PRINT_STYLEA(0,"FontColor","#666666");

LODOP.ADD_PRINT_LINE(138,15,138,785,0,1);

// ═══ 明细表头 ═══
var col=[35,130,110,90,110,80,75,100];
var colName=["序号","出库单号","材料名称","材质","规格","件重","数量","重量(吨)"];
var colLeft=[20,58,192,306,400,514,598,677];
for(var i=0;i<colName.length;i++){
  LODOP.ADD_PRINT_TEXT(144,colLeft[i],col[i],13,colName[i]);
  LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
  LODOP.SET_PRINT_STYLEA(0,"Bold",1);
}
LODOP.ADD_PRINT_LINE(159,15,159,785,0,1);

// ═══ 明细数据 ═══
var DetailList = [
{{#each details}}
  {idx:"{{_index}}",billNo:"{{billNo}}",materialName:"{{materialName}}",model:"{{model}}",standard:"{{standard}}",operNumber:"{{operNumber}}",itemWeight:"{{itemWeight}}"},
{{/each}}
];

var rowTop=163;
var rowH=18;
for(var k=0;k<DetailList.length;k++){
  var d=DetailList[k];
  var pw="";
  if(d.itemWeight && d.operNumber){
    var w=parseFloat(d.itemWeight);
    var n=parseFloat(d.operNumber);
    if(!isNaN(w)&&!isNaN(n)&&n>0) pw=(w/n).toFixed(3);
  }
  var arr=[d.idx,d.billNo,d.materialName,d.model,d.standard,pw,d.operNumber,d.itemWeight];
  for(var i=0;i<arr.length;i++){
    LODOP.ADD_PRINT_TEXT(rowTop,colLeft[i],col[i],13,arr[i]||"");
    LODOP.SET_PRINT_STYLEA(0,"FontSize",8);
  }
  rowTop+=rowH;
}

LODOP.PREVIEW();`

// ─── 导出 ───

export const defaultTemplates = {
  retailOut: buildRetailTemplate('零售出库单'),
  retailBack: buildRetailTemplate('零售退货入库单'),
  purchaseApply: buildSimpleTemplate('请购单'),
  purchaseOrder: buildTaxTemplate('采购订单'),
  purchaseIn: buildTaxTemplate('采购入库单'),
  purchaseBack: buildTaxTemplate('采购退货出库单'),
  saleOrder: buildTaxTemplate('销售订单'),
  saleOut: saleOutJiancaiTemplate,
  saleOutA: saleOutATemplate,
  saleOutTax: buildTaxTemplate('销售出库单'),
  saleOutJiancai: saleOutJiancaiTemplate,
  saleBack: buildTaxTemplate('销售退货入库单'),
  otherIn: buildSimpleTemplate('其它入库单'),
  otherOut: buildSimpleTemplate('其它出库单'),
  allocationOut: buildSimpleTemplate('调拨出库单'),
  assemble: buildSimpleTemplate('组装单'),
  disassemble: buildSimpleTemplate('拆卸单'),
  stockCheckReplay: buildSimpleTemplate('盘点复盘单'),
  freightBill: freightBillClodopTemplate
}

export function getDefaultTemplate(billType) {
  return defaultTemplates[billType] || defaultTemplates['saleOut']
}
