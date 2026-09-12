package com.ai.assistance.operit.data.model

import kotlinx.serialization.Serializable

/**
 * 权限层级定义，按能力从低到高排列
 */
enum class PermissionLayer(val displayName: String, val description: String) {
    STANDARD("标准权限", "应用普通权限，无需额外授权"),
    ACCESSIBILITY("无障碍权限", "通过无障碍服务执行 UI 操作"),
    SHIZUKU("Shizuku/ADB", "通过 Shizuku 获得 ADB 级调试权限"),
    DEBUGGER("调试权限", "通过 ADB 调试通道执行操作"),
    ROOT("Root 权限", "通过 Root 获得最高系统权限");

    companion object {
        fun fromName(name: String): PermissionLayer? = entries.find { it.name == name }
        val defaultPriority: List<PermissionLayer> = listOf(STANDARD, ACCESSIBILITY, SHIZUKU, DEBUGGER, ROOT)
    }
}

/**
 * 单个工具的多权限优先级配置覆盖
 */
@Serializable
data class ToolPermissionPriorityOverride(
    val toolName: String,
    val enabledLayers: List<String> = emptyList(),  // 空表示使用全局配置
    val customPriority: List<String> = emptyList(),  // 空表示使用全局优先级
    val autoFallbackOnFailure: Boolean = true
)

/**
 * 多权限优先级多层启动配置
 *
 * 用于配置 AI 可同时使用的多个权限层级、优先级顺序，
 * 以及工具失败时是否自动切换到下一个权限层级重试。
 */
@Serializable
data class MultiPermissionConfig(
    /** 是否启用多权限多层启动 */
    val enabled: Boolean = false,

    /** 全局启用的权限层级 */
    val enabledLayers: List<String> = listOf(
        PermissionLayer.STANDARD.name,
        PermissionLayer.ACCESSIBILITY.name
    ),

    /** 全局权限优先级顺序（从高到低） */
    val globalPriority: List<String> = listOf(
        PermissionLayer.STANDARD.name,
        PermissionLayer.ACCESSIBILITY.name,
        PermissionLayer.SHIZUKU.name,
        PermissionLayer.DEBUGGER.name,
        PermissionLayer.ROOT.name
    ),

    /** 工具执行失败时是否自动切换到下一个权限层级重试 */
    val autoFallbackOnFailure: Boolean = true,

    /** 最大重试次数（切换权限层级的次数） */
    val maxFallbackRetries: Int = 3,

    /** 重试间隔毫秒 */
    val fallbackDelayMs: Long = 200L,

    /** 单个工具的优先级覆盖 */
    val toolOverrides: Map<String, ToolPermissionPriorityOverride> = emptyMap(),

    /** 是否允许并行执行多个工具 */
    val allowParallelToolExecution: Boolean = true,

    /** 并行执行的最大工具数 */
    val maxParallelTools: Int = 4
) {
    /**
     * 获取指定工具的有效优先级顺序
     */
    fun getEffectivePriority(toolName: String): List<PermissionLayer> {
        val override = toolOverrides[toolName]
        val priorityNames = if (override != null && override.customPriority.isNotEmpty()) {
            override.customPriority
        } else {
            globalPriority
        }
        val enabledNames = if (override != null && override.enabledLayers.isNotEmpty()) {
            override.enabledLayers.toSet()
        } else {
            enabledLayers.toSet()
        }
        return priorityNames
            .mapNotNull { PermissionLayer.fromName(it) }
            .filter { it.name in enabledNames }
    }

    /**
     * 获取指定工具是否启用失败自动回退
     */
    fun isAutoFallbackEnabled(toolName: String): Boolean {
        val override = toolOverrides[toolName]
        return override?.autoFallbackOnFailure ?: autoFallbackOnFailure
    }

    /**
     * 获取指定工具的最大重试次数
     */
    fun getMaxRetries(toolName: String): Int {
        return if (isAutoFallbackEnabled(toolName)) maxFallbackRetries else 0
    }
}
