package com.example.raylicallservice.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.GridView

class DraggableGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridView(context, attrs, defStyleAttr) {

    private var dragListener: OnDragListener? = null
    private var draggedItem: View? = null
    private var draggedPosition = -1

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x.toInt()
                val y = ev.y.toInt()
                val position = pointToPosition(x, y)
                if (position != INVALID_POSITION) {
                    draggedPosition = position
                    draggedItem = getChildAt(position - firstVisiblePosition)
                    draggedItem?.alpha = 0.5f
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedItem != null) {
                    val x = ev.x.toInt()
                    val y = ev.y.toInt()
                    val position = pointToPosition(x, y)
                    if (position != INVALID_POSITION && position != draggedPosition) {
                        dragListener?.onDrag(draggedPosition, position)
                        draggedPosition = position
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedItem?.alpha = 1.0f
                draggedItem = null
                draggedPosition = -1
            }
        }
        return super.onTouchEvent(ev)
    }

    fun setOnDragListener(listener: OnDragListener) {
        dragListener = listener
    }

    interface OnDragListener {
        fun onDrag(fromPosition: Int, toPosition: Int)
    }
} 