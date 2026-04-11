# Notion 记账本

一款基于 Notion 的轻量级记账应用，支持本地记账与 Notion 云端实时双向同步。

## 特性

- **本地记账**：简单快速地记录日常收支
- **Notion 同步**：本地数据实时双向同步至 Notion 云端，支持冲突智能处理
- **AI 智能识别**：支持任意兼容 LLM API 的图片识别，自动识别账单信息
- **数据安全**：本地自动备份 + 文件导出

## 新功能

- **Notion 双向同步**：本地与 Notion 实时同步，支持冲突智能合并/覆盖策略
- **通用 AI API**：支持 OpenAI、SiliconFlow、Claude 等任意兼容 LLM API
- **冲突处理**：智能合并/覆盖策略处理同步冲突
- **数据备份**：本地自动备份 + 文件导出

## 使用说明

### Notion 配置

1. 创建 Notion Integration
   - 访问 [Notion Integrations](https://www.notion.so/my-integrations)
   - 点击 "New integration" 创建
   - 复制生成的 Token（以 `secret_` 开头）

2. 创建 Notion 数据库
   - 在 Notion 中创建新页面
   - 输入 `/table` 召唤块菜单，选择 "Table - Full page"
   - 配置以下字段：
     | 字段名称 | 类型 | 说明 |
     |----------|------|------|
     | 金额 | Number | 记账金额 |
     | 类型 | Select | 支出/收入 |
     | 分类 | Text | 分类名称 |
     | 时间 | Date | 记账日期 |
     | 备注 | Text | 备注说明 |

3. 获取 Database ID
   - 点击数据库右上角 `...` 菜单
   - 选择 "Copy link to view"
   - 从链接中提取 32 位字符作为 Database ID

4. 授权 Integration
   - 打开数据库页面
   - 点击 `...` 菜单，选择 "Connections"
   - 添加您的 Integration

### AI 识别配置

支持多种 AI 服务提供商：

| 提供商 | API 地址 | 模型示例 |
|--------|----------|---------|
| SiliconFlow | `https://api.siliconflow.cn/v1` | `Qwen/Qwen2.5-VL-72B-Instruct` |
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
| Claude | `https://api.anthropic.com/v1` | `claude-sonnet-4-7-20250611` |

1. 在设置中选择提供商
2. 填写 API Key
3. 输入模型名称
4. 点击"测试连接"验证配置

## 技术栈

- **客户端**：Android（Java）
- **本地数据库**：Room
- **云端**：Notion API
- **AI**：OpenAI 兼容 API

## 项目结构

```
app/src/main/java/com/coderpage/mine/
├── app/tally/
│   ├── ai/                    # AI 识别模块
│   │   ├── AiApiConfig.java      # API 配置
│   │   ├── AiRecognizer.java     # 识别器接口
│   │   ├── OpenAiRecognizer.java # OpenAI 兼容实现
│   │   └── AiSettingActivity.java # AI 设置页面
│   ├── config/
│   │   └── NotionConfig.java     # Notion 配置
│   ├── sync/
│   │   ├── NotionApiClient.java   # Notion API 客户端
│   │   ├── NotionSyncManager.java # 同步管理器
│   │   └── ConflictResolver.java  # 冲突处理
│   ├── module/
│   │   ├── home/             # 首页
│   │   ├── edit/             # 记账编辑
│   │   ├── setting/           # 设置页面
│   │   └── auto/             # 图片识别
│   └── persistence/
│       ├── sql/               # Room 数据库
│       └── model/             # 数据模型
```

## 注意事项

- 请妥善保管您的 Notion API Token 和 AI API Key
- 首次使用请先在设置中配置 Notion 和 AI
- 同步前请确保 Notion 数据库字段已正确配置

## 许可证

本项目仅供学习交流使用。
