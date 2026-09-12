package com.ai.assistance.operit.ui.features.permission.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.data.model.MultiPermissionConfig
import com.ai.assistance.operit.data.model.PermissionLayer
import com.ai.assistance.operit.data.preferences.MultiPermissionManager
import kotlinx.coroutines.launch

/**
 * 多权限优先级多层启动配置界面
 *
 * 允许用户配置：
 * - 启用/禁用多权限多层启动
 * - 启用的权限层级
 * - 权限优先级顺序（拖拽排序）
 * - 失败自动回退设置
 * - 并行执行设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPermissionConfigScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { MultiPermissionManager.getInstance(context) }
    var config by remember { mutableStateOf(MultiPermissionConfig()) }
    var showToolOverrideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        config = manager.loadConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("多权限优先级配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 总开关
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "多权限多层启动",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "启用后 AI 可按优先级顺序使用多个权限层级执行工具，失败时自动切换",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { enabled ->
                                    config = config.copy(enabled = enabled)
                                    scope.launch { manager.saveConfig(config) }
                                }
                            )
                        }
                    }
                }
            }

            if (config.enabled) {
                // 权限层级选择
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "启用的权限层级",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "选择 AI 可以使用的权限层级",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PermissionLayer.entries.forEach { layer ->
                                val isEnabled = layer.name in config.enabledLayers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newLayers = if (isEnabled) {
                                                config.enabledLayers - layer.name
                                            } else {
                                                config.enabledLayers + layer.name
                                            }
                                            config = config.copy(enabledLayers = newLayers)
                                            scope.launch { manager.saveConfig(config) }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = layer.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = layer.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // 优先级排序
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "权限优先级顺序",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "从上到下优先级递减，AI 会优先使用高层级权限",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val enabledPriority = config.globalPriority
                                .mapNotNull { PermissionLayer.fromName(it) }
                                .filter { it.name in config.enabledLayers }
                            enabledPriority.forEachIndexed { index, layer ->
                                PriorityItem(
                                    layer = layer,
                                    priority = index + 1,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < enabledPriority.size - 1,
                                    onMoveUp = {
                                        val newPriority = config.globalPriority.toMutableList()
                                        val currentIdx = newPriority.indexOf(layer.name)
                                        if (currentIdx > 0) {
                                            val prev = newPriority[currentIdx - 1]
                                            newPriority[currentIdx - 1] = layer.name
                                            newPriority[currentIdx] = prev
                                            config = config.copy(globalPriority = newPriority)
                                            scope.launch { manager.saveConfig(config) }
                                        }
                                    },
                                    onMoveDown = {
                                        val newPriority = config.globalPriority.toMutableList()
                                        val currentIdx = newPriority.indexOf(layer.name)
                                        if (currentIdx < newPriority.size - 1) {
                                            val next = newPriority[currentIdx + 1]
                                            newPriority[currentIdx + 1] = layer.name
                                            newPriority[currentIdx] = next
                                            config = config.copy(globalPriority = newPriority)
                                            scope.launch { manager.saveConfig(config) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 失败回退设置
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "失败自动回退",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("工具失败时自动切换权限层级重试")
                                Switch(
                                    checked = config.autoFallbackOnFailure,
                                    onCheckedChange = { enabled ->
                                        config = config.copy(autoFallbackOnFailure = enabled)
                                        scope.launch { manager.saveConfig(config) }
                                    }
                                )
                            }
                            if (config.autoFallbackOnFailure) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("最大重试次数")
                                    Text(
                                        text = "${config.maxFallbackRetries} 次",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = config.maxFallbackRetries.toFloat(),
                                    onValueChange = { value ->
                                        config = config.copy(maxFallbackRetries = value.toInt())
                                    },
                                    onValueChangeFinished = {
                                        scope.launch { manager.saveConfig(config) }
                                    },
                                    valueRange = 1f..5f,
                                    steps = 3
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("重试间隔")
                                    Text(
                                        text = "${config.fallbackDelayMs} ms",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = config.fallbackDelayMs.toFloat(),
                                    onValueChange = { value ->
                                        config = config.copy(fallbackDelayMs = value.toLong())
                                    },
                                    onValueChangeFinished = {
                                        scope.launch { manager.saveConfig(config) }
                                    },
                                    valueRange = 0f..1000f,
                                    steps = 9
                                )
                            }
                        }
                    }
                }

                // 并行执行设置
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "并行工具执行",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("允许 AI 同时执行多个工具")
                                Switch(
                                    checked = config.allowParallelToolExecution,
                                    onCheckedChange = { enabled ->
                                        config = config.copy(allowParallelToolExecution = enabled)
                                        scope.launch { manager.saveConfig(config) }
                                    }
                                )
                            }
                            if (config.allowParallelToolExecution) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("最大并行工具数")
                                    Text(
                                        text = "${config.maxParallelTools} 个",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = config.maxParallelTools.toFloat(),
                                    onValueChange = { value ->
                                        config = config.copy(maxParallelTools = value.toInt())
                                    },
                                    onValueChangeFinished = {
                                        scope.launch { manager.saveConfig(config) }
                                    },
                                    valueRange = 1f..8f,
                                    steps = 6
                                )
                            }
                        }
                    }
                }

                // 说明
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "使用说明",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. AI 执行工具时，会按优先级顺序尝试各权限层级\n" +
                                        "2. 当前层级执行失败或权限不足时，自动切换到下一层级重试\n" +
                                        "3. 所有层级均失败后，返回最终错误结果\n" +
                                        "4. 并行执行可显著提升多工具任务的效率\n" +
                                        "5. 建议优先启用标准权限和无障碍权限，Root 权限请谨慎使用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 优先级列表项
 */
@Composable
private fun PriorityItem(
    layer: PermissionLayer,
    priority: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 优先级序号
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$priority",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = layer.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = layer.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // 上下移动按钮
        Column {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
    HorizontalDivider()
}
