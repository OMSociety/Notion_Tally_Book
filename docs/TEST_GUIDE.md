# 单元测试指南

## 概述

本文档描述 Notion_Tally_Book 项目的单元测试架构和运行方法。

## 测试目录结构

```
app/src/
├── test/                    # 单元测试 (JVM)
│   └── java/
│       └── com/coderpage/mine/app/tally/
│           ├── persistence/
│           │   ├── model/
│           │   │   ├── RecordTest.java
│           │   │   └── CategoryModelTest.java
│           │   └── sql/
│           │       └── entity/
│           │           ├── RecordEntityTest.java
│           │           └── SyncHistoryEntityTest.java
│           ├── sync/
│           │   └── SyncHistoryManagerTest.java
│           ├── network/
│           │   └── RequestInterceptorTest.java
│           └── utils/
│               ├── TimeUtilsTest.java
│               └── DateUtilsTest.java
│
└── androidTest/             # Android 仪器化测试
    └── java/
```

## 测试依赖

项目使用以下测试库：

- **JUnit 4** - 基础测试框架
- **Mockito** - Mock 框架，用于隔离依赖
- **AndroidX Test** - Android 测试支持库

## 运行测试

### 运行所有单元测试

```bash
./gradlew test
```

### 运行特定模块测试

```bash
./gradlew testDebugUnitTest                    # Debug 变体
./gradlew testReleaseUnitTest                 # Release 变体
```

### 运行特定测试类

```bash
./gradlew testDebugUnitTest --tests "com.coderpage.mine.app.tally.utils.TimeUtilsTest"
```

### 运行特定测试方法

```bash
./gradlew testDebugUnitTest --tests "com.coderpage.mine.app.tally.utils.TimeUtilsTest.testGetDaysTotalOfMonthFebruary"
```

### 查看测试报告

```bash
# HTML 报告
open app/build/reports/tests/testDebugUnitTest/index.html

# XML 结果
cat app/build/test-results/testDebugUnitTest/*.xml
```

## 测试覆盖率

生成覆盖率报告：

```bash
./gradlew testDebugUnitTest jacocoTestReport
open app/build/reports/jacoco/testDebugUnitTest/index.html
```

## 编写新测试

### 基本测试结构

```java
package com.coderpage.mine.app.tally.module;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class YourClassTest {

    private YourClass instance;

    @Before
    public void setUp() {
        instance = new YourClass();
    }

    @Test
    public void testYourMethod() {
        // 测试逻辑
        assertEquals(expected, instance.yourMethod(input));
    }

    @Test(expected = Exception.class)
    public void testException() {
        instance.methodThatThrows();
    }
}
```

### 使用 Mockito Mock 依赖

```java
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YourClassWithDepsTest {

    @Mock
    private DependencyA mockDepA;

    @Mock
    private DependencyB mockDepB;

    @Test
    public void testWithMocks() {
        when(mockDepA.getValue()).thenReturn("mocked");
        // ...
    }
}
```

## 测试原则

1. **独立性** - 每个测试应该独立运行，不依赖其他测试
2. **可重复性** - 相同的测试应该产生相同的结果
3. **快速执行** - 单元测试应该在毫秒级别完成
4. **清晰命名** - 测试方法名应该清晰表达测试内容
5. **单一职责** - 每个测试只验证一个行为

## CI/CD 集成

测试已在 CI 中自动运行：

```yaml
# .github/workflows/android.yml
- name: Run unit tests
  run: ./gradlew testDebugUnitTest
```

## 覆盖的模块

| 模块 | 测试类 | 说明 |
|------|--------|------|
| persistence/model | RecordTest | 记账记录模型 |
| persistence/model | CategoryModelTest | 分类模型 |
| persistence/entity | RecordEntityTest | 数据库实体 |
| persistence/entity | SyncHistoryEntityTest | 同步历史实体 |
| sync | SyncHistoryManagerTest | 同步历史管理 |
| network | RequestInterceptorTest | 网络请求拦截器 |
| utils | TimeUtilsTest | 时间工具 |
| utils | DateUtilsTest | 日期工具 |
