// com/example/logix/undo/TextCommand.kt
package com.example.logix.undo

import android.view.ViewGroup
import com.example.logix.logoOptions.DraggableCurvedTextView
import com.example.logix.logoOptions.DraggableTextView
import com.example.logix.logoOptions.TextOptionsDialog

data class TextViewState(
    val textView: DraggableTextView,
    val text: String,
    val color: Int,
    val textSize: Float,
    val transparency: Int,
    val rotation: Float,
    val bgColor: Int,
    val bgTransparency: Int,
    val hasBackground: Boolean,
    val x: Float,
    val y: Float,
    val fontIndex: Int
)

data class CurvedTextViewState(
    val curvedTextView: DraggableCurvedTextView,
    val text: String,
    val color: Int,
    val textSize: Float,
    val transparency: Int,
    val rotation: Float,
    val bgColor: Int,
    val bgTransparency: Int,
    val hasBackground: Boolean,
    val curveRadius: Float,
    val curveUp: Boolean,
    val x: Float,
    val y: Float,
    val fontIndex: Int
)

class AddTextCommand(
    private val container: ViewGroup,
    private val textView: DraggableTextView,
    private val onTextAdded: (DraggableTextView) -> Unit
) : UndoRedoManager.UndoableCommand {

    override fun execute() {
        container.addView(textView)
        onTextAdded(textView)
    }

    override fun undo() {
        container.removeView(textView)
    }

    override fun redo() {
        container.addView(textView)
        onTextAdded(textView)
    }
}

class AddCurvedTextCommand(
    private val container: ViewGroup,
    private val curvedTextView: DraggableCurvedTextView,
    private val onTextAdded: (DraggableCurvedTextView) -> Unit
) : UndoRedoManager.UndoableCommand {

    override fun execute() {
        container.addView(curvedTextView)
        onTextAdded(curvedTextView)
    }

    override fun undo() {
        container.removeView(curvedTextView)
    }

    override fun redo() {
        container.addView(curvedTextView)
        onTextAdded(curvedTextView)
    }
}

class UpdateTextViewCommand(
    private val textView: DraggableTextView,
    private val previousState: TextViewState,
    private val newState: TextViewState,
    private val onUpdate: (TextViewState) -> Unit
) : UndoRedoManager.UndoableCommand {

    override fun execute() {
        applyState(newState)
    }

    override fun undo() {
        applyState(previousState)
    }

    override fun redo() {
        applyState(newState)
    }

    private fun applyState(state: TextViewState) {
        textView.text = state.text
        textView.setTextColor(state.color)
        textView.textSize = state.textSize
        textView.alpha = (100 - state.transparency) / 100f
        textView.rotation = state.rotation
        textView.x = state.x
        textView.y = state.y

        if (state.hasBackground) {
            val alpha = state.bgTransparency / 100f
            val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (state.bgColor and 0x00FFFFFF)
            textView.setBackgroundColor(colorWithAlpha)
        } else {
            textView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        onUpdate(state)
    }
}

class DeleteTextCommand(
    private val container: ViewGroup,
    private val textView: DraggableTextView,
    private val onTextDeleted: (DraggableTextView) -> Unit
) : UndoRedoManager.UndoableCommand {

    override fun execute() {
        container.removeView(textView)
        onTextDeleted(textView)
    }

    override fun undo() {
        container.addView(textView)
    }

    override fun redo() {
        container.removeView(textView)
        onTextDeleted(textView)
    }
}