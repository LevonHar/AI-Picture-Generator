package com.example.logix.user_fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.logix.MainActivity // Replace with your actual login activity
import com.example.logix.R
import com.example.logix.utils.SharedPrefManager

class UserFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var linearLogOut: View
    private lateinit var deleteAccount: View
    private lateinit var aboutApp: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        initViews(view)
        loadUserInfo()
        setupClickListeners()

        return view
    }

    private fun initViews(view: View) {
        tvUserName = view.findViewById(R.id.userName)
        tvUserEmail = view.findViewById(R.id.userGmail)
        linearLogOut = view.findViewById(R.id.linearLogOut)
        deleteAccount = view.findViewById(R.id.deleteAccount)
        aboutApp = view.findViewById(R.id.aboutApp)
    }

    private fun loadUserInfo() {
        val sharedPrefManager = SharedPrefManager.getInstance(requireContext())
        val userName = sharedPrefManager.getUserName()
        val userEmail = sharedPrefManager.getUserEmail()

        tvUserName.text = userName ?: "User"
        tvUserEmail.text = userEmail ?: "user@example.com"
    }

    private fun setupClickListeners() {
        // Logout click listener
        linearLogOut.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Delete account click listener (optional)
        deleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // About app click listener (optional)
        aboutApp.setOnClickListener {
            showAboutAppDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun performLogout() {
        val sharedPrefManager = SharedPrefManager.getInstance(requireContext())
        val rememberMe = sharedPrefManager.isRememberMeEnabled()

        if (rememberMe) {
            // Logout but keep credentials for next login
            sharedPrefManager.clearUserData()
            Toast.makeText(requireContext(),
                "Logged out. Your credentials are saved.",
                Toast.LENGTH_SHORT).show()
        } else {
            // Complete logout - clear everything
            sharedPrefManager.clearAll()
            Toast.makeText(requireContext(),
                "Logged out completely",
                Toast.LENGTH_SHORT).show()
        }

        // Navigate back to login screen
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                performDeleteAccount()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun performDeleteAccount() {
        // TODO: Implement delete account API call
        Toast.makeText(requireContext(),
            "Account deletion feature coming soon",
            Toast.LENGTH_SHORT).show()
    }

    private fun showAboutAppDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About App")
            .setMessage("App Name: Logix\nVersion: 1.0\n\nYour app description here.")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }
}