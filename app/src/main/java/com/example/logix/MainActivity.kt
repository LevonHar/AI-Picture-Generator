package com.example.logix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.logix.databinding.ActivityMainBinding
import com.example.logix.fragments.LogInFragment
import com.example.logix.fragments.SignUpFragment
import com.example.logix.fragments.VerifyAccountFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LogInFragment())
                .commit()
        }
    }
}