package com.example.logix.undo

import android.view.ViewGroup
import com.example.logix.logoOptions.DraggableCurvedTextView

class DeleteCurvedTextCommand(
    private val container: ViewGroup,
    private val curvedTextView: DraggableCurvedTextView,
    private val onTextDeleted: (DraggableCurvedTextView) -> Unit
) : UndoRedoManager.UndoableCommand {

    override fun execute() {
        container.removeView(curvedTextView)
        onTextDeleted(curvedTextView)
    }

    override fun undo() {
        container.addView(curvedTextView)
    }

    override fun redo() {
        container.removeView(curvedTextView)
        onTextDeleted(curvedTextView)
    }
}