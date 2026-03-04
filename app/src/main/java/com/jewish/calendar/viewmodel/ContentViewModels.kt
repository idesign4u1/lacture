package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BlessingsViewModel @Inject constructor(
    repo: ContentRepository
) : ViewModel() {
    val blessings: StateFlow<List<BlessingContent>> = repo.blessingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class SpecialPrayersViewModel @Inject constructor(
    repo: ContentRepository
) : ViewModel() {
    val prayers: StateFlow<List<SpecialPrayerContent>> = repo.specialPrayersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class ShalomBayitViewModel @Inject constructor(
    repo: ContentRepository
) : ViewModel() {
    val tips: StateFlow<List<ShalomTipContent>> = repo.shalomTipsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val verses: StateFlow<List<String>> = repo.shalomVersesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class ChallaViewModel @Inject constructor(
    repo: ContentRepository
) : ViewModel() {
    val steps: StateFlow<List<ChallaStepContent>> = repo.challaStepsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recipes: StateFlow<List<ChallaRecipeContent>> = repo.challaRecipesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
