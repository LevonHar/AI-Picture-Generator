// com/example/logix/undo/UndoRedoManager.kt
package com.example.logix.undo

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.example.logix.logoOptions.DraggableCurvedTextView
import com.example.logix.logoOptions.DraggableTextView
import java.util.*

class UndoRedoManager {
    private val undoStack = Stack<UndoableCommand>()
    private val redoStack = Stack<UndoableCommand>()

    interface UndoableCommand {
        fun execute()
        fun undo()
        fun redo()
    }

    fun executeCommand(command: UndoableCommand) {
        command.execute()
        undoStack.push(command)
        redoStack.clear()
    }

    fun undo(): Boolean {
        return if (undoStack.isNotEmpty()) {
            val command = undoStack.pop()
            command.undo()
            redoStack.push(command)
            true
        } else false
    }

    fun redo(): Boolean {
        return if (redoStack.isNotEmpty()) {
            val command = redoStack.pop()
            command.redo()
            undoStack.push(command)
            true
        } else false
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}