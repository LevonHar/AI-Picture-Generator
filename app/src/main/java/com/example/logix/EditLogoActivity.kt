package com.example.logix

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class EditLogoActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_logo)

        val imageView = findViewById<ImageView>(R.id.editLogoImage)

        // Get image from intent
        val logo = intent.getIntExtra("logo", 0)

        if (logo != 0) {
            imageView.setImageResource(logo)
        }
    }
}
