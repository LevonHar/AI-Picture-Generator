package com.example.logix.login_fragments

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import com.example.logix.databinding.FragmentVerifyAccountBinding
import com.example.logix.models.VerifyRequest
import com.example.logix.models.VerifyResponse
import com.example.logix.utils.SharedPrefManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyAccountFragment : Fragment() {

    private var _binding: FragmentVerifyAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var timer: CountDownTimer
    private val totalTime = 2 * 60 * 1000L
    private val TAG = "VerifyAccountFragment"

    // Store email as a class variable to ensure it's available throughout
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadUserEmail() // Load email first
        startTimer()
        setupEditTextNavigation()
        setupClickListeners()
        displayUserEmail()
    }

    private fun setupUI() {
        binding.etCode1.text?.clear()
        binding.etCode2.text?.clear()
        binding.etCode3.text?.clear()
        binding.etCode4.text?.clear()
        binding.etCode5.text?.clear()
        binding.etCode6.text?.clear()

        // Request focus on first edit text
        binding.etCode1.requestFocus()
    }

    private fun loadUserEmail() {
        // Try to get email from SharedPreferences
        userEmail = SharedPrefManager.getInstance(requireContext()).getEmail()

        // If email is null, try to get it from arguments
        if (userEmail == null) {
            arguments?.let {
                userEmail = it.getString("email")
                if (userEmail != null) {
                    // Save it to SharedPreferences for future use
                    SharedPrefManager.getInstance(requireContext()).saveEmail(userEmail!!)
                }
            }
        }

        // If still null, try to get from saved user email
        if (userEmail == null) {
            userEmail = SharedPrefManager.getInstance(requireContext()).getUserEmail()
        }

        Log.d(TAG, "Loaded email: $userEmail")
    }

    private fun displayUserEmail() {
        if (userEmail != null) {
            binding.sendToEmailText.text = "Code sent to ${maskEmail(userEmail!!)}"
        } else {
            binding.sendToEmailText.text = "Code sent to your email"
            // Show a warning that email is missing
            Toast.makeText(requireContext(),
                "Email information missing. Please sign up again.",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun maskEmail(email: String): String {
        // Optional: Mask email for privacy (e.g., u***r@example.com)
        val parts = email.split("@")
        if (parts.size == 2) {
            val username = parts[0]
            val domain = parts[1]
            val maskedUsername = if (username.length > 2) {
                username[0] + "***" + username[username.length - 1]
            } else {
                "***"
            }
            return "$maskedUsername@$domain"
        }
        return email
    }

    private fun startTimer() {
        timer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvTimer.text = "00:00"
                binding.tvResendCode.isEnabled = true
                binding.tvResendCode.setTextColor(resources.getColor(R.color.black))
            }
        }
        timer.start()
    }

    private fun setupEditTextNavigation() {
        val editTexts = arrayOf(
            binding.etCode1,
            binding.etCode2,
            binding.etCode3,
            binding.etCode4,
            binding.etCode5,
            binding.etCode6
        )

        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        } else {
                            // Auto verify when last digit is entered
                            binding.verifyCodeButton.postDelayed({
                                if (getEnteredCode().length == 6) {
                                    verifyCode()
                                }
                            }, 200)
                        }
                    } else if (s?.isEmpty() == true && i > 0) {
                        editTexts[i - 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setupClickListeners() {
        binding.verifyCodeButton.setOnClickListener {
            verifyCode()
        }

        binding.tvResendCode.setOnClickListener {
            resendVerificationCode()
        }
    }

    private fun getEnteredCode(): String {
        return binding.etCode1.text.toString() +
                binding.etCode2.text.toString() +
                binding.etCode3.text.toString() +
                binding.etCode4.text.toString() +
                binding.etCode5.text.toString() +
                binding.etCode6.text.toString()
    }

    private fun verifyCode() {
        val code = getEnteredCode()

        if (code.length < 6) {
            Toast.makeText(requireContext(), "Please enter complete verification code", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the stored email from class variable
        val email = userEmail ?: SharedPrefManager.getInstance(requireContext()).getEmail()

        if (email == null) {
            Log.e(TAG, "Email is null in verifyCode")
            Toast.makeText(requireContext(),
                "Session expired. Please sign up again.",
                Toast.LENGTH_LONG).show()

            // Navigate back to signup
            navigateToSignUp()
            return
        }

        Log.d(TAG, "Verifying code for email: $email")
        showLoading(true)

        val verifyRequest = VerifyRequest(email, code)
        // In your fragment
        RetrofitClient.instance.verify(verifyRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)

                when (response.code()) {
                    200 -> {
                        Log.d(TAG, "Verification successful for email: $email")
                        handleSuccessfulVerification(email)
                    }
                    207 -> {
                        Log.d(TAG, "Email already verified: $email")
                        handleEmailAlreadyVerified(email)
                    }
                    400 -> {
                        Toast.makeText(requireContext(),
                            "Invalid verification code. Please try again.",
                            Toast.LENGTH_SHORT).show()
                        clearInputFields()
                    }
                    404 -> {
                        Toast.makeText(requireContext(),
                            "User not found. Please sign up again.",
                            Toast.LENGTH_LONG).show()
                        navigateToSignUp()
                    }
                    else -> handleErrorResponse(response, "Verification failed")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network error: ${t.message}")
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccessfulVerification(email: String) {
        val tempUserId = SharedPrefManager.getInstance(requireContext()).getTempUserId()
        val tempUserName = SharedPrefManager.getInstance(requireContext()).getTempUserName()

        if (tempUserId != null && tempUserName != null) {
            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                tempUserId,
                tempUserName,
                email
            )
            Log.d(TAG, "User info saved - ID: $tempUserId, Name: $tempUserName, Email: $email")
        } else {
            Log.w(TAG, "Temp user info is missing - UserId: $tempUserId, UserName: $tempUserName")
        }

        Toast.makeText(requireContext(),
            "Email verified successfully! Please login to continue.",
            Toast.LENGTH_LONG).show()

        SharedPrefManager.getInstance(requireContext()).clearTempUserInfo()

        // ✅ Navigate to LoginFragment on success
        navigateToLoginFragment()
    }

    private fun navigateToLoginFragment() {
        try {
            Log.d(TAG, "Navigating to LoginFragment")

            val loginFragment = LogInFragment().apply {
                arguments = Bundle().also { bundle ->
                    userEmail?.let { bundle.putString("email", it) }
                }
            }

            // ✅ Use your actual fragment container ID (R.id.fragment_container or similar)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, loginFragment) // 🔁 Replace with your actual container ID
                .addToBackStack(null)
                .commitAllowingStateLoss()

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to LoginFragment: ${e.message}")
            try {
                val intent = Intent(requireContext(), UserActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback navigation failed: ${ex.message}")
            }
        }
    }

    private fun handleEmailAlreadyVerified(email: String) {
        // Get temp user info
        val tempUserId = SharedPrefManager.getInstance(requireContext()).getTempUserId()
        val tempUserName = SharedPrefManager.getInstance(requireContext()).getTempUserName()

        // Save user info if available
        if (tempUserId != null && tempUserName != null) {
            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                tempUserId,
                tempUserName,
                email
            )
        }

        Toast.makeText(requireContext(),
            "Email already verified. Please login to continue.",
            Toast.LENGTH_LONG).show()

        // Clear temp data
        SharedPrefManager.getInstance(requireContext()).clearTempUserInfo()

        // Navigate to LoginFragment
        navigateToLoginFragment()
    }

    private fun navigateToSignUp() {
        try {
            requireActivity().supportFragmentManager.popBackStack(
                "SignUpFragment",
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )

            // If popBackStack doesn't work, create new SignUpFragment
            val signUpFragment = SignUpFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, signUpFragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to SignUpFragment: ${e.message}")
        }
    }

    private fun resendVerificationCode() {
        // Use the stored email from class variable
        val email = userEmail ?: SharedPrefManager.getInstance(requireContext()).getEmail()

        if (email == null) {
            Log.e(TAG, "Email is null in resendVerificationCode")
            Toast.makeText(requireContext(),
                "Session expired. Please sign up again.",
                Toast.LENGTH_SHORT).show()
            navigateToSignUp()
            return
        }

        Log.d(TAG, "Resending code to: $email")
        showLoading(true)

        RetrofitClient.instance.resendVerification(email).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(),
                        "Verification code resent to your email",
                        Toast.LENGTH_SHORT).show()

                    timer.cancel()
                    startTimer()

                    binding.tvResendCode.isEnabled = false
                    binding.tvResendCode.setTextColor(resources.getColor(R.color.gray))

                    clearInputFields()
                } else {
                    Toast.makeText(requireContext(),
                        "Failed to resend code. Please try again.",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network error type: ${t.javaClass.simpleName}")
                Log.e(TAG, "Network error message: ${t.message}")
                Log.e(TAG, "Network error cause: ${t.cause}")
                Toast.makeText(requireContext(),
                    "Error: ${t.javaClass.simpleName} - ${t.message}",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleErrorResponse(response: Response<*>, defaultMessage: String) {
        try {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Error response: $errorBody")

            if (errorBody != null) {
                Toast.makeText(requireContext(), errorBody, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), defaultMessage, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), defaultMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputFields() {
        binding.etCode1.text?.clear()
        binding.etCode2.text?.clear()
        binding.etCode3.text?.clear()
        binding.etCode4.text?.clear()
        binding.etCode5.text?.clear()
        binding.etCode6.text?.clear()
        binding.etCode1.requestFocus()
    }

    private fun showLoading(show: Boolean) {
        binding.verifyCodeButton.isEnabled = !show
        binding.verifyCodeButton.text = if (show) "Verifying..." else "Verify Code"
        binding.tvResendCode.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::timer.isInitialized) {
            timer.cancel()
        }
        _binding = null
    }
}