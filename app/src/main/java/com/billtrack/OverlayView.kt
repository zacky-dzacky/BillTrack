package com.billtrack

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val transparentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f // Adjust stroke width as needed
    }

    private val cardAspectRatio = 85.6f / 53.98f
    private var internalFrameRect = RectF() // Renamed to avoid confusion
    private var frameCornerRadius = 24f

    init {
        backgroundPaint.color = Color.parseColor("#A0000000")
        framePaint.color = Color.WHITE
    }

    // Public getter for the frame rectangle relative to this view's bounds
    fun getFrameRectRelativeToView(): RectF {
        // Ensure onDraw has been called and the rect is calculated
        // This might return an empty or default rect if called before layout/draw
        return RectF(internalFrameRect) // Return a copy
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val frameWidth: Float
        val frameHeight: Float

        val desiredFrameWidth = viewWidth * 0.9f
        val desiredFrameHeight = desiredFrameWidth / cardAspectRatio

        if (desiredFrameHeight <= viewHeight * 0.9f) {
            frameWidth = desiredFrameWidth
            frameHeight = desiredFrameHeight
        } else {
            frameHeight = viewHeight * 0.9f
            frameWidth = frameHeight * cardAspectRatio
        }

        val left = (viewWidth - frameWidth) / 2
        val top = (viewHeight - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight
        internalFrameRect.set(left, top, right, bottom)

        canvas.drawRect(0f, 0f, viewWidth, viewHeight, backgroundPaint)
        canvas.drawRoundRect(internalFrameRect, frameCornerRadius, frameCornerRadius, transparentPaint)
        canvas.drawRoundRect(internalFrameRect, frameCornerRadius, frameCornerRadius, framePaint)
    }
}