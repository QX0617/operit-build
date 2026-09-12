package com.ai.assistance.operit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.data.model.MultiPermissionConfig
import com.ai.assistance.operit.data.model.PermissionLayer
import com.ai.assistance.operit.data.model.ToolPermissionPriorityOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 多权限优先级配置管理器
 *
 * 负责多权限多层启动配置的持久化、读取和更新。
 * 使用 DataStore 存储 JSON 序列化的配置。
 */
class MultiPermissionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MultiPermissionManager"
        private const val DATA_STORE_NAME = "multi_permission_config"
        private val CONFIG_KEY = stringPreferencesKey("multi_permission_config_json")

        @Volatile
        private var INSTANCE: MultiPermissionManager? = null

        fun getInstance(context: Context): MultiPermissionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MultiPermissionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _configFlow = MutableStateFlow(MultiPermissionConfig())
    val configFlow: Flow<MultiPermissionConfig> = _configFlow.asStateFlow()

    /**
     * 从 DataStore 加载配置
     */
    suspend fun loadConfig(): MultiPermissionConfig {
        val preferences = context.dataStore.data.first()
        val jsonStr = preferences[CONFIG_KEY]
        return if (jsonStr != null) {
            try {
                json.decodeFromString<MultiPermissionConfig>(jsonStr)
            } catch (e: Exception) {
                MultiPermissionConfig()
            }
        } else {
            MultiPermissionConfig()
        }.also { _configFlow.value = it }
    }

    /**
     * 保存配置到 DataStore
     */
    suspend fun saveConfig(config: MultiPermissionConfig) {
        val jsonStr = json.encodeToString(config)
        context.dataStore.edit { preferences ->
            preferences[CONFIG_KEY] = jsonStr
        }
        _configFlow.value = config
    }

    /**
     * 更新配置的部分字段
     */
    suspend fun updateConfig(updater: (MultiPermissionConfig) -> MultiPermissionConfig) {
        val current = _configFlow.value
        val updated = updater(current)
        saveConfig(updated)
    }

    /**
     * 启用/禁用多权限多层启动
     */
    suspend fun setEnabled(enabled: Boolean) {
        updateConfig { it.copy(enabled = enabled) }
    }

    /**
     * 设置启用的权限层级
     */
    suspend fun setEnabledLayers(layers: List<PermissionLayer>) {
        updateConfig { it.copy(enabledLayers = layers.map { it.name }) }
    }

    /**
     * 设置全局优先级顺序
     */
    suspend fun setGlobalPriority(priority: List<PermissionLayer>) {
        updateConfig { it.copy(globalPriority = priority.map { it.name }) }
    }

    /**
     * 设置是否自动回退
     */
    suspend fun setAutoFallback(enabled: Boolean) {
        updateConfig { it.copy(autoFallbackOnFailure = enabled) }
    }

    /**
     * 设置最大重试次数
     */
    suspend fun setMaxRetries(max: Int) {
        updateConfig { it.copy(maxFallbackRetries = max.coerceIn(0, 5)) }
    }

    /**
     * 设置是否允许并行执行
     */
    suspend fun setParallelExecution(enabled: Boolean, maxParallel: Int = 4) {
        updateConfig {
            it.copy(
                allowParallelToolExecution = enabled,
                maxParallelTools = maxParallel.coerceIn(1, 8)
            )
        }
    }

    /**
     * 设置单个工具的优先级覆盖
     */
    suspend fun setToolOverride(toolName: String, override: ToolPermissionPriorityOverride?) {
        updateConfig { config ->
            val newOverrides = config.toolOverrides.toMutableMap()
            if (override == null) {
                newOverrides.remove(toolName)
            } else {
                newOverrides[toolName] = override
            }
            config.copy(toolOverrides = newOverrides)
        }
    }

    /**
     * 获取指定工具的有效优先级
     */
    fun getEffectivePriority(toolName: String): List<PermissionLayer> {
        return _configFlow.value.getEffectivePriority(toolName)
    }

    /**
     * 获取当前配置（非阻塞）
     */
    fun currentConfig(): MultiPermissionConfig = _configFlow.value

    /**
     * 重置为默认配置
     */
    suspend fun resetToDefault() {
        saveConfig(MultiPermissionConfig())
    }
}
