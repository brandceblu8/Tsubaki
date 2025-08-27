package com.ncclab.tsubaki.ui.screen

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ncclab.tsubaki.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onScanResult: (format: String, rawValue: String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 这里是侧边栏的内容
            AppDrawerContent(
                onLoginClick = {
                    scope.launch { drawerState.close() }
                    // TODO: 在这里处理登录逻辑或导航到登录页面
                },
                onCreateQrClick = {
                    scope.launch { drawerState.close() }
                    // TODO: 在这里导航到创建二维码的 Activity
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    // TODO: 在这里导航到设置 Activity
                }
            )
        }
    ) {


    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanResult by viewModel.scanResult.collectAsState()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(executor, viewModel.analyzer) }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            ScannerOverlay(modifier = Modifier.fillMaxSize()) {
                // 这里是菜单按钮的点击事件回调
                scope.launch {
                    drawerState.open()
                }
            }

        } else {
            // 如果没有权限，显示提示信息
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("需要相机权限才能扫描", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("授予权限")
                }
            }
        }

        if (scanResult != null) {
            // 使用 LaunchedEffect 确保导航只执行一次
            LaunchedEffect(scanResult) {
                scanResult?.let { result ->
                    onScanResult(result.format, result.rawValue)
                    viewModel.clearResult() // 清除结果，防止重复导航
                }
            }
        }
    }
    }
}

@Composable
fun AppDrawerContent(
    onLoginClick: () -> Unit,
    onCreateQrClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tsubaki Scanner",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 1. 用户登录行
            DrawerItem(
                icon = Icons.Default.AccountCircle,
                text = "登录/同步",
                onClick = onLoginClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // 2. 创建二维码
            DrawerItem(
                icon = Icons.Default.Add,
                text = "创建二维码",
                onClick = onCreateQrClick
            )

            // 3. 设置
            DrawerItem(
                icon = Icons.Default.Settings,
                text = "设置",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


/**
 * ✅ 新增：扫描界面的覆盖层 (UI Overlay)
 * 包含一个半透明的背景、一个透明的中心扫描区域、四个角的标记和一条移动的扫描线。
 */
@Composable
fun ScannerOverlay(modifier: Modifier, onMenuClick: () -> Unit) {
    // 用于动画
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_line")
    val boxSizePx = with(LocalDensity.current) { 256.dp.toPx() }
    val animatedProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = boxSizePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 200),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner_line_animation"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxSize = size.minDimension * 0.7f // 扫描框大小为屏幕最小边的70%

            val rect = Rect(
                left = (canvasWidth - boxSize) / 2,
                top = (canvasHeight - boxSize) / 2,
                right = (canvasWidth + boxSize) / 2,
                bottom = (canvasHeight + boxSize) / 2
            )

            // 绘制半透明的背景
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
            )

            // 在半透明背景上“挖”出一个透明的矩形区域
            drawRoundRect(
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(16.dp.toPx()),
                color = Color.Transparent,
                blendMode = BlendMode.Clear // 关键！这会使绘制区域变透明
            )

            // 绘制扫描框的白色边框
            drawRoundRect(
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(16.dp.toPx()),
                color = Color.White,
                style = Stroke(width = 2.dp.toPx())
            )

            // 绘制移动的扫描线
//            val gradient = Brush.verticalGradient(
//                colors = listOf(Color.Transparent, Color(0xFF00C853).copy(alpha = 0.8f), Color.Transparent),
//                startY = rect.top,
//                endY = rect.bottom
//            )
//            drawLine(
//                brush = gradient,
//                start = Offset(rect.left, rect.top + animatedProgress.value),
//                end = Offset(rect.right, rect.top + animatedProgress.value),
//                strokeWidth = 4.dp.toPx()
//            )
        }

        // 左上角的菜单按钮
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp, 34.dp, 16.dp, 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "菜单",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}