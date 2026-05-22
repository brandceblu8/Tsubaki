package com.ncclab.tsubaki.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ncclab.tsubaki.data.model.ParsedPayload
import com.ncclab.tsubaki.data.model.PayloadParser
import com.ncclab.tsubaki.data.wifi.WifiConnector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    format: String,
    rawValue: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val parsed = remember(rawValue) { PayloadParser.parse(rawValue) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描结果") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "类型: ${displayTypeName(format, parsed)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Wi-Fi 专属卡片
            if (parsed is ParsedPayload.Wifi) {
                WifiCard(
                    wifi = parsed,
                    onConnect = {
                        val result = WifiConnector.connect(context, parsed)
                        val msg = when (result) {
                            is WifiConnector.Result.Launched -> "已唤起系统连接面板"
                            is WifiConnector.Result.Suggested -> result.message
                            is WifiConnector.Result.FellBackToSettings -> result.message
                            is WifiConnector.Result.Failed -> result.message
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    onCopyPassword = {
                        clipboardManager.setText(AnnotatedString(parsed.password))
                        Toast.makeText(context, "已复制密码", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "内容:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = rawValue,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    text = "复制",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawValue))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                )

                ActionButton(
                    icon = Icons.Default.Share,
                    text = "分享",
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, rawValue)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "分享到"))
                    }
                )

                if (parsed is ParsedPayload.Url) {
                    ActionButton(
                        icon = Icons.Default.OpenInBrowser,
                        text = "打开",
                        onClick = { openUrlWithChooser(context, parsed.url) }
                    )
                }
            }
        }
    }
}

/**
 * 用 Intent.createChooser 包一层强制拉起选择器，避免被默认浏览器静默打开。
 * 这样诸如微信链接、淘宝链接、weixin:// / mqqapi:// 这种 scheme 才会
 * 列出对应应用让用户选择。
 */
private fun openUrlWithChooser(context: android.content.Context, url: String) {
    try {
        val uri = url.toUri()
        val viewIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooser = Intent.createChooser(viewIntent, "用什么打开？")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun displayTypeName(format: String, parsed: ParsedPayload): String {
    val semantic = when (parsed) {
        is ParsedPayload.Wifi -> " · Wi-Fi"
        is ParsedPayload.Url -> " · 链接"
        is ParsedPayload.PlainText -> ""
    }
    return "$format$semantic"
}

@Composable
private fun WifiCard(
    wifi: ParsedPayload.Wifi,
    onConnect: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Wi-Fi 网络",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LabeledValue(label = "SSID", value = wifi.ssid)
            Spacer(modifier = Modifier.height(8.dp))

            if (wifi.security != ParsedPayload.Wifi.Security.NONE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        LabeledValue(
                            label = "密码",
                            value = if (passwordVisible) wifi.password else "•".repeat(wifi.password.length.coerceAtLeast(6))
                        )
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onCopyPassword) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制密码",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            LabeledValue(label = "加密", value = wifi.security.name)
            if (wifi.hidden) {
                Spacer(modifier = Modifier.height(4.dp))
                LabeledValue(label = "可见性", value = "隐藏网络")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("连接")
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SelectionContainer {
            Text(
                text = value.ifEmpty { "(空)" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
