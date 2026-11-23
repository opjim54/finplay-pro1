package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val flagLabel: String) {
    BANGLA("bn", "বাংলা", "🇧🇩 বাংলা"),
    ENGLISH("en", "English", "🇺🇸 English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: BANGLA
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.BANGLA }

