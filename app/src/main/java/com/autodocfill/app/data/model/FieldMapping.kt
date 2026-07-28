package com.autodocfill.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps detected PDF fields to profile data keys with confidence scoring
 */
@Entity(
    tableName = "field_mappings",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class FieldMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Document association
    val documentId: Long,
    
    // PDF field information
    val fieldName: String, // Field name from PDF
    val fieldType: FieldType,
    val fieldPage: Int, // Page number (0-indexed)
    val fieldRect: String? = null, // JSON: {x, y, width, height}
    
    // Mapping to profile
    val profileKey: String, // Key in Profile data class (e.g., "firstName", "email")
    val suggestedValue: String = "", // Value suggested for this field
    val finalValue: String = "", // Value after user review/edit
    
    // Confidence and validation
    val confidenceScore: Float = 0f, // 0.0 to 1.0
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW,
    val isValidated: Boolean = false,
    val validationError: String? = null,
    
    // Field properties
    val isRequired: Boolean = false,
    val isReadOnly: Boolean = false,
    val isSensitive: Boolean = false, // Requires user approval
    
    // State
    val isAutofilled: Boolean = false,
    val isUserEdited: Boolean = false,
    val needsReview: Boolean = true,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Validation result for a field
 */
data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null,
    val suggestions: List<String> = emptyList()
)
