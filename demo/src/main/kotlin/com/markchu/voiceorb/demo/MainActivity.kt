package com.markchu.voiceorb.demo

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.markchu.voiceorb.CaptionBubbleView
import com.markchu.voiceorb.VoiceOrbView

/**
 * Interactive showcase for [VoiceOrbView] and [CaptionBubbleView]:
 * four buttons drive the orb through its states, and a slider simulates microphone
 * energy so the LISTENING blob can be seen reacting to "voice".
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val orb = VoiceOrbView(this)
        val caption = CaptionBubbleView(this)

        val stateRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                VoiceOrbView.OrbState.entries.forEach { orbState ->
                    addView(
                        Button(context).apply {
                            text = orbState.name.lowercase().replaceFirstChar { it.uppercase() }
                            isAllCaps = false
                            setOnClickListener {
                                orb.state = orbState
                                when (orbState) {
                                    VoiceOrbView.OrbState.SPEAKING ->
                                        caption.showCaption("今天台北天氣晴朗，氣溫二十八度，適合出門走走。")
                                    else -> caption.hideCaption()
                                }
                            }
                        },
                        LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                            marginStart = dp(4)
                            marginEnd = dp(4)
                        },
                    )
                }
            }

        val levelLabel =
            TextView(this).apply {
                text = "Voice level (drives the LISTENING blob)"
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val levelSlider =
            SeekBar(this).apply {
                max = 100
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                            orb.inputLevel = value / 100f
                        }

                        override fun onStartTrackingTouch(bar: SeekBar?) = Unit

                        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                    },
                )
            }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.rgb(10, 12, 18))
                setPadding(dp(16), dp(24), dp(16), dp(24))
                addView(
                    FrameLayout(context).apply { addView(orb) },
                    LinearLayout.LayoutParams(MATCH, 0, 1f),
                )
                addView(
                    caption,
                    LinearLayout.LayoutParams(WRAP, WRAP).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(16)
                    },
                )
                addView(levelLabel, LinearLayout.LayoutParams(MATCH, WRAP))
                addView(
                    levelSlider,
                    LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(16) },
                )
                addView(stateRow, LinearLayout.LayoutParams(MATCH, WRAP))
            }

        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
