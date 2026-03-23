// com/example/logix/undo/ImageAdjustmentCommand.kt
package com.example.logix.undo

import android.graphics.Bitmap
import android.widget.ImageView

data class ImageAdjustmentState(
    val bitmap: Bitmap?,
    val rotation: Float,
    val scale: Float,
    val opacity: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float
)

class ImageAdjustmentCommand(
    private val imageView: ImageView,
    private val previousState: ImageAdjustmentState,
    private val newState: ImageAdjustmentState,
    private val onStateApplied: (ImageAdjustmentState) -> Unit
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

    private fun applyState(state: ImageAdjustmentState) {
        state.bitmap?.let { imageView.setImageBitmap(it) }
        imageView.rotation = state.rotation
        imageView.scaleX = state.scale
        imageView.scaleY = state.scale
        onStateApplied(state)
    }
}