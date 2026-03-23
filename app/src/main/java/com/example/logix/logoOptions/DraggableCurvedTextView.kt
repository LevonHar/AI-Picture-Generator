package com.example.logix.logoOptions

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout

class DraggableCurvedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val curvedTextView: CurvedTextView
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private var gestureDetector: GestureDetector
    private val mainHandler = Handler(Looper.getMainLooper())
    private var doubleTapRunnable: Runnable? = null

    var onDoubleClickListener: ((DraggableCurvedTextView) -> Unit)? = null
    var onSingleClickListener: ((DraggableCurvedTextView) -> Unit)? = null
    var onLongClickListener: ((DraggableCurvedTextView) -> Unit)? = null

    var textTransparency: Int
        get() = ((1 - curvedTextView.alpha) * 100).toInt()
        set(value) {
            curvedTextView.alpha = (100 - value) / 100f
        }

    var backgroundColorWithAlpha: Int = Color.TRANSPARENT
        get() = background?.let {
            if (it is android.graphics.drawable.ColorDrawable) it.color else Color.TRANSPARENT
        } ?: Color.TRANSPARENT

    init {
        curvedTextView = CurvedTextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        addView(curvedTextView)

        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true

        setOnLongClickListener {
            onLongClickListener?.invoke(this)
            true
        }

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                doubleTapRunnable?.let { mainHandler.removeCallbacks(it) }
                onDoubleClickListener?.invoke(this@DraggableCurvedTextView)
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                bringToFront()
                onSingleClickListener?.invoke(this@DraggableCurvedTextView)
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = x - event.rawX
                dY = y - event.rawY
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX + dX - x
                val dy = event.rawY + dY - y
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (!isDragging && distance > 10) {
                    isDragging = true
                }

                if (isDragging) {
                    x = event.rawX + dX
                    y = event.rawY + dY
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setText(newText: String) {
        curvedTextView.setText(newText)
    }

    fun getText(): String = curvedTextView.getText()

    fun setTextColor(color: Int) {
        curvedTextView.setTextColor(color)
    }

    fun getTextColor(): Int = curvedTextView.getTextColor()

    fun setTextSize(size: Float) {
        curvedTextView.setTextSize(size)
    }

    fun getTextSizeValue(): Float = curvedTextView.getTextSizeValue()

    fun setTypeface(typeface: Typeface?) {
        curvedTextView.setTypeface(typeface)
    }

    fun getCustomTypeface(): Typeface? = curvedTextView.getCustomTypeface()

    fun setCurveRadius(radius: Float) {
        curvedTextView.setRadius(radius)
    }

    fun getCurveRadius(): Float = curvedTextView.getRadius()

    fun setCurveUp(up: Boolean) {
        curvedTextView.setCurveUp(up)
    }

    fun isCurveUp(): Boolean = curvedTextView.isCurveUp()

    fun setRotationDegrees(degrees: Float) {
        curvedTextView.setRotation(degrees)
    }

    fun getRotationDegrees(): Float = curvedTextView.getRotationDegrees()

    fun setTextAlpha(alpha: Float) {
        curvedTextView.alpha = alpha
    }

    fun getTextAlpha(): Float = curvedTextView.alpha

    fun setBackgroundColorWithAlpha(color: Int, alphaPercent: Int) {
        if (color != Color.TRANSPARENT) {
            val alpha = alphaPercent / 100f
            val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (color and 0x00FFFFFF)
            setBackgroundColor(colorWithAlpha)
        } else {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun getCurvedTextView(): CurvedTextView = curvedTextView
}