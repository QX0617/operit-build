package com.ai.assistance.operit.ui.features.elderly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.data.preferences.ElderlyModeManager
import kotlinx.coroutines.launch

/**
 * 老年人/无障碍模式主界面
 * 极简设计：大字、大按钮、语音输入为主
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyModeScreen(
    onExit: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ElderlyModeManager.getInstance(context) }

    var inputText by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<ElderlyChatMessage>()) }
    var permissionInfo by remember { mutableStateOf<ElderlyModeManager.PermissionChannelInfo?>(null) }

    LaunchedEffect(Unit) {
        permissionInfo = manager.detectPermissionChannels()
        // 如果无障碍未开启，提示
        permissionInfo?.let { info ->
            if (!info.accessibilityEnabled) {
                messages = messages + ElderlyChatMessage(
                    isFromUser = false,
                    text = "您好！我需要无障碍权限来帮您操作手机。请在设置中开启 Operit 的无障碍服务。"
                )
            } else {
                messages = messages + ElderlyChatMessage(
                    isFromUser = false,
                    text = "您好！我已经准备就绪。您可以按住麦克风说话，或点击键盘图标输入文字。"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助手", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 权限状态提示
                permissionInfo?.let { info ->
                    if (!info.accessibilityEnabled) {
                        Button(
                            onClick = { manager.openAccessibilitySettings() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .height(48.dp)
                        ) {
                            Text("开启无障碍权限", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showTextInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                            placeholder = { Text("输入消息...", fontSize = 18.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    messages = messages + ElderlyChatMessage(isFromUser = true, text = inputText)
                                    // TODO: 发送到AI
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White)
                        }
                    }
                }

                // 大字语音按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文字输入入口
                    IconButton(
                        onClick = { showTextInput = !showTextInput },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "文字输入", modifier = Modifier.size(32.dp))
                    }

                    // 主麦克风按钮
                    Button(
                        onClick = {
                            // TODO: 启动语音识别
                            messages = messages + ElderlyChatMessage(
                                isFromUser = false,
                                text = "🎤 语音识别功能即将上线，请先用文字输入"
                            )
                        },
                        modifier = Modifier
                            .size(100.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "语音输入", modifier = Modifier.size(48.dp), tint = Color.White)
                    }

                    // 占位保持对称
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                ElderlyMessageBubble(msg)
            }
        }
    }
}

@Composable
private fun ElderlyMessageBubble(msg: ElderlyChatMessage) {
    val alignment = if (msg.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (msg.isFromUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (msg.isFromUser) Color.White else Color.Black

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            tonalElevation = 2.dp
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

data class ElderlyChatMessage(
    val isFromUser: Boolean,
    val text: String
)
