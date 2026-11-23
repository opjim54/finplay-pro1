package com.example.ui.screens.games

import android.annotation.SuppressLint
import android.app.Activity
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
import com.example.data.local.GameMatchResult

class FruitSlashWebBridge(
    private val onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit,
    private val onExit: () -> Unit
) {
    @JavascriptInterface
    fun onMatchEnd(outcomeStr: String, score: Int, vsBot: Boolean, botAccuracy: Int) {
        val outcome = when (outcomeStr.uppercase()) {
            "WIN" -> GameMatchResult.WIN
            "DRAW" -> GameMatchResult.DRAW
            else -> GameMatchResult.LOSS
        }
        onRecordMatch(outcome, score, vsBot, botAccuracy)
    }

    @JavascriptInterface
    fun exitToArena() {
        onExit()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FruitSlashScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Lock to portrait for standard fruit ninja slicing feel
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler {
        onExit()
    }

    val bridge = remember { FruitSlashWebBridge(onRecordMatch, onExit) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
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
                    webViewClient = object : WebViewClient() {}

                    loadUrl("file:///android_asset/fruitslash/index.html")
                }
            }
        )
    }
}
