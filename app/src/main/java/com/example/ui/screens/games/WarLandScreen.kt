package com.example.ui.screens.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.local.GameMatchResult

class WarLandWebBridge(
    private val getOnRecordMatch: () -> (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit,
    private val getOnExit: () -> () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMatchResult(kills: Int, rank: Int, coins: Double) {
        val outcome = if (rank == 1) GameMatchResult.WIN else GameMatchResult.LOSS
        val score = (kills * 100) + if (rank == 1) 500 else ((30 - rank).coerceAtLeast(1) * 10)
        mainHandler.post {
            getOnRecordMatch().invoke(outcome, score, true, 85)
        }
    }

    @JavascriptInterface
    fun exitToArena() {
        mainHandler.post {
            getOnExit().invoke()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WarLandScreen(
    playerDisplayName: String = "Warrior_Pro",
    equippedCharSkin: String = "war_char_commando",
    equippedGunSkin: String = "war_gun_dragon_ak",
    mode: String = "TOURNAMENT",
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentOnRecordMatch by rememberUpdatedState(onRecordMatch)
    val currentOnExit by rememberUpdatedState(onExit)

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
            activity?.requestedOrientation = originalOrientation
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        currentOnExit()
    }

    val bridge = remember {
        WarLandWebBridge(
            getOnRecordMatch = { currentOnRecordMatch },
            getOnExit = { currentOnExit }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .build()

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
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val cleanName = playerDisplayName.replace("'", "")
                            val cleanSkin = equippedCharSkin.replace("'", "")
                            val cleanGun = equippedGunSkin.replace("'", "")
                            view?.evaluateJavascript(
                                "if(window.setPlayerData) window.setPlayerData('$cleanName', '', '$cleanSkin', '$cleanGun');",
                                null
                            )
                        }
                    }

                    addJavascriptInterface(bridge, "AndroidBridge")
                    val safeMode = mode.trim().uppercase().ifBlank { "TOURNAMENT" }
                    loadUrl("https://appassets.androidplatform.net/assets/warland/index.html?mode=$safeMode")
                }
            },
            update = { /* no-op */ },
            onRelease = { webView ->
                webView.stopLoading()
                webView.removeJavascriptInterface("AndroidBridge")
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.destroy()
            }
        )
    }
}
