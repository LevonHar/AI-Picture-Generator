package com.example.logix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.logix.databinding.ActivityUserBinding
import com.example.logix.user_fragments.FavoriteFragment
import com.example.logix.user_fragments.HomeFragment
import com.example.logix.user_fragments.UserFragment
import com.example.logix.utils.SharedPrefManager
import com.example.logix.viewmodel.FavoriteViewModel

class UserActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserBinding
    private lateinit var favoriteViewModel: FavoriteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FavoriteViewModel
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]

        // Initialize SharedPreferences and load favorites
        val sharedPrefManager = SharedPrefManager.getInstance(this)
        favoriteViewModel.initialize(sharedPrefManager.getSharedPreferences())

        // Set default fragment
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

    override fun onResume() {
        super.onResume()
        // Refresh favorites when returning to activity
        favoriteViewModel.favoriteNetworkLogos.value?.let {
            // This ensures the data is still loaded
        }
    }
}