package com.example.i18n

import com.example.data.local.PreferencesStore
import com.example.ui.screens.games.GameUiStrings
import com.example.ui.screens.games.gameStringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Central LanguageManager singleton enabling instantaneous dynamic toggling
 * between Bangla and English across all UI components and game screens without
 * requiring an app or activity restart, backed by PreferencesStore persistence.
 */
object LanguageManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var prefsStore: PreferencesStore? = null

    @Volatile
    private var syncJob: Job? = null

    private val _currentLanguage = MutableStateFlow(AppLanguage.BANGLA)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _appStrings = MutableStateFlow(stringsFor(AppLanguage.BANGLA))
    val appStrings: StateFlow<AppStrings> = _appStrings.asStateFlow()

    private val _gameStrings = MutableStateFlow(gameStringsFor(AppLanguage.BANGLA))
    val gameStrings: StateFlow<GameUiStrings> = _gameStrings.asStateFlow()

    /**
     * Binds LanguageManager to DataStore preferences once during Application startup
     * so persisted language (including backup restores) stays reactively synchronized.
     */
    fun bind(preferencesStore: PreferencesStore) {
        synchronized(this) {
            if (prefsStore === preferencesStore && syncJob?.isActive == true) return
            prefsStore = preferencesStore
            syncJob?.cancel()
            syncJob = scope.launch {
                preferencesStore.state
                    .map { it.language }
                    .distinctUntilChanged()
                    .collect { persisted ->
                        applyInMemory(persisted)
                    }
            }
        }
    }

    /**
     * Immediately updates all UI and game strings in memory (zero-restart) and persists
     * asynchronously to DataStore when bound.
     */
    fun setLanguage(language: AppLanguage) {
        applyInMemory(language)
        val store = prefsStore
        if (store != null) {
            scope.launch {
                runCatching { store.setLanguage(language) }
            }
        }
    }

    /**
     * Toggles dynamically between Bangla and English and returns the newly active language.
     */
    fun toggleLanguage(): AppLanguage {
        val next = if (_currentLanguage.value == AppLanguage.BANGLA) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.BANGLA
        }
        setLanguage(next)
        return next
    }

    /**
     * Returns a localized string pair immediately using the active language.
     */
    fun localized(bn: String, en: String): String =
        if (_currentLanguage.value == AppLanguage.BANGLA) bn else en

    /**
     * Helper for deterministic unit testing.
     */
    fun resetForTest(language: AppLanguage = AppLanguage.BANGLA) {
        synchronized(this) {
            syncJob?.cancel()
            syncJob = null
            prefsStore = null
            applyInMemory(language)
        }
    }

    private fun applyInMemory(language: AppLanguage) {
        if (_currentLanguage.value == language &&
            _appStrings.value === stringsFor(language)
        ) {
            return
        }
        _currentLanguage.value = language
        _appStrings.value = stringsFor(language)
        _gameStrings.value = gameStringsFor(language)
    }
}
