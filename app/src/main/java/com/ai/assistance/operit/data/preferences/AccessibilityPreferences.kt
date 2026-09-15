package com.ai.assistance.operit.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 无障碍 / 老年人模式偏好。
 *
 * 与系统主题/字体偏好解耦，独立存储，便于在主题层叠加：
 *  - [elderlyModeEnabled]：老年人/无障碍总开关（开启后默认放大字号）
 *  - [fontScalePercent]：无障碍字号（100~160，100 表示不额外放大）
 *  - [highContrastEnabled]：高对比配色（纯黑/白文字 + 高对比表面）
 *
 * 主界面与悬浮窗都使用 OperitTheme，因此此处的改动会同时作用于对话页和悬浮窗。
 */
class AccessibilityPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _elderlyModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_ELDERLY, false))
    val elderlyModeEnabled: StateFlow<Boolean> = _elderlyModeEnabled.asStateFlow()

    private val _fontScalePercent =
            MutableStateFlow(prefs.getInt(KEY_FONT_SCALE, DEFAULT_FONT_SCALE))
    val fontScalePercent: StateFlow<Int> = _fontScalePercent.asStateFlow()

    private val _highContrastEnabled = MutableStateFlow(prefs.getBoolean(KEY_HIGH_CONTRAST, false))
    val highContrastEnabled: StateFlow<Boolean> = _highContrastEnabled.asStateFlow()

    fun setElderlyModeEnabled(enabled: Boolean) {
        _elderlyModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ELDERLY, enabled).apply()
        AppLogger.d(TAG, "elderlyModeEnabled=$enabled")
    }

    /** percent: 100..160 */
    fun setFontScalePercent(percent: Int) {
        val clamped = percent.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        _fontScalePercent.value = clamped
        prefs.edit().putInt(KEY_FONT_SCALE, clamped).apply()
        AppLogger.d(TAG, "fontScalePercent=$clamped")
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        _highContrastEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        AppLogger.d(TAG, "highContrastEnabled=$enabled")
    }

    /** 实际作用于主题的字号倍率（1.0 为不放大）。老年人模式默认带 1.25 倍。 */
    fun effectiveFontScale(): Float {
        val base = if (_elderlyModeEnabled.value) DEFAULT_ELDERLY_SCALE else 1f
        val manual = _fontScalePercent.value / 100f
        // 手动字号优先；未手动调整（100）时老年人模式给默认放大
        return if (_fontScalePercent.value == DEFAULT_FONT_SCALE) base else manual
    }

    companion object {
        private const val TAG = "AccessibilityPreferences"
        private const val PREFS_NAME = "accessibility_preferences"
        private const val KEY_ELDERLY = "elderly_mode_enabled"
        private const val KEY_FONT_SCALE = "accessibility_font_scale_percent"
        private const val KEY_HIGH_CONTRAST = "high_contrast_enabled"

        const val DEFAULT_FONT_SCALE = 100
        const val MIN_FONT_SCALE = 100
        const val MAX_FONT_SCALE = 160
        private const val DEFAULT_ELDERLY_SCALE = 1.25f

        @Volatile private var instance: AccessibilityPreferences? = null

        fun getInstance(context: Context): AccessibilityPreferences {
            return instance ?: synchronized(this) {
                instance ?: AccessibilityPreferences(context).also { instance = it }
            }
        }
    }
}
