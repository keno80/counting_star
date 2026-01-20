package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

interface GreetingRepository {
    fun greetingFlow(): Flow<String>

    suspend fun setGreeting(text: String)
}
