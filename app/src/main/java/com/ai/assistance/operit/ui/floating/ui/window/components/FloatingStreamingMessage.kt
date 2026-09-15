package com.ai.assistance.operit.ui.floating.ui.window.components

import androidx.compose.animation.*
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
import com.ai.assistance.operit.ui.features.chat.components.ThinkingContentExtractor
import com.ai.assistance.operit.ui.features.chat.components.ThinkingProcessDisplay
import com.ai.assistance.operit.util.stream.Stream
import kotlinx.coroutines.flow.collect

/**
 * 悬浮窗流式消息组件
 *
 * 专为悬浮窗优化的轻量级消息渲染器，支持：
 * - 流式内容实时显示
 * - 思考过程展示
 * - AI 状态指示器
 * - 用户/AI 消息区分
 * - 紧凑布局适配小窗口
 */
@Composable
fun FloatingStreamingMessage(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    isLatest: Boolean = false,
    showThinking: Boolean = true,
    maxContentLines: Int = Int.MAX_VALUE
) {
    val isAi = message.sender == "ai"
    val isStreaming = message.contentStream != null && isLatest

    // 流式内容收集
    var streamingContent by remember(message.contentStream) { mutableStateOf("") }
    LaunchedEffect(message.contentStream) {
        streamingContent = ""
        message.contentStream?.let { stream ->
            stream.collect { chunk ->
                streamingContent += chunk
            }
        }
    }

    val displayContent = if (isStreaming && streamingContent.isNotBlank()) {
        streamingContent
    } else {
        message.content
    }

    // 提取思考过程
    val thinkingData = remember(message.content, message.contentStream) {
        ThinkingContentExtractor.extractFromMessage(message)
    }
    val hasThinking = showThinking && thinkingData.state != com.ai.assistance.operit.ui.features.chat.components.ThinkingState.IDLE

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
    ) {
        // 思考过程显示
        if (hasThinking && isAi) {
            ThinkingProcessDisplay(
                data = thinkingData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                initiallyExpanded = thinkingData.isActive,
                showDuration = true,
                showTokenCount = false,
                showModelName = false
            )
        }

        // 消息气泡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isAi) 4.dp else 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = if (isAi) 12.dp else 4.dp
                        )
                    )
                    .background(
                        if (isAi) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (displayContent.isBlank() && isStreaming) {
                    // 流式加载中指示器
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreamingTypingIndicator()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "正在输入...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAi) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimary,
                        maxLines = maxContentLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 流式状态指示器（最新 AI 消息）
        if (isAi && isLatest && isStreaming) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "流式输出中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.modelName.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${message.modelName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 打字中指示器（三个跳动的点）
 */
@Composable
private fun StreamingTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typing_dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    .graphicsLayer { translationY = offset }
            )
        }
    }
}

/**
 * 悬浮窗消息列表
 *
 * 管理多条消息的显示，自动滚动到底部，支持流式消息
 */
@Composable
fun FloatingMessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    showThinking: Boolean = true,
    onMessageClick: ((ChatMessage) -> Unit)? = null
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 新消息时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = messages,
            key = { it.timestamp }
        ) { message ->
            FloatingStreamingMessage(
                message = message,
                isLatest = message.timestamp == messages.lastOrNull()?.timestamp,
                showThinking = showThinking,
                modifier = Modifier.animateItem()
            )
        }
    }
}
