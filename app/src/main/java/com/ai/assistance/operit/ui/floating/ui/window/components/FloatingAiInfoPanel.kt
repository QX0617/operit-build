package com.ai.assistance.operit.ui.floating.ui.window.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.data.model.InputProcessingState
import com.ai.assistance.operit.util.stream.Stream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

/**
 * 悬浮窗 AI 信息面板
 *
 * 显示当前对话的 AI 状态信息，包括：
 * - AI 处理状态（空闲/思考/回复中/出错）
 * - 当前使用的模型名称和提供商
 * - 流式回复内容预览
 * - Token 使用统计
 * - 思考过程指示器
 */
@Composable
fun FloatingAiInfoPanel(
    currentMessage: ChatMessage?,
    inputProcessingState: InputProcessingState,
    modifier: Modifier = Modifier,
    showStreamingPreview: Boolean = true,
    showTokenInfo: Boolean = true,
    onExpandClick: (() -> Unit)? = null
) {
    val isProcessing = inputProcessingState != InputProcessingState.Idle
    val isThinking = inputProcessingState == InputProcessingState.Processing
    val isStreaming = currentMessage?.contentStream != null && isProcessing

    // 流式内容实时更新
    var streamingContent by remember { mutableStateOf("") }
    LaunchedEffect(currentMessage?.contentStream) {
        streamingContent = ""
        currentMessage?.contentStream?.let { stream ->
            stream.collect { chunk ->
                streamingContent += chunk
            }
        }
    }

    // 处理时长
    var processingDuration by remember { mutableStateOf(0L) }
    LaunchedEffect(isProcessing, currentMessage?.sentAt) {
        if (isProcessing && currentMessage?.sentAt ?: 0L > 0L) {
            while (true) {
                processingDuration = System.currentTimeMillis() - (currentMessage?.sentAt ?: 0L)
                delay(100)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 状态行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态指示器
                when {
                    isThinking -> ThinkingDotsIndicator()
                    isStreaming -> StreamingIndicator()
                    isProcessing -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    else -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 状态文本
                Text(
                    text = when {
                        isThinking -> "思考中..."
                        isStreaming -> "回复中..."
                        isProcessing -> "处理中..."
                        else -> "就绪"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isProcessing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // 模型信息
                if (currentMessage?.modelName?.isNotBlank() == true) {
                    Text(
                        text = currentMessage.modelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 处理时长
                if (isProcessing && processingDuration > 0) {
                    Text(
                        text = formatElapsedTime(processingDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 流式内容预览
            if (showStreamingPreview && isStreaming && streamingContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = streamingContent.take(200),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (streamingContent.length > 200) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "... 展开查看完整内容",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickableSafe { onExpandClick?.invoke() }
                    )
                }
            }

            // Token 信息
            if (showTokenInfo && currentMessage != null &&
                (currentMessage.outputTokens > 0 || currentMessage.inputTokens > 0)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentMessage.inputTokens > 0) {
                        TokenStat(label = "输入", value = currentMessage.inputTokens)
                    }
                    if (currentMessage.outputTokens > 0) {
                        TokenStat(label = "输出", value = currentMessage.outputTokens)
                    }
                    if (currentMessage.cachedInputTokens > 0) {
                        TokenStat(label = "缓存", value = currentMessage.cachedInputTokens)
                    }
                }
            }
        }
    }
}

/**
 * 思考点动画指示器
 */
@Composable
private fun ThinkingDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_thinking_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floating_dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}

/**
 * 流式指示器（脉冲动画）
 */
@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_streaming_alpha"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

/**
 * Token 统计项
 */
@Composable
private fun TokenStat(label: String, value: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 格式化经过的时间
 */
private fun formatElapsedTime(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}

/**
 * 安全的 clickable modifier（避免在 Card 内的嵌套点击问题）
 */
private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable { onClick() })

// 需要的导入
private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable { onClick() })
