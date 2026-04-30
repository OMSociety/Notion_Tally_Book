# Bug Report - Notion Tally Book

**排查日期:** 2026-04-29
**排查范围:** 全部 .java, .gradle, .xml 源文件（~140+ 文件，全模块覆盖）

---

## 本次修复的 Bug（15 项）

### 1. [严重] BaseActivity - setToolbarAsBack/setToolbarAsClose 空指针崩溃
**文件:** `app/.../ui/BaseActivity.java:55-67`
**问题:** `mToolbar` 可能为 null（布局中无 `R.id.toolbar`），但 `setToolbarAsBack()` 和 `setToolbarAsClose()` 直接调用 `mToolbar.setNavigationIcon()`，导致 NPE。
**修复:** 添加 `if (mToolbar == null) return;` 防护。

### 2. [严重] HomeAdapter - onCreateViewHolder 返回 null
**文件:** `app/.../module/home/HomeAdapter.java:127`
**问题:** `switch` 的 `default` 分支返回 `null`。RecyclerView 使用 null ViewHolder 会 NPE 崩溃。
**修复:** 改为 `throw new IllegalArgumentException("Unknown viewType: " + viewType)`。

### 3. [严重] RecordEditFragment - getArguments() 空指针崩溃
**文件:** `app/.../module/edit/record/RecordEditFragment.java:50`
**问题:** `getArguments().getSerializable(...)` 在 Fragment 被系统重建（如进程死亡后恢复）时，`getArguments()` 返回 null，导致 NPE。
**修复:** 添加 null 检查，null 时默认使用 `RecordType.EXPENSE`。

### 4. [严重] RecordViewModel - 不安全的类型转换 + 空参数
**文件:** `app/.../module/edit/record/RecordViewModel.java:383-386`
**问题:** `(RecordEditFragment) owner` 若被其他 LifecycleOwner 观察会 ClassCastException。`fragment.getArguments()` 可能返回 null。
**修复:** 添加 `instanceof` 检查和 null 参数防护。

### 5. [严重] RecordDetailViewModel - mRecord 不安全类型转换
**文件:** `app/.../module/detail/RecordDetailViewModel.java:74-82`
**问题:** `(Record) mRecord` 在查询未完成时 mRecord 为 null，导致 NPE。
**修复:** 添加 `if (mRecord instanceof Record)` 防护。

### 6. [高] RecordDetailViewModel - 收入详情日元符号粘贴错误
**文件:** `app/.../module/detail/RecordDetailViewModel.java:125,129`
**问题:** 收入分支中，"¥" 符号被错误地添加到时间字符串而非金额字符串。对比支出分支（line 104）可知这是复制粘贴错误。
**修复:** 金额加 "¥" 前缀，时间不加。

### 7. [严重] FileDownloader - HTTP Response 未关闭导致连接泄漏
**文件:** `libupdate/.../FileDownloader.java:44-57`
**问题:** `okHttpClient.newCall(request).execute()` 返回的 Response 从未关闭，泄漏连接和文件描述符，最终导致 `SocketException: Too many open files`。
**修复:** 重构为 `try/finally` 模式，确保 `response.close()`。同时添加 `response.body()` null 检查。

### 8. [高] PreferencesUtils - 返回固定大小列表导致 UnsupportedOperationException
**文件:** `app/.../utils/PreferencesUtils.java:51`
**问题:** `Arrays.asList(source.split(","))` 返回固定大小的列表。SearchViewModel 调用 `add()` 和 `remove()` 时抛出 `UnsupportedOperationException` 崩溃。
**修复:** 包装为 `new ArrayList<>(Arrays.asList(...))`。

### 9. [高] RecordGroup - float 精度丢失（金额聚合）
**文件:** `app/.../persistence/model/RecordGroup.java:22`
**问题:** SQLite 的 `sum()` 返回 double，但映射到 Java `float`，导致金额聚合精度丢失。例如 `99999.99` 存为 float 变为 `~99999.992`。
**修复:** `float amount` 改为 `double amount`，更新 getter/setter。

### 10. [高] RecordsViewModel - 直接修改 LiveData 内部列表
**文件:** `app/.../module/records/RecordsViewModel.java:127-129,204-208`
**问题:** `mRecordList.getValue().clear()` 和 `formatRecordList` 直接修改 LiveData 持有的列表对象，导致观察者看到数据突然消失或不一致。
**修复:** `setQuery` 发送新空列表；`formatRecordList` 创建新 ArrayList。

### 11. [高] SearchViewModel - 直接修改 LiveData 列表 + subList 返回视图
**文件:** `app/.../module/search/SearchViewModel.java:142-144,185-194`
**问题:** `onRemoveHistoryItemClick` 和 `addSearchHistory` 直接修改 `mSearchHistoryList.getValue()` 返回的列表。`subList()` 返回的是原列表的视图而非副本。
**修复:** 修改前创建防御性副本；`subList` 包装为 `new ArrayList<>(...)`。

### 12. [高] Record.createEntity - null desc/syncId 传入 @NonNull 字段
**文件:** `app/.../persistence/model/Record.java:70-71`
**问题:** Record 的 `desc` 和 `syncId` 可为 null，但 RecordEntity 声明为 @NonNull。传 null 违反约束，可能导致运行时崩溃。
**修复:** 添加 null 到空字符串回退：`entity.setDesc(getDesc() != null ? getDesc() : "")`。

### 13. [中等] SettingPreference - Activity Context 内存泄漏
**文件:** `app/.../persistence/preference/SettingPreference.java:41`
**问题:** `context.getSharedPreferences()` 持有传入的 Activity 引用，阻止垃圾回收。
**修复:** 改为 `context.getApplicationContext().getSharedPreferences(...)`。

### 14. [中等] BackupCache - backupFolderPath 用 null 初始化
**文件:** `app/.../module/backup/BackupCache.java:46`
**问题:** `backupFolderPath` 在类加载时用 `DATA_ROOT_PATH + ...` 初始化，但 `DATA_ROOT_PATH` 此时为 null。
**修复:** 移到构造函数中，在 `DATA_ROOT_PATH` 赋值后初始化。

### 15. [中等] BackupCache - deleteAllBackupFile 空数组崩溃
**文件:** `app/.../module/backup/BackupCache.java:157`
**问题:** `backupFileFolder.list()` 在 I/O 错误时返回 null，增强 for 循环遍历 null 数组会 NPE。
**修复:** 添加 `if (backupFileNames == null) return true;` 防护。

---

## 此前已修复的 Bug（上一轮排查）

| # | 问题 | 文件 | 状态 |
|---|------|------|------|
| 1 | AutoBackupWorker ClassCastException | AutoBackupWorker.java | 已修复 |
| 2 | ImageReceiverActivity RuntimeException | ImageReceiverActivity.java | 已修复 |
| 3 | BackupCache FileWriter 资源泄漏 | BackupCache.java | 已修复（try-with-resources） |
| 4 | Backup.readBackupJsonFile finally 块 NPE | Backup.java | 已修复（try-with-resources） |
| 5 | ImageReceiverActivity InputStream 泄漏 | ImageReceiverActivity.java | 已修复 |
| 6 | NotionSyncManager isSyncing 线程安全 | NotionSyncManager.java | 已修复（volatile） |
| 7 | OpenAiRecognizer Anthropic API 格式错误 | OpenAiRecognizer.java | 已修复 |
| 8 | BackupFileViewModel 主线程阻塞 | BackupFileViewModel.java | 已修复 |
| 9 | Backup.writeCsvFile/writeExcelFile 资源泄漏 | Backup.java | 已修复（try-with-resources） |
| 10 | NotionApiClient response.body() 空指针 | NotionApiClient.java | 已修复（null 检查） |
| 11 | MineDatabase 锁对象错误 | MineDatabase.java | 已修复（用 MineDatabase.class） |
| 12 | TallyDatabase/MineDatabase sInstance 非 volatile | 两个 Database 类 | 已修复（volatile） |

---

## 建议改进项（需确认后处理）

### ⚠️ 已决定：短信识别功能整体砍掉

**用户明确要求：短信识别功能（SmsReceiver + 相关模块）直接移除，不再维护。**

涉及文件：
- `SmsReceiver.java` - 整个文件删除
- `ImageReceiverActivity.java` - 整个文件删除（短信/图片识别入口）
- 相关的权限声明、BroadcastReceiver 注册等

**以下短信/图片识别相关问题全部跳过，不再处理：**
- #1 SmsReceiver ANR 风险 → 跳过（功能砍掉）
- #2 ImageReceiverActivity finish() 提前调用 → 跳过（功能砍掉）
- #3 ImageReceiverActivity ExecutorService 未关闭 → 跳过（功能砍掉）
- #36 ImageReceiverActivity ExecutorService 未关闭 → 跳过（功能砍掉）
- #37 ImageReceiverActivity Toast Context → 跳过（功能砍掉）
- #21 SmsMessage.createFromPdu 弃用 → 跳过（功能砍掉）

---

### 高优先级（剩余待确认）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| 2 | `ImageReceiverActivity.java:59-68` | finish() 在异步任务完成前调用 | ⚠️ 跳过（功能砍掉） |
| 3 | `ImageReceiverActivity.java:93-114` | ExecutorService 从未关闭 | ⚠️ 跳过（功能砍掉） |
| 4 | `HomeRepository.java:145-168` | IO 线程写、UI 线程读，无同步 | ✅ 需修复 |
| 5 | `TallyChartRepository.java:33-39` | 所有回调在 IO 线程执行 | ✅ 需修复 |
| 6 | `TallyChartRepository.java:241-245` | queryMonthList 方法体为空 | ✅ 需修复 |
| 7 | `RecordsRepository.java:31-53` | queryRecords 回调在 IO 线程 | ✅ 需修复 |
| 8 | `SettingViewModel.java:260-262` | clearAllRecords 回调在后台线程 | ✅ 需修复 |
| 9 | `TallyChartViewModel.java:247-282` | init() 异步与 refreshData() 竞争 | ✅ 需修复 |
| 10 | `PresenterImpl.java:144` | 无效用户操作抛 RuntimeException | ✅ 需修复 |
| 11 | `LatestVersionFetcher.java:39` | HTTP（非 HTTPS）检查更新 | ✅ 需修复 |
| 12 | `OpenAiRecognizer.java:153-158` | testConnection 不支持 Anthropic | ✅ 需修复（做 Anthropic 兼容） |

### 中等优先级

| # | 文件 | 问题 | 状态 |
|---|------|------|------|
| 13 | `build.gradle:27-29` | targetSdkVersion 26，远低于 Play Store 要求（33+） | ✅ 需修复（升级 targetSdk） |
| 14 | `build.gradle:86-91` | 签名密钥硬编码在版本控制中 | ⚠️ 跳过（用户不关心） |
| 15 | `RecordDao.java:268-291` | GROUP BY 查询中 SELECT 非聚合列 record_time | ✅ 需修复 |
| 16 | `RecordDao.java:91-96` | 空数组传入 IN 子句导致 SQL 语法错误 | ✅ 需修复 |
| 17 | `CsvImporter.java:104-107` | 数据库插入不在事务中，失败时部分插入 | ✅ 需修复 |
| 18 | `SensitiveDataCipher.java:54-57,79-82` | 加密/解密失败静默返回空字符串，丢失用户数据 | ✅ 需修复 |
| 19 | `NotionConfig.java:50-53` | 解密失败时 token 静默丢失 | ✅ 需修复 |
| 20 | `BackupFileViewModel.java:99` | WorkManager.get() 主线程阻塞 | ✅ 需修复 |
| 21 | `BackupFileViewModel.java:633-641` | 导出完成提示在导出实际完成前显示 | ✅ 需修复 |
| 22 | `DefaultWorkExecutor.java:12` | KEEP_ALIVE_TIME 为 1 毫秒（单位错误） | ✅ 需修复 |
| 23 | `AndroidUtils.java:23-28` | 已弃用 API（getDeviceId），API 29+ 抛 SecurityException | ⚠️ 跳过（用户不关心） |
| 24 | `AudioRecordManager.java:56-61` | AudioRecord 创建时未检查初始化状态 | ✅ 需修复 |
| 25 | `AudioRecordManager.java:26-53` | isStart 标志未声明 volatile，线程间数据竞争 | ✅ 需修复 |
| 26 | `PermissionReqHandler.java:145-149` | 强制请求模式下可能无限循环 | ✅ 需修复 |
| 27 | `SearchRepository.java:40-41` | 用户输入的 SQL 通配符（%、_）未转义 | ✅ 需修复 |

### 低优先级

| # | 文件 | 问题 | 状态 |
|---|------|------|------|
| 28 | `TallyDatabase.java:68` | allowMainThreadQueries() 允许主线程数据库操作 | ✅ 需修复 |
| 29 | `Record.java:46` | double 用于货币计算有浮点精度问题 | ✅ 需修复 |
| 30 | `HomeAdapter.java:27` | Adapter 持有 Activity 强引用 | ✅ 需修复 |
| 31 | `SearchViewModel.java:53` | `new Handler()` 未指定 Looper（API 30+ 弃用） | ✅ 需修复 |
| 32 | `build.gradle:53` | fastjson 1.x 有已知 CVE 漏洞 | ✅ 需修复 |
| 33 | `build.gradle:58` | Retrofit 2.0.0 版本过旧（2016 年） | ✅ 需修复 |
| 34 | `RecordsActivity.java:151-162` | getToolbarTitle() 重复注册 Observer | ✅ 需修复 |
| 35 | `KeyValue.java:17` | 主键列上创建了冗余索引 | ✅ 需修复 |

---

## 统计（累计）

| 严重程度 | 发现数量 | 已修复 | 跳过 | 待修复 |
|---------|---------|--------|------|--------|
| 严重    | 12      | 12     | 0    | 0      |
| 高      | 23      | 22     | 4    | 12     |
| 中等    | 20      | 5      | 2    | 18     |
| 低/建议 | 20      | 0      | 0    | 10     |
| **合计** | **75** | **39** | **6** | **40** |

**短信识别功能砍掉：** 6 项跳过（#1, #2, #3, #21, #36, #37）
**用户不关心：** 2 项跳过（#14 签名密钥, #23 设备ID）
**需修复：** 40 项待处理

---

## 第三轮修复的 Bug（18 项）

### 16. [严重] ImageReceiverActivity - finish() 在异步任务完成前调用
**文件:** `app/.../module/auto/ImageReceiverActivity.java:69`
**问题:** `finish()` 在 `onCreate()` 末尾无条件调用，后台线程仍在运行 `recognize()` 和数据库操作。Activity 销毁后 Toast 引用已销毁的 Context。
**修复:** 移除提前的 `finish()`，仅在 executor 的 `finally` 块中调用。

### 17. [严重] ImageReceiverActivity/SmsReceiver - Math.abs 静态导入截断 double 为 int
**文件:** `ImageReceiverActivity.java:3`, `SmsReceiver.java:4`
**问题:** `import static java.lang.Math.abs` 导入的是 `int` 重载。`amount` 返回 `double`，`abs(amount)` 静默截断为 int（如 123.45 变为 123）。
**修复:** 移除静态导入，使用 `Math.abs()` 限定名（解析为 `double` 重载）。

### 18. [高] HomeViewModel - mRefreshing 从未设为 true
**文件:** `app/.../module/home/HomeViewModel.java:95-98`
**问题:** `refresh()` 检查 `mRefreshing` 防止重复刷新，但从未设为 `true`。每次 EventBus 事件都触发完整重载。
**修复:** 在 guard 检查后添加 `mRefreshing.setValue(true)`。

### 19. [严重] RecordDetailViewModel - EventBus POSTING 模式在后台线程调用 setValue
**文件:** `app/.../module/detail/RecordDetailViewModel.java:161`
**问题:** `@Subscribe(threadMode = ThreadMode.POSTING)` 在 EventBus 发布线程执行。调用 `refreshData()` 中的 `mRecordData.setValue()` 会抛 `IllegalStateException`（setValue 必须在主线程）。
**修复:** 改为 `@Subscribe(threadMode = ThreadMode.MAIN)`。

### 20. [高] SettingActivity - mToolbar 遮蔽父类字段
**文件:** `app/.../module/setting/SettingActivity.java:63`
**问题:** `private Toolbar mToolbar` 遮蔽 `BaseActivity` 的 `protected Toolbar mToolbar`。父类的 `getToolbar()` 始终重新查找。
**修复:** 移除本地声明，使用继承的字段。

### 21. [严重] SmsReceiver - 已弃用的 SmsMessage.createFromPdu
**文件:** `app/.../module/auto/SmsReceiver.java:68`
**问题:** `createFromPdu(byte[])` 自 API 23 弃用，在多 SIM 卡设备上可能产生乱码。
**修复:** API 23+ 使用 `createFromPdu(byte[], intent.getStringExtra("format"))`。

### 22. [严重] BackupFileViewModel - onActivityResult 空指针
**文件:** `app/.../module/backup/BackupFileViewModel.java:733`
**问题:** `data.getData()` 未检查 `data` 是否为 null。Android 文档明确 data 可为 null。
**修复:** 添加 `data != null` 和 `uri == null` 检查。

### 23. [高] BackupFileManagerViewModel - deleteBackupFile 不通知 LiveData 观察者
**文件:** `app/.../module/backup/BackupFileManagerViewModel.java:156-158`
**问题:** `ArrayUtils.remove` 原地修改列表，`setValue` 传入同一对象引用。LiveData 比较引用相同则跳过分发。
**修复:** 删除后创建新列表再 `setValue`。

### 24. [高] CsvImporter - 数据库插入不在事务中
**文件:** `app/.../module/backup/CsvImporter.java:104-106`
**问题:** `database.recordDao().insert(insertArray)` 未包裹事务。中途失败导致部分插入。
**修复:** 包裹在 `database.runInTransaction()` 中。

### 25. [严重] NotionSyncManager - isSyncing 竞态条件
**文件:** `app/.../sync/NotionSyncManager.java:87-97`
**问题:** `volatile boolean` 仅保证可见性，不保证 check-then-act 原子性。两个并发调用可同时通过 guard。
**修复:** 替换为 `AtomicBoolean.compareAndSet(false, true)`。

### 26. [高] NotionSyncManager - e.getMessage() 可能为 null
**文件:** `app/.../sync/NotionSyncManager.java:136-141`
**问题:** NPE 等异常的 `getMessage()` 返回 null。null 传入 `onSyncError(null)` 导致下游 NPE。
**修复:** 使用 `e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()`。

### 27. [高] NotionSyncManager - lastModified 使用交易时间而非 Notion last_edited_time
**文件:** `app/.../sync/NotionSyncManager.java:348`
**问题:** `record.lastModified = record.time` 设置为交易日期而非实际修改日期，破坏冲突解决。
**修复:** 使用 Notion 页面对象的 `last_edited_time`。

### 28. [严重] CategoryEntity - @NonNull 字段无默认值
**文件:** `app/.../persistence/sql/entity/CategoryEntity.java:28`
**问题:** `uniqueName` 标注 `@NonNull` 但无默认值。未调用 setter 时运行时为 null。
**修复:** 初始化为 `private String uniqueName = ""`。

### 29. [高] RecordDao - queryFirst() 缺少 category JOIN
**文件:** `app/.../persistence/sql/dao/RecordDao.java:298-300`
**问题:** 查询未 JOIN category 表，`categoryName` 和 `categoryIcon` 始终为 null。
**修复:** 添加 `left outer join category`。

### 30. [高] TallyChartRepository - queryMonthList 方法体为空
**文件:** `app/.../module/chart/TallyChartRepository.java:241-245`
**问题:** 回调永远不会执行，调用方永久挂起。
**修复:** 实现查询逻辑。

### 31. [高] GiteeSourceFetcher/GitHubSourceFetcher - HTTP Response 资源泄漏
**文件:** `GiteeSourceFetcher.java:109`, `GitHubSourceFetcher.java:64`
**问题:** Retrofit `execute()` 返回的 Response 从未关闭，泄漏 OkHttp 连接。
**修复:** 添加 `finally` 块关闭 response。

### 32. [高] DownloadService - 进度回调除零错误
**文件:** `libupdate/.../DownloadService.java:98`
**问题:** `bytesRead / contentLength` 在 `contentLength` 为 0 时产生 Infinity/NaN。
**修复:** 添加 `if (contentLength > 0)` 防护。

### 33. [高] SearchViewModel - Handler 未指定 Looper
**文件:** `app/.../module/search/SearchViewModel.java:53`
**问题:** `new Handler()` 隐式使用当前线程 Looper。API 30+ 弃用。
**修复:** 改为 `new Handler(Looper.getMainLooper())`。

### 34. [高] HomeAdapter - areItemsTheSame 空指针风险
**文件:** `app/.../module/home/HomeAdapter.java:72-74`
**问题:** `getInternal()` 强转为 Record 无 null 检查。
**修复:** 添加 null guard。

### 35. [高] RecordsViewModel - setQuery 与 postValue 竞态
**文件:** `app/.../module/records/RecordsViewModel.java:126-135`
**问题:** `postValue` 异步，`getValue()` 可能返回旧列表。load/refresh 条件逻辑不可靠。
**修复:** 改用 `setValue`（同步），始终调用 `refresh()`。

### 36. [高] ImageReceiverActivity - ExecutorService 未关闭
**文件:** `app/.../module/auto/ImageReceiverActivity.java:93`
**问题:** 每次调用创建新 executor 但从不关闭，线程累积。
**修复:** 在 `finally` 块中添加 `executor.shutdown()`。

### 37. [高] ImageReceiverActivity - Toast 使用 Activity Context
**文件:** `app/.../module/auto/ImageReceiverActivity.java:212`
**问题:** Activity 已 finish 后 Toast 仍使用 Activity Context。
**修复:** 改为 `MineApp.getAppContext()`。

---

## 第三轮新增建议改进项

| # | 文件 | 问题 | 优先级 | 状态 |
|---|------|------|--------|------|
| 38 | `SyncRepository.java:26-64` | 所有数据库操作静默吞异常，计数不准 | 高 | ✅ 需修复 |
| 39 | `RecordConverter.java:85` | syncId 被覆盖为原始记录 ID，可能冲突 | 高 | ✅ 需修复 |
| 40 | `BackupCache.java:47-48` | SimpleDateFormat 线程不安全 | 中 | ✅ 需修复 |
| 41 | `Backup.java:355-358` | restoreExpenseTable 设置 null categoryUniqueName | 中 | ✅ 需修复 |
| 42 | `AutoBackupWorker.java:38-48` | 后台 Worker 显示 Toast（Android 11+ 受限） | 中 | ✅ 需修复 |
| 43 | `HomeRepository.java:144-168` | IO 线程写、UI 线程读无同步 | 高 | ✅ 需修复 |
| 44 | `SettingViewModel.java:256-285` | clearAllRecords 回调在后台线程 | 高 | ✅ 需修复 |
| 45 | `BackupFileViewModel.java:568-569` | null Long 自动拆箱 NPE | 中 | ✅ 需修复 |
| 46 | `TallyDatabase.java:114` | ContentValues 容量不匹配 | 低 | ✅ 需修复 |
| 47 | `NotionApiClient.java:56` | 每次调用创建新 Gson 实例 | 低 | ✅ 需修复 |
| 48 | `SensitiveDataCipher.java:85-101` | 弱密钥派生（ANDROID_ID 可预测） | 高 | ✅ 需修复 |
| 49 | `SensitiveDataCipher.java:36-57` | 加密失败静默返回空字符串，丢失 token | 高 | ✅ 需修复 |
| 50 | `OpenAiRecognizer.java:108` | 无 HTTPS 强制验证 | 中 | ✅ 需修复 |
| 51 | `Backup.java:495-523` | CSV 注入风险 | 中 | ✅ 需修复 |

---

## 第三轮修改的文件清单

```
app/src/main/java/com/coderpage/mine/app/tally/module/auto/ImageReceiverActivity.java
app/src/main/java/com/coderpage/mine/app/tally/module/home/HomeViewModel.java
app/src/main/java/com/coderpage/mine/app/tally/module/detail/RecordDetailViewModel.java
app/src/main/java/com/coderpage/mine/app/tally/module/setting/SettingActivity.java
app/src/main/java/com/coderpage/mine/app/tally/module/auto/SmsReceiver.java
app/src/main/java/com/coderpage/mine/app/tally/module/backup/BackupFileViewModel.java
app/src/main/java/com/coderpage/mine/app/tally/module/backup/BackupFileManagerViewModel.java
app/src/main/java/com/coderpage/mine/app/tally/module/backup/CsvImporter.java
app/src/main/java/com/coderpage/mine/app/tally/sync/NotionSyncManager.java
app/src/main/java/com/coderpage/mine/app/tally/persistence/sql/entity/CategoryEntity.java
app/src/main/java/com/coderpage/mine/app/tally/persistence/sql/dao/RecordDao.java
app/src/main/java/com/coderpage/mine/app/tally/module/chart/TallyChartRepository.java
app/src/main/java/com/coderpage/mine/app/tally/update/GiteeSourceFetcher.java
app/src/main/java/com/coderpage/mine/app/tally/update/GitHubSourceFetcher.java
libupdate/src/main/java/com/coderpage/lib/update/DownloadService.java
app/src/main/java/com/coderpage/mine/app/tally/module/search/SearchViewModel.java
app/src/main/java/com/coderpage/mine/app/tally/module/home/HomeAdapter.java
app/src/main/java/com/coderpage/mine/app/tally/module/records/RecordsViewModel.java
```
