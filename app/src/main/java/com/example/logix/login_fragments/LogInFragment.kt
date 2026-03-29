package com.example.logix.login_fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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
    private var isPasswordVisible = false

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
        setupPasswordVisibilityToggle()
        loadSavedCredentials()
        checkAutoLogin()
    }

    private fun setupLogin() {
        binding.buttonLogIn.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
    }

    private fun loadSavedCredentials() {
        val sharedPrefManager = SharedPrefManager.getInstance(requireContext())
        if (sharedPrefManager.isRememberMeEnabled()) {
            val savedEmail = sharedPrefManager.getSavedEmail()
            val savedPassword = sharedPrefManager.getSavedPassword()

            if (!savedEmail.isNullOrEmpty()) {
                binding.etEmail.setText(savedEmail)
            }
            if (!savedPassword.isNullOrEmpty()) {
                binding.etPassword.setText(savedPassword)
            }

            // Auto-check the remember me checkbox if credentials were saved
            binding.cbRememberMe.isChecked = true
        }
    }

    private fun checkAutoLogin() {
        val sharedPrefManager = SharedPrefManager.getInstance(requireContext())

        // Check if user is already logged in and remember me is enabled
        if (sharedPrefManager.isValidSession() && sharedPrefManager.isRememberMeEnabled()) {
            // Auto navigate to UserActivity
            val intent = Intent(requireContext(), UserActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun setupPasswordVisibilityToggle() {
        binding.ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(
                editText = binding.etPassword,
                imageView = binding.ivPasswordVisibility,
                isVisible = isPasswordVisible
            )
        }
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: android.widget.ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_open)
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_closed)
        }
        editText.setSelection(editText.text.length)
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    binding.emailLayout.setBackgroundResource(R.drawable.edit_text_background)
                    binding.etEmail.error = null
                } else if (!s.isNullOrEmpty()) {
                    binding.emailLayout.setBackgroundResource(R.drawable.edit_text_background)
                }
            }
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_background)
                    binding.etPassword.error = null
                }
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etEmail.error = "Enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val loginRequest = LoginRequest(
            email = email,
            password = password
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
                            val sharedPrefManager = SharedPrefManager.getInstance(requireContext())
                            sharedPrefManager.saveToken(loginResponse.token)

                            val emailToSave = loginResponse.email ?: email
                            sharedPrefManager.saveUserInfo(
                                loginResponse.userId.toString(),
                                loginResponse.userName,
                                emailToSave
                            )

                            // Handle Remember Me
                            val rememberMe = binding.cbRememberMe.isChecked
                            if (rememberMe) {
                                // Save credentials for next login
                                sharedPrefManager.setRememberMe(true, email, password)
                            } else {
                                // Clear saved credentials if they exist
                                if (sharedPrefManager.isRememberMeEnabled()) {
                                    sharedPrefManager.setRememberMe(false)
                                }
                            }

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

                        // Save email for verification
                        val sharedPrefManager = SharedPrefManager.getInstance(requireContext())
                        sharedPrefManager.saveEmail(email)

                        val verifyAccountFragment = VerifyAccountFragment()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, verifyAccountFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    else -> {
                        Log.e(TAG, "Error response code: ${response.code()}")
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(requireContext(),
                            errorBody ?: "Login failed. Please try again.",
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
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(requireContext(),
                        errorBody ?: "Failed to send reset link",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.buttonLogIn.isEnabled = !show
        binding.buttonLogIn.text = if (show) "Please wait..." else "Log In"

        // Disable password visibility toggle during loading
        binding.ivPasswordVisibility.isEnabled = !show

        // Disable inputs during loading
        binding.etEmail.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.forgotPasswordText.isEnabled = !show
        binding.textSignUp.isEnabled = !show
        binding.cbRememberMe.isEnabled = !show
    }
}