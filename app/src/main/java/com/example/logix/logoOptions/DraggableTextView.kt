package com.example.logix.logoOptions

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import android.os.Handler
import android.os.Looper

class DraggableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private var gestureDetector: GestureDetector
    private val mainHandler = Handler(Looper.getMainLooper())
    private var doubleTapRunnable: Runnable? = null

    var onDoubleClickListener: ((DraggableTextView) -> Unit)? = null
    var onSingleClickListener: ((DraggableTextView) -> Unit)? = null
    var onLongClickListener: ((DraggableTextView) -> Unit)? = null

    var textTransparency: Int
        get() = ((1 - alpha) * 100).toInt()
        set(value) {
            alpha = (100 - value) / 100f
        }

    init {
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
                onDoubleClickListener?.invoke(this@DraggableTextView)
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                bringToFront()
                onSingleClickListener?.invoke(this@DraggableTextView)
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
}