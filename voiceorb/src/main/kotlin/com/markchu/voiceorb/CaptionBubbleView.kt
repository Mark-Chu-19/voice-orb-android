package com.markchu.voiceorb

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

/**
 * A caption bubble that pairs with [VoiceOrbView]: shows what the assistant is saying (or what it
 * heard) in short, centered lines under the orb.
 *
 * Text set through [showCaption] is re-wrapped into fixed-length lines ([charsPerLine], default
 * 10) so the bubble keeps a compact, predictable silhouette regardless of display density or font
 * scaling — CJK text in particular benefits from hard line lengths because word-boundary wrapping
 * has nothing to break on. `BREAK_STRATEGY_SIMPLE` stops the platform from re-breaking the manual
 * pagination.
 *
 * ```kotlin
 * caption.showCaption("今天台北天氣晴，氣溫二十八度。")
 * caption.hideCaption()
 * ```
 */
class CaptionBubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr) {

    /** Fixed caption line length in characters; spaces are stripped before chunking. */
    var charsPerLine: Int = DEFAULT_CHARS_PER_LINE
        set(value) {
            require(value > 0) { "charsPerLine must be positive, was $value" }
            field = value
        }

    init {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER_HORIZONTAL
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        }
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background =
            GradientDrawable().apply {
                setColor(Color.argb(230, 22, 26, 34))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(150, 130, 150, 190))
            }
        visibility = GONE
    }

    /** Shows [text] re-wrapped to [charsPerLine] characters per centered line. */
    fun showCaption(text: CharSequence) {
        val compact = text.toString().replace(" ", "")
        if (compact.isEmpty()) {
            hideCaption()
            return
        }
        setText(compact.chunked(charsPerLine).joinToString("\n"))
        visibility = VISIBLE
    }

    /** Hides the bubble without clearing its last text. */
    fun hideCaption() {
        visibility = GONE
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val DEFAULT_CHARS_PER_LINE = 10
    }
}
