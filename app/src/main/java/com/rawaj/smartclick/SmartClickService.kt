package com.rawaj.smartclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SmartClickService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastClickAt = 0L
    private var gestureInProgress = false
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val runner = object : Runnable {
        override fun run() {
            if (!prefs.getBoolean(KEY_ENABLED, false)) {
                handler.postDelayed(this, 500L)
                return
            }

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    private fun tryClick(target: String) {
        if (gestureInProgress || System.currentTimeMillis() - lastClickAt < 450L) return
        val root = rootInActiveWindow ?: return
        try {
            val nodes = root.findAccessibilityNodeInfosByText(target) ?: return
            for (node in nodes) {
                if (!node.isVisibleToUser) {
                    node.recycle()
                    continue
                }

                var candidate: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
                var clickTarget: AccessibilityNodeInfo? = null
                while (candidate != null) {
                    if (candidate.isVisibleToUser && candidate.isEnabled && candidate.isClickable) {
                        clickTarget = AccessibilityNodeInfo.obtain(candidate)
                        val clicked = candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        candidate.recycle()
                        candidate = null
                        if (clicked) {
                            lastClickAt = System.currentTimeMillis()
                            clickTarget.recycle()
                            node.recycle()
                            return
                        }
                        break
                    }
                    val parent = candidate.parent
                    candidate.recycle()
                    candidate = parent
                }

                val bounds = Rect()
                (clickTarget ?: node).getBoundsInScreen(bounds)
                clickTarget?.recycle()
                if (bounds.width() > 0 && bounds.height() > 0) {
                    node.recycle()
                    dispatchTap(bounds)
                    return
                }
                node.recycle()
            }
        } catch (_: Exception) {
            // Some apps expose limited accessibility information.
        } finally {
            root.recycle()
        }
    }

    private fun dispatchTap(bounds: Rect) {
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
            .build()

        gestureInProgress = true
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                lastClickAt = System.currentTimeMillis()
                gestureInProgress = false
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                gestureInProgress = false
            }
        }, handler)
        if (!accepted) gestureInProgress = false
    }

    override fun onInterrupt() {
        handler.removeCallbacks(runner)
        handler.postDelayed(runner, 500L)
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
