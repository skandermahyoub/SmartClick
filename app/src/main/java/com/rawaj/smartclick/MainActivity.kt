package com.rawaj.smartclick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var targetInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences(SmartClickService.PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }
        layout.addView(TextView(this).apply {
            text = "SmartClick Assistant"
            textSize = 25f
            gravity = Gravity.CENTER
        }, match())
        layout.addView(TextView(this).apply {
            text = "أداة نقر آلي بإذن المستخدم عبر إمكانية الوصول"
            textSize = 16f
            gravity = Gravity.CENTER
        }, margins(top = pad / 2, bottom = pad))
        targetInput = EditText(this).apply {
            hint = "النص الظاهر على الزر المراد نقره"
            textSize = 16f
            setSingleLine(true)
        }
        layout.addView(targetInput, match())
        intervalInput = EditText(this).apply {
            hint = "الفاصل بين النقرات بالمللي ثانية (مثلاً 1500)"
            setSingleLine(true)
            setText("1500")
            inputType = 2
        }
        layout.addView(intervalInput, margins(top = pad / 2))
        val accessibility = Button(this).apply {
            text = "فتح إعدادات إمكانية الوصول"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        layout.addView(accessibility, margins(top = pad))
        layout.addView(Button(this).apply {
            text = "بدء النقر"
            setOnClickListener {
                val target = targetInput.text.toString().trim()
                val interval = intervalInput.text.toString().toLongOrNull() ?: 1500L
                if (target.isBlank()) {
                    status.text = "أدخل النص المطلوب أولاً."
                } else if (interval < 500L) {
                    status.text = "اجعل الفاصل 500 مللي ثانية أو أكثر."
                } else {
                    prefs.edit().putString(SmartClickService.KEY_TARGET, target)
                        .putLong(SmartClickService.KEY_INTERVAL, interval)
                        .putBoolean(SmartClickService.KEY_ENABLED, true).apply()
                    status.text = "تم التفعيل. افتح التطبيق المستهدف؛ سيحاول النقر على النص المطابق."
                }
            }
        }, margins(top = pad / 2))
        layout.addView(Button(this).apply {
            text = "إيقاف النقر"
            setOnClickListener {
                prefs.edit().putBoolean(SmartClickService.KEY_ENABLED, false).apply()
                status.text = "تم إيقاف النقر."
            }
        }, margins(top = pad / 2))
        status = TextView(this).apply {
            text = "يجب تفعيل الخدمة يدويًا من إعدادات إمكانية الوصول. لا توجد نافذة عائمة."
            textSize = 14f
            gravity = Gravity.CENTER
        }
        layout.addView(status, margins(top = pad))
        setContentView(layout)
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
    private fun margins(top: Int = 0, bottom: Int = 0) =
        match().apply { topMargin = top; bottomMargin = bottom }
}
