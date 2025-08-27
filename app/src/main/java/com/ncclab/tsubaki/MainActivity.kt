package com.ncclab.tsubaki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ncclab.tsubaki.ui.screen.ResultScreen
import com.ncclab.tsubaki.ui.screen.ScannerScreen
import com.ncclab.tsubaki.ui.theme.TsubakiTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint // This annotation is required for Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TsubakiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ✅ 设置应用导航
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "scanner") {
        // 扫描页面
        composable("scanner") {
            ScannerScreen(
                onScanResult = { format, rawValue ->
                    // 对内容进行URL编码以安全传递
                    val encodedValue = URLEncoder.encode(rawValue, "UTF-8")
                    navController.navigate("result/$format/$encodedValue")
                }
            )
        }
        // 结果页面
        composable(
            route = "result/{format}/{rawValue}",
            arguments = listOf(
                navArgument("format") { type = NavType.StringType },
                navArgument("rawValue") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val format = backStackEntry.arguments?.getString("format") ?: ""
            val rawValue = backStackEntry.arguments?.getString("rawValue") ?: ""
            // 对接收到的内容进行URL解码
            val decodedValue = URLDecoder.decode(rawValue, "UTF-8")

            ResultScreen(
                format = format,
                rawValue = decodedValue,
                onNavigateBack = {
                    navController.popBackStack() // 返回上一个页面
                }
            )
        }
    }
}