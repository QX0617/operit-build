package com.ai.assistance.operit.data.preferences

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.elderlyModeDataStore: DataStore<androidx.datastore.preferences.core.Preferences> by
    preferencesDataStore(name = "elderly_mode")

/**
 * 老年人/无障碍模式管理器
 * 管理模式开关、权限自动检测状态和极简UI偏好
 */
class ElderlyModeManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ElderlyModeManager? = null

        fun getInstance(context: Context): ElderlyModeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ElderlyModeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val ELDERLY_MODE_ENABLED = booleanPreferencesKey("elderly_mode_enabled")
    private val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")
    private val VOICE_DOUBLE_CONFIRM = booleanPreferencesKey("voice_double_confirm")

    val elderlyModeFlow: Flow<Boolean> = context.elderlyModeDataStore.data.map { it[ELDERLY_MODE_ENABLED] ?: false }
    val ttsAutoSpeakFlow: Flow<Boolean> = context.elderlyModeDataStore.data.map { it[TTS_AUTO_SPEAK] ?: true }
    val voiceDoubleConfirmFlow: Flow<Boolean> = context.elderlyModeDataStore.data.map { it[VOICE_DOUBLE_CONFIRM] ?: true }

    suspend fun setElderlyMode(enabled: Boolean) {
        context.elderlyModeDataStore.edit { it[ELDERLY_MODE_ENABLED] = enabled }
    }

    suspend fun setTtsAutoSpeak(enabled: Boolean) {
        context.elderlyModeDataStore.edit { it[TTS_AUTO_SPEAK] = enabled }
    }

    suspend fun setVoiceDoubleConfirm(enabled: Boolean) {
        context.elderlyModeDataStore.edit { it[VOICE_DOUBLE_CONFIRM] = enabled }
    }

    /**
     * 检测当前设备可用的权限通道
     */
    fun detectPermissionChannels(): PermissionChannelInfo {
        val accessibilityEnabled = isAccessibilityEnabled()
        val shizukuAvailable = isShizukuAvailable()
        val rootAvailable = isRootAvailable()

        return PermissionChannelInfo(
            accessibilityEnabled = accessibilityEnabled,
            shizukuAvailable = shizukuAvailable,
            rootAvailable = rootAvailable,
            recommendedMode = when {
                accessibilityEnabled -> "accessibility"
                shizukuAvailable -> "shizuku"
                rootAvailable -> "root"
                else -> "none"
            }
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            // Check Shizuku service via binder
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which su")
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开无障碍设置页面
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 打开 Shizuku 应用
     */
    fun openShizukuApp() {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { context.startActivity(it) }
    }

    data class PermissionChannelInfo(
        val accessibilityEnabled: Boolean,
        val shizukuAvailable: Boolean,
        val rootAvailable: Boolean,
        val recommendedMode: String
    )
}
