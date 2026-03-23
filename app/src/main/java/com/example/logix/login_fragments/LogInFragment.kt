package com.example.logix.login_fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.logix.R
import com.example.logix.UserActivity
import com.example.logix.api.RetrofitClient
import com.example.logix.databinding.FragmentLogInBinding
import com.example.logix.models.LoginRequest
import com.example.logix.models.LoginResponse
import com.example.logix.utils.SharedPrefManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LogInFragment : Fragment() {

    private lateinit var binding: FragmentLogInBinding
    private val TAG = "LogInFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLogin()
        setupForgotPassword()
        setupSignUp()
        setupTextWatchers()
    }

    private fun setupLogin() {
        binding.buttonLogIn.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.emailText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.emailLayout.setBackgroundResource(R.drawable.edit_text_background)
                }
            }
        })

        binding.passwordText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_background)
                }
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.emailText.text.toString().trim()
        val password = binding.passwordText.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.emailText.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.emailText.error = "Enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.passwordText.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        val loginRequest = LoginRequest(
            email = binding.emailText.text.toString().trim(),
            password = binding.passwordText.text.toString().trim()
        )

        showLoading(true)
        Log.d(TAG, "Sending login request: $loginRequest")

        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                showLoading(false)

                when (response.code()) {
                    200 -> {
                        response.body()?.let { loginResponse ->
                            Log.d(TAG, "Login successful: ${loginResponse.userName}")

                            // Save token and user info
                            SharedPrefManager.getInstance(requireContext()).saveToken(loginResponse.token)
                            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                                loginResponse.userId.toString(),
                                loginResponse.userName,
                                loginResponse.email
                            )

                            Toast.makeText(requireContext(),
                                "Welcome back, ${loginResponse.userName}!",
                                Toast.LENGTH_SHORT).show()

                            // Navigate to UserActivity
                            val intent = Intent(requireContext(), UserActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                    401 -> {
                        Log.e(TAG, "Invalid credentials")
                        Toast.makeText(requireContext(),
                            "Invalid email or password",
                            Toast.LENGTH_LONG).show()
                    }
                    403 -> {
                        Log.e(TAG, "Email not verified")
                        Toast.makeText(requireContext(),
                            "Please verify your email before logging in",
                            Toast.LENGTH_LONG).show()

                        // Navigate to verification fragment
                        val email = binding.emailText.text.toString().trim()
                        SharedPrefManager.getInstance(requireContext()).saveEmail(email)

                        val verifyAccountFragment = VerifyAccountFragment()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, verifyAccountFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    else -> {
                        Log.e(TAG, "Error response code: ${response.code()}")
                        Toast.makeText(requireContext(),
                            "Login failed. Please try again.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network error: ${t.message}")
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupForgotPassword() {
        binding.forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun setupSignUp() {
        binding.textSignUp.setOnClickListener {
            val signUpFragment = SignUpFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, signUpFragment)
                .addToBackStack(null)
                .commit()
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
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                // Call reset password API
                resetPassword(email)
                dialog.dismiss()
            } else {
                emailEd.error = "Valid email required"
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun resetPassword(email: String) {
        showLoading(true)
        RetrofitClient.instance.forgotPassword(email).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(),
                        "Password reset link sent to your email",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(),
                        "Failed to send reset link",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                Toast.makeText(requireContext(),
                    "Network error",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.buttonLogIn.isEnabled = !show
        binding.buttonLogIn.text = if (show) "Please wait..." else "Log In"
    }
}