package com.autodocfill.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.autodocfill.app.data.local.Converters

/**
 * User profile stored locally on device
 * Contains all personal information for autofilling PDF forms
 */
@Entity(tableName = "profiles")
@TypeConverters(Converters::class)
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Personal Information
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val fullName: String = "", // Computed or stored
    val dateOfBirth: String = "", // Format: YYYY-MM-DD
    val gender: String = "", // Male, Female, Other
    val nationality: String = "",
    
    // Contact Information
    val email: String = "",
    val phoneNumber: String = "",
    val cellphoneCountryCode: String = "", // +1, +44, etc.
    val alternatePhone: String = "",
    val alternatePhoneCountryCode: String = "",
    
    // Residential Address
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    
    // Postal/Mailing Address (if different)
    val postalAddressLine1: String = "",
    val postalAddressLine2: String = "",
    val postalCity: String = "",
    val postalState: String = "",
    val postalZipCode: String = "",
    val postalCountry: String = "",
    
    // Identification
    val idNumber: String = "", // National ID/SSN
    val passportNumber: String = "",
    val driverLicenseNumber: String = "",
    val taxNumber: String = "",
    
    // Employment Information
    val company: String = "",
    val jobTitle: String = "",
    val occupation: String = "",
    val employmentStatus: String = "", // Full-time, Part-time, Self-employed, Unemployed, Retired
    val workAddressLine1: String = "",
    val workAddressLine2: String = "",
    val workCity: String = "",
    val workState: String = "",
    val workZipCode: String = "",
    val workCountry: String = "",
    val workPhone: String = "",
    val workPhoneCountryCode: String = "",
    val workEmail: String = "",
    val annualIncome: String = "",
    val startDate: String = "", // Format: YYYY-MM-DD
    
    // Emergency Contact
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelation: String = "",
    
    // Custom Fields - User-defined fields for frequently used data
    // Stored as Map<String, String> where key is field name (e.g., "maiden_name") and value is the data
    val customFields: Map<String, String> = emptyMap(), // Examples: maiden_name, spouse_name, previous_address, etc.
    
    // Document Attachments (file paths, encrypted)
    val idPhotoPath: String? = null,
    val proofOfAddressPath: String? = null,
    val signaturePath: String? = null,
    
    // Field-level permissions (JSON string or separate table)
    val sensitiveFields: List<String> = emptyList(), // Fields requiring approval
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true, // Allow multiple profiles
    val profileName: String = "Default Profile" // User-friendly name
)

/**
 * Confidence levels for field matching
 */
enum class ConfidenceLevel {
    HIGH,    // Green - 90-100%
    MEDIUM,  // Yellow - 70-89%
    LOW      // Red - Below 70%
}

/**
 * Field types detected in PDFs
 */
enum class FieldType {
    TEXT,
    CHECKBOX,
    RADIO_BUTTON,
    SIGNATURE,
    DATE,
    DROPDOWN
}
