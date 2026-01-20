package com.countingstar.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countingstar.domain.GetGreetingUseCase
import com.countingstar.domain.SetGreetingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getGreetingUseCase: GetGreetingUseCase,
        private val setGreetingUseCase: SetGreetingUseCase,
    ) : ViewModel() {
        val greeting: StateFlow<String> =
            getGreetingUseCase().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                "Counting Star",
            )
    }
