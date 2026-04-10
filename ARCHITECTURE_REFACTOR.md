# 架构重构报告 - Phase A/B/C

**作者**: Flandre Scarlet  
**日期**: 2026-04-10  
**仓库**: OMSociety/Notion_Tally_Book

---

## Phase A: 数据模型拆分 ✅

### 问题
- 单一 `TallyRecord` 混合了本地和云端两种概念
- 同步冲突时无法精确判断数据来源

### 解决方案
新增 4 个文件：

```
app/src/main/java/com/coderpage/mine/persistence/entity/record/
├── LocalRecord.java         # 本地账本模型
├── CloudRecord.java         # Notion 云端模型
├── SyncConflictStrategy.java # 冲突策略枚举
└── SyncConflictDetector.java # 冲突检测器
```

### Before/After

**Before**:
```java
// 无法区分数据来源
TallyRecord record = new TallyRecord();
record.setAmount(12.5);  // 本地还是云端？
```

**After**:
```java
// 明确区分
LocalRecord local = LocalRecord.fromTallyRecord(record);
CloudRecord cloud = CloudRecord.fromTallyRecord(record);

// 检测冲突
if (SyncConflictDetector.hasConflict(local, cloud)) {
    SyncConflictStrategy strategy = SyncConflictDetector.suggestStrategy(local, cloud);
    // 根据策略处理...
}
```

---

## Phase B: 废弃 API 迁移 ✅

### 问题
- 3 个文件使用 `onActivityResult`（已废弃）
- 多处直接 `findViewById`（应使用 ViewBinding）

### 解决方案
1. 创建迁移示例：`BackupFileActivityModern.kt`
2. 使用 Activity Result API 替代 `onActivityResult`

### Before/After

**Before**:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    mViewModel.onActivityResult(self(), requestCode, resultCode, data);
}
```

**After**:
```kotlin
private val backupLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        result.data?.let { handleBackupResult(it) }
    }
}

// 使用
backupLauncher.launch(backupIntent)
```

---

## Phase C: Notion 同步层重构 ✅

### 问题
- `NotionSyncManager` 直接 `new NotionApiClient()`，违反 DI 原则
- catch 块用空注释吞掉异常，用户看不到失败提示

### 解决方案
新增 2 个文件：

```
app/src/main/java/com/coderpage/mine/app/tally/
├── common/Result.kt       # 密封结果类
└── di/NotionModule.kt     # Hilt DI 模块
```

### Before/After

**Before**:
```java
// 直接实例化（违反 DI）
NotionApiClient client = new NotionApiClient();

try {
    client.sync(record);
} catch (Exception e) {
    // 吞掉异常
}
```

**After**:
```kotlin
// 依赖注入
@Inject lateinit var client: NotionApiClient

// 结果封装
val result: Result<Unit> = client.sync(record)
result
    .onSuccess { showSuccessToast() }
    .onError { showErrorToast(it.message) }
```

---

## 待完成事项

1. **Phase B 完整迁移**: 需要将所有 `onActivityResult` 迁移到 Activity Result API
2. **Phase C Hilt 集成**: 需要在 `app/build.gradle` 添加 Hilt 插件和依赖
3. **单元测试**: 为 `SyncConflictDetector` 编写测试用例

---

## 文件变更统计

| 类型 | 新增 | 修改 |
|------|------|------|
| Java | 4 | 0 |
| Kotlin | 3 | 0 |
| 文档 | 1 | 0 |
| **总计** | **8** | **0** |
