package com.countingstar.core.state

interface UiState

interface UiEvent

interface UiEffect

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>

    data class Error(val error: AppError) : Result<Nothing>
}

sealed class AppError(open val userMessage: String, open val errorCode: String) {
    data class Network(
        override val userMessage: String,
        override val errorCode: String = "NETWORK",
    ) : AppError(userMessage, errorCode)

    data class Auth(
        override val userMessage: String,
        override val errorCode: String = "AUTH",
    ) : AppError(userMessage, errorCode)

    data class NotFound(
        override val userMessage: String,
        override val errorCode: String = "NOT_FOUND",
    ) : AppError(userMessage, errorCode)

    data class Validation(
        override val userMessage: String,
        override val errorCode: String = "VALIDATION",
    ) : AppError(userMessage, errorCode)

    data class Database(
        override val userMessage: String,
        override val errorCode: String = "DATABASE",
    ) : AppError(userMessage, errorCode)

    data class Unknown(
        override val userMessage: String,
        override val errorCode: String = "UNKNOWN",
    ) : AppError(userMessage, errorCode)
}

fun Throwable.toAppError(
    userMessage: String = message ?: "Unknown error",
): AppError {
    return when (this) {
        is IllegalArgumentException -> AppError.Validation(userMessage)
        is java.io.IOException -> AppError.Network(userMessage)
        else -> AppError.Unknown(userMessage)
    }
}
