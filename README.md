# Notion Tally Book

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/AI%20Generated-LLM-brightgreen?style=flat-square" alt="AI">
</p>

> ⚠️ **AI Generated** — 本项目文档由 AI 辅助生成

---

## 📱 项目简介

**Notion Tally Book** 是一款基于 [Mine](https://github.com/coderpage/Mine) fork 的 Android 记账应用，在原版基础上扩展了 **Notion 云同步** 功能。

### 核心能力

| 功能 | 说明 |
|------|------|
| 本地记账 | 快速记录收支，支持分类、备注、时间 |
| AI 智能识别 | 截图/短信自动解析金额和分类 |
| Notion 同步 | 本地数据实时双向同步至 Notion 云端 |
| 冲突处理 | 智能合并/覆盖策略处理同步冲突 |
| 数据备份 | 本地自动备份 + 文件导出 |

---

## 🏗️ 项目结构

```
Notion_Tally_Book/
├── app/                          # 主应用模块
│   └── src/main/java/com/coderpage/mine/
│       ├── app/tally/
│       │   ├── module/           # 功能模块
│       │   │   ├── home/          # 首页
│       │   │   ├── edit/          # 记账编辑
│       │   │   ├── detail/        # 记录详情
│       │   │   ├── records/       # 记录列表
│       │   │   ├── chart/         # 统计图表
│       │   │   ├── search/        # 搜索
│       │   │   ├── setting/       # 设置（含 Notion 同步配置）
│       │   │   ├── backup/        # 备份管理
│       │   │   ├── auto/          # AI 自动识别
│       │   │   ├── about/         # 关于
│       │   │   └── debug/         # 调试
│       │   ├── sync/              # Notion 同步核心
│       │   │   ├── NotionSyncManager.java      # 同步管理器
│       │   │   ├── NotionApiClient.java         # API 客户端
│       │   │   ├── ConflictResolver.java        # 冲突处理器
│       │   │   └── NotionDatabaseValidator.java # 数据库校验
│       │   └── config/
│       │       └── NotionConfig.java            # 同步配置
│       └── framework/             # MVP 框架
├── libbase/                       # 基础库
└── libupdate/                     # 更新模块
```

---

## 🔧 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 平台 | Android | minSdk 21, targetSdk 34 |
| 语言 | Java + Kotlin | 主体 Java，扩展 Kotlin |
| 架构 | MVP | Model-View-Presenter |
| 数据库 | Room | 本地 SQLite ORM |
| 网络 | Retrofit + OkHttp | API 通信 |
| 同步 | Notion API v1 | 云端数据同步 |
| AI | 阿里云百炼 | Qwen2.5-VL-32B 视觉模型 |
| UI | DataBinding + Material | 响应式 UI |

---

## ☁️ Notion 同步说明

### 同步模式

| 模式 | 说明 |
|------|------|
| 本地 → Notion | 仅上传本地记录到云端 |
| Notion → 本地 | 仅从云端拉取到本地 |
| 双向同步 | 双向增量同步，自动冲突处理 |

### Notion 数据库模板

需要创建包含以下属性的数据库：

| 属性名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 金额 | Number | ✅ | 记账金额 |
| 类型 | Select | ✅ | 支出 / 收入 |
| 分类 | Select | ✅ | 支出分类 |
| 时间 | Date | ✅ | 记账日期 |
| 备注 | Text | ❌ | 附加说明 |
| 状态 | Select | ✅ | 活跃 / 已删除 |

详细配置见 [NOTION_DATABASE_TEMPLATE.md](./docs/NOTION_DATABASE_TEMPLATE.md)

---

## 🚀 快速开始

### 编译构建

```bash
# 克隆源码
git clone https://github.com/OMSociety/Notion_Tally_Book.git
cd Notion_Tally_Book

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需配置签名）
./gradlew assembleRelease
```

### 配置 Notion 同步

1. 访问 [Notion Developers](https://www.notion.so/my-integrations) 创建 Integration，复制 Token
2. 在 Notion 中创建数据库，按上述模板配置属性
3. 将数据库分享给 Integration（`...` → `Add connections`）
4. 从数据库 URL 提取 Database ID
5. 在 App「设置」→「Notion 同步」中填入 Token 和 Database ID

---

## 📊 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | 基础编译修复、缺失类补全、代码重构 | ✅ 完成 |
| P1 | Notion 同步核心（API 客户端、双向同步、冲突处理、数据库校验） | ✅ 完成 |
| P2 | Notion 同步设置 UI 集成 | ✅ 完成 |
| P3 | 同步历史记录、错误重试机制 | 🚧 规划中 |

---

## 📄 许可证

本项目基于 [Mine](https://github.com/coderpage/Mine) 修改，继承其 MIT 许可证。

---

## 🙏 致谢

### 开源项目

| 项目 | 地址 | 用途 |
|------|------|------|
| Mine | [GitHub](https://github.com/coderpage/Mine) | 本项目基础 |
| Room | [Android Developers](https://developer.android.com/jetpack/androidx/releases/room) | 数据库 ORM |
| Retrofit | [Square](https://square.github.io/retrofit/) | HTTP 客户端 |
| Notion API | [Developers.notion.so](https://developers.notion.com/) | 云端同步 |

### 资源

| 资源 | 来源 |
|------|------|
| 应用图标 | [iconfont](http://iconfont.cn/) |
| UI 图标 | [Material Design Icons](https://github.com/google/material-design-icons) |
| AI 模型 | [阿里云百炼](https://bailian.console.aliyun.com/) (Qwen2.5-VL) |

### 开发工具

| 工具 | 用途 |
|------|------|
| [AstrBot](https://github.com/SynCLK/AstrBot) | AI 辅助开发（代码修复、文档生成） |

---

## 📬 联系

- 原作者微信公众号：**搜索 MINE应用**
- Issues：本仓库接受 Bug 报告和功能建议

---

*本项目仅供学习交流使用，如需商业使用请联系原项目作者。*
