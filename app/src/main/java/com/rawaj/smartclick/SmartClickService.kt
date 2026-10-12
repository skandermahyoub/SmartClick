package com.rawaj.smartclick

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SmartClickService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastClickAt = 0L
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val runner = object : Runnable {
        override fun run() {
            if (!prefs.getBoolean(KEY_ENABLED, false)) return
            val target = prefs.getString(KEY_TARGET, "").orEmpty()
            val interval = prefs.getLong(KEY_INTERVAL, 1500L).coerceAtLeast(500L)
            if (target.isNotBlank()) tryClick(target)
            handler.postDelayed(this, interval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(runner)
        handler.post(runner)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // The scheduled runner only acts while the user has explicitly enabled it.
    }

    private fun tryClick(target: String) {
        val root = rootInActiveWindow ?: return
        try {
            val nodes = root.findAccessibilityNodeInfosByText(target) ?: return
            for (node in nodes) {
                if (!node.isVisibleToUser) continue
                var candidate: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
                while (candidate != null) {
                    if (candidate.isClickable && candidate.isEnabled) {
                        val now = System.currentTimeMillis()
                        if (now - lastClickAt >= 450L) {
                            if (candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) lastClickAt = now
                        }
                        candidate.recycle()
                        return
                    }
                    val parent = candidate.parent
                    candidate.recycle()
                    candidate = parent
                }
            }
        } catch (_: Exception) {
            // Some apps do not expose their UI hierarchy to accessibility services.
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        handler.removeCallbacks(runner)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runner)
        super.onDestroy()
    }

    companion object {
        const val PREFS = "smartclick_settings"
        const val KEY_TARGET = "target_text"
        const val KEY_INTERVAL = "interval_ms"
        const val KEY_ENABLED = "enabled"
    }
}
