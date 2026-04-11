/**
 * CLodop 打印控件注册号配置（按域名顶级注册）
 *
 * 授权级别：按域名顶级注册
 *   - 支持一级域名（如 *.example.com），子域名不限
 *   - 支持 C-Lodop 直接打印 PDF 文档
 *   - 支持"文档式模板"、"获取原始指令"等高端功能（样例50及后）
 *   - 客户端操作系统语言不限，中外文皆可，支持外文+中文包
 *   - 支持未来新增功能（限 Windows 平台 Lodop 和 C-Lodop 本机模式）
 *
 * 部署时在此文件填入购买的注册号，无需重新构建前端。
 * 未填写注册号时打印功能仍可使用，但会有水印/弹窗提示。
 *
 * SET_LICENSES 参数说明：
 *   参数1 — 公司名称（Lodop 注册时使用的名称）
 *   参数2 — Lodop 注册号
 *   参数3 — 公司名称（C-Lodop 注册时使用的名称，可与参数1相同）
 *   参数4 — C-Lodop 注册号
 */
window._CONFIG = window._CONFIG || {};
window._CONFIG['clodopLicense'] = {
  // Lodop 注册（普通打印控件）
  companyName: '',
  licenseA: '',
  // C-Lodop 注册（支持 PDF 打印、文档式模板等高端功能）
  companyNameB: '',
  licenseB: ''
};
