package com.autodocfill.app.domain.pdf

import com.autodocfill.app.data.model.ValidationResult

/**
 * Validates profile values before they are written into a PDF field.
 * Pure Kotlin so it can be unit tested without Android.
 */
object FieldValidator {

    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phonePattern = Regex("^\\+?\\d{7,15}$")
    private val postalCodePattern = Regex("^[A-Za-z0-9][A-Za-z0-9 -]{1,9}$")
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val idNumberPattern = Regex("^[A-Za-z0-9]{6,20}$")

    fun validate(profileKey: String, value: String): ValidationResult {
        if (value.isEmpty()) {
            return ValidationResult(isValid = true) // Empty is valid (optional field)
        }

        return when (profileKey) {
            "email", "workEmail" -> validateEmail(value)
            "phoneNumber", "alternatePhone", "workPhone", "emergencyContactPhone" -> validatePhone(value)
            "zipCode", "postalZipCode", "workZipCode" -> validatePostalCode(value)
            "dateOfBirth", "startDate" -> validateDate(value)
            "idNumber" -> validateIdNumber(value)
            else -> ValidationResult(isValid = true)
        }
    }

    private fun validateEmail(email: String): ValidationResult =
        if (emailPattern.matches(email.trim())) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid email format")
        }

    private fun validatePhone(phone: String): ValidationResult {
        val cleanPhone = phone.replace(Regex("[\\s().-]"), "")
        return if (phonePattern.matches(cleanPhone)) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                error = "Invalid phone number format",
                suggestions = listOf("Use format: +1234567890 or (123) 456-7890")
            )
        }
    }

    private fun validatePostalCode(code: String): ValidationResult =
        if (postalCodePattern.matches(code.trim())) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid ZIP/postal code format")
        }

    private fun validateDate(date: String): ValidationResult {
        if (!datePattern.matches(date)) {
            return ValidationResult(
                isValid = false,
                error = "Invalid date format",
                suggestions = listOf("Use format: YYYY-MM-DD")
            )
        }

        val (year, month, day) = date.split("-").map { it.toInt() }
        val daysInMonth = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        return if (year in 1900..2100 && month in 1..12 && day in 1..daysInMonth) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid date values")
        }
    }

    private fun validateIdNumber(idNumber: String): ValidationResult =
        if (idNumberPattern.matches(idNumber)) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid ID number format")
        }
}
