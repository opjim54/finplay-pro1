package com.example.ui.screens.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.local.GameMatchResult

class WarLandWebBridge(
    private val onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit,
    private val onExit: () -> Unit
) {
    @JavascriptInterface
    fun onMatchResult(kills: Int, rank: Int, coins: Double) {
        val outcome = if (rank == 1) GameMatchResult.WIN else GameMatchResult.LOSS
        val score = (kills * 100) + if (rank == 1) 500 else (30 - rank) * 10
        onRecordMatch(outcome, score, true, 85)
    }

    @JavascriptInterface
    fun exitToArena() {
        onExit()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WarLandScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Auto lock to Landscape mode and hide system bars for immersive gameplay
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        onExit()
    }

    val bridge = remember { WarLandWebBridge(onRecordMatch, onExit) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0xFF000000.toInt())

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Pass default gamer alias and avatar to Three.js game engine
                            view?.evaluateJavascript(
                                "if(window.setPlayerData) window.setPlayerData('Warrior_Pro', '', 'war_char_commando', 'war_gun_dragon_ak');",
                                null
                            )
                        }
                    }

                    addJavascriptInterface(bridge, "AndroidBridge")
                    loadUrl("file:///android_asset/warland/index.html")
                }
            },
            update = { /* no-op */ }
        )
    }
}
