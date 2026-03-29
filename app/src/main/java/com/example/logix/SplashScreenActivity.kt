package com.example.logix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.logix.utils.SharedPrefManager

class SplashScreenActivity : AppCompatActivity() {
    private val splashDuration: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val gifImageView = findViewById<ImageView>(R.id.splashGif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.splash_animation)
            .into(gifImageView)

        // Navigate based on login state after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStateAndNavigate()
        }, splashDuration)
    }

    private fun checkLoginStateAndNavigate() {
        val sharedPrefManager = SharedPrefManager.getInstance(this)

        // Check if user has valid session and remember me is enabled
        if (sharedPrefManager.isValidSession() && sharedPrefManager.isRememberMeEnabled()) {
            // User is logged in, go to UserActivity
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        } else {
            // User is not logged in, go to MainActivity (login screen)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}