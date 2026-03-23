package com.example.logix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.caverock.androidsvg.SVG
import com.example.logix.databinding.ActivityEditLogoBinding
import com.example.logix.logoOptions.DraggableCurvedTextView
import com.example.logix.logoOptions.DraggableTextView
import com.example.logix.logoOptions.TextOptionsDialog
import com.example.logix.models.FontOption
import com.example.logix.undo.*

class EditLogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLogoBinding
    private val textOverlays = mutableListOf<DraggableTextView>()
    private val curvedTextOverlays = mutableListOf<DraggableCurvedTextView>()
    private lateinit var fontOptions: List<FontOption>

    private val undoRedoManager = UndoRedoManager()

    // Image adjustment variables
    private var currentRotation = 0f
    private var currentScale = 1f
    private var currentOpacity = 100f
    private var currentBrightness = 100f
    private var currentContrast = 100f
    private var currentSaturation = 100f

    // Store original resources
    private var originalDrawable: Drawable? = null
    private var originalSvg: SVG? = null
    private var originalResourceId: Int = 0
    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var isApplyingAdjustments = false
    private var lastImageState: ImageAdjustmentState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFontOptions()
        loadLogoImage()
        setupImageAdjustmentControls()
        setupToolbar()

        saveCurrentImageState()

        binding.addTextButton.setOnClickListener {
            showTextOptionsDialog()
        }

        binding.saveButton.setOnClickListener {
            saveEditedLogo()
        }

        setupUndoRedoButtons()
    }

    private fun setupUndoRedoButtons() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_undo -> {
                    undo()
                    true
                }
                R.id.action_redo -> {
                    redo()
                    true
                }
                else -> false
            }
        }
    }

    private fun undo() {
        if (undoRedoManager.undo()) {
            updateUndoRedoButtonsState()
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redo() {
        if (undoRedoManager.redo()) {
            updateUndoRedoButtonsState()
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUndoRedoButtonsState() {
        val menu = binding.topAppBar.menu
        menu.findItem(R.id.action_undo)?.isEnabled = undoRedoManager.canUndo()
        menu.findItem(R.id.action_redo)?.isEnabled = undoRedoManager.canRedo()
    }

    private fun saveCurrentImageState() {
        lastImageState = ImageAdjustmentState(
            bitmap = currentBitmap?.copy(Bitmap.Config.ARGB_8888, true),
            rotation = currentRotation,
            scale = currentScale,
            opacity = currentOpacity,
            brightness = currentBrightness,
            contrast = currentContrast,
            saturation = currentSaturation
        )
    }

    private fun applyImageAdjustmentsWithUndo() {
        if (isApplyingAdjustments) return

        val previousState = lastImageState ?: return

        // Apply the actual adjustments to get the new bitmap
        applyImageAdjustmentsInternal()

        val newState = ImageAdjustmentState(
            bitmap = currentBitmap?.copy(Bitmap.Config.ARGB_8888, true),
            rotation = currentRotation,
            scale = currentScale,
            opacity = currentOpacity,
            brightness = currentBrightness,
            contrast = currentContrast,
            saturation = currentSaturation
        )

        val command = ImageAdjustmentCommand(
            imageView = binding.editLogoImage,
            previousState = previousState,
            newState = newState,
            onStateApplied = { state ->
                currentRotation = state.rotation
                currentScale = state.scale
                currentOpacity = state.opacity
                currentBrightness = state.brightness
                currentContrast = state.contrast
                currentSaturation = state.saturation
                currentBitmap = state.bitmap
                lastImageState = state

                updateSliderValues(state)
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
        lastImageState = newState
    }

    private fun updateSliderValues(state: ImageAdjustmentState) {
        binding.rotationSlider.value = state.rotation
        binding.scaleSlider.value = state.scale
        binding.opacitySlider.value = state.opacity
        binding.brightnessSlider.value = state.brightness
        binding.contrastSlider.value = state.contrast
        binding.saturationSlider.value = state.saturation

        binding.rotationValue.text = "Rotation: ${state.rotation.toInt()}°"
        binding.scaleValue.text = "Scale: ${(state.scale * 100).toInt()}%"
        binding.opacityValue.text = "Opacity: ${state.opacity.toInt()}%"
        binding.brightnessValue.text = "Brightness: ${state.brightness.toInt()}%"
        binding.contrastValue.text = "Contrast: ${state.contrast.toInt()}%"
        binding.saturationValue.text = "Saturation: ${state.saturation.toInt()}%"
    }

    private fun applyImageAdjustmentsInternal() {
        if (isApplyingAdjustments) return
        isApplyingAdjustments = true

        try {
            val sourceBitmap = originalBitmap ?: return

            var processedBitmap = sourceBitmap

            if (currentOpacity < 100f) {
                processedBitmap = applyOpacityToBitmap(processedBitmap, currentOpacity / 100f)
            }

            if (currentBrightness != 100f || currentContrast != 100f || currentSaturation != 100f) {
                processedBitmap = applyColorAdjustmentsToBitmap(processedBitmap)
            }

            currentBitmap = processedBitmap
            binding.editLogoImage.setImageBitmap(processedBitmap)
            binding.editLogoImage.rotation = currentRotation
            binding.editLogoImage.scaleX = currentScale
            binding.editLogoImage.scaleY = currentScale
            binding.editLogoImage.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isApplyingAdjustments = false
        }
    }

    private fun applyOpacityToBitmap(source: Bitmap, opacity: Float): Bitmap {
        if (opacity >= 1.0f) return source

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (opacity * 255).toInt()
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyColorAdjustmentsToBitmap(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val colorFilter = createColorFilter(
            currentBrightness / 100f,
            currentContrast / 100f,
            currentSaturation / 100f
        )

        paint.colorFilter = colorFilter
        canvas.drawBitmap(source, 0f, 0f, paint)

        return result
    }

    private fun createColorFilter(brightness: Float, contrast: Float, saturation: Float): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()

        val brightnessValue = brightness
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.setScale(brightnessValue, brightnessValue, brightnessValue, 1f)
        colorMatrix.postConcat(brightnessMatrix)

        val contrastValue = contrast
        val scale = contrastValue
        val translate = (1f - contrastValue) * 0.5f

        val contrastArray = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(contrastArray)
        colorMatrix.postConcat(contrastMatrix)

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)

        return ColorMatrixColorFilter(colorMatrix)
    }

    private fun setupImageAdjustmentControls() {
        binding.rotationSlider.addOnChangeListener { _, value, _ ->
            currentRotation = value
            binding.rotationValue.text = "Rotation: ${value.toInt()}°"
            applyImageAdjustmentsWithUndo()
        }

        binding.scaleSlider.addOnChangeListener { _, value, _ ->
            currentScale = value
            val percentage = (value * 100).toInt()
            binding.scaleValue.text = "Scale: $percentage%"
            applyImageAdjustmentsWithUndo()
        }

        binding.opacitySlider.addOnChangeListener { _, value, _ ->
            currentOpacity = value
            binding.opacityValue.text = "Opacity: ${value.toInt()}%"
            applyImageAdjustmentsWithUndo()
        }

        binding.brightnessSlider.addOnChangeListener { _, value, _ ->
            currentBrightness = value
            binding.brightnessValue.text = "Brightness: ${value.toInt()}%"
            applyImageAdjustmentsWithUndo()
        }

        binding.contrastSlider.addOnChangeListener { _, value, _ ->
            currentContrast = value
            binding.contrastValue.text = "Contrast: ${value.toInt()}%"
            applyImageAdjustmentsWithUndo()
        }

        binding.saturationSlider.addOnChangeListener { _, value, _ ->
            currentSaturation = value
            binding.saturationValue.text = "Saturation: ${value.toInt()}%"
            applyImageAdjustmentsWithUndo()
        }

        binding.resetImageButton.setOnClickListener {
            resetImageAdjustmentsWithUndo()
        }
    }

    private fun resetImageAdjustmentsWithUndo() {
        if (originalBitmap != null) {
            val previousState = lastImageState ?: return

            val newState = ImageAdjustmentState(
                bitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true),
                rotation = 0f,
                scale = 1f,
                opacity = 100f,
                brightness = 100f,
                contrast = 100f,
                saturation = 100f
            )

            val command = ImageAdjustmentCommand(
                imageView = binding.editLogoImage,
                previousState = previousState,
                newState = newState,
                onStateApplied = { state ->
                    currentRotation = state.rotation
                    currentScale = state.scale
                    currentOpacity = state.opacity
                    currentBrightness = state.brightness
                    currentContrast = state.contrast
                    currentSaturation = state.saturation
                    currentBitmap = state.bitmap
                    lastImageState = state

                    binding.rotationSlider.value = 0f
                    binding.scaleSlider.value = 1f
                    binding.opacitySlider.value = 100f
                    binding.brightnessSlider.value = 100f
                    binding.contrastSlider.value = 100f
                    binding.saturationSlider.value = 100f

                    binding.rotationValue.text = "Rotation: 0°"
                    binding.scaleValue.text = "Scale: 100%"
                    binding.opacityValue.text = "Opacity: 100%"
                    binding.brightnessValue.text = "Brightness: 100%"
                    binding.contrastValue.text = "Contrast: 100%"
                    binding.saturationValue.text = "Saturation: 100%"
                }
            )

            undoRedoManager.executeCommand(command)
            updateUndoRedoButtonsState()

            Toast.makeText(this, "All adjustments reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLogoImage() {
        originalResourceId = intent.getIntExtra("logo", 0)
        if (originalResourceId != 0) {
            try {
                val inputStream = resources.openRawResource(originalResourceId)
                originalSvg = SVG.getFromInputStream(inputStream)

                val svgPicture = originalSvg!!.renderToPicture()
                val pictureDrawable = PictureDrawable(svgPicture)

                val width = if (pictureDrawable.intrinsicWidth > 0) pictureDrawable.intrinsicWidth else 500
                val height = if (pictureDrawable.intrinsicHeight > 0) pictureDrawable.intrinsicHeight else 500

                originalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(originalBitmap!!)
                pictureDrawable.setBounds(0, 0, width, height)
                pictureDrawable.draw(canvas)

                binding.editLogoImage.setImageBitmap(originalBitmap)
                currentBitmap = originalBitmap
                originalDrawable = pictureDrawable

                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val bitmap = BitmapFactory.decodeResource(resources, originalResourceId)
                    if (bitmap != null) {
                        if (bitmap.config != Bitmap.Config.ARGB_8888) {
                            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            bitmap.recycle()
                        } else {
                            originalBitmap = bitmap
                        }
                        binding.editLogoImage.setImageBitmap(originalBitmap)
                        currentBitmap = originalBitmap
                    } else {
                        binding.editLogoImage.setImageResource(originalResourceId)
                        val drawable = binding.editLogoImage.drawable
                        if (drawable != null) {
                            originalBitmap = drawableToBitmap(drawable)
                            currentBitmap = originalBitmap
                        }
                    }
                    originalDrawable = binding.editLogoImage.drawable
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 500
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 500

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun exportLogo() {
        try {
            val previewCard = binding.previewCard
            val bitmap = Bitmap.createBitmap(
                previewCard.width,
                previewCard.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            previewCard.draw(canvas)

            Toast.makeText(this, "Logo exported successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error exporting logo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeFontOptions() {
        fontOptions = listOf(
            FontOption("Default", Typeface.DEFAULT),
            FontOption("Default Bold", Typeface.DEFAULT_BOLD),
            FontOption("Monospace", Typeface.MONOSPACE),
            FontOption("Serif", Typeface.SERIF),
            FontOption("Sans Serif", Typeface.SANS_SERIF)
        )
    }

    private fun showTextOptionsDialog() {
        TextOptionsDialog(
            activity = this,
            fontOptions = fontOptions,
            onTextAdded = { options ->
                if (options.isCurved) {
                    addCurvedTextToLogo(options)
                } else {
                    addTextToLogo(options)
                }
            }
        ).show()
    }

    private fun addTextToLogo(options: TextOptionsDialog.TextOptions) {
        val textView = DraggableTextView(this)

        textView.text = options.text
        textView.setTextColor(options.color)
        textView.textSize = options.textSize
        textView.rotation = options.rotation
        textView.alpha = (100 - options.transparency) / 100f

        applyFontToTextView(textView, options.fontOption)

        if (options.hasBackground) {
            val alpha = options.bgTransparency / 100f
            val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (options.bgColor and 0x00FFFFFF)
            textView.setBackgroundColor(colorWithAlpha)
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        textView.layoutParams = params

        textView.onDoubleClickListener = { draggableTextView ->
            showEditTextDialog(draggableTextView)
        }

        textView.onSingleClickListener = { draggableTextView ->
            draggableTextView.bringToFront()
        }

        textView.onLongClickListener = { draggableTextView ->
            deleteText(draggableTextView)
            true
        }

        val command = AddTextCommand(
            container = binding.textOverlayContainer,
            textView = textView,
            onTextAdded = { addedTextView ->
                textOverlays.add(addedTextView)
                Toast.makeText(this, "Text added - drag to move, double-tap to edit, long-press to delete", Toast.LENGTH_SHORT).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
    }

    private fun deleteText(textView: DraggableTextView) {
        val index = textOverlays.indexOf(textView)
        if (index != -1) {
            val command = DeleteTextCommand(
                container = binding.textOverlayContainer,
                textView = textView,
                onTextDeleted = { deletedTextView ->
                    textOverlays.remove(deletedTextView)
                    Toast.makeText(this, "Text deleted", Toast.LENGTH_SHORT).show()
                }
            )

            undoRedoManager.executeCommand(command)
            updateUndoRedoButtonsState()
        }
    }

    private fun deleteCurvedText(curvedTextView: DraggableCurvedTextView) {
        val index = curvedTextOverlays.indexOf(curvedTextView)
        if (index != -1) {
            val command = DeleteCurvedTextCommand(
                container = binding.textOverlayContainer,
                curvedTextView = curvedTextView,
                onTextDeleted = { deletedTextView ->
                    curvedTextOverlays.remove(deletedTextView)
                    Toast.makeText(this, "Curved text deleted", Toast.LENGTH_SHORT).show()
                }
            )

            undoRedoManager.executeCommand(command)
            updateUndoRedoButtonsState()
        }
    }

    private fun addCurvedTextToLogo(options: TextOptionsDialog.TextOptions) {
        val curvedTextView = DraggableCurvedTextView(this).apply {
            setText(options.text)
            setTextColor(options.color)
            setTextSize(options.textSize)
            setRotationDegrees(options.rotation)
            setCurveRadius(options.curveRadius)
            setCurveUp(options.curveUp)
            setTextAlpha((100 - options.transparency) / 100f)

            applyFontToCurvedTextView(this, options.fontOption)
            setBackgroundColorWithAlpha(options.bgColor, options.bgTransparency)

            onDoubleClickListener = { draggableCurvedTextView ->
                showEditCurvedTextDialog(draggableCurvedTextView)
            }

            onSingleClickListener = { draggableCurvedTextView ->
                draggableCurvedTextView.bringToFront()
            }

            onLongClickListener = { draggableCurvedTextView ->
                deleteCurvedText(draggableCurvedTextView)
                true
            }
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        curvedTextView.layoutParams = params

        val command = AddCurvedTextCommand(
            container = binding.textOverlayContainer,
            curvedTextView = curvedTextView,
            onTextAdded = { addedTextView ->
                curvedTextOverlays.add(addedTextView)
                Toast.makeText(this, "Curved text added - drag to move, double-tap to edit, long-press to delete", Toast.LENGTH_SHORT).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
    }

    private fun showEditTextDialog(textView: DraggableTextView) {
        val currentText = textView.text.toString()
        val currentColor = textView.currentTextColor
        val currentTextSize = textView.textSize / resources.displayMetrics.scaledDensity
        val currentAlpha = textView.alpha
        val currentTransparency = ((1 - currentAlpha) * 100).toInt()
        val currentRotation = textView.rotation
        val currentTypeface = textView.typeface

        val background = textView.background
        var currentBgColor = Color.TRANSPARENT
        var currentBgTransparency = 100
        var hasBackground = false

        if (background is ColorDrawable) {
            currentBgColor = background.color
            hasBackground = currentBgColor != Color.TRANSPARENT
            if (hasBackground) {
                val alpha = Color.alpha(currentBgColor)
                currentBgTransparency = (alpha / 255f * 100).toInt()
            }
        }

        var currentFontIndex = 0
        for ((index, font) in fontOptions.withIndex()) {
            if (font.typeface == currentTypeface) {
                currentFontIndex = index
                break
            }
        }

        val options = TextOptionsDialog.TextOptions(
            text = currentText,
            color = currentColor,
            textSize = currentTextSize,
            transparency = currentTransparency,
            fontOption = fontOptions[currentFontIndex],
            rotation = currentRotation,
            bgColor = currentBgColor,
            bgTransparency = currentBgTransparency,
            hasBackground = hasBackground,
            isCurved = false
        )

        TextOptionsDialog(
            activity = this,
            fontOptions = fontOptions,
            onTextAdded = { },
            onTextUpdated = { updatedOptions ->
                updateTextView(textView, updatedOptions)
            },
            existingText = options,
            isEditMode = true
        ).show()
    }

    private fun showEditCurvedTextDialog(curvedTextView: DraggableCurvedTextView) {
        val currentText = curvedTextView.getText()
        val currentColor = curvedTextView.getTextColor()
        val currentTextSize = curvedTextView.getTextSizeValue()
        val currentAlpha = curvedTextView.getTextAlpha()
        val currentTransparency = ((1 - currentAlpha) * 100).toInt()
        val currentRotation = curvedTextView.getRotationDegrees()
        val currentRadius = curvedTextView.getCurveRadius()
        val currentCurveUp = curvedTextView.isCurveUp()
        val currentTypeface = curvedTextView.getCustomTypeface()

        val background = curvedTextView.background
        var currentBgColor = Color.TRANSPARENT
        var currentBgTransparency = 100
        var hasBackground = false

        if (background is ColorDrawable) {
            currentBgColor = background.color
            hasBackground = currentBgColor != Color.TRANSPARENT
            if (hasBackground) {
                val alpha = Color.alpha(currentBgColor)
                currentBgTransparency = (alpha / 255f * 100).toInt()
            }
        }

        var currentFontIndex = 0
        for ((index, font) in fontOptions.withIndex()) {
            if (font.typeface == currentTypeface) {
                currentFontIndex = index
                break
            }
        }

        val options = TextOptionsDialog.TextOptions(
            text = currentText,
            color = currentColor,
            textSize = currentTextSize,
            transparency = currentTransparency,
            fontOption = fontOptions[currentFontIndex],
            rotation = currentRotation,
            bgColor = currentBgColor,
            bgTransparency = currentBgTransparency,
            hasBackground = hasBackground,
            isCurved = true,
            curveRadius = currentRadius,
            curveUp = currentCurveUp
        )

        TextOptionsDialog(
            activity = this,
            fontOptions = fontOptions,
            onTextAdded = { },
            onTextUpdated = { updatedOptions ->
                updateCurvedTextView(curvedTextView, updatedOptions)
            },
            existingText = options,
            isEditMode = true
        ).show()
    }

    private fun updateTextView(textView: DraggableTextView, options: TextOptionsDialog.TextOptions) {
        val previousState = TextViewState(
            textView = textView,
            text = textView.text.toString(),
            color = textView.currentTextColor,
            textSize = textView.textSize / resources.displayMetrics.scaledDensity,
            transparency = ((1 - textView.alpha) * 100).toInt(),
            rotation = textView.rotation,
            bgColor = (textView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT,
            bgTransparency = ((textView.background as? ColorDrawable)?.let { Color.alpha(it.color) } ?: 0) * 100 / 255,
            hasBackground = textView.background is ColorDrawable && (textView.background as ColorDrawable).color != Color.TRANSPARENT,
            x = textView.x,
            y = textView.y,
            fontIndex = fontOptions.indexOfFirst { it.typeface == textView.typeface }
        )

        val newState = TextViewState(
            textView = textView,
            text = options.text,
            color = options.color,
            textSize = options.textSize,
            transparency = options.transparency,
            rotation = options.rotation,
            bgColor = options.bgColor,
            bgTransparency = options.bgTransparency,
            hasBackground = options.hasBackground,
            x = textView.x,
            y = textView.y,
            fontIndex = fontOptions.indexOf(options.fontOption)
        )

        val command = UpdateTextViewCommand(
            textView = textView,
            previousState = previousState,
            newState = newState,
            onUpdate = { state ->
                Toast.makeText(this, "Text updated", Toast.LENGTH_SHORT).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
    }

    private fun updateCurvedTextView(curvedTextView: DraggableCurvedTextView, options: TextOptionsDialog.TextOptions) {
        curvedTextView.setText(options.text)
        curvedTextView.setTextColor(options.color)
        curvedTextView.setTextSize(options.textSize)
        curvedTextView.setTextAlpha((100 - options.transparency) / 100f)
        curvedTextView.setRotationDegrees(options.rotation)
        curvedTextView.setCurveRadius(options.curveRadius)
        curvedTextView.setCurveUp(options.curveUp)

        applyFontToCurvedTextView(curvedTextView, options.fontOption)
        curvedTextView.setBackgroundColorWithAlpha(options.bgColor, options.bgTransparency)

        Toast.makeText(this, "Curved text updated", Toast.LENGTH_SHORT).show()
    }

    private fun applyFontToTextView(textView: TextView, fontOption: FontOption) {
        when {
            fontOption.fontResource != null -> {
                ResourcesCompat.getFont(this, fontOption.fontResource)?.let { typeface ->
                    textView.typeface = typeface
                }
            }
            fontOption.typeface != null -> {
                textView.typeface = fontOption.typeface
            }
        }
    }

    private fun applyFontToCurvedTextView(curvedTextView: DraggableCurvedTextView, fontOption: FontOption) {
        when {
            fontOption.fontResource != null -> {
                ResourcesCompat.getFont(this, fontOption.fontResource)?.let { typeface ->
                    curvedTextView.setTypeface(typeface)
                }
            }
            fontOption.typeface != null -> {
                curvedTextView.setTypeface(fontOption.typeface)
            }
        }
    }

    private fun saveEditedLogo() {
        exportLogo()
    }

    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        currentBitmap?.recycle()
    }
}