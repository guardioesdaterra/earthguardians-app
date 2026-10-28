package org.earthguardians.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Earth Guardians palette.
 *
 * Brand rule (black / white / red / green only):
 *  - red   = action, urgency, "take action now"
 *  - green = nature, progress, positive impact
 *  - black/white surfaces, everything else is translucent glass over them.
 */
@Immutable
data class EGPalette(
    val isDark: Boolean,
    val background: Color,
    val onBackground: Color,
    val muted: Color,
    val outline: Color,
    val glass: Color,
    val glassAlt: Color,
    val glassStroke: Color,
    val glassHighlight: Color,
    val accent: Color,
    val onAccent: Color,
    val eco: Color,
    val onEco: Color,
) {
    companion object {
        fun light(): EGPalette = EGPalette(
            isDark = false,
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF050505),
            muted = Color(0xFF6E6E73), // iOS secondaryLabel gray
            outline = Color(0xFFE3E3E8),
            glass = Color(0xFFF2F2F7), // iOS grouped-background fill: cards read on white
            glassAlt = Color(0xFFF9F9FB),
            glassStroke = Color(0x24808085),
            glassHighlight = Color(0x3DFFFFFF),
            accent = Color(0xFFFF3B30), // iOS system red
            onAccent = Color(0xFFFFFFFF),
            eco = Color(0xFF34C759), // iOS system green
            onEco = Color(0xFFFFFFFF),
        )

        fun dark(): EGPalette = EGPalette(
            isDark = true,
            background = Color(0xFF000000),
            onBackground = Color(0xFFFAFAFA),
            muted = Color(0xFF98989F),
            outline = Color(0xFF2C2C30),
            glass = Color(0xFF1C1C1E), // iOS elevated dark fill: cards read on black
            glassAlt = Color(0xFF111114),
            glassStroke = Color(0x26FFFFFF),
            glassHighlight = Color(0x14FFFFFF),
            accent = Color(0xFFFF453A), // iOS dark-mode red
            onAccent = Color(0xFFFFFFFF),
            eco = Color(0xFF30D158), // iOS dark-mode green
            onEco = Color(0xFF050505),
        )
    }
}

val LocalEGPalette = staticCompositionLocalOf<EGPalette> { error("EGPalette not provided") }
val LocalIsDark = staticCompositionLocalOf { false }

val egPalette: EGPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalEGPalette.current

enum class ThemeMode(val label: String, val hint: String) {
    System("Automatic", "Follows device setting"),
    Light("Light", "Always light"),
    Dark("Dark", "Always dark"),
}

private val Context.egSettingsStore by preferencesDataStore(name = "eg_settings")

class ThemeRepository(private val context: Context) {

    /** Emits the persisted theme mode; System until the user chooses otherwise. */
    val mode: Flow<ThemeMode> = context.egSettingsStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            ThemeMode.entries.firstOrNull { it.name == prefs[KEY_MODE] } ?: ThemeMode.System
        }

    suspend fun setMode(mode: ThemeMode) {
        context.egSettingsStore.edit { it[KEY_MODE] = mode.name }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("theme_mode")
    }
}

@Composable
fun EGAppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (isDark) EGPalette.dark() else EGPalette.light()
    val scheme = if (isDark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.eco,
            tertiary = palette.onBackground,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.glassAlt,
            onSurface = palette.onBackground,
            surfaceVariant = palette.glassAlt,
            onSurfaceVariant = palette.muted,
            outline = palette.outline,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.eco,
            tertiary = palette.onBackground,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.glassAlt,
            onSurface = palette.onBackground,
            surfaceVariant = palette.glassAlt,
            onSurfaceVariant = palette.muted,
            outline = palette.outline,
        )
    }

    KeepSystemBarsInSync(isDark)

    CompositionLocalProvider(
        LocalEGPalette provides palette,
        LocalIsDark provides isDark,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = egTypography(),
            shapes = egShapes(),
            content = content,
        )
    }
}

/** Status/navigation bar icon tint follows the palette like an iOS app. */
@Composable
private fun KeepSystemBarsInSync(isDark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context.findActivity()
        val window = activity?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** iOS-flavored type ramp (system sans, SF-like weights). */
fun egTypography() = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

fun egShapes() = ShapeDefaults.copy(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)
