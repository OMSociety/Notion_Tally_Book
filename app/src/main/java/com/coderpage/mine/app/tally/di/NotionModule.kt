package com.coderpage.mine.app.tally.di

import com.coderpage.mine.app.tally.sync.NotionApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Notion API 依赖注入模块
 * 
 * Phase C: 使用 Hilt 进行依赖注入
 * 
 * Before:
 * ```java
 * // 直接实例化（违反 DI 原则）
 * NotionApiClient client = new NotionApiClient();
 * ```
 * 
 * After:
 * ```kotlin
 * @Inject lateinit var client: NotionApiClient
 * ```
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object NotionModule {
    
    @Provides
    @Singleton
    fun provideNotionApiClient(): NotionApiClient {
        return NotionApiClient()
    }
}
