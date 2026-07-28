package com.autodocfill.app.domain.pdf

import android.content.Context
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.Profile
import com.autodocfill.app.data.model.ValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autofill engine with validation
 * Maps profile data to PDF fields with confidence scoring
 */
@Singleton
class AutofillEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Autofill field mappings with profile data
     */
    suspend fun autofillFields(
        fieldMappings: List<FieldMapping>,
        profile: Profile
    ): List<FieldMapping> = withContext(Dispatchers.Default) {
        fieldMappings.map { mapping ->
            val suggestedValue = getProfileValue(mapping.profileKey, profile)
            val validationResult = validateFieldValue(mapping.profileKey, suggestedValue)
            
            mapping.copy(
                suggestedValue = suggestedValue,
                finalValue = if (validationResult.isValid) suggestedValue else "",
                isValidated = validationResult.isValid,
                validationError = validationResult.error,
                isAutofilled = validationResult.isValid && suggestedValue.isNotEmpty(),
                needsReview = !validationResult.isValid || mapping.isSensitive
            )
        }
    }
    
    /**
     * Get value from profile by key
     */
    private fun getProfileValue(profileKey: String, profile: Profile): String {
        // First check if it's a custom field
        if (profile.customFields.containsKey(profileKey)) {
            return profile.customFields[profileKey] ?: ""
        }
        
        // Then check standard fields
        return when (profileKey) {
            "firstName" -> profile.firstName
            "middleName" -> profile.middleName
            "lastName" -> profile.lastName
            "fullName" -> profile.fullName.ifEmpty { 
                "${profile.firstName} ${profile.middleName} ${profile.lastName}".trim() 
            }
            "dateOfBirth" -> profile.dateOfBirth
            "gender" -> profile.gender
            "nationality" -> profile.nationality
            "email" -> profile.email
            "phoneNumber" -> profile.phoneNumber
            "cellphoneCountryCode" -> profile.cellphoneCountryCode
            "alternatePhone" -> profile.alternatePhone
            "alternatePhoneCountryCode" -> profile.alternatePhoneCountryCode
            "addressLine1" -> profile.addressLine1
            "addressLine2" -> profile.addressLine2
            "city" -> profile.city
            "state" -> profile.state
            "zipCode" -> profile.zipCode
            "country" -> profile.country
            "postalAddressLine1" -> profile.postalAddressLine1
            "postalAddressLine2" -> profile.postalAddressLine2
            "postalCity" -> profile.postalCity
            "postalState" -> profile.postalState
            "postalZipCode" -> profile.postalZipCode
            "postalCountry" -> profile.postalCountry
            "idNumber" -> profile.idNumber
            "passportNumber" -> profile.passportNumber
            "driverLicenseNumber" -> profile.driverLicenseNumber
            "taxNumber" -> profile.taxNumber
            "company" -> profile.company
            "jobTitle" -> profile.jobTitle
            "occupation" -> profile.occupation
            "employmentStatus" -> profile.employmentStatus
            "workAddressLine1" -> profile.workAddressLine1
            "workAddressLine2" -> profile.workAddressLine2
            "workCity" -> profile.workCity
            "workState" -> profile.workState
            "workZipCode" -> profile.workZipCode
            "workCountry" -> profile.workCountry
            "workPhone" -> profile.workPhone
            "workPhoneCountryCode" -> profile.workPhoneCountryCode
            "workEmail" -> profile.workEmail
            "annualIncome" -> profile.annualIncome
            "startDate" -> profile.startDate
            "emergencyContactName" -> profile.emergencyContactName
            "emergencyContactPhone" -> profile.emergencyContactPhone
            "emergencyContactRelation" -> profile.emergencyContactRelation
            else -> ""
        }
    }
    
    /**
     * Validate field value based on type
     */
    private fun validateFieldValue(profileKey: String, value: String): ValidationResult {
        if (value.isEmpty()) {
            return ValidationResult(isValid = true) // Empty is valid (optional field)
        }
        
        return when (profileKey) {
            "email" -> validateEmail(value)
            "phoneNumber", "alternatePhone", "emergencyContactPhone" -> validatePhone(value)
            "zipCode" -> validateZipCode(value)
            "dateOfBirth" -> validateDate(value)
            "idNumber" -> validateIdNumber(value)
            else -> ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate email format
     */
    private fun validateEmail(email: String): ValidationResult {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return if (emailPattern.matcher(email).matches()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid email format")
        }
    }
    
    /**
     * Validate phone number format
     */
    private fun validatePhone(phone: String): ValidationResult {
        // Remove common formatting characters
        val cleanPhone = phone.replace(Regex("[\\s().-]"), "")
        
        return if (cleanPhone.matches(Regex("^\\+?\\d{10,15}$"))) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                error = "Invalid phone number format",
                suggestions = listOf("Use format: +1234567890 or (123) 456-7890")
            )
        }
    }
    
    /**
     * Validate ZIP/postal code
     */
    private fun validateZipCode(zipCode: String): ValidationResult {
        // US ZIP code: 12345 or 12345-6789
        val usZipPattern = Regex("^\\d{5}(-\\d{4})?$")
        
        return if (zipCode.matches(usZipPattern) || zipCode.matches(Regex("^[A-Z0-9]{3,10}$"))) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid ZIP/postal code format")
        }
    }
    
    /**
     * Validate date format (YYYY-MM-DD)
     */
    private fun validateDate(date: String): ValidationResult {
        val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        
        return if (date.matches(datePattern)) {
            // Additional validation for valid date ranges
            val parts = date.split("-")
            val year = parts[0].toIntOrNull() ?: 0
            val month = parts[1].toIntOrNull() ?: 0
            val day = parts[2].toIntOrNull() ?: 0
            
            if (year in 1900..2100 && month in 1..12 && day in 1..31) {
                ValidationResult(isValid = true)
            } else {
                ValidationResult(isValid = false, error = "Invalid date values")
            }
        } else {
            ValidationResult(
                isValid = false,
                error = "Invalid date format",
                suggestions = listOf("Use format: YYYY-MM-DD")
            )
        }
    }
    
    /**
     * Validate ID number (basic check)
     */
    private fun validateIdNumber(idNumber: String): ValidationResult {
        // Basic validation - alphanumeric, 6-20 characters
        return if (idNumber.matches(Regex("^[A-Za-z0-9]{6,20}$"))) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, error = "Invalid ID number format")
        }
    }
}
