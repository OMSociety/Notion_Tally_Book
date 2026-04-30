# Notion 记账本

[![Android](https://img.shields.io/badge/Android-5.0+-3DDC84?logo=android)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-8-ED8B00?logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 基于 Notion 的 Android 记账应用，本地 Room 数据库 + Notion 云端双向同步，支持 AI 图片识别自动记账。

---

## 功能概览

**核心能力** 覆盖五个维度：

- **本地记账** — 收入/支出快速录入，分类管理，月度统计图表，历史搜索
- **Notion 双向同步** — 本地 ↔ Notion 实时同步，冲突智能合并/覆盖，断网自动队列重试
- **AI 图片识别** — 拍照/截图自动识别金额、分类、时间；兼容 OpenAI / Claude / SiliconFlow 等任意 LLM API
- **自动记账** — 监听短信账单自动解析录入；通知栏图片快捷识别
- **数据安全** — 本地自动备份 + CSV/JSON 导出导入 + 敏感信息 AES 加密存储

**智能特性：**

- 短信自动解析：银行/支付宝/微信消费短信 → 自动提取金额、时间
- 语音记账：录音自动转文字 → 提取账单信息（需 AI 服务）
- 冲突策略可配：本地优先 / 云端优先 / 智能合并
- 全链路日志：同步失败不再静默，每一步可追溯

---

## 快速开始

### 1. 构建安装

```bash
git clone https://github.com/OMSociety/Notion_Tally_Book.git
cd Notion_Tally_Book
./gradlew assembleDebug
# 安装 app/build/outputs/apk/debug/app-debug.apk
```

> 构建基线：JDK 8 + Gradle 5.1.1 + Android Gradle Plugin 3.4.0

### 2. 配置 Notion

1. [创建 Notion Integration](https://www.notion.so/my-integrations) → 复制 Token（`secret_` 开头）
2. 在 Notion 中新建数据库（`/table` → Table - Full page），配置字段：

   | 字段 | 类型 | 说明 |
   |------|------|------|
   | 金额 | Number | 收入/支出金额 |
   | 类型 | Select | 支出 / 收入 |
   | 分类 | Text | 分类名称 |
   | 时间 | Date | 记账日期 |
   | 备注 | Text | 备注说明 |

3. 获取 Database ID：数据库右上角 `...` → Copy link → 提取 32 位 ID
4. 授权：数据库 `...` → Connections → 添加你的 Integration

### 3. 配置 AI 识别

1. 设置 → AI 识别 → 选择提供商
2. 填写 API Key + 模型名称

   | 提供商 | API 地址 | 推荐模型 |
   |--------|----------|----------|
   | SiliconFlow | `https://api.siliconflow.cn/v1` | `Qwen/Qwen2.5-VL-72B-Instruct` |
   | OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
   | Claude | `https://api.anthropic.com/v1` | `claude-sonnet-4-7-20250611` |

3. 点击「测试连接」验证

---

## 功能列表

### 记账模块

| 功能 | 说明 |
|------|------|
| 首页概览 | 当月收支统计、最近记录、分类饼图 |
| 记账编辑 | 金额/类型/分类/时间/备注 完整录入 |
| 分类管理 | 自定义分类、排序、分组 |
| 历史查询 | 按月份/分类/关键词搜索 |
| 月度统计 | 分类支出/收入柱状图 & 饼图 |
| CSV 导入 | 从 CSV 文件批量导入账单 |

### 同步模块

| 功能 | 说明 |
|------|------|
| 全量同步 | 本地全量推送至 Notion |
| 增量同步 | 仅同步变更记录 |
| 冲突处理 | 本地优先 / 云端优先 / 智能合并 |
| 自动同步 | 定时 + 网络恢复自动触发 |
| 同步日志 | 每次同步结果可查 |

### AI 模块

| 功能 | 说明 |
|------|------|
| 图片识别 | 拍照/相册 → AI 提取账单信息 |
| 语音识别 | 录音 → AI 转文字 → 提取账单 |
| 多模型支持 | 兼容 OpenAI 格式 API |
| 自定义 Prompt | 可调整识别提示词 |

### 自动记账模块

| 功能 | 说明 |
|------|------|
| 短信识别 | 监听银行/支付短信自动解析 |
| 通知栏快捷 | 截图后通知栏一键识别 |
| 识别确认 | 自动识别后弹窗确认再录入 |

### 备份模块

| 功能 | 说明 |
|------|------|
| 自动备份 | 定时本地备份，保留最近 N 份 |
| 手动备份 | 一键导出 CSV / JSON |
| 备份恢复 | 从备份文件恢复数据 |
| 备份管理 | 查看/删除历史备份 |

---

## 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| Notion Token | string | - | Integration Secret |
| Notion Database ID | string | - | 数据库 32 位 ID |
| 同步策略 | enum | 增量 | 全量 / 增量 |
| 冲突策略 | enum | 智能合并 | 本地优先 / 云端优先 / 智能合并 |
| 自动同步间隔 | int | 30 | 分钟，0 = 关闭 |
| AI 提供商 | enum | - | OpenAI / Claude / SiliconFlow / 自定义 |
| AI API Key | string | - | 服务商 API Key |
| AI 模型 | string | - | 模型名称 |
| AI 自定义 Prompt | string | - | 覆盖默认识别提示词 |
| 短信监听 | bool | false | 开启短信自动解析 |
| 自动备份间隔 | int | 24 | 小时，0 = 关闭 |
| 备份保留份数 | int | 5 | 自动清理超出数量 |

---

## 使用示例

**场景 1：手动记账**
> 首页点 + → 输入金额 → 选支出/收入 → 选分类 → 确认 → 自动同步至 Notion

**场景 2：AI 拍照记账**
> 首页点相机 → 拍购物小票 → AI 识别金额 ¥128.5 分类「餐饮」→ 确认录入 → 同步至 Notion

**场景 3：短信自动记账**
> 收到银行扣款短信 → 通知栏弹出「检测到消费 ¥99.00」→ 点击确认 → 自动录入

**场景 4：数据迁移**
> 旧手机导出 CSV → 新手机设置 → CSV 导入 → 一键同步至 Notion

---

## 权限说明

| 权限 | 用途 | 必须 |
|------|------|------|
| 网络 | Notion 同步 / AI API 调用 | ✅ |
| 相机 | AI 拍照识别 | 可选 |
| 麦克风 | 语音记账 | 可选 |
| 短信 | 自动解析消费短信 | 可选 |
| 通知 | 通知栏快捷记账 | 可选 |
| 存储 | 备份文件读写 / CSV 导入 | 可选 |

---

## 项目结构

```
app/src/main/java/com/coderpage/mine/app/tally/
├── ai/                    # AI 识别模块
│   ├── AiApiConfig.java
│   ├── AiRecognizer.java
│   ├── OpenAiRecognizer.java
│   └── AiSettingActivity.java
├── config/
│   └── NotionConfig.java
├── sync/                  # Notion 同步模块
│   ├── NotionApiClient.java
│   ├── NotionSyncManager.java
│   ├── RecordConverter.java
│   └── SyncRepository.java
├── module/
│   ├── home/              # 首页 & 统计
│   ├── edit/              # 记账编辑 & 分类管理
│   ├── setting/           # 设置页面
│   ├── backup/            # 备份 & 恢复 & CSV 导入
│   ├── auto/              # 短信监听 & 图片识别入口
│   ├── records/           # 历史记录
│   ├── search/            # 搜索
│   └── chart/             # 统计图表
├── persistence/
│   ├── sql/               # Room 数据库 (TallyDatabase)
│   └── model/             # 数据模型
├── security/
│   └── SensitiveDataCipher.java  # AES 加密
├── update/                # 应用更新检查
└── utils/
    ├── TimeUtils.java
    └── DatePickUtils.java
```

---

## 环境要求

- Android 5.0 (API 21) +
- Notion 账号 + Integration Token
- （可选）AI API Key（OpenAI / Claude / SiliconFlow 等）

---

## 贡献与反馈

Issues & PR: [github.com/OMSociety/Notion_Tally_Book](https://github.com/OMSociety/Notion_Tally_Book)

---

## 许可证

MIT License

---

## 作者

**OMSociety** — [@OMSociety](https://github.com/OMSociety)
