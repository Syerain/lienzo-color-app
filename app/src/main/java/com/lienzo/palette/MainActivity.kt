package com.lienzo.palette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.lienzo.palette.data.ColorRepository
import com.lienzo.palette.ui.add.AddColorScreen
import com.lienzo.palette.ui.detail.DetailScreen
import com.lienzo.palette.ui.home.HomeScreen
import com.lienzo.palette.ui.theme.LienzoTheme
import com.lienzo.palette.ui.theme.RhineBlue
import kotlinx.coroutines.delay

/** 全局 UI 设置：动画速率（0.5x–2x），导航转场时长按此缩放 */
object UiSettings {
    var animSpeed by mutableStateOf(1f)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 立即从启动屏主题切到常规主题：窗口背景不再是莱茵蓝，
        // 导航/切换动画期间不会闪烁蓝底（冷启动首帧仍由启动屏主题保证蓝色）
        setTheme(R.style.Theme_Lienzo)
        enableEdgeToEdge()
        ColorRepository.load(this)
        UiSettings.animSpeed = getSharedPreferences("lienzo_ui", 0).getFloat("animSpeed", 1f)
        setContent {
            LienzoTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }
    val speed = UiSettings.animSpeed
    fun dur(base: Int) = (base / speed).toInt().coerceAtLeast(30)

    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = {
            fadeIn(tween(dur(220))) + slideInHorizontally(tween(dur(300))) { it / 10 }
        },
        exitTransition = { fadeOut(tween(dur(160))) },
        popEnterTransition = { fadeIn(tween(dur(220))) },
        popExitTransition = {
            fadeOut(tween(dur(160))) + slideOutHorizontally(tween(dur(300))) { it / 10 }
        }
    ) {
        composable("home") {
            HomeScreen(
                onAdd = { nav.navigate("add") },
                onOpenColor = { id -> nav.navigate("detail/$id") }
            )
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            DetailScreen(
                colorId = entry.arguments?.getString("id") ?: "",
                onBack = { nav.popBackStack() }
            )
        }
        composable("add") {
            AddColorScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }
    }

    if (showSplash) {
        SplashScreen(onDone = { showSplash = false })
    }
}

/** 开屏：莱茵蓝底 + 白色衬线大写 LIENZO（淡出） */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "splashAlpha"
    )
    LaunchedEffect(Unit) {
        delay(1300)
        visible = false
        delay(420)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhineBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.background(Color.Transparent)
        ) {
            Text(
                text = "LIENZO",
                color = Color.White.copy(alpha = alpha),
                fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 10.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "PALETTE",
                color = Color.White.copy(alpha = alpha * 0.65f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = 8.sp
            )
        }
    }
}
