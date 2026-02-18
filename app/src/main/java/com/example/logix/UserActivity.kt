package com.example.logix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.logix.databinding.ActivityUserBinding
import com.example.logix.user_fragments.FavoriteFragment
import com.example.logix.user_fragments.HomeFragment
import com.example.logix.user_fragments.UserFragment

class UserActivity : AppCompatActivity() {
    lateinit var binding : ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment())

        binding.bNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.item1 -> {
                    replaceFragment(HomeFragment())
                }
                R.id.item2 -> {
                    replaceFragment(FavoriteFragment())
                }
                R.id.item3 -> {
                    replaceFragment(UserFragment())
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}