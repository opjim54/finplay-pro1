package com.example.security

import android.app.Activity
import android.os.Build
import android.os.Debug
import android.view.WindowManager
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class SecurityAuditReport(
    val isDebuggerAttached: Boolean,
    val isRootIndicatorFound: Boolean,
    val isTestKeysBuild: Boolean,
    val pbkdf2HardeningActive: Boolean = true,
    val failedAdminAttempts: Int = 0,
    val lockoutRemainingSeconds: Long = 0L,
    val screenShieldEnabled: Boolean = true
) {
    val integrityScore: Int
        get() {
            var score = 100
            if (isDebuggerAttached) score -= 20
            if (isRootIndicatorFound) score -= 25
            if (isTestKeysBuild) score -= 10
            if (!screenShieldEnabled) score -= 10
            return score.coerceIn(0, 100)
        }

    val statusLabel: String
        get() = when {
            integrityScore >= 90 -> "FORTIFIED"
            integrityScore >= 70 -> "PROTECTED"
            else -> "ELEVATED RISK"
        }
}

/**
 * Application-wide Anti-Tamper, Brute-Force Lockout & Screen-Capture Protection Shield.
 */
object SecurityShield {
    private const val MAX_FAILED_ADMIN_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 60_000L

    private val failedAdminAttempts = AtomicInteger(0)
    private val lockedUntilMs = AtomicLong(0L)

    fun remainingAdminLockoutSeconds(): Long {
        val now = System.currentTimeMillis()
        val until = lockedUntilMs.get()
        return if (until > now) ((until - now) / 1000L) + 1L else 0L
    }

    fun recordFailedAdminAttempt(): Long {
        val count = failedAdminAttempts.incrementAndGet()
        if (count >= MAX_FAILED_ADMIN_ATTEMPTS) {
            val multiplier = (count - MAX_FAILED_ADMIN_ATTEMPTS + 1).coerceAtMost(5)
            val lockDuration = BASE_LOCKOUT_MS * multiplier
            lockedUntilMs.set(System.currentTimeMillis() + lockDuration)
            return lockDuration / 1000L
        }
        return 0L
    }

    fun resetAdminAttempts() {
        failedAdminAttempts.set(0)
        lockedUntilMs.set(0L)
    }

    fun applyScreenCaptureShield(activity: Activity, enabled: Boolean) {
        runCatching {
            if (enabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    fun runIntegrityAudit(screenShieldEnabled: Boolean = true): SecurityAuditReport {
        val debugger = runCatching { Debug.isDebuggerConnected() || Debug.waitingForDebugger() }.getOrDefault(false)
        val testKeys = Build.TAGS?.contains("test-keys") == true
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        val rootFound = runCatching { suPaths.any { File(it).exists() } }.getOrDefault(false)
        return SecurityAuditReport(
            isDebuggerAttached = debugger,
            isRootIndicatorFound = rootFound,
            isTestKeysBuild = testKeys,
            pbkdf2HardeningActive = true,
            failedAdminAttempts = failedAdminAttempts.get(),
            lockoutRemainingSeconds = remainingAdminLockoutSeconds(),
            screenShieldEnabled = screenShieldEnabled
        )
    }
}
