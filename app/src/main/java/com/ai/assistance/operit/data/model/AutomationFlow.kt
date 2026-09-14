package com.ai.assistance.operit.data.model

import kotlinx.serialization.Serializable

/**
 * AI 控制手机流程库
 * 按执行模式、目标应用、目的三级分类
 */
@Serializable
data class AutomationFlow(
    val id: String,
    val name: String,
    val mode: String,          // accessibility / root / adb
    val targetApp: String,     // 微信 / 抖音 / 系统设置 等
    val purpose: String,      // 发信息 / 刷视频 / 截图 等
    val description: String,
    val steps: List<FlowStep>,
    val difficulty: String = "easy", // easy / medium / hard
    val estimatedSeconds: Int = 10
)

@Serializable
data class FlowStep(
    val action: String,        // click / input / scroll / wait / launch / screenshot / back / home
    val target: String = "",   // 元素描述或坐标
    val text: String = "",     // 输入文本
    val durationMs: Long = 500,
    val description: String = ""
)

/**
 * 流程库查询结果
 */
@Serializable
data class FlowLibraryQuery(
    val mode: String? = null,
    val targetApp: String? = null,
    val purpose: String? = null
)

object FlowLibraryCatalog {

    val flows: List<AutomationFlow> = listOf(
        // ===== 无障碍模式 =====
        AutomationFlow(
            id = "wx_send_msg",
            name = "微信发消息",
            mode = "accessibility",
            targetApp = "微信",
            purpose = "发信息",
            description = "打开微信，进入聊天，发送文字消息",
            steps = listOf(
                FlowStep("launch", target = "com.tencent.mm", description = "启动微信"),
                FlowStep("wait", durationMs = 1500),
                FlowStep("click", target = "搜索按钮", description = "点击搜索"),
                FlowStep("input", text = "联系人名", description = "输入联系人"),
                FlowStep("wait", durationMs = 800),
                FlowStep("click", target = "第一个搜索结果", description = "点击联系人"),
                FlowStep("click", target = "输入框", description = "点击输入框"),
                FlowStep("input", text = "消息内容", description = "输入消息"),
                FlowStep("click", target = "发送按钮", description = "点击发送")
            ),
            estimatedSeconds = 15
        ),
        AutomationFlow(
            id = "douyin_open",
            name = "打开抖音刷视频",
            mode = "accessibility",
            targetApp = "抖音",
            purpose = "刷视频",
            description = "打开抖音并开始播放推荐视频",
            steps = listOf(
                FlowStep("launch", target = "com.ss.android.ugc.aweme", description = "启动抖音"),
                FlowStep("wait", durationMs = 2000),
                FlowStep("screenshot", description = "截图确认进入首页")
            ),
            estimatedSeconds = 5
        ),
        AutomationFlow(
            id = "wx_moments",
            name = "微信朋友圈",
            mode = "accessibility",
            targetApp = "微信",
            purpose = "浏览朋友圈",
            description = "打开微信朋友圈",
            steps = listOf(
                FlowStep("launch", target = "com.tencent.mm", description = "启动微信"),
                FlowStep("wait", durationMs = 1500),
                FlowStep("click", target = "发现 tab", description = "点击发现"),
                FlowStep("click", target = "朋友圈", description = "点击朋友圈")
            ),
            estimatedSeconds = 8
        ),
        // ===== Root 模式 =====
        AutomationFlow(
            id = "root_screenshot",
            name = "Root 截图",
            mode = "root",
            targetApp = "系统",
            purpose = "截图",
            description = "通过 root 权限执行 screencap 截图",
            steps = listOf(
                FlowStep("shell", target = "screencap -p /sdcard/screen.png", description = "执行截图命令"),
                FlowStep("wait", durationMs = 500)
            ),
            estimatedSeconds = 2,
            difficulty = "easy"
        ),
        AutomationFlow(
            id = "root_clear_cache",
            name = "清理应用缓存",
            mode = "root",
            targetApp = "系统",
            purpose = "清理",
            description = "通过 root 清除指定应用缓存",
            steps = listOf(
                FlowStep("shell", target = "pm clear <package>", description = "清除应用数据"),
                FlowStep("wait", durationMs = 1000)
            ),
            estimatedSeconds = 3,
            difficulty = "medium"
        ),
        // ===== ADB / Shizuku 模式 =====
        AutomationFlow(
            id = "shizuku_install_apk",
            name = "ADB 安装 APK",
            mode = "adb",
            targetApp = "系统",
            purpose = "安装应用",
            description = "通过 Shizuku/ADB 静默安装 APK",
            steps = listOf(
                FlowStep("shell", target = "pm install -r /path/to/app.apk", description = "静默安装"),
                FlowStep("wait", durationMs = 3000)
            ),
            estimatedSeconds = 5,
            difficulty = "easy"
        ),
        AutomationFlow(
            id = "adb_input_tap",
            name = "ADB 点击坐标",
            mode = "adb",
            targetApp = "任意应用",
            purpose = "点击操作",
            description = "通过 ADB input tap 模拟点击",
            steps = listOf(
                FlowStep("shell", target = "input tap <x> <y>", description = "点击坐标")
            ),
            estimatedSeconds = 1,
            difficulty = "easy"
        )
    )

    fun query(mode: String? = null, targetApp: String? = null, purpose: String? = null): List<AutomationFlow> {
        return flows.filter { flow ->
            (mode == null || flow.mode.equals(mode, ignoreCase = true)) &&
            (targetApp == null || flow.targetApp.contains(targetApp, ignoreCase = true)) &&
            (purpose == null || flow.purpose.contains(purpose, ignoreCase = true))
        }
    }

    fun getById(id: String): AutomationFlow? = flows.find { it.id == id }

    fun listCategories(): Map<String, List<String>> {
        return mapOf(
            "mode" to flows.map { it.mode }.distinct(),
            "targetApp" to flows.map { it.targetApp }.distinct(),
            "purpose" to flows.map { it.purpose }.distinct()
        )
    }
}
