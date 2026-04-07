package com.example.logix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.logix.databinding.ActivityProductPreviewBinding

class ProductPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductPreviewBinding
    private var logoBitmap: Bitmap? = null
    private var currentProductImage: ImageView? = null
    private var logoOverlayView: LogoOverlayView? = null
    private var currentProductType = "tshirt"

    data class Product(
        val id: Int,
        val name: String,
        val imageRes: Int,
        val type: String
    )

    private val products = listOf(
        Product(R.id.product_tshirt, "T-Shirt", R.drawable.product_tshirt, "tshirt"),
        Product(R.id.product_cup, "Cup", R.drawable.product_cup, "cup"),
        Product(R.id.product_cart, "Cart", R.drawable.product_cart, "cart"),
        Product(R.id.product_wall, "Cart", R.drawable.product_wall, "wall")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProductPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        loadLogoBitmap()
        setupProductSelection()
        setupCloseButton()
        setupSaveButton()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadLogoBitmap() {
        try {
            val intent = intent
            if (intent.hasExtra("logo_bitmap")) {
                logoBitmap = intent.getParcelableExtra("logo_bitmap")
            } else if (intent.hasExtra("logo_path")) {
                val path = intent.getStringExtra("logo_path")
                path?.let {
                    logoBitmap = android.graphics.BitmapFactory.decodeFile(it)
                }
            }

            if (logoBitmap == null) {
                Toast.makeText(this, "No logo found to preview", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Scale logo to reasonable size (but keep original for zoom)
            logoBitmap = scaleLogoBitmap(logoBitmap!!)

            // Initialize with first product
            setupProductPreview(products[0])

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading logo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scaleLogoBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 400
        val width = bitmap.width
        val height = bitmap.height

        return if (width > maxSize || height > maxSize) {
            val scale = maxSize.toFloat() / maxOf(width, height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun setupProductSelection() {
        binding.productTshirt.setOnClickListener {
            selectProduct(products[0])
        }

        binding.productCup.setOnClickListener {
            selectProduct(products[1])
        }

        binding.productCart.setOnClickListener {
            selectProduct(products[2])
        }

        binding.productWall.setOnClickListener {
            selectProduct(products[3])
        }

        // Highlight first product by default
        updateProductSelection(products[0].id)
    }

    private fun selectProduct(product: Product) {
        currentProductType = product.type
        setupProductPreview(product)
        updateProductSelection(product.id)
    }

    private fun setupProductPreview(product: Product) {
        // Clear previous overlay
        binding.productImageContainer.removeAllViews()

        // Create and add product image
        val productImageView = ImageView(this).apply {
            setImageResource(product.imageRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        binding.productImageContainer.addView(productImageView)
        currentProductImage = productImageView

        // Create and add logo overlay
        logoOverlayView = LogoOverlayView(this, logoBitmap!!, product.type).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setOnLogoPositionListener { x, y ->
                // Optional: Save position or update UI
            }
            setOnLogoScaleListener { scale ->
                // Optional: Show current scale
                binding.instructionText.text = "Logo scaled to ${String.format("%.0f", scale * 100)}%"
            }
        }

        binding.productImageContainer.addView(logoOverlayView)

        // Show instructions
        binding.instructionText.text = "Tap and drag to move logo | Pinch to zoom logo on ${product.name}"
        binding.instructionText.visibility = View.VISIBLE
    }

    private fun updateProductSelection(selectedId: Int) {
        // Reset all product cards
        val cards = listOf(
            binding.productTshirt to R.drawable.product_tshirt,
            binding.productCup to R.drawable.product_cup,
            binding.productCart to R.drawable.product_cart,
            binding.productWall to R.drawable.product_wall
        )

        cards.forEach { (card, _) ->
            card.isSelected = card.id == selectedId
            card.setCardBackgroundColor(
                if (card.isSelected)
                    ContextCompat.getColor(this, R.color.purple)
                else
                    ContextCompat.getColor(this, R.color.white)
            )
        }
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener {
            finish()
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveProductPreview()
        }
    }

    private fun saveProductPreview() {
        try {
            val container = binding.productImageContainer
            val bitmap = Bitmap.createBitmap(
                container.width,
                container.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            container.draw(canvas)

            // Save bitmap to gallery
            saveBitmapToGallery(bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "ProductPreview_$timeStamp.png"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Logix")
                }

                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        Toast.makeText(this, "Preview saved to Pictures/Logix folder", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val appDir = java.io.File(picturesDir, "Logix")
                if (!appDir.exists()) appDir.mkdirs()

                val imageFile = java.io.File(appDir, fileName)
                java.io.FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    Toast.makeText(this, "Preview saved to ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Custom View for draggable and scalable logo overlay
    // In ProductPreviewActivity.kt - Updated LogoOverlayView

    inner class LogoOverlayView(
        context: android.content.Context,
        private val originalLogo: Bitmap,
        private val productType: String
    ) : View(context) {

        private var logoX = 0f
        private var logoY = 0f
        private var downX = 0f
        private var downY = 0f
        private var isDragging = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            // Ensure we don't draw any background
            isAntiAlias = true
            isFilterBitmap = true
            // No background color set
        }

        // Zoom related variables
        private var currentScale = 1.0f
        private var baseScale = 1.0f
        private var logoWidth = 0
        private var logoHeight = 0
        private var baseLogoWidth = 0
        private var baseLogoHeight = 0
        private var scaleGestureDetector: ScaleGestureDetector
        private var logoMatrix = Matrix()
        private var positionListener: ((Float, Float) -> Unit)? = null
        private var scaleListener: ((Float) -> Unit)? = null

        // Minimum and maximum scale factors
        private val MIN_SCALE = 0.5f
        private val MAX_SCALE = 3.0f

        // Store the bitmap with preserved transparency
        private var transparentLogo: Bitmap? = null

        init {
            scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

            // Ensure the logo has transparency support
            transparentLogo = ensureTransparentBitmap(originalLogo)

            // Calculate base logo size (80% of original bitmap size)
            post {
                baseLogoWidth = (transparentLogo!!.width * 0.8f).toInt()
                baseLogoHeight = (transparentLogo!!.height * 0.8f).toInt()
                logoWidth = baseLogoWidth
                logoHeight = baseLogoHeight
                currentScale = 1.0f

                logoX = (width - logoWidth) / 2f
                logoY = getInitialYPosition()
                invalidate()
            }
        }

        /**
         * Ensure the bitmap has an alpha channel and remove any white/black background
         */
        private fun ensureTransparentBitmap(bitmap: Bitmap): Bitmap {
            // Create a new bitmap with ARGB_8888 config to support transparency
            val result = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }

            // Draw the original bitmap
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Optional: Remove white background if needed
            // This is useful if the original logo had a white background
            removeWhiteBackground(result)

            return result
        }

        /**
         * Remove white or near-white background from the logo
         * This helps if the original image had a solid background
         */
        private fun removeWhiteBackground(bitmap: Bitmap) {
            val width = bitmap.width
            val height = bitmap.height

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = android.graphics.Color.red(pixel)
                    val green = android.graphics.Color.green(pixel)
                    val blue = android.graphics.Color.blue(pixel)

                    // If pixel is white or near-white, make it transparent
                    if (red > 240 && green > 240 && blue > 240) {
                        bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
                    }
                }
            }
        }

        private fun getInitialYPosition(): Float {
            return when (productType) {
                "tshirt" -> height * 0.4f
                "cup" -> height * 0.35f
                "cart" -> height * 0.45f
                "wall" -> height * 0.4f
                else -> height * 0.4f
            }
        }

        fun setOnLogoPositionListener(listener: (Float, Float) -> Unit) {
            this.positionListener = listener
        }

        fun setOnLogoScaleListener(listener: (Float) -> Unit) {
            this.scaleListener = listener
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Clear the canvas with transparent color
            canvas.drawColor(android.graphics.Color.TRANSPARENT)

            if (logoX >= 0 && logoY >= 0 && logoWidth > 0 && logoHeight > 0) {
                transparentLogo?.let { logo ->
                    // Create scaled bitmap with current scale while preserving transparency
                    val scaledLogo = Bitmap.createScaledBitmap(
                        logo,
                        logoWidth,
                        logoHeight,
                        true
                    )

                    // Draw with a paint that preserves alpha channel
                    val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        isDither = true
                    }

                    canvas.drawBitmap(scaledLogo, logoX, logoY, drawPaint)

                    // Recycle scaled bitmap to prevent memory leaks
                    if (scaledLogo != logo) {
                        scaledLogo.recycle()
                    }
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Let the ScaleGestureDetector process the event first
            scaleGestureDetector.onTouchEvent(event)

            // If scaling is in progress, don't handle dragging
            if (scaleGestureDetector.isInProgress) {
                return true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isPointInsideLogo(event.x, event.y)) {
                        downX = event.x - logoX
                        downY = event.y - logoY
                        isDragging = true
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        var newX = event.x - downX
                        var newY = event.y - downY

                        // Constrain to bounds
                        newX = newX.coerceIn(0f, width - logoWidth.toFloat())
                        newY = newY.coerceIn(0f, height - logoHeight.toFloat())

                        if (newX != logoX || newY != logoY) {
                            logoX = newX
                            logoY = newY
                            invalidate()
                            positionListener?.invoke(logoX, logoY)
                        }
                        return true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        private fun isPointInsideLogo(x: Float, y: Float): Boolean {
            return x in logoX..(logoX + logoWidth) && y in logoY..(logoY + logoHeight)
        }

        private fun applyScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            // Calculate new scale
            var newScale = currentScale * scaleFactor

            // Clamp scale to min/max values
            newScale = newScale.coerceIn(MIN_SCALE, MAX_SCALE)

            if (newScale != currentScale) {
                // Calculate new dimensions
                val newWidth = (baseLogoWidth * newScale).toInt()
                val newHeight = (baseLogoHeight * newScale).toInt()

                // Calculate position adjustment to keep logo centered around the focus point
                val deltaWidth = (newWidth - logoWidth).toFloat()
                val deltaHeight = (newHeight - logoHeight).toFloat()

                // Calculate where the focus point is relative to the logo's position
                val focusRelativeX = focusX - logoX
                val focusRelativeY = focusY - logoY

                // Calculate the new position that keeps the focus point under the finger
                var newX = logoX - (focusRelativeX * (scaleFactor - 1))
                var newY = logoY - (focusRelativeY * (scaleFactor - 1))

                // Constrain to bounds
                newX = newX.coerceIn(0f, width - newWidth.toFloat())
                newY = newY.coerceIn(0f, height - newHeight.toFloat())

                // Update logo properties
                logoWidth = newWidth
                logoHeight = newHeight
                logoX = newX
                logoY = newY
                currentScale = newScale

                invalidate()
                scaleListener?.invoke(currentScale)
            }
        }

        // Inner class to handle scale gestures
        inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val focusX = detector.focusX
                val focusY = detector.focusY

                applyScale(scaleFactor, focusX, focusY)
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                // Save the base scale when scaling starts
                baseScale = currentScale
                return true
            }
        }

        // Reset logo to initial position and scale
        fun resetLogo() {
            post {
                currentScale = 1.0f
                logoWidth = baseLogoWidth
                logoHeight = baseLogoHeight
                logoX = (width - logoWidth) / 2f
                logoY = getInitialYPosition()
                invalidate()
                scaleListener?.invoke(currentScale)
                positionListener?.invoke(logoX, logoY)
            }
        }

        // Get current logo position and scale
        fun getLogoState(): Triple<Float, Float, Float> {
            return Triple(logoX, logoY, currentScale)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            // Clean up bitmaps to prevent memory leaks
            transparentLogo?.recycle()
        }
    }
}