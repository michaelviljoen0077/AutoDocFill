package com.autodocfill.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.autodocfill.app.data.local.Converters

/**
 * Represents a PDF document uploaded by the user
 */
@Entity(tableName = "documents")
@TypeConverters(Converters::class)
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Document identification
    val documentName: String,
    val originalFileName: String,
    
    // File paths (encrypted storage)
    val originalPdfPath: String, // Original uploaded PDF
    val filledPdfPath: String? = null, // Autofilled version
    val exportedPdfPath: String? = null, // Final exported version
    
    // Document type and status
    val documentType: DocumentType = DocumentType.OTHER,
    val status: DocumentStatus = DocumentStatus.UPLOADED,
    
    // PDF metadata
    val isFillable: Boolean = false, // AcroForm vs scanned
    val requiresOcr: Boolean = false,
    val pageCount: Int = 0,
    val fileSizeBytes: Long = 0,
    
    // Processing metadata
    val detectedFieldsCount: Int = 0,
    val filledFieldsCount: Int = 0,
    val requiredFieldsCount: Int = 0,
    
    // Associations
    val profileId: Long? = null, // Associated profile used for autofill
    
    // Timestamps
    val uploadedAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val exportedAt: Long? = null,
    
    // Version control
    val version: Int = 1,
    val previousVersionId: Long? = null
)

/**
 * Document types for categorization
 */
enum class DocumentType {
    JOB_APPLICATION,
    LEASE_AGREEMENT,
    INSURANCE_CLAIM,
    BANK_FORM,
    GOVERNMENT_FORM,
    MEDICAL_FORM,
    TAX_FORM,
    OTHER
}

/**
 * Document processing status
 */
enum class DocumentStatus {
    UPLOADED,       // Just uploaded
    PROCESSING,     // Analyzing fields
    READY_TO_FILL,  // Fields detected, ready for autofill
    FILLED,         // Autofilled, awaiting review
    REVIEWED,       // User reviewed changes
    SIGNED,         // Signature added
    EXPORTED,       // Final PDF exported
    ERROR           // Processing error
}
