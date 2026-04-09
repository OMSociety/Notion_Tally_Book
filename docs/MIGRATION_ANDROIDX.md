# AndroidX & Gradle 迁移指南

## 迁移状态

| 组件 | 当前版本 | 目标版本 | 状态 |
|------|----------|----------|------|
| Gradle | 5.1.1 | 8.4 | ✅ 完成 |
| AGP | 3.4.0 | 8.2.0 | ✅ 完成 |
| compileSdkVersion | 27 | 34 | ✅ 完成 |
| buildToolsVersion | 27.0.3 | - | ✅ 已移除 |
| minSdkVersion | 18 | 21 | ✅ 完成 |

## 依赖迁移状态

| 依赖 | 旧版本 | 新版本 | 状态 |
|------|--------|--------|------|
| Support Library | 27.1.1 | AndroidX | ✅ 完成 |
| Room | 1.1.1 | 2.6.1 | ✅ 完成 |
| Lifecycle | 1.1.1 | 2.6.2 | ✅ 完成 |
| Retrofit | 2.0.0 | 2.9.0 | ✅ 完成 |
| OkHttp | 3.2.0 | 4.12.0 | ✅ 完成 |

## 待手动修改清单

### 1. Java 源码迁移（Support Library → AndroidX）

需要全局替换：

```bash
# 包名替换
android.arch.persistence.room → androidx.room
android.arch.lifecycle → androidx.lifecycle
android.arch.work → androidx.work
android.support.v4 → androidx.core.content
android.support.annotation → androidx.annotation
android.support.design → com.google.android.material
android.support.v7.appcompat → androidx.appcompat.app
android.support.v7.widget → androidx.recyclerview.widget
android.support.constraint.ConstraintLayout → androidx.constraintlayout.widget.ConstraintLayout
```

### 2. Kotlin 源码迁移

```kotlin
// 旧
import android.arch.persistence.room.*

// 新
import androidx.room.*
```

### 3. XML 布局文件迁移

```xml
<!-- 旧 -->
<android.support.design.widget.CoordinatorLayout>

<!-- 新 -->
<androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 4. 测试配置迁移

```groovy
// 旧
android.support.test.runner.AndroidJUnitRunner

// 新
androidx.test.runner.AndroidJUnitRunner
```

## 验证步骤

1. ✅ 构建文件已更新（Gradle 8.4, AGP 8.2.0）
2. ⏳ 源码迁移完成
3. ⏳ 布局 XML 迁移完成
4. ⏳ 本地构建测试通过
5. ⏳ 单元测试通过

## 已知问题

### 问题 1: ARouter 兼容性

ARouter 1.5.x 已支持 AndroidX，如遇问题考虑升级到 1.5.2

### 问题 2: Fastjson 兼容性

新版本 Fastjson2 与旧 API 有差异，如遇序列化问题检查 import 语句

### 问题 3: WorkManager

迁移到 androidx.work 需要更新 Worker 实现类

## 快速开始

```bash
# 1. 安装 JDK 17
# 2. 清理 Gradle 缓存
rm -rf ~/.gradle/caches/transforms-*

# 3. 同步并构建
./gradlew clean assembleDebug

# 4. 运行单元测试
./gradlew test
```

## 回滚方案

如遇无法解决的问题，可以回滚到迁移前的分支：

```bash
git checkout main
git branch -D upgrade/androidx-and-gradle
```
