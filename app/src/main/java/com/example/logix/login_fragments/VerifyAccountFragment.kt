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

    private fun displayUserEmail() {
        val email = SharedPrefManager.getInstance(requireContext()).getEmail()
        if (email != null) {
            binding.sendToEmailText.text = "Code sent to $email"
        } else {
            binding.sendToEmailText.text = "Code sent to your email"
        }
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

        val email = SharedPrefManager.getInstance(requireContext()).getEmail()
        if (email == null) {
            Toast.makeText(requireContext(), "Session expired. Please sign up again.", Toast.LENGTH_SHORT).show()

            // Navigate back to signup
            requireActivity().supportFragmentManager.popBackStack(
                "SignUpFragment",
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            return
        }

        showLoading(true)
        Log.d(TAG, "Verifying code for email: $email")

        val verifyRequest = VerifyRequest(email, code)
        RetrofitClient.instance.verify(verifyRequest).enqueue(object : Callback<VerifyResponse> {
            override fun onResponse(call: Call<VerifyResponse>, response: Response<VerifyResponse>) {
                showLoading(false)

                when (response.code()) {
                    200 -> {
                        Log.d(TAG, "Verification successful")

                        // Get temp user info
                        val tempUserId = SharedPrefManager.getInstance(requireContext()).getTempUserId()
                        val tempUserName = SharedPrefManager.getInstance(requireContext()).getTempUserName()
                        val tempPassword = SharedPrefManager.getInstance(requireContext()).getTempPassword()

                        // Save permanent user info (now user is fully registered)
                        if (tempUserId != null && tempUserName != null) {
                            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                                tempUserId,
                                tempUserName,
                                email
                            )

                            // If your API returns a token after verification, save it
                            response.body()?.message?.let { token ->
                                SharedPrefManager.getInstance(requireContext()).saveToken(token)
                            }
                        }

                        Toast.makeText(requireContext(),
                            "Email verified successfully! Welcome to Logix.",
                            Toast.LENGTH_LONG).show()

                        // Clear temp data
                        SharedPrefManager.getInstance(requireContext()).clearTempUserInfo()
                        SharedPrefManager.getInstance(requireContext()).clearEmail()

                        // Navigate directly to UserActivity
                        val intent = Intent(requireContext(), UserActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    207 -> {
                        Log.d(TAG, "Email already verified")

                        // Get temp user info
                        val tempUserId = SharedPrefManager.getInstance(requireContext()).getTempUserId()
                        val tempUserName = SharedPrefManager.getInstance(requireContext()).getTempUserName()

                        // Save user info
                        if (tempUserId != null && tempUserName != null) {
                            SharedPrefManager.getInstance(requireContext()).saveUserInfo(
                                tempUserId,
                                tempUserName,
                                email
                            )
                        }

                        Toast.makeText(requireContext(),
                            "Email already verified. Welcome back!",
                            Toast.LENGTH_LONG).show()

                        // Clear temp data
                        SharedPrefManager.getInstance(requireContext()).clearTempUserInfo()
                        SharedPrefManager.getInstance(requireContext()).clearEmail()

                        // Navigate directly to UserActivity
                        val intent = Intent(requireContext(), UserActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    400 -> {
                        // Invalid code
                        Toast.makeText(requireContext(),
                            "Invalid verification code. Please try again.",
                            Toast.LENGTH_SHORT).show()
                        clearInputFields()
                    }
                    else -> handleErrorResponse(response, "Verification failed")
                }
            }

            override fun onFailure(call: Call<VerifyResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network error: ${t.message}")
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resendVerificationCode() {
        val email = SharedPrefManager.getInstance(requireContext()).getEmail()
        if (email == null) {
            Toast.makeText(requireContext(), "Session expired. Please sign up again.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        Log.d(TAG, "Resending code to: $email")

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
                Log.e(TAG, "Network error: ${t.message}")
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection.",
                    Toast.LENGTH_SHORT).show()
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
        timer.cancel()
        _binding = null
    }
}