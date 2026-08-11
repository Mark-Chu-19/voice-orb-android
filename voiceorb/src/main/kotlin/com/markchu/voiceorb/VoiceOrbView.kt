package com.markchu.voiceorb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * An animated "voice orb" — the visual heart of a voice assistant.
 *
 * The orb renders as a layered composition:
 *  1. a soft radial **aura** that breathes with a slow sine rhythm,
 *  2. an **organic blob** whose outline is deformed by three sine harmonics so it feels alive
 *     rather than mechanically circular,
 *  3. state-specific ornaments — an orbiting dot trail while [OrbState.THINKING] and expanding
 *     pulse rings while [OrbState.SPEAKING].
 *
 * Each [OrbState] also carries a target presentation scale (small while idle, enlarged while
 * listening or speaking). The rendered scale eases toward the target every frame with exponential
 * smoothing, so state changes read as one continuous motion instead of a jump cut.
 *
 * While listening, feed microphone energy into [inputLevel] (0..1) and the blob amplitude reacts
 * to the user's voice in real time.
 *
 * The animation loop runs on [postInvalidateOnAnimation] and stops itself whenever the view is
 * detached or not visible, so an off-screen orb costs nothing.
 *
 * ```kotlin
 * val orb = VoiceOrbView(context)
 * orb.state = VoiceOrbView.OrbState.LISTENING
 * orb.inputLevel = normalizedMicRms
 * ```
 *
 * Colors are customizable in XML via `orbCoreColor`, `orbAuraColor` and `orbAccentColor`.
 */
class VoiceOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** The orb's behavioural state; each maps to a distinct animation vocabulary. */
    enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING }

    /** Current state. Setting a new value retargets the animation; no restart, no jump. */
    var state: OrbState = OrbState.IDLE
        set(value) {
            if (field != value) {
                field = value
                stateChangedAtMillis = SystemClock.uptimeMillis()
                invalidate()
            }
        }

    /**
     * Normalized voice energy in `0f..1f`, honoured while [OrbState.LISTENING].
     * Values are clamped; out-of-range input never breaks rendering.
     */
    var inputLevel: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private var coreColor = DEFAULT_CORE_COLOR
    private var auraColor = DEFAULT_AURA_COLOR
    private var accentColor = DEFAULT_ACCENT_COLOR

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val blobPath = android.graphics.Path()

    private val startUptimeMillis = SystemClock.uptimeMillis()
    private var stateChangedAtMillis = startUptimeMillis

    /** Smoothed presentation scale, eased toward the state target each frame. */
    private var renderedScale = 1f

    /** Smoothed input level so voice reactivity feels springy instead of jittery. */
    private var renderedLevel = 0f

    init {
        attrs?.let { set ->
            context.theme.obtainStyledAttributes(set, R.styleable.VoiceOrbView, defStyleAttr, 0)
                .apply {
                    try {
                        coreColor =
                            getColor(R.styleable.VoiceOrbView_orbCoreColor, DEFAULT_CORE_COLOR)
                        auraColor =
                            getColor(R.styleable.VoiceOrbView_orbAuraColor, DEFAULT_AURA_COLOR)
                        accentColor =
                            getColor(R.styleable.VoiceOrbView_orbAccentColor, DEFAULT_ACCENT_COLOR)
                    } finally {
                        recycle()
                    }
                }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height)
        if (size <= 0) return

        val t = (SystemClock.uptimeMillis() - startUptimeMillis) / 1000f
        renderedScale += (targetScale() - renderedScale) * SCALE_SMOOTHING
        renderedLevel += (inputLevel - renderedLevel) * LEVEL_SMOOTHING

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = size * BASE_RADIUS_FRACTION * renderedScale

        drawAura(canvas, cx, cy, baseRadius, t)
        drawBlob(canvas, cx, cy, baseRadius, t)
        when (state) {
            OrbState.THINKING -> drawOrbitingDots(canvas, cx, cy, baseRadius, t)
            OrbState.SPEAKING -> drawPulseRings(canvas, cx, cy, baseRadius, t)
            OrbState.IDLE, OrbState.LISTENING -> Unit
        }

        if (isShown) postInvalidateOnAnimation()
    }

    /** Soft breathing halo behind the blob; its reach swells slightly with voice input. */
    private fun drawAura(canvas: Canvas, cx: Float, cy: Float, radius: Float, t: Float) {
        val breath = 1f + 0.08f * sin(t * BREATH_SPEED) + 0.20f * renderedLevel
        val auraRadius = radius * AURA_RADIUS_FACTOR * breath
        auraPaint.shader =
            RadialGradient(
                cx,
                cy,
                auraRadius,
                intArrayOf(withAlpha(auraColor, 110), withAlpha(auraColor, 0)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(cx, cy, auraRadius, auraPaint)
    }

    /**
     * The organic body: a circle deformed by three sine harmonics of different frequency and
     * phase speed. Harmonic amplitudes grow with voice input while listening, so the blob
     * literally ripples to the user's speech.
     */
    private fun drawBlob(canvas: Canvas, cx: Float, cy: Float, radius: Float, t: Float) {
        val reactivity = if (state == OrbState.LISTENING) 1f + renderedLevel * 2.2f else 1f
        val wobble = radius * WOBBLE_FRACTION * reactivity

        blobPath.rewind()
        val steps = BLOB_VERTEX_COUNT
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
            val deform =
                wobble * (
                    0.55f * sin(3f * angle + t * 1.9f) +
                        0.30f * sin(5f * angle - t * 2.6f) +
                        0.15f * sin(8f * angle + t * 3.3f)
                )
            val r = radius + deform
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) blobPath.moveTo(x, y) else blobPath.lineTo(x, y)
        }
        blobPath.close()

        blobPaint.shader =
            RadialGradient(
                cx,
                cy - radius * 0.35f,
                radius * 1.6f,
                intArrayOf(lighten(coreColor, 0.35f), coreColor, darken(coreColor, 0.25f)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(blobPath, blobPaint)
    }

    /** THINKING ornament: a trail of dots orbiting the blob with a comet-style alpha falloff. */
    private fun drawOrbitingDots(canvas: Canvas, cx: Float, cy: Float, radius: Float, t: Float) {
        val orbitRadius = radius * 1.35f
        val head = t * ORBIT_SPEED
        for (i in 0 until ORBIT_DOT_COUNT) {
            val angle = head - i * 0.42f
            val fade = 1f - i.toFloat() / ORBIT_DOT_COUNT
            dotPaint.color = withAlpha(accentColor, (200 * fade).toInt())
            canvas.drawCircle(
                cx + orbitRadius * cos(angle),
                cy + orbitRadius * sin(angle),
                radius * 0.055f * (0.5f + fade),
                dotPaint,
            )
        }
    }

    /** SPEAKING ornament: rings born at the blob edge that expand and dissolve, like sound. */
    private fun drawPulseRings(canvas: Canvas, cx: Float, cy: Float, radius: Float, t: Float) {
        ringPaint.strokeWidth = radius * 0.05f
        for (i in 0 until PULSE_RING_COUNT) {
            val phase = ((t * PULSE_SPEED) + i.toFloat() / PULSE_RING_COUNT) % 1f
            val ringRadius = radius * (1.05f + phase * 0.55f)
            ringPaint.color = withAlpha(accentColor, ((1f - phase) * 150).toInt())
            canvas.drawCircle(cx, cy, ringRadius, ringPaint)
        }
    }

    /** Per-state presentation scale: subdued when idle, enlarged when engaged. */
    private fun targetScale(): Float =
        when (state) {
            OrbState.IDLE -> 0.82f
            OrbState.LISTENING -> 1.0f
            OrbState.THINKING -> 0.90f
            OrbState.SPEAKING -> 1.04f
        }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Restart the self-driving invalidate loop when the orb becomes visible again.
        if (visibility == VISIBLE) postInvalidateOnAnimation()
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun lighten(color: Int, amount: Float): Int =
        Color.rgb(
            (Color.red(color) + (255 - Color.red(color)) * amount).toInt(),
            (Color.green(color) + (255 - Color.green(color)) * amount).toInt(),
            (Color.blue(color) + (255 - Color.blue(color)) * amount).toInt(),
        )

    private fun darken(color: Int, amount: Float): Int =
        Color.rgb(
            (Color.red(color) * (1 - amount)).toInt(),
            (Color.green(color) * (1 - amount)).toInt(),
            (Color.blue(color) * (1 - amount)).toInt(),
        )

    private companion object {
        val DEFAULT_CORE_COLOR = Color.rgb(64, 156, 255)
        val DEFAULT_AURA_COLOR = Color.rgb(64, 156, 255)
        val DEFAULT_ACCENT_COLOR = Color.rgb(140, 210, 255)

        /** Blob radius as a fraction of the view's short edge, before state scaling. */
        const val BASE_RADIUS_FRACTION = 0.28f

        /** Aura reach relative to the blob radius. */
        const val AURA_RADIUS_FACTOR = 1.9f

        /** Peak outline deformation as a fraction of the blob radius. */
        const val WOBBLE_FRACTION = 0.10f

        /** Polygon resolution of the blob outline; 90 vertices renders visually smooth. */
        const val BLOB_VERTEX_COUNT = 90

        /** Fraction of the remaining gap applied per frame (~0.3 s perceived transition). */
        const val SCALE_SMOOTHING = 0.14f

        /** Faster smoothing for voice level so reactivity feels immediate but not jittery. */
        const val LEVEL_SMOOTHING = 0.35f

        const val BREATH_SPEED = 1.6f
        const val ORBIT_SPEED = 3.4f
        const val ORBIT_DOT_COUNT = 7
        const val PULSE_RING_COUNT = 3
        const val PULSE_SPEED = 0.55f
    }
}
