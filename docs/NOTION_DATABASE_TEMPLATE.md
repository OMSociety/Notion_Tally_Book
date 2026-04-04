# Notion 数据库模板

## 创建步骤
1. 在 Notion 创建新数据库
2. 添加以下列：

| 列名 | 类型 | 说明 |
|------|------|------|
| 金额 | Number | 记账金额 |
| 分类 | Select | 支出分类 |
| 备注 | Text | 备注 |
| 时间 | Date | 日期 |
| 类型 | Select | 支出/收入 |
| 状态 | Select | 活跃/已删除 |

## Integration 设置
1. 创建后复制 Token（secret_xxx）
2. 在数据库中点击 `...` → `Add connections` → 添加 Integration
3. 从 URL 提取 Database ID
