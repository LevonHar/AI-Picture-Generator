package com.example.logix

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.caverock.androidsvg.SVG
import com.example.logix.databinding.ActivityEditLogoBinding
import com.example.logix.databinding.DialogDownloadOptionsBinding
import com.example.logix.logoOptions.DraggableCurvedTextView
import com.example.logix.logoOptions.DraggableTextView
import com.example.logix.logoOptions.TextOptionsDialog
import com.example.logix.models.EditedLogoEntry
import com.example.logix.models.FontOption
import com.example.logix.undo.*
import com.example.logix.utils.EditedLogoRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditLogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLogoBinding
    private val textOverlays = mutableListOf<DraggableTextView>()
    private val curvedTextOverlays = mutableListOf<DraggableCurvedTextView>()
    private lateinit var fontOptions: List<FontOption>

    private val undoRedoManager = UndoRedoManager()

    private var currentRotation = 0f
    private var currentScale = 1f
    private var currentOpacity = 100f
    private var currentBrightness = 100f
    private var currentContrast = 100f
    private var currentSaturation = 100f

    private var originalDrawable: Drawable? = null
    private var originalSvg: SVG? = null
    private var originalResourceId: Int = 0
    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var isApplyingAdjustments = false
    private var lastImageState: ImageAdjustmentState? = null

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFontOptions()
        loadLogoImage()
        setupImageAdjustmentControls()
        setupToolbar()
        setupHelpTooltips()

        binding.addTextButton.setOnClickListener {
            showTextOptionsDialog()
        }

        binding.saveButton.setOnClickListener {
            saveEditedLogo()
        }

        setupUndoRedoButtons()
        checkPermissions()
    }

    // Add this method to setup help tooltips
    private fun setupHelpTooltips() {

        // Rotation help
        binding.helpRotation.setOnClickListener {
            showHelpDialog(
                "Rotation",
                "Rotate your logo from 0° to 360°.\n\n" +
                        "• 0° = No rotation\n" +
                        "• 90° = Quarter turn\n" +
                        "• 180° = Half turn\n" +
                        "• 270° = Three-quarter turn\n" +
                        "• 360° = Full rotation\n\n" +
                        "Use the slider or type a specific value."
            )
        }

        // Scale help
        binding.helpScale.setOnClickListener {
            showHelpDialog(
                "Scale",
                "Zoom in or out on your logo.\n\n" +
                        "• 50% = Half size\n" +
                        "• 100% = Original size\n" +
                        "• 150% = 1.5x larger\n" +
                        "• 200% = Double size\n\n" +
                        "The logo maintains its aspect ratio when scaled."
            )
        }

        // Opacity help
        binding.helpOpacity.setOnClickListener {
            showHelpDialog(
                "Opacity",
                "Control how transparent your logo appears.\n\n" +
                        "• 0% = Completely invisible\n" +
                        "• 50% = Semi-transparent\n" +
                        "• 100% = Fully opaque (solid)\n\n" +
                        "Use lower opacity for watermark effects or to blend with backgrounds."
            )
        }

        // Brightness help
        binding.helpBrightness.setOnClickListener {
            showHelpDialog(
                "Brightness",
                "Adjust the brightness level of your logo.\n\n" +
                        "• 0% = Completely dark (black)\n" +
                        "• 100% = Original brightness\n" +
                        "• 200% = Twice as bright\n\n" +
                        "Higher values make the image lighter; lower values make it darker."
            )
        }

        // Contrast help
        binding.helpContrast.setOnClickListener {
            showHelpDialog(
                "Contrast",
                "Adjust the difference between light and dark areas.\n\n" +
                        "• 0% = No contrast (uniform gray)\n" +
                        "• 100% = Original contrast\n" +
                        "• 200% = High contrast (more dramatic)\n\n" +
                        "Higher contrast makes colors more distinct; lower contrast creates a softer look."
            )
        }

        // Saturation help
        binding.helpSaturation.setOnClickListener {
            showHelpDialog(
                "Saturation",
                "Control the intensity of colors.\n\n" +
                        "• 0% = Grayscale (no color)\n" +
                        "• 100% = Original saturation\n" +
                        "• 200% = Vibrant, intense colors\n\n" +
                        "Lower saturation creates a muted look; higher saturation makes colors pop."
            )
        }
    }

    // Helper method to show help dialog
    private fun showHelpDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .setIcon(android.R.drawable.ic_menu_gallery)
            .show()
    }

    // ================================================================
    // IMAGE LOADING
    // ================================================================

    private fun loadLogoImage() {
        val editedLogoPath  = intent.getStringExtra("edited_logo_path")
        val logoResourceId  = intent.getIntExtra("logo", 0)
        val logoImageUrl    = intent.getStringExtra("logo_image_url")

        when {
            // Priority 1: re-opening a previously saved edit → load that PNG
            !editedLogoPath.isNullOrEmpty() ->
                loadLogoFromEditedFile(editedLogoPath, logoResourceId, logoImageUrl)

            // Priority 2: network logo
            logoImageUrl != null -> loadLogoFromUrl(logoImageUrl)

            // Priority 3: local SVG resource
            logoResourceId != 0  -> loadLogoFromResource(logoResourceId)

            else -> Toast.makeText(this, "No logo to load", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Loads the already-edited PNG from internal storage so the user
     * continues editing from where they left off.
     * originalResourceId / logoImageUrl are preserved so "Save Edits"
     * keeps tagging the entry with the correct sourceId.
     */
    private fun loadLogoFromEditedFile(
        editedPath: String,
        fallbackResourceId: Int,
        fallbackUrl: String?
    ) {
        val file = File(editedPath)

        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(editedPath)
                if (bitmap != null) {
                    val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                        bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
                    } else {
                        bitmap
                    }

                    originalBitmap = argbBitmap
                    currentBitmap  = argbBitmap
                    binding.editLogoImage.setImageBitmap(argbBitmap)

                    // Keep the original source reference for correct sourceId on re-save
                    if (fallbackResourceId != 0) {
                        originalResourceId = fallbackResourceId
                    }

                    saveCurrentImageState()
                    Toast.makeText(this, "Continuing from saved edits", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Edited file missing or corrupted — fall back to original source
        Toast.makeText(this, "Saved edit not found, loading original", Toast.LENGTH_SHORT).show()
        when {
            fallbackUrl != null      -> loadLogoFromUrl(fallbackUrl)
            fallbackResourceId != 0  -> loadLogoFromResource(fallbackResourceId)
            else -> Toast.makeText(this, "No logo to load", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLogoFromUrl(url: String) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()

                if (bitmap != null) {
                    val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                        bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
                    } else {
                        bitmap
                    }

                    runOnUiThread {
                        originalBitmap = argbBitmap
                        currentBitmap = argbBitmap
                        binding.editLogoImage.setImageBitmap(argbBitmap)
                        saveCurrentImageState()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadLogoFromResource(resourceId: Int) {
        originalResourceId = resourceId
        try {
            val inputStream = resources.openRawResource(resourceId)
            originalSvg = SVG.getFromInputStream(inputStream)

            val svgPicture = originalSvg!!.renderToPicture()
            val pictureDrawable = PictureDrawable(svgPicture)

            val width  = if (pictureDrawable.intrinsicWidth  > 0) pictureDrawable.intrinsicWidth  else 500
            val height = if (pictureDrawable.intrinsicHeight > 0) pictureDrawable.intrinsicHeight else 500

            originalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(originalBitmap!!)
            pictureDrawable.setBounds(0, 0, width, height)
            pictureDrawable.draw(canvas)

            binding.editLogoImage.setImageBitmap(originalBitmap)
            currentBitmap    = originalBitmap
            originalDrawable = pictureDrawable

            inputStream.close()
            saveCurrentImageState()

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val bitmap = BitmapFactory.decodeResource(resources, resourceId)
                if (bitmap != null) {
                    originalBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                        bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
                    } else {
                        bitmap
                    }
                    binding.editLogoImage.setImageBitmap(originalBitmap)
                    currentBitmap = originalBitmap
                } else {
                    binding.editLogoImage.setImageResource(resourceId)
                    val drawable = binding.editLogoImage.drawable
                    if (drawable != null) {
                        originalBitmap = drawableToBitmap(drawable)
                        currentBitmap  = originalBitmap
                    }
                }
                originalDrawable = binding.editLogoImage.drawable
                saveCurrentImageState()
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================================================================
    // PERMISSIONS
    // ================================================================

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission needed to save images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ================================================================
    // TOOLBAR & UNDO/REDO
    // ================================================================

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }
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
                R.id.action_download -> {
                    showDownloadDialog()
                    true
                }
                R.id.eye_open -> {
                    openProductPreview()
                    true
                }
                R.id.action_save_edits -> {
                    saveEditsToChangedList()
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

    // ================================================================
    // IMAGE ADJUSTMENT STATE
    // ================================================================

    private fun saveCurrentImageState() {
        lastImageState = ImageAdjustmentState(
            bitmap     = currentBitmap?.copy(Bitmap.Config.ARGB_8888, true),
            rotation   = currentRotation,
            scale      = currentScale,
            opacity    = currentOpacity,
            brightness = currentBrightness,
            contrast   = currentContrast,
            saturation = currentSaturation
        )
    }

    // ================================================================
    // SAVE EDITS TO CHANGED LIST (persists across app restarts)
    // ================================================================

    private fun saveEditsToChangedList() {
        try {
            // Capture the current preview card as a bitmap
            val previewCard = binding.previewCard
            val bitmap = Bitmap.createBitmap(
                previewCard.width,
                previewCard.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            previewCard.draw(canvas)

            // Save PNG to internal storage (no permissions needed, survives restarts)
            val timeStamp  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName   = "edited_logo_$timeStamp.png"
            val dir        = File(filesDir, "edited_logos").also { it.mkdirs() }
            val outputFile = File(dir, fileName)

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            bitmap.recycle()

            // Determine sourceId:
            //   • If we were re-editing a previously saved logo, reuse the same sourceId
            //     so we overwrite the old slot instead of creating a duplicate.
            //   • Otherwise derive from network URL or resource id.
            val logoResourceId = intent.getIntExtra("logo", 0)
            val logoImageUrl   = intent.getStringExtra("logo_image_url")
            val passedSourceId = intent.getStringExtra("source_id")

            val sourceId  = passedSourceId ?: logoImageUrl ?: logoResourceId.toString()
            val isNetwork = logoImageUrl != null

            val repository = EditedLogoRepository.getInstance(this)

            // Delete the old PNG file for this sourceId to avoid stale files piling up
            repository.getEditedLogos()
                .firstOrNull { it.sourceId == sourceId }
                ?.savedImagePath
                ?.let { File(it).delete() }

            // Persist the new entry (overwrites any existing entry with same sourceId)
            val entry = EditedLogoEntry(
                sourceId       = sourceId,
                isNetworkLogo  = isNetwork,
                savedImagePath = outputFile.absolutePath
            )
            repository.saveEditedLogo(entry)

            Toast.makeText(this, "Edits saved to Edited Logos!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save edits: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // IMAGE ADJUSTMENTS WITH UNDO
    // ================================================================

    private fun applyImageAdjustmentsWithUndo() {
        if (isApplyingAdjustments) return

        val previousState = lastImageState ?: return

        applyImageAdjustmentsInternal()

        val newState = ImageAdjustmentState(
            bitmap     = currentBitmap?.copy(Bitmap.Config.ARGB_8888, true),
            rotation   = currentRotation,
            scale      = currentScale,
            opacity    = currentOpacity,
            brightness = currentBrightness,
            contrast   = currentContrast,
            saturation = currentSaturation
        )

        val command = ImageAdjustmentCommand(
            imageView     = binding.editLogoImage,
            previousState = previousState,
            newState      = newState,
            onStateApplied = { state ->
                currentRotation   = state.rotation
                currentScale      = state.scale
                currentOpacity    = state.opacity
                currentBrightness = state.brightness
                currentContrast   = state.contrast
                currentSaturation = state.saturation
                currentBitmap     = state.bitmap
                lastImageState    = state
                updateSliderValues(state)
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
        lastImageState = newState
    }

    private fun updateSliderValues(state: ImageAdjustmentState) {
        binding.rotationSlider.value   = state.rotation
        binding.scaleSlider.value      = state.scale
        binding.opacitySlider.value    = state.opacity
        binding.brightnessSlider.value = state.brightness
        binding.contrastSlider.value   = state.contrast
        binding.saturationSlider.value = state.saturation

        binding.rotationValue.text   = "Rotation: ${state.rotation.toInt()}°"
        binding.scaleValue.text      = "Scale: ${(state.scale * 100).toInt()}%"
        binding.opacityValue.text    = "Opacity: ${state.opacity.toInt()}%"
        binding.brightnessValue.text = "Brightness: ${state.brightness.toInt()}%"
        binding.contrastValue.text   = "Contrast: ${state.contrast.toInt()}%"
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
            binding.editLogoImage.scaleX   = currentScale
            binding.editLogoImage.scaleY   = currentScale
            binding.editLogoImage.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isApplyingAdjustments = false
        }
    }

    // ================================================================
    // IMAGE ADJUSTMENT CONTROLS
    // ================================================================

    private fun setupImageAdjustmentControls() {
        // Set custom colors for sliders
        ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)?.let { binding.brightnessSlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)?.let { binding.brightnessSlider.setTrackTintList(it) }

        ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark)?.let { binding.contrastSlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.holo_blue_light)?.let { binding.contrastSlider.setTrackTintList(it) }

        ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)?.let { binding.saturationSlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.holo_red_light)?.let { binding.saturationSlider.setTrackTintList(it) }

        // Rotation slider (keep default or set a color)
        ContextCompat.getColorStateList(this, android.R.color.holo_purple)?.let { binding.rotationSlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.holo_purple)?.let { binding.rotationSlider.setTrackTintList(it) }

        // Scale slider (keep default or set a color)
        ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)?.let { binding.scaleSlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.holo_green_light)?.let { binding.scaleSlider.setTrackTintList(it) }

        // Opacity slider (keep default or set a color)
        ContextCompat.getColorStateList(this, android.R.color.darker_gray)?.let { binding.opacitySlider.setThumbTintList(it) }
        ContextCompat.getColorStateList(this, android.R.color.darker_gray)?.let { binding.opacitySlider.setTrackTintList(it) }

        // Rest of your existing listener code...
        binding.rotationSlider.addOnChangeListener { _, value, _ ->
            currentRotation = value
            binding.rotationValue.text = "Rotation: ${value.toInt()}°"
            applyImageAdjustmentsWithUndo()
        }

        binding.scaleSlider.addOnChangeListener { _, value, _ ->
            currentScale = value
            binding.scaleValue.text = "Scale: ${(value * 100).toInt()}%"
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
        val previousState = lastImageState ?: return

        val newState = ImageAdjustmentState(
            bitmap     = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true),
            rotation   = 0f,
            scale      = 1f,
            opacity    = 100f,
            brightness = 100f,
            contrast   = 100f,
            saturation = 100f
        )

        val command = ImageAdjustmentCommand(
            imageView      = binding.editLogoImage,
            previousState  = previousState,
            newState       = newState,
            onStateApplied = { state ->
                currentRotation   = state.rotation
                currentScale      = state.scale
                currentOpacity    = state.opacity
                currentBrightness = state.brightness
                currentContrast   = state.contrast
                currentSaturation = state.saturation
                currentBitmap     = state.bitmap
                lastImageState    = state

                binding.rotationSlider.value   = 0f
                binding.scaleSlider.value      = 1f
                binding.opacitySlider.value    = 100f
                binding.brightnessSlider.value = 100f
                binding.contrastSlider.value   = 100f
                binding.saturationSlider.value = 100f

                binding.rotationValue.text   = "Rotation: 0°"
                binding.scaleValue.text      = "Scale: 100%"
                binding.opacityValue.text    = "Opacity: 100%"
                binding.brightnessValue.text = "Brightness: 100%"
                binding.contrastValue.text   = "Contrast: 100%"
                binding.saturationValue.text = "Saturation: 100%"
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
        Toast.makeText(this, "All adjustments reset", Toast.LENGTH_SHORT).show()
    }

    // ================================================================
    // COLOR / OPACITY FILTERS
    // ================================================================

    private fun applyOpacityToBitmap(source: Bitmap, opacity: Float): Bitmap {
        if (opacity >= 1.0f) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (opacity * 255).toInt()
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyColorAdjustmentsToBitmap(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = createColorFilter(
            currentBrightness / 100f,
            currentContrast   / 100f,
            currentSaturation / 100f
        )
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun createColorFilter(
        brightness: Float,
        contrast:   Float,
        saturation: Float
    ): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()

        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.setScale(brightness, brightness, brightness, 1f)
        colorMatrix.postConcat(brightnessMatrix)

        val scale     = contrast
        val translate = (1f - contrast) * 0.5f
        val contrastArray = floatArrayOf(
            scale, 0f,    0f,    0f, translate,
            0f,    scale, 0f,    0f, translate,
            0f,    0f,    scale, 0f, translate,
            0f,    0f,    0f,    1f, 0f
        )
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(contrastArray)
        colorMatrix.postConcat(contrastMatrix)

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)

        return ColorMatrixColorFilter(colorMatrix)
    }

    // ================================================================
    // DRAWABLE HELPER
    // ================================================================

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val width  = if (drawable.intrinsicWidth  > 0) drawable.intrinsicWidth  else 500
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // ================================================================
    // TEXT OVERLAYS
    // ================================================================

    private fun initializeFontOptions() {
        fontOptions = listOf(
            FontOption("Default",      Typeface.DEFAULT),
            FontOption("Default Bold", Typeface.DEFAULT_BOLD),
            FontOption("Monospace",    Typeface.MONOSPACE),
            FontOption("Serif",        Typeface.SERIF),
            FontOption("Sans Serif",   Typeface.SANS_SERIF)
        )
    }

    private fun showTextOptionsDialog() {
        TextOptionsDialog(
            activity    = this,
            fontOptions = fontOptions,
            onTextAdded = { options ->
                if (options.isCurved) addCurvedTextToLogo(options)
                else addTextToLogo(options)
            }
        ).show()
    }

    private fun addTextToLogo(options: TextOptionsDialog.TextOptions) {
        val textView = DraggableTextView(this)

        textView.text = options.text
        textView.setTextColor(options.color)
        textView.textSize = options.textSize
        textView.rotation = options.rotation
        textView.alpha    = (100 - options.transparency) / 100f

        applyFontToTextView(textView, options.fontOption)

        if (options.hasBackground) {
            val alpha        = options.bgTransparency / 100f
            val colorWithAlpha = (Math.round(alpha * 255) shl 24) or (options.bgColor and 0x00FFFFFF)
            textView.setBackgroundColor(colorWithAlpha)
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity    = Gravity.CENTER
        textView.layoutParams = params

        textView.onDoubleClickListener = { showEditTextDialog(it) }
        textView.onSingleClickListener = { it.bringToFront() }
        textView.onLongClickListener   = { deleteText(it); true }

        val command = AddTextCommand(
            container   = binding.textOverlayContainer,
            textView    = textView,
            onTextAdded = { addedTextView ->
                textOverlays.add(addedTextView)
                Toast.makeText(
                    this,
                    "Text added - drag to move, double-tap to edit, long-press to delete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
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
            onDoubleClickListener = { showEditCurvedTextDialog(it) }
            onSingleClickListener = { it.bringToFront() }
            onLongClickListener   = { deleteCurvedText(it); true }
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity           = Gravity.CENTER
        curvedTextView.layoutParams = params

        val command = AddCurvedTextCommand(
            container   = binding.textOverlayContainer,
            curvedTextView = curvedTextView,
            onTextAdded = { addedTextView ->
                curvedTextOverlays.add(addedTextView)
                Toast.makeText(
                    this,
                    "Curved text added - drag to move, double-tap to edit, long-press to delete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
    }

    private fun deleteText(textView: DraggableTextView) {
        if (textOverlays.contains(textView)) {
            val command = DeleteTextCommand(
                container   = binding.textOverlayContainer,
                textView    = textView,
                onTextDeleted = {
                    textOverlays.remove(it)
                    Toast.makeText(this, "Text deleted", Toast.LENGTH_SHORT).show()
                }
            )
            undoRedoManager.executeCommand(command)
            updateUndoRedoButtonsState()
        }
    }

    private fun deleteCurvedText(curvedTextView: DraggableCurvedTextView) {
        if (curvedTextOverlays.contains(curvedTextView)) {
            val command = DeleteCurvedTextCommand(
                container      = binding.textOverlayContainer,
                curvedTextView = curvedTextView,
                onTextDeleted  = {
                    curvedTextOverlays.remove(it)
                    Toast.makeText(this, "Curved text deleted", Toast.LENGTH_SHORT).show()
                }
            )
            undoRedoManager.executeCommand(command)
            updateUndoRedoButtonsState()
        }
    }

    private fun showEditTextDialog(textView: DraggableTextView) {
        val background = textView.background
        var currentBgColor        = Color.TRANSPARENT
        var currentBgTransparency = 100
        var hasBackground         = false

        if (background is ColorDrawable) {
            currentBgColor    = background.color
            hasBackground     = currentBgColor != Color.TRANSPARENT
            if (hasBackground) {
                currentBgTransparency = (Color.alpha(currentBgColor) / 255f * 100).toInt()
            }
        }

        val currentFontIndex = fontOptions.indexOfFirst { it.typeface == textView.typeface }
            .coerceAtLeast(0)

        val options = TextOptionsDialog.TextOptions(
            text          = textView.text.toString(),
            color         = textView.currentTextColor,
            textSize      = textView.textSize / resources.displayMetrics.scaledDensity,
            transparency  = ((1 - textView.alpha) * 100).toInt(),
            fontOption    = fontOptions[currentFontIndex],
            rotation      = textView.rotation,
            bgColor       = currentBgColor,
            bgTransparency = currentBgTransparency,
            hasBackground = hasBackground,
            isCurved      = false
        )

        TextOptionsDialog(
            activity       = this,
            fontOptions    = fontOptions,
            onTextAdded    = { },
            onTextUpdated  = { updatedOptions -> updateTextView(textView, updatedOptions) },
            existingText   = options,
            isEditMode     = true
        ).show()
    }

    private fun showEditCurvedTextDialog(curvedTextView: DraggableCurvedTextView) {
        val background = curvedTextView.background
        var currentBgColor        = Color.TRANSPARENT
        var currentBgTransparency = 100
        var hasBackground         = false

        if (background is ColorDrawable) {
            currentBgColor = background.color
            hasBackground  = currentBgColor != Color.TRANSPARENT
            if (hasBackground) {
                currentBgTransparency = (Color.alpha(currentBgColor) / 255f * 100).toInt()
            }
        }

        val currentFontIndex = fontOptions.indexOfFirst {
            it.typeface == curvedTextView.getCustomTypeface()
        }.coerceAtLeast(0)

        val options = TextOptionsDialog.TextOptions(
            text          = curvedTextView.getText(),
            color         = curvedTextView.getTextColor(),
            textSize      = curvedTextView.getTextSizeValue(),
            transparency  = ((1 - curvedTextView.getTextAlpha()) * 100).toInt(),
            fontOption    = fontOptions[currentFontIndex],
            rotation      = curvedTextView.getRotationDegrees(),
            bgColor       = currentBgColor,
            bgTransparency = currentBgTransparency,
            hasBackground = hasBackground,
            isCurved      = true,
            curveRadius   = curvedTextView.getCurveRadius(),
            curveUp       = curvedTextView.isCurveUp()
        )

        TextOptionsDialog(
            activity      = this,
            fontOptions   = fontOptions,
            onTextAdded   = { },
            onTextUpdated = { updatedOptions -> updateCurvedTextView(curvedTextView, updatedOptions) },
            existingText  = options,
            isEditMode    = true
        ).show()
    }

    private fun updateTextView(textView: DraggableTextView, options: TextOptionsDialog.TextOptions) {
        val previousState = TextViewState(
            textView      = textView,
            text          = textView.text.toString(),
            color         = textView.currentTextColor,
            textSize      = textView.textSize / resources.displayMetrics.scaledDensity,
            transparency  = ((1 - textView.alpha) * 100).toInt(),
            rotation      = textView.rotation,
            bgColor       = (textView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT,
            bgTransparency = ((textView.background as? ColorDrawable)
                ?.let { Color.alpha(it.color) } ?: 0) * 100 / 255,
            hasBackground = textView.background is ColorDrawable
                    && (textView.background as ColorDrawable).color != Color.TRANSPARENT,
            x         = textView.x,
            y         = textView.y,
            fontIndex = fontOptions.indexOfFirst { it.typeface == textView.typeface }
        )

        val newState = TextViewState(
            textView      = textView,
            text          = options.text,
            color         = options.color,
            textSize      = options.textSize,
            transparency  = options.transparency,
            rotation      = options.rotation,
            bgColor       = options.bgColor,
            bgTransparency = options.bgTransparency,
            hasBackground = options.hasBackground,
            x         = textView.x,
            y         = textView.y,
            fontIndex = fontOptions.indexOf(options.fontOption)
        )

        val command = UpdateTextViewCommand(
            textView      = textView,
            previousState = previousState,
            newState      = newState,
            onUpdate      = {
                Toast.makeText(this, "Text updated", Toast.LENGTH_SHORT).show()
            }
        )

        undoRedoManager.executeCommand(command)
        updateUndoRedoButtonsState()
    }

    private fun updateCurvedTextView(
        curvedTextView: DraggableCurvedTextView,
        options: TextOptionsDialog.TextOptions
    ) {
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
                ResourcesCompat.getFont(this, fontOption.fontResource)?.let {
                    textView.typeface = it
                }
            }
            fontOption.typeface != null -> textView.typeface = fontOption.typeface
        }
    }

    private fun applyFontToCurvedTextView(
        curvedTextView: DraggableCurvedTextView,
        fontOption: FontOption
    ) {
        when {
            fontOption.fontResource != null -> {
                ResourcesCompat.getFont(this, fontOption.fontResource)?.let {
                    curvedTextView.setTypeface(it)
                }
            }
            fontOption.typeface != null -> curvedTextView.setTypeface(fontOption.typeface)
        }
    }

    // ================================================================
    // EXPORT / SAVE TO GALLERY
    // ================================================================

    private fun saveEditedLogo() {
        exportLogo("png")
    }

    private fun showDownloadDialog() {
        val dialogBinding = DialogDownloadOptionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.radioSvg.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.svgWarning.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dialogBinding.btnExport.setOnClickListener {
            val format = when (dialogBinding.formatRadioGroup.checkedRadioButtonId) {
                R.id.radioSvg -> "svg"
                else          -> "png"
            }
            dialog.dismiss()
            exportLogo(format)
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun exportLogo(format: String) {
        try {
            if (format == "svg") exportAsSvg() else exportAsPng()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error exporting logo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportAsPng() {
        try {
            val previewCard = binding.previewCard
            val bitmap = Bitmap.createBitmap(
                previewCard.width,
                previewCard.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            previewCard.draw(canvas)
            saveBitmapToGallery(bitmap)
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating PNG: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportAsSvg() {
        try {
            if (originalSvg != null) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName  = "Logo_$timeStamp.svg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(originalSvg!!.toString().toByteArray())
                            outputStream.flush()
                            Toast.makeText(this, "SVG saved to Downloads", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val svgFile = File(downloadsDir, fileName)
                    FileOutputStream(svgFile).use { outputStream ->
                        outputStream.write(originalSvg!!.toString().toByteArray())
                        outputStream.flush()
                        Toast.makeText(this, "SVG saved to ${svgFile.absolutePath}", Toast.LENGTH_LONG).show()
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(svgFile)
                        sendBroadcast(mediaScanIntent)
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "SVG not available for this logo. Saving as PNG instead.",
                    Toast.LENGTH_SHORT
                ).show()
                exportAsPng()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving SVG: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName  = "Logo_$timeStamp.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Logix"
                    )
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        Toast.makeText(this, "Logo saved to Pictures/Logix", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val appDir    = File(picturesDir, "Logix").also { it.mkdirs() }
                val imageFile = File(appDir, fileName)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    Toast.makeText(this, "Logo saved to ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(imageFile)
                    sendBroadcast(mediaScanIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // PRODUCT PREVIEW
    // ================================================================

    private fun openProductPreview() {
        try {
            val previewCard = binding.previewCard
            val bitmap = Bitmap.createBitmap(
                previewCard.width,
                previewCard.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            previewCard.draw(canvas)

            val tempFile = File(
                cacheDir,
                "temp_logo_preview_${System.currentTimeMillis()}.png"
            )
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            val intent = Intent(this, ProductPreviewActivity::class.java).apply {
                putExtra("logo_path", tempFile.absolutePath)
            }
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error preparing preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // LIFECYCLE
    // ================================================================

    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        currentBitmap?.recycle()
    }
}