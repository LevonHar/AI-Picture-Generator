package com.example.logix.logoOptions

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

class CurvedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var text = ""
    private var textColor = Color.BLACK
    private var textSize = 24f
    private var typeface: Typeface? = null
    private var radius = 200f
    private var curveUp = true
    private var rotation = 0f

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var textWidth = 0f

    init {
        paint.textAlign = Paint.Align.CENTER
    }

    fun setText(newText: String) {
        text = newText
        updateTextWidth()
        invalidate()
    }

    fun getText(): String = text

    fun setTextColor(color: Int) {
        textColor = color
        paint.color = color
        invalidate()
    }

    fun getTextColor(): Int = textColor

    fun setTextSize(size: Float) {
        textSize = size
        paint.textSize = size * resources.displayMetrics.scaledDensity
        updateTextWidth()
        invalidate()
    }

    fun getTextSizeValue(): Float = textSize

    fun setTypeface(tf: Typeface?) {
        typeface = tf
        paint.typeface = tf
        updateTextWidth()
        invalidate()
    }

    fun getCustomTypeface(): Typeface? = typeface

    fun setRadius(newRadius: Float) {
        radius = newRadius
        invalidate()
    }

    fun getRadius(): Float = radius

    fun setCurveUp(up: Boolean) {
        curveUp = up
        invalidate()
    }

    fun isCurveUp(): Boolean = curveUp

    override fun setRotation(degrees: Float) {
        rotation = degrees
        invalidate()
    }

    fun getRotationDegrees(): Float = rotation

    private fun updateTextWidth() {
        textWidth = paint.measureText(text)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (text.isEmpty()) return

        canvas.save()
        canvas.rotate(rotation, width / 2f, height / 2f)

        val centerX = width / 2f
        val centerY = height / 2f

        path.reset()

        if (curveUp) {
            // Arc curving upward
            val startAngle = 180f - (textWidth * 180f) / (Math.PI.toFloat() * radius * 2) * 0.5f
            val sweepAngle = (textWidth * 180f) / (Math.PI.toFloat() * radius)
            path.addArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle
            )
        } else {
            // Arc curving downward
            val startAngle = 0f
            val sweepAngle = -(textWidth * 180f) / (Math.PI.toFloat() * radius)
            path.addArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle
            )
        }

        canvas.drawTextOnPath(text, path, 0f, 0f, paint)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (radius * 2 + 100).toInt()
        val desiredHeight = (radius * 2 + 100).toInt()

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }
}