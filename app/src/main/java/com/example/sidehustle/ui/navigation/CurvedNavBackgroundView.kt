package com.example.sidehustle.ui.navigation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.sidehustle.R

/**
 * Paints the floating nav island: clear frosted glass fill + cyan edge highlight.
 * The top edge curves upward at [notchCenterX] to create the active-tab cutout.
 */
class CurvedNavBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    // Clear glass — white alpha in nav_bar_glass_fill lets content show through
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.nav_bar_glass_fill)
        setShadowLayer(14f, 0f, 8f, 0x40000000)
    }

    // Thin cyan border sells the glass look
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.nav_bar_glass_stroke)
    }

    // Soft highlight along the top rim
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * resources.displayMetrics.density
        alpha = 60
    }

    private val path = Path()
    private var animatedNotchCenterX = 0f

    private val cornerRadius = resources.getDimension(R.dimen.curved_nav_corner_radius)
    private val curveWidth = resources.getDimension(R.dimen.curved_nav_bubble_size) * 1.35f
    private val curveDepth = resources.getDimension(R.dimen.curved_nav_bubble_overflow) * 0.85f
    private val topInset = resources.getDimension(R.dimen.curved_nav_bubble_overflow)

    init {
        // Shadow requires software layer on older devices
        setLayerType(LAYER_TYPE_SOFTWARE, barPaint)
    }

    /** Moves the curved notch to [x], optionally animating over 380 ms. */
    fun setNotchCenter(x: Float, animate: Boolean) {
        if (!animate) {
            animatedNotchCenterX = x
            invalidate()
            return
        }

        ValueAnimator.ofFloat(animatedNotchCenterX, x).apply {
            duration = 380L
            addUpdateListener {
                animatedNotchCenterX = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        buildPath(width.toFloat(), height.toFloat())
        canvas.drawPath(path, barPaint)
        canvas.drawPath(path, borderPaint)

        // Cyan-to-transparent rim highlight
        highlightPaint.shader = LinearGradient(
            0f, topInset, width.toFloat(), topInset,
            ContextCompat.getColor(context, R.color.cyan),
            ContextCompat.getColor(context, android.R.color.transparent),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, highlightPaint)
        highlightPaint.shader = null
    }

    /** Builds a rounded rect with a cubic-bezier bump at the active tab. */
    private fun buildPath(width: Float, height: Float) {
        path.reset()

        val top = topInset
        val bottom = height
        val center = if (animatedNotchCenterX > 0f) animatedNotchCenterX else width / 2f
        val halfCurve = curveWidth / 2f
        val leftCurve = center - halfCurve
        val rightCurve = center + halfCurve

        path.moveTo(0f, top + cornerRadius)
        path.quadTo(0f, top, cornerRadius, top)

        if (leftCurve > cornerRadius) path.lineTo(leftCurve, top)

        path.cubicTo(
            leftCurve + halfCurve * 0.28f, top,
            leftCurve + halfCurve * 0.28f, top - curveDepth,
            center, top - curveDepth,
        )
        path.cubicTo(
            rightCurve - halfCurve * 0.28f, top - curveDepth,
            rightCurve - halfCurve * 0.28f, top,
            rightCurve, top,
        )

        if (rightCurve < width - cornerRadius) path.lineTo(width - cornerRadius, top)

        path.quadTo(width, top, width, top + cornerRadius)
        path.lineTo(width, bottom - cornerRadius)
        path.quadTo(width, bottom, width - cornerRadius, bottom)
        path.lineTo(cornerRadius, bottom)
        path.quadTo(0f, bottom, 0f, bottom - cornerRadius)
        path.close()
    }

    /** Y position where the active icon bubble should sit. */
    fun bubbleCenterY(): Float = topInset - curveDepth * 0.15f
}
