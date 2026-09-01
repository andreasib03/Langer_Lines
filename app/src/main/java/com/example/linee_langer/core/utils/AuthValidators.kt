package com.example.linee_langer.core.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AuthValidators {
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    fun isEmailValid(email: String): Boolean =
        email.isNotBlank() && EMAIL_REGEX.matches(email)

    fun hasMinLength(password: String): Boolean = password.length >= 8
    fun hasUpperCase(password: String): Boolean = password.any {it.isUpperCase()}
    fun hasSpecialChar(password: String): Boolean = password.any {!it.isLetterOrDigit()}
    fun hasNumber(password: String): Boolean = password.any {it.isDigit()}

    fun isPasswordValid(password: String): Boolean =
        hasMinLength(password) && hasUpperCase(password) && hasSpecialChar(password) && hasNumber(password)

    fun validateBirthDate(birthDate: String): AgeValidationResult {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            val dob = sdf.parse(birthDate) ?: return AgeValidationResult.InvalidFormat
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = dob }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
            when {
                age < 13  -> AgeValidationResult.TooYoung
                age > 120 -> AgeValidationResult.TooOld
                else      -> AgeValidationResult.Valid(age)
            }
        } catch (e: ParseException) {
            AgeValidationResult.InvalidFormat
        }
    }
}

sealed class AgeValidationResult {
    object InvalidFormat : AgeValidationResult()
    object TooYoung : AgeValidationResult()
    object TooOld : AgeValidationResult()
    data class Valid(val age: Int) : AgeValidationResult()
}