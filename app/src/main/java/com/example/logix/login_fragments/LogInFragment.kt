package com.example.logix.login_fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import com.example.logix.R
import com.example.logix.databinding.FragmentLogInBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class LogInFragment : Fragment() {

    private lateinit var binding: FragmentLogInBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogInBinding.inflate(inflater, container, false)

        setupForgotPassword()

        return binding.root
    }

    private fun setupForgotPassword() {
        binding.forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(
            R.layout.reset_password_dialog,
            null
        )

        val emailEd = view.findViewById<EditText>(R.id.edResetPassword)
        val sendBtn = view.findViewById<AppCompatButton>(R.id.verifyCodeButton)

        sendBtn.setOnClickListener {
            val email = emailEd.text.toString()
            if (email.isNotEmpty()) {
                dialog.dismiss()
            } else {
                emailEd.error = "Email required"
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

}
