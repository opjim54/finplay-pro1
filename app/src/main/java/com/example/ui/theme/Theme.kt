package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Immutable
data class FinanceExtendedColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val savings: Color,
    val savingsContainer: Color,
    val cardBorder: Color,
    val isDark: Boolean,
    val champagneGold: Color = ChampagneGoldLight,
    val elevatedSurface: Color = LightSurfaceVariant
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceExtendedColors(
        income = IncomeGreenLight,
        onIncome = Color.White,
        incomeContainer = Color(0xFFD1FAE5),
        expense = ExpenseRedLight,
        onExpense = Color.White,
        expenseContainer = Color(0xFFFEE2E2),
        warning = WarningAmberLight,
        warningContainer = Color(0xFFFEF3C7),
        transfer = TransferBlueLight,
        transferContainer = Color(0xFFE0F2FE),
        savings = SavingsVioletLight,
        savingsContainer = Color(0xFFEDE9FE),
        cardBorder = CardBorderLight,
        isDark = false,
        champagneGold = ChampagneGoldLight,
        elevatedSurface = LightSurfaceVariant
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = TealOnSecondaryLight,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = TealOnSecondaryContainerLight,
    tertiary = GoldTertiaryLight,
    onTertiary = GoldOnTertiaryLight,
    tertiaryContainer = GoldTertiaryContainerLight,
    onTertiaryContainer = GoldOnTertiaryContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ExpenseRedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = GoldOnTertiaryDark,
    tertiaryContainer = GoldTertiaryContainerDark,
    onTertiaryContainer = GoldOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ExpenseRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartFinanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        FinanceExtendedColors(
            income = IncomeGreenDark,
            onIncome = Color(0xFF022C22),
            incomeContainer = Color(0xFF064E3B),
            expense = ExpenseRedDark,
            onExpense = Color(0xFF450A0A),
            expenseContainer = Color(0xFF7F1D1D),
            warning = WarningAmberDark,
            warningContainer = Color(0xFF4D3507),
            transfer = TransferBlueDark,
            transferContainer = Color(0xFF0C4A6E),
            savings = SavingsVioletDark,
            savingsContainer = Color(0xFF4C1D95),
            cardBorder = CardBorderDark,
            isDark = true,
            champagneGold = ChampagneGoldDark,
            elevatedSurface = DarkSurfaceVariant
        )
    } else {
        FinanceExtendedColors(
            income = IncomeGreenLight,
            onIncome = Color.White,
            incomeContainer = Color(0xFFD2F4E6),
            expense = ExpenseRedLight,
            onExpense = Color.White,
            expenseContainer = Color(0xFFFEE2E2),
            warning = WarningAmberLight,
            warningContainer = Color(0xFFFEF3C7),
            transfer = TransferBlueLight,
            transferContainer = Color(0xFFE0F2FE),
            savings = SavingsVioletLight,
            savingsContainer = Color(0xFFEDE9FE),
            cardBorder = CardBorderLight,
            isDark = false,
            champagneGold = ChampagneGoldLight,
            elevatedSurface = LightSurfaceVariant
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
