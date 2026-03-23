package com.example.logix.logoOptions

import com.example.logix.EditLogoActivity
import com.example.logix.adapter.FontAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import com.example.logix.R
import com.example.logix.databinding.DialogTextOptionsBinding
import com.example.logix.models.FontOption
import com.google.android.material.bottomsheet.BottomSheetDialog
import yuku.ambilwarna.AmbilWarnaDialog

class TextOptionsDialog(
    private val activity: EditLogoActivity,
    private val fontOptions: List<FontOption>,
    private val onTextAdded: (TextOptions) -> Unit,
    private val onTextUpdated: ((TextOptions) -> Unit)? = null,
    private val existingText: TextOptions? = null,
    private val isEditMode: Boolean = false
) {

    data class TextOptions(
        val text: String,
        val color: Int,
        val textSize: Float,
        val transparency: Int,
        val fontOption: FontOption,
        val rotation: Float,
        val bgColor: Int,
        val bgTransparency: Int,
        val hasBackground: Boolean,
        val isCurved: Boolean = false,
        val curveRadius: Float = 200f,
        val curveUp: Boolean = true
    )

    private lateinit var binding: DialogTextOptionsBinding
    private lateinit var dialog: BottomSheetDialog

    // State variables
    private var selectedColor: Int = Color.BLACK
    private var selectedTextSize: Float = 24f
    private var selectedTransparency: Int = 0
    private var selectedFont: FontOption = fontOptions[0]
    private var selectedRotation: Float = 0f
    private var isCurved: Boolean = false
    private var curveRadius: Float = 200f
    private var curveUp: Boolean = true
    private var selectedBgColor: Int = Color.TRANSPARENT
    private var selectedBgTransparency: Int = 100
    private var hasBackground: Boolean = false
    private var curvedPreview: CurvedTextView? = null

    fun show() {
        dialog = BottomSheetDialog(activity)
        binding = DialogTextOptionsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(binding.root)

        if (isEditMode && existingText != null) {
            initializeWithExistingValues()
        } else {
            initializeDefaultValues()
        }

        setupUI()
        setupListeners()

        dialog.show()
    }

    private fun initializeDefaultValues() {
        selectedColor = Color.BLACK
        selectedTextSize = 24f
        selectedTransparency = 0
        selectedFont = fontOptions[0]
        selectedRotation = 0f
        isCurved = false
        curveRadius = 200f
        curveUp = true
        selectedBgColor = Color.TRANSPARENT
        selectedBgTransparency = 100
        hasBackground = false

        // Initialize preview with default values
        binding.previewText.setTextColor(selectedColor)
        binding.previewText.textSize = 24f
        binding.previewText.text = "Preview Text"
        binding.previewText.alpha = 1.0f
        binding.previewText.setBackgroundColor(Color.TRANSPARENT)
        binding.previewText.rotation = 0f
        applyFontToPreview(binding.previewText, selectedFont)
    }

    private fun initializeWithExistingValues() {
        existingText?.let { options ->
            selectedColor = options.color
            selectedTextSize = options.textSize
            selectedTransparency = options.transparency
            selectedFont = options.fontOption
            selectedRotation = options.rotation
            isCurved = options.isCurved
            curveRadius = options.curveRadius
            curveUp = options.curveUp
            selectedBgColor = options.bgColor
            selectedBgTransparency = options.bgTransparency
            hasBackground = options.hasBackground

            // Set input text
            binding.textInput.setText(options.text)

            if (isCurved) {
                setupCurvedPreview()
            } else {
                // Initialize normal preview
                binding.previewText.setTextColor(selectedColor)
                binding.previewText.textSize = selectedTextSize
                binding.previewText.text = options.text
                binding.previewText.alpha = (100 - selectedTransparency) / 100f
                binding.previewText.rotation = selectedRotation
                applyFontToPreview(binding.previewText, selectedFont)

                if (hasBackground) {
                    val alpha = selectedBgTransparency / 100f
                    val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (selectedBgColor and 0x00FFFFFF)
                    binding.previewText.setBackgroundColor(colorWithAlpha)
                }
            }
        }
    }

    private fun setupUI() {
        // Set up font dropdown
        val fontAdapter = FontAdapter(activity, fontOptions)
        binding.fontDropdown.setAdapter(fontAdapter)
        binding.fontDropdown.setText(selectedFont.name, false)

        // Initialize sliders
        binding.transparencySlider.value = selectedTransparency.toFloat()
        binding.bgTransparencySlider.isEnabled = hasBackground
        binding.bgTransparencySlider.value = selectedBgTransparency.toFloat()
        binding.rotationSlider.value = selectedRotation
        binding.rotationValue.setText(selectedRotation.toInt().toString())
        binding.curveRadiusSlider.value = curveRadius
        binding.curveRadiusValue.text = curveRadius.toInt().toString()
        binding.textSizeSlider.value = selectedTextSize

        // Set curve toggle state
        binding.curveToggle.isChecked = isCurved
        binding.curveControls.visibility = if (isCurved) View.VISIBLE else View.GONE

        // Set curve direction
        if (curveUp) {
            binding.curveDirectionUp.isChecked = true
        } else {
            binding.curveDirectionDown.isChecked = true
        }

        // Set button text for edit mode
        if (isEditMode) {
            binding.confirmButton.text = "Update"
        }

        // Set up color chips
        setupColorChips()
        setupBackgroundChips()
    }

    private fun setupListeners() {
        // Font selection
        binding.fontDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedFont = fontOptions[position]
            if (isCurved && curvedPreview != null) {
                applyFontToCurvedPreview(curvedPreview!!, selectedFont)
            } else {
                applyFontToPreview(binding.previewText, selectedFont)
            }
        }

        // Text input change
        binding.textInput.doAfterTextChanged { editable ->
            val displayText = if (!editable.isNullOrEmpty()) editable.toString() else "Preview Text"
            if (isCurved && curvedPreview != null) {
                curvedPreview?.setText(displayText)
            } else {
                binding.previewText.text = displayText
            }
        }

        // Text size slider
        binding.textSizeSlider.addOnChangeListener { _, value, _ ->
            selectedTextSize = value
            if (isCurved && curvedPreview != null) {
                curvedPreview?.setTextSize(value)
            } else {
                binding.previewText.textSize = value
            }
        }

        // Transparency slider
        binding.transparencySlider.addOnChangeListener { _, value, _ ->
            selectedTransparency = value.toInt()
            val alpha = (100 - value) / 100f
            if (isCurved && curvedPreview != null) {
                curvedPreview?.alpha = alpha
            } else {
                binding.previewText.alpha = alpha
            }
        }

        // Rotation controls
        setupRotationControls()

        // Curve controls
        setupCurveControls()

        // Background transparency slider
        binding.bgTransparencySlider.addOnChangeListener { _, value, _ ->
            if (hasBackground) {
                selectedBgTransparency = value.toInt()
                applyBackgroundColor()
            }
        }

        // Custom color picker
        binding.colorCustom.setOnClickListener {
            showColorPicker { color ->
                selectedColor = color
                if (isCurved && curvedPreview != null) {
                    curvedPreview?.setTextColor(color)
                } else {
                    binding.previewText.setTextColor(color)
                }
                updateCustomColorChip(binding.colorCustom, color)
            }
        }

        // Custom background color picker
        binding.bgColorCustom.setOnClickListener {
            showColorPicker { color ->
                hasBackground = true
                selectedBgColor = color
                binding.bgTransparencySlider.isEnabled = true
                applyBackgroundColor()
                updateCustomColorChip(binding.bgColorCustom, color)
                binding.bgColorChipGroup.clearCheck()
            }
        }

        // Transparent background
        binding.bgColorTransparent.setOnClickListener {
            hasBackground = false
            binding.bgTransparencySlider.isEnabled = false
            clearBackground()
            binding.bgColorChipGroup.clearCheck()
        }

        // Cancel button
        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Confirm button
        binding.confirmButton.setOnClickListener {
            val text = binding.textInput.text.toString()
            if (text.isNotEmpty()) {
                val options = TextOptions(
                    text = text,
                    color = selectedColor,
                    textSize = selectedTextSize,
                    transparency = selectedTransparency,
                    fontOption = selectedFont,
                    rotation = selectedRotation,
                    bgColor = if (hasBackground) selectedBgColor else Color.TRANSPARENT,
                    bgTransparency = if (hasBackground) selectedBgTransparency else 0,
                    hasBackground = hasBackground,
                    isCurved = isCurved,
                    curveRadius = curveRadius,
                    curveUp = curveUp
                )

                if (isEditMode) {
                    onTextUpdated?.invoke(options)
                } else {
                    onTextAdded(options)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(activity, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }

        // Dialog dismiss to clean up curved preview
        dialog.setOnDismissListener {
            curvedPreview?.let {
                (it.parent as? ViewGroup)?.removeView(it)
            }
        }
    }

    private fun setupColorChips() {
        // Individual click listeners for color chips
        binding.colorBlack.setOnClickListener { updateTextColor(Color.BLACK) }
        binding.colorWhite.setOnClickListener { updateTextColor(Color.WHITE) }
        binding.colorRed.setOnClickListener { updateTextColor(Color.RED) }
        binding.colorBlue.setOnClickListener { updateTextColor(Color.BLUE) }

        // Set initial selection
        when (selectedColor) {
            Color.BLACK -> binding.colorBlack.isChecked = true
            Color.WHITE -> binding.colorWhite.isChecked = true
            Color.RED -> binding.colorRed.isChecked = true
            Color.BLUE -> binding.colorBlue.isChecked = true
            else -> {
                binding.colorCustom.setChipBackgroundColorResource(android.R.color.transparent)
                binding.colorCustom.chipBackgroundColor = ColorStateList.valueOf(selectedColor)
                binding.colorCustom.isChecked = true
            }
        }
    }

    private fun setupBackgroundChips() {
        // Individual click listeners for background color chips
        binding.bgColorBlack.setOnClickListener { updateBackgroundColor(Color.BLACK) }
        binding.bgColorWhite.setOnClickListener { updateBackgroundColor(Color.WHITE) }
        binding.bgColorRed.setOnClickListener { updateBackgroundColor(Color.RED) }
        binding.bgColorBlue.setOnClickListener { updateBackgroundColor(Color.BLUE) }

        // Set initial selection
        if (!hasBackground || selectedBgColor == Color.TRANSPARENT) {
            binding.bgColorTransparent.isChecked = true
        } else {
            when (selectedBgColor) {
                Color.BLACK -> binding.bgColorBlack.isChecked = true
                Color.WHITE -> binding.bgColorWhite.isChecked = true
                Color.RED -> binding.bgColorRed.isChecked = true
                Color.BLUE -> binding.bgColorBlue.isChecked = true
                else -> {
                    binding.bgColorCustom.setChipBackgroundColorResource(android.R.color.transparent)
                    binding.bgColorCustom.chipBackgroundColor = ColorStateList.valueOf(selectedBgColor)
                    binding.bgColorCustom.isChecked = true
                }
            }
        }
    }

    private fun setupRotationControls() {
        // Slider change listener
        binding.rotationSlider.addOnChangeListener { _, value, _ ->
            val rotationValue = value.toInt()
            binding.rotationValue.setText(rotationValue.toString())
            updateRotation(value)
        }

        // Manual input change listener
        binding.rotationValue.doAfterTextChanged { editable ->
            val value = editable.toString().toIntOrNull()
            if (value != null && value in 0..360) {
                binding.rotationSlider.value = value.toFloat()
                updateRotation(value.toFloat())
            }
        }
    }

    private fun setupCurveControls() {
        // Toggle curve mode
        binding.curveToggle.setOnCheckedChangeListener { _, isChecked ->
            isCurved = isChecked
            binding.curveControls.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (isChecked) {
                setupCurvedPreview()
            } else {
                switchToNormalPreview()
            }
        }

        // Curve radius slider
        binding.curveRadiusSlider.addOnChangeListener { _, value, _ ->
            curveRadius = value
            binding.curveRadiusValue.text = value.toInt().toString()
            if (isCurved && curvedPreview != null) {
                curvedPreview?.setRadius(value)
            }
        }

        // Curve direction toggle
        binding.curveDirectionToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                curveUp = checkedId == R.id.curveDirectionUp
                if (isCurved && curvedPreview != null) {
                    curvedPreview?.setCurveUp(curveUp)
                }
            }
        }
    }

    private fun setupCurvedPreview() {
        if (curvedPreview == null) {
            curvedPreview = CurvedTextView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            binding.previewContainer.addView(curvedPreview)
        }

        // Update curved preview with current values
        val displayText = binding.textInput.text.toString().ifEmpty { "Preview Text" }
        curvedPreview?.apply {
            setText(displayText)
            setTextColor(selectedColor)
            setTextSize(selectedTextSize)
            alpha = (100 - selectedTransparency) / 100f
            setRadius(curveRadius)
            setCurveUp(curveUp)
            setRotation(selectedRotation)
            applyFontToCurvedPreview(this@apply, selectedFont)
            visibility = View.VISIBLE
        }

        binding.previewText.visibility = View.GONE

        // Apply background to container
        applyBackgroundColor()
    }

    private fun switchToNormalPreview() {
        curvedPreview?.visibility = View.GONE
        binding.previewText.visibility = View.VISIBLE

        // Update normal preview with current values
        val displayText = binding.textInput.text.toString().ifEmpty { "Preview Text" }
        binding.previewText.apply {
            text = displayText
            setTextColor(selectedColor)
            textSize = selectedTextSize
            alpha = (100 - selectedTransparency) / 100f
            rotation = selectedRotation
        }
        applyFontToPreview(binding.previewText, selectedFont)

        // Apply background to preview text
        applyBackgroundColor()
    }

    private fun updateTextColor(color: Int) {
        selectedColor = color
        if (isCurved && curvedPreview != null) {
            curvedPreview?.setTextColor(color)
        } else {
            binding.previewText.setTextColor(color)
        }
    }

    private fun updateBackgroundColor(color: Int) {
        hasBackground = true
        selectedBgColor = color
        binding.bgTransparencySlider.isEnabled = true
        applyBackgroundColor()
    }

    private fun updateRotation(rotation: Float) {
        selectedRotation = rotation
        if (isCurved && curvedPreview != null) {
            curvedPreview?.setRotation(rotation)
        } else {
            binding.previewText.rotation = rotation
        }
    }

    private fun applyBackgroundColor() {
        if (hasBackground) {
            val alpha = selectedBgTransparency / 100f
            val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (selectedBgColor and 0x00FFFFFF)

            if (isCurved && curvedPreview != null) {
                binding.previewContainer.setBackgroundColor(colorWithAlpha)
            } else {
                binding.previewText.setBackgroundColor(colorWithAlpha)
            }
        } else {
            clearBackground()
        }
    }

    private fun clearBackground() {
        if (isCurved && curvedPreview != null) {
            binding.previewContainer.setBackgroundColor(Color.TRANSPARENT)
        } else {
            binding.previewText.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        val colorPicker = AmbilWarnaDialog(
            activity,
            selectedColor,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    onColorSelected(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // do nothing
                }
            })
        colorPicker.show()
    }

    private fun updateCustomColorChip(chip: com.google.android.material.chip.Chip, color: Int) {
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipBackgroundColor = ColorStateList.valueOf(color)
        chip.isChecked = true
    }

    private fun applyFontToPreview(textView: TextView, fontOption: FontOption) {
        when {
            fontOption.fontResource != null -> {
                ResourcesCompat.getFont(activity, fontOption.fontResource)?.let { typeface ->
                    textView.typeface = typeface
                }
            }
            fontOption.typeface != null -> {
                textView.typeface = fontOption.typeface
            }
        }
    }

    private fun applyFontToCurvedPreview(curvedTextView: CurvedTextView, fontOption: FontOption) {
        when {
            fontOption.fontResource != null -> {
                ResourcesCompat.getFont(activity, fontOption.fontResource)?.let { typeface ->
                    curvedTextView.setTypeface(typeface)
                }
            }
            fontOption.typeface != null -> {
                curvedTextView.setTypeface(fontOption.typeface)
            }
        }
    }
}