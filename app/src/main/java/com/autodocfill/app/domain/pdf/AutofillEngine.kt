package com.autodocfill.app.domain.pdf

import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.autodocfill.app.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autofill engine with validation
 * Maps profile data to PDF fields with confidence scoring
 */
@Singleton
class AutofillEngine @Inject constructor() {
    
    /**
     * Autofill field mappings with profile data
     */
    suspend fun autofillFields(
        fieldMappings: List<FieldMapping>,
        profile: Profile
    ): List<FieldMapping> = withContext(Dispatchers.Default) {
        fieldMappings.map { mapping ->
            // Never overwrite values the user typed, and leave signatures to the signing flow
            if (mapping.isUserEdited || mapping.isReadOnly || mapping.fieldType == FieldType.SIGNATURE) {
                return@map mapping
            }

            val suggestedValue = getProfileValue(mapping.profileKey, profile)
            val validationResult = FieldValidator.validate(mapping.profileKey, suggestedValue)

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
    internal fun getProfileValue(profileKey: String, profile: Profile): String {
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
                listOf(profile.firstName, profile.middleName, profile.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
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
}
