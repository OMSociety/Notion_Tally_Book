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

## 构建基线（稳定组合）

- **JDK**：8（推荐 Temurin 8）
- **Gradle**：5.1.1（`gradle/wrapper/gradle-wrapper.properties`）
- **Android Gradle Plugin**：3.4.0（`build.gradle`）
- **buildSrc AGP 依赖**：3.3.2（`buildSrc/build.gradle`）

> 说明：当前构建依赖 Google Maven 仓库，网络环境需可访问 `dl.google.com` 才能完成依赖下载。

## 构建前置条件与故障排查

| 检查项 | 期望状态 | 常见失败现象 | 处理建议 |
|---|---|---|---|
| Java 版本 | JDK 8 | `Could not initialize class org.codehaus.groovy...` | 切换到 JDK 8 后重试（如 `JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64`） |
| `dl.google.com` DNS | 可解析 | `dl.google.com: No address associated with hostname` | 修复 DNS、配置公司网络白名单或代理放行 `dl.google.com` |
| Google Maven 访问 | HTTPS 可达 | `Could not GET ... dl/android/maven2/...` | 检查代理/防火墙策略，确保可访问 `https://dl.google.com/dl/android/maven2/` |
| 仓库源配置 | `google + mavenCentral + jitpack` | 依赖解析失败/源不一致 | 不回引 `jcenter()`，统一使用仓库根 `build.gradle` 当前配置 |

建议在构建前执行：
1. `java -version`（确认 JDK 8）
2. `nslookup dl.google.com` 或 `getent hosts dl.google.com`
3. `./gradlew test --no-daemon`

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

## 发布前端到端验证清单

1. 本地新增一笔支出与一笔收入，确认首页/图表展示正常。
2. 执行 Notion 同步并确认远端记录创建成功。
3. 人工制造同一条记录的本地/远端差异，验证冲突处理策略结果。
4. 执行 AI 配置测试连接，验证识别流程可用。
5. 执行本地备份与导出，检查导出文件可读取。

## 许可证

本项目仅供学习交流使用。
