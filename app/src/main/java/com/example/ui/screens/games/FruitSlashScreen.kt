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
import com.example.data.local.GameMatchResult

class FruitSlashWebBridge(
    private val getOnRecordMatch: () -> (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit,
    private val getOnExit: () -> () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMatchEnd(outcomeStr: String, score: Int, vsBot: Boolean, botAccuracy: Int) {
        val outcome = when (outcomeStr.uppercase()) {
            "WIN" -> GameMatchResult.WIN
            "DRAW" -> GameMatchResult.DRAW
            else -> GameMatchResult.LOSS
        }
        mainHandler.post {
            getOnRecordMatch().invoke(outcome, score, vsBot, botAccuracy)
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
fun FruitSlashScreen(
    mode: String = "classic",
    botAccuracy: Int = 80,
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentOnRecordMatch by rememberUpdatedState(onRecordMatch)
    val currentOnExit by rememberUpdatedState(onExit)

    // Lock to portrait for standard fruit ninja slicing feel
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler {
        currentOnExit()
    }

    val bridge = remember {
        FruitSlashWebBridge(
            getOnRecordMatch = { currentOnRecordMatch },
            getOnExit = { currentOnExit }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
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
                    setBackgroundColor(0xFF0F172A.toInt())

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mediaPlaybackRequiresUserGesture = false
                    }

                    addJavascriptInterface(bridge, "AndroidBridge")

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                        }
                    }

                    val safeMode = mode.trim().lowercase().ifBlank { "classic" }
                    loadUrl("https://appassets.androidplatform.net/assets/fruitslash/index.html?mode=$safeMode&botAcc=$botAccuracy")
                }
            },
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
