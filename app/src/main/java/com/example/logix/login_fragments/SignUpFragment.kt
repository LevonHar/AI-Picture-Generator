package com.example.logix.login_fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.logix.R
import com.example.logix.UserActivity
import com.example.logix.api.RetrofitClient
import com.example.logix.databinding.FragmentSignUpBinding
import com.example.logix.models.SignUpRequest
import com.example.logix.models.SignUpResponse
import com.example.logix.utils.SharedPrefManager
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private val TAG = "SignUpFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSignUp()
        setupTextWatchers()
    }

    private fun setupSignUp() {
        binding.buttonSignIn.setOnClickListener {
            if (validateInputs()) {
                performSignUp()
            }
        }

        binding.textLogIn.setOnClickListener {
            // Navigate back to LoginFragment
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupTextWatchers() {
        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.userNameLayout.setBackgroundResource(R.drawable.edit_text_background)
                }
            }
        })

        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
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
                }
            }
        })

        binding.etRepeatPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.repeatPasswordLayout.setBackgroundResource(R.drawable.edit_text_background)
                }
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val repeatPassword = binding.etRepeatPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.userNameLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etUsername.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            binding.userNameLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etUsername.error = "Username must be at least 3 characters"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etEmail.error = "Enter a valid email"
            isValid = false
        } else if (!email.endsWith("@gmail.com")) {
            binding.emailLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etEmail.error = "Only Gmail addresses are allowed"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etPassword.error = "Password is required"
            isValid = false
        } else if (!isValidPassword(password)) {
            binding.passwordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etPassword.error = "Password must contain uppercase, lowercase, number, and special character"
            isValid = false
        }

        if (repeatPassword.isEmpty()) {
            binding.repeatPasswordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etRepeatPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != repeatPassword) {
            binding.repeatPasswordLayout.setBackgroundResource(R.drawable.edit_text_error_background)
            binding.etRepeatPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
        return Regex(passwordPattern).matches(password)
    }

    private fun performSignUp() {
        val signUpRequest = SignUpRequest(
            userName = binding.etUsername.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            password = binding.etPassword.text.toString().trim(),
            repeatPassword = binding.etRepeatPassword.text.toString().trim()
        )

        showLoading(true)
        Log.d(TAG, "Sending signup request: $signUpRequest")

        RetrofitClient.instance.signUp(signUpRequest).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                showLoading(false)

                when (response.code()) {
                    201 -> {
                        response.body()?.let { signUpResponse ->
                            Log.d(TAG, "Signup successful: ${signUpResponse.userName}")

                            // Save user info directly (assuming the response contains token)
                            // You might need to adjust this based on your actual SignUpResponse structure
                            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                                signUpResponse.userId.toString(),
                                signUpResponse.userName,
                                signUpRequest.email
                            )

                            // If your API returns a token in signup response, save it
                            // signUpResponse.token?.let {
                            //     SharedPrefManager.getInstance(requireContext()).saveToken(it)
                            // }

                            Toast.makeText(requireContext(),
                                "Account created successfully!",
                                Toast.LENGTH_LONG).show()

                            // Navigate directly to UserActivity
                            val intent = Intent(requireContext(), UserActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                    400 -> handleErrorResponse(response)
                    409 -> {
                        val error = response.errorBody()?.string() ?: "User already exists"
                        Log.e(TAG, "Conflict: $error")
                        Toast.makeText(requireContext(), "Email already registered. Please login.", Toast.LENGTH_LONG).show()

                        // Navigate to login fragment
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    else -> {
                        Log.e(TAG, "Error response code: ${response.code()}")
                        Toast.makeText(requireContext(),
                            "Signup failed. Please try again.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network error: ${t.message}")
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleErrorResponse(response: Response<SignUpResponse>) {
        try {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Error response: $errorBody")

            if (errorBody != null) {
                if (errorBody.startsWith("[")) {
                    val errorArray = JSONArray(errorBody)
                    val errors = mutableListOf<String>()
                    for (i in 0 until errorArray.length()) {
                        errors.add(errorArray.getString(i))
                    }
                    Toast.makeText(requireContext(), errors.joinToString("\n"), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), errorBody, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Validation failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.buttonSignIn.isEnabled = !show
        binding.buttonSignIn.text = if (show) "Please wait..." else "SignUp"
    }
}