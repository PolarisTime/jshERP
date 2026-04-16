# jshERP-web Cloudflare Pages 部署指南

## 一、前置条件

- Cloudflare 账号（免费即可）
- 后端 API 已部署并可公网访问（如 `https://api.example.com/jshERP-boot`）
- Node.js 20+、Yarn 已安装（本地构建时需要）

## 二、部署前代码修改

### 2.1 添加 SPA 路由回退文件

项目使用 Vue Router **history 模式**，Cloudflare Pages 需要将所有路径回退到 `index.html`。

在 `public/` 目录下创建 `_redirects` 文件：

```
/* /index.html 200
```

### 2.2 配置后端 API 地址

`public/index.html` 中有全局配置：

```javascript
window._CONFIG = {};
window._CONFIG['domianURL'] = '/jshERP-boot';
```

部署时需要将 `/jshERP-boot` 改为后端实际地址。有两种方案：

**方案 A：直连后端（推荐简单场景）**

```javascript
window._CONFIG['domianURL'] = 'https://api.example.com/jshERP-boot';
```

> 注意：后端需配置 CORS，允许 Pages 域名访问。

**方案 B：通过 Cloudflare Workers 反代（推荐生产环境）**

保持 `domianURL` 为 `/jshERP-boot` 不变，创建 Workers 反代 API 请求（见第五节）。

### 2.3 移除百度统计（可选）

`public/index.html` 中内嵌了百度统计脚本，如不需要可删除：

```javascript
// 删除以下代码块（约 260-272 行）
let statisticsCode = '1cd9bcbaae133f03a6eb19da6579aaba'
window._statistics = 'https://hm.baidu.com/hm.js?' + statisticsCode
// ... 及后续 _hmt 代码
```

### 2.4 CLodop 打印说明

打印功能依赖本地 CLodop 服务（`localhost:8000/18000`），这是客户端组件，与部署无关。用户电脑需安装 CLodop 客户端才能使用打印功能，此项无需修改。

## 三、部署方式

### 方式一：Git 连接部署（推荐）

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com/) → **Workers & Pages** → **Create**
2. 选择 **Pages** → **Connect to Git**
3. 授权并选择 jshERP 仓库
4. 配置构建设置：

| 配置项 | 值 |
|-------|---|
| 项目名称 | `jsherp-web`（自定义） |
| 生产分支 | `release`（或你的部署分支） |
| 构建命令 | `cd jshERP-web && yarn install && yarn build` |
| 构建输出目录 | `jshERP-web/dist` |
| 根目录 | `/`（仓库根目录） |
| Node.js 版本 | 在环境变量中设置 `NODE_VERSION` = `20` |

5. 点击 **Save and Deploy**

### 方式二：CLI 直接上传

```bash
# 1. 安装 Wrangler CLI
npm install -g wrangler

# 2. 登录
wrangler login

# 3. 本地构建
cd jshERP-web
yarn install
yarn build

# 4. 部署
wrangler pages deploy dist --project-name=jsherp-web
```

## 四、环境变量配置

在 Cloudflare Pages 项目设置中，可配置以下环境变量：

| 变量名 | 值 | 说明 |
|-------|---|------|
| `NODE_VERSION` | `20` | 指定 Node.js 版本 |
| `YARN_VERSION` | `1.22.22` | 指定 Yarn 版本 |

> 注意：`window._CONFIG` 是运行时配置，不是构建时环境变量。如需按环境切换 API 地址，建议在构建脚本中替换 `index.html` 中的值，或使用 Workers 反代方案。

## 五、API 反代方案（Workers）

如果不想让前端直连后端（避免 CORS 问题），可以创建 Cloudflare Workers 反代。

### 5.1 创建 Workers 反代

在 Pages 项目中创建 `functions/jshERP-boot/[[path]].js`：

```javascript
export async function onRequest(context) {
  const url = new URL(context.request.url);
  // 将请求转发到后端服务器
  const backendUrl = 'https://api.example.com' + url.pathname + url.search;

  const newRequest = new Request(backendUrl, {
    method: context.request.method,
    headers: context.request.headers,
    body: context.request.body,
  });

  return fetch(newRequest);
}
```

### 5.2 Workers 免费额度

| 项目 | 免费额度 |
|------|---------|
| 请求次数 | 10 万次/天 |
| CPU 时间 | 10ms/请求 |

对于 ERP 系统日常使用足够。超出后按 $0.50/百万请求计费。

## 六、后端 CORS 配置（方案 A 需要）

如果前端直连后端，需在 Spring Boot 后端添加 CORS 配置：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("https://jsherp-web.pages.dev") // Pages 域名
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

## 七、自定义域名（可选）

1. Pages 项目 → **Custom domains** → **Set up a custom domain**
2. 输入域名（如 `erp.example.com`）
3. Cloudflare 自动配置 DNS 和 SSL 证书
4. 等待 DNS 生效（通常几分钟）

## 八、WebSocket 注意事项

项目使用 WebSocket 进行消息通知（`HeaderNotice.vue`），连接地址从 `window._CONFIG['domianURL']` 动态生成：

- 方案 A（直连）：后端需支持 WebSocket，前端自动将 `https://` 转为 `wss://`
- 方案 B（Workers 反代）：Cloudflare Workers **免费版不支持 WebSocket**，需升级到 Paid 计划（$5/月），或对 WebSocket 单独直连后端

## 九、部署检查清单

- [ ] `public/_redirects` 文件已创建（SPA 路由回退）
- [ ] `public/index.html` 中 `domianURL` 已配置为正确的后端地址
- [ ] 后端已配置 CORS（方案 A）或 Workers 反代已创建（方案 B）
- [ ] 后端服务器已部署且公网可访问
- [ ] 构建命令和输出目录配置正确
- [ ] `NODE_VERSION` 环境变量已设置为 `20`
- [ ] 首次部署后测试：登录、数据加载、打印功能

## 十、费用总结

| 服务 | 免费额度 | 超出费用 |
|------|---------|---------|
| **Pages**（静态托管） | 无限请求、无限带宽、500 次构建/月 | Pro $20/月（5000 次构建） |
| **Workers**（API 反代） | 10 万请求/天 | $5/月 + $0.50/百万请求 |
| **自定义域名** | 包含 SSL | 免费 |

**典型场景：中小企业 ERP（<50 用户）完全免费。**

## 十一、常见问题

### Q: 部署后页面空白？
检查构建输出目录是否正确设置为 `jshERP-web/dist`。

### Q: 刷新页面 404？
确认 `public/_redirects` 文件已添加且内容为 `/* /index.html 200`。

### Q: API 请求失败？
1. 检查 `window._CONFIG['domianURL']` 是否指向正确的后端地址
2. 检查浏览器控制台是否有 CORS 错误
3. 确认后端服务器正常运行

### Q: 打印功能不可用？
正常现象。CLodop 是客户端本地服务，用户需在自己电脑上安装 CLodop 客户端。
