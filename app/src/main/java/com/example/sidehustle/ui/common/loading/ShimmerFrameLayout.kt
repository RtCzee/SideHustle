package com.example.sidehustle.ui.common.loading

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.provider.Settings
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.sidehustle.R

/**
 * A FrameLayout that sweeps a soft highlight across its children, used for skeleton
 * ("shimmer") placeholders. The highlight is only painted over pixels the children drew, so
 * the layout's own background is untouched.
 *
 * The animation runs only while the view is attached and actually visible, and is stopped
 * otherwise, so a hidden or destroyed skeleton costs nothing. If the user has turned
 * animations off in system settings the placeholders are shown static.
 */
class ShimmerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val highlightColor = ContextCompat.getColor(context, R.color.skeleton_highlight)
    private val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val shaderMatrix = Matrix()
    private var shader: LinearGradient? = null
    private var gradientWidth = 0f
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SWEEP_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientWidth = w * GRADIENT_WIDTH_FRACTION
        shader = if (gradientWidth > 0f) {
            val transparent = ColorUtils.setAlphaComponent(highlightColor, 0)
            LinearGradient(
                0f, 0f, gradientWidth, 0f,
                intArrayOf(transparent, highlightColor, transparent),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        } else {
            null
        }
        paint.shader = shader
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }

    // Fires when this view OR any ancestor becomes visible/hidden, which is what a skeleton
    // that is shown and hidden by LoadingStateHandler needs.
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        updateAnimation()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val currentShader = shader
        if (!animator.isRunning || currentShader == null || width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val saveCount = canvas.saveLayer(0f, 0f, w, h, null)
        super.dispatchDraw(canvas)

        // Slide the highlight from fully off the left edge to fully off the right edge.
        shaderMatrix.setTranslate(-gradientWidth + (w + gradientWidth) * progress, 0f)
        currentShader.setLocalMatrix(shaderMatrix)
        canvas.drawRect(0f, 0f, w, h, paint)
        canvas.restoreToCount(saveCount)
    }

    private fun updateAnimation() {
        if (isAttachedToWindow && isShown && animationsEnabled()) startShimmer() else stopShimmer()
    }

    private fun startShimmer() {
        if (!animator.isRunning) animator.start()
    }

    private fun stopShimmer() {
        if (animator.isRunning) animator.cancel()
        progress = 0f
        invalidate() // clear any highlight that is still painted
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f

    private companion object {
        const val SWEEP_DURATION_MS = 1300L
        const val GRADIENT_WIDTH_FRACTION = 0.6f
    }
}
