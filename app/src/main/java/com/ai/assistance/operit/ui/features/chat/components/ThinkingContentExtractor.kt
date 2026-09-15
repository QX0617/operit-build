package com.ai.assistance.operit.ui.features.chat.components

import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.stream

/**
 * 从 ChatMessage 中提取思考过程数据
 *
 * 支持从消息内容中解析 `<think>` 和 `<thinking>` 标签，
 * 并将其转换为 ThinkingProcessData 用于显示。
 */
object ThinkingContentExtractor {

    private const val THINK_OPEN_TAG = "<think>"
    private const val THINK_CLOSE_TAG = "</think>"
    private const val THINKING_OPEN_TAG = "<thinking>"
    private const val THINKING_CLOSE_TAG = "</thinking>"

    /**
     * 从消息内容中提取思考内容
     */
    fun extractFromMessage(message: ChatMessage): ThinkingProcessData {
        val content = message.content
        val isStreaming = message.contentStream != null

        // 尝试提取 think 标签内容
        val thinkContent = extractTagContent(content, THINK_OPEN_TAG, THINK_CLOSE_TAG)
            ?: extractTagContent(content, THINKING_OPEN_TAG, THINKING_CLOSE_TAG)

        val hasThinkTag = content.contains(THINK_OPEN_TAG) || content.contains(THINKING_OPEN_TAG)
        val isThinkClosed = content.contains(THINK_CLOSE_TAG) || content.contains(THINKING_CLOSE_TAG)

        val state = when {
            !hasThinkTag && thinkContent == null -> ThinkingState.IDLE
            isStreaming && hasThinkTag && !isThinkClosed -> ThinkingState.THINKING
            hasThinkTag && isThinkClosed -> ThinkingState.COMPLETED
            hasThinkTag && !isThinkClosed -> ThinkingState.THINKING
            else -> ThinkingState.IDLE
        }

        // 如果是流式且思考未完成，创建流式内容
        val thinkingStream = if (state == ThinkingState.THINKING && message.contentStream != null) {
            createThinkingStream(message.contentStream!!)
        } else {
            null
        }

        return ThinkingProcessData(
            state = state,
            content = thinkContent ?: "",
            contentStream = thinkingStream,
            startTimeMs = message.sentAt,
            endTimeMs = if (state == ThinkingState.COMPLETED) message.completedAt else 0L,
            tokenCount = (thinkContent?.length ?: 0) / 4,
            modelName = message.modelName
        )
    }

    /**
     * 从文本中提取指定标签的内容
     */
    private fun extractTagContent(text: String, openTag: String, closeTag: String): String? {
        val startIndex = text.indexOf(openTag)
        if (startIndex == -1) return null

        val contentStart = startIndex + openTag.length
        val endIndex = text.indexOf(closeTag, contentStart)

        return if (endIndex != -1) {
            text.substring(contentStart, endIndex).trim()
        } else {
            // 标签未闭合，返回从开始到末尾的内容
            text.substring(contentStart).trim()
        }
    }

    /**
     * 创建思考内容流，从原始流中过滤出 think 标签内的内容
     */
    private fun createThinkingStream(originalStream: Stream<String>): Stream<String> {
        return stream { emit ->
            var inThinkBlock = false
            var buffer = StringBuilder()

            originalStream.collect { chunk ->
                var remaining = chunk
                while (remaining.isNotEmpty()) {
                    if (!inThinkBlock) {
                        val openIdx = remaining.indexOf(THINK_OPEN_TAG).takeIf { it >= 0 }
                            ?: remaining.indexOf(THINKING_OPEN_TAG).takeIf { it >= 0 }
                        if (openIdx != null) {
                            val tagLen = if (remaining.contains(THINK_OPEN_TAG)) THINK_OPEN_TAG.length else THINKING_OPEN_TAG.length
                            inThinkBlock = true
                            remaining = remaining.substring(openIdx + tagLen)
                            buffer = StringBuilder()
                        } else {
                            remaining = ""
                        }
                    } else {
                        val closeIdx = remaining.indexOf(THINK_CLOSE_TAG).takeIf { it >= 0 }
                            ?: remaining.indexOf(THINKING_CLOSE_TAG).takeIf { it >= 0 }
                        if (closeIdx != null) {
                            buffer.append(remaining.substring(0, closeIdx))
                            emit(buffer.toString())
                            inThinkBlock = false
                            val tagLen = if (remaining.contains(THINK_CLOSE_TAG)) THINK_CLOSE_TAG.length else THINKING_CLOSE_TAG.length
                            remaining = remaining.substring(closeIdx + tagLen)
                            buffer = StringBuilder()
                        } else {
                            buffer.append(remaining)
                            emit(buffer.toString())
                            buffer = StringBuilder()
                            remaining = ""
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断消息是否包含思考过程
     */
    fun hasThinkingContent(message: ChatMessage): Boolean {
        return message.content.contains(THINK_OPEN_TAG) ||
                message.content.contains(THINKING_OPEN_TAG) ||
                message.contentStream != null
    }
}
