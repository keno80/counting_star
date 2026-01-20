package com.countingstar.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGreetingUseCase @Inject constructor(
    private val repository: GreetingRepository,
) {
    operator fun invoke(): Flow<String> = repository.greetingFlow()
}
