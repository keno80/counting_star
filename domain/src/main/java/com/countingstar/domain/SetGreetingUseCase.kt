package com.countingstar.domain

import javax.inject.Inject

class SetGreetingUseCase @Inject constructor(
    private val repository: GreetingRepository,
) {
    suspend operator fun invoke(text: String) {
        repository.setGreeting(text)
    }
}
