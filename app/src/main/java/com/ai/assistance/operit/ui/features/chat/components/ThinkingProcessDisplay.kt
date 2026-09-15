package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.stream
import kotlinx.coroutines.delay

/**
 * 思考过程状态
 */
enum class ThinkingState {
    IDLE,        // 未开始
    THINKING,    // 思考中
    COMPLETED,   // 思考完成
    ERROR        // 思考出错
}

/**
 * 思考过程数据
 */
data class ThinkingProcessData(
    val state: ThinkingState = ThinkingState.IDLE,
    val content: String = "",
    val contentStream: Stream<String>? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val tokenCount: Int = 0,
    val modelName: String = "",
    val errorMessage: String? = null
) {
    val durationMs: Long
        get() = if (endTimeMs > 0) endTimeMs - startTimeMs else 0L

    val isActive: Boolean
        get() = state == ThinkingState.THINKING

    val hasContent: Boolean
        get() = content.isNotBlank() || contentStream != null
}

/**
 * 思考过程显示组件
 *
 * 提供丰富的思考过程可视化，包括：
 * - 思考状态指示器（动画点）
 * - 思考内容流式显示
 * - 思考时长统计
 * - Token 数量显示
 * - 展开/折叠控制
 * - 模型名称显示
 */
@Composable
fun ThinkingProcessDisplay(
    data: ThinkingProcessData,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    showDuration: Boolean = true,
    showTokenCount: Boolean = true,
    showModelName: Boolean = true,
    onExpandChange: ((Boolean) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val isThinking = data.state == ThinkingState.THINKING

    // 思考时长实时更新
    var currentDuration by remember { mutableStateOf(0L) }
    LaunchedEffect(data.state, data.startTimeMs) {
        if (isThinking && data.startTimeMs > 0) {
            while (true) {
                currentDuration = System.currentTimeMillis() - data.startTimeMs
                delay(100)
            }
        } else if (data.durationMs > 0) {
            currentDuration = data.durationMs
        }
    }

    val displayDuration = if (isThinking) currentDuration else data.durationMs

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isThinking) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        onExpandChange?.invoke(expanded)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 思考状态图标
                if (isThinking) {
                    ThinkingAnimationDots()
                } else {
                    Icon(
                        imageVector = when (data.state) {
                            ThinkingState.COMPLETED -> Icons.Default.CheckCircle
                            ThinkingState.ERROR -> Icons.Default.Error
                            else -> Icons.Default.Psychology
                        },
                        contentDescription = null,
                        tint = when (data.state) {
                            ThinkingState.COMPLETED -> MaterialTheme.colorScheme.primary
                            ThinkingState.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 标题
                Text(
                    text = when (data.state) {
                        ThinkingState.THINKING -> "思考中..."
                        ThinkingState.COMPLETED -> "已完成思考"
                        ThinkingState.ERROR -> "思考出错"
                        else -> "思考过程"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isThinking) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // 元信息
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showDuration && displayDuration > 0) {
                        Text(
                            text = formatDuration(displayDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (showTokenCount && data.tokenCount > 0) {
                        Text(
                            text = "${data.tokenCount} tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (showModelName && data.modelName.isNotBlank()) {
                        Text(
                            text = data.modelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 100.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // 展开箭头
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 内容区域
            if (expanded) {
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .heightIn(max = 300.dp)
                ) {
                    when {
                        data.state == ThinkingState.ERROR -> {
                            Text(
                                text = data.errorMessage ?: "思考过程出错",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        data.contentStream != null && isThinking -> {
                            // 流式思考内容
                            StreamingThinkingContent(
                                stream = data.contentStream,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        data.content.isNotBlank() -> {
                            Text(
                                text = data.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.verticalScrollIfNeeded()
                            )
                        }
                        isThinking -> {
                            Text(
                                text = "正在深度思考，请稍候...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        else -> {
                            Text(
                                text = "无思考内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 思考动画点（三个跳动的点）
 */
@Composable
private fun ThinkingAnimationDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_dots")
    val dotCount = 3

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}

/**
 * 流式思考内容显示
 */
@Composable
private fun StreamingThinkingContent(
    stream: Stream<String>,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf("") }

    LaunchedEffect(stream) {
        stream.collect { chunk ->
            content += chunk
        }
    }

    if (content.isBlank()) {
        Text(
            text = "正在思考...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    } else {
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

/**
 * 格式化时长为可读字符串
 */
private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
}

/**
 * 思考过程管理器
 *
 * 用于在 ViewModel 或 Service 中跟踪思考过程状态
 */
class ThinkingProcessManager {
    private val _state = mutableStateOf(ThinkingProcessData())
    val state: State<ThinkingProcessData> = _state

    fun startThinking(modelName: String = "") {
        _state.value = ThinkingProcessData(
            state = ThinkingState.THINKING,
            startTimeMs = System.currentTimeMillis(),
            modelName = modelName
        )
    }

    fun updateContent(content: String) {
        val current = _state.value
        _state.value = current.copy(
            content = content,
            tokenCount = content.length / 4  // 粗略估算 token 数
        )
    }

    fun appendContent(chunk: String) {
        val current = _state.value
        val newContent = current.content + chunk
        _state.value = current.copy(
            content = newContent,
            tokenCount = newContent.length / 4
        )
    }

    fun setStream(stream: Stream<String>) {
        val current = _state.value
        _state.value = current.copy(contentStream = stream)
    }

    fun completeThinking() {
        val current = _state.value
        _state.value = current.copy(
            state = ThinkingState.COMPLETED,
            endTimeMs = System.currentTimeMillis(),
            contentStream = null
        )
    }

    fun errorThinking(message: String) {
        val current = _state.value
        _state.value = current.copy(
            state = ThinkingState.ERROR,
            endTimeMs = System.currentTimeMillis(),
            errorMessage = message,
            contentStream = null
        )
    }

    fun reset() {
        _state.value = ThinkingProcessData()
    }
}

// 需要的 Modifier 扩展
private fun Modifier.verticalScrollIfNeeded(): Modifier = this
