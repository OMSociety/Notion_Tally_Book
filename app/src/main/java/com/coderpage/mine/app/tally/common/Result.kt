package com.coderpage.mine.app.tally.common

/**
 * 密封结果类 - 用于包装 Notion API 调用结果
 * 
 * Phase C: 统一错误处理，避免异常被吞没
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
sealed class Result<out T> {
    
    data class Success<T>(val data: T) : Result<T>()
    
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error",
        val code: Int = -1
    ) : Result<Nothing>()
    
    object Loading : Result<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }
    
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable, code: Int = -1): Result<Nothing> = Error(exception, code = code)
        fun loading(): Result<Nothing> = Loading
    }
}
