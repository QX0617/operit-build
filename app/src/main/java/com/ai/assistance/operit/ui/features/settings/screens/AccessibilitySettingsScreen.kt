package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.data.preferences.AccessibilityPreferences

/**
 * 无障碍 / 老年人模式设置：
 *  - 老年人模式总开关
 *  - 字号大小（100%~160%）
 *  - 高对比配色
 * 主对话页与悬浮窗共用 OperitTheme，改动即时生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AccessibilityPreferences.getInstance(context) }
    val elderlyEnabled by prefs.elderlyModeEnabled.collectAsState()
    val fontScalePercent by prefs.fontScalePercent.collectAsState()
    val highContrast by prefs.highContrastEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("无障碍 / 老年人模式") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("老年人 / 无障碍模式", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "开启后默认放大字号、加大点击区域，适合视力较弱用户",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = elderlyEnabled,
                            onCheckedChange = { prefs.setElderlyModeEnabled(it) }
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("字号大小", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "当前 ${fontScalePercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = fontScalePercent.toFloat(),
                        onValueChange = { prefs.setFontScalePercent(it.toInt()) },
                        valueRange = AccessibilityPreferences.MIN_FONT_SCALE.toFloat()..
                                AccessibilityPreferences.MAX_FONT_SCALE.toFloat(),
                        steps = 5 // 100/110/120/130/140/150/160
                    )
                    Text(
                        "拖动调整全局对话与悬浮窗字号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("高对比配色", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "黑白高对比文字，提升可读性",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { prefs.setHighContrastEnabled(it) }
                        )
                    }
                }
            }
        }
    }
}
