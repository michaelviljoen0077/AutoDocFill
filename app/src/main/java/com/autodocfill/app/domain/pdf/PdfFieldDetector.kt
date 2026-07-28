package com.autodocfill.app.domain.pdf

import android.content.Context
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.autodocfill.app.data.model.ConfidenceLevel
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF field detection and extraction engine
 * Detects fillable fields from AcroForm PDFs
 */
@Singleton
class PdfFieldDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Detect and extract all fillable fields from a PDF
     */
    suspend fun detectFields(pdfPath: String, documentId: Long): List<FieldMapping> = withContext(Dispatchers.IO) {
        val fieldMappings = mutableListOf<FieldMapping>()
        
        try {
            val pdfFile = File(pdfPath)
            if (!pdfFile.exists()) {
                return@withContext emptyList()
            }
            
            val pdfReader = PdfReader(pdfFile)
            val pdfDocument = PdfDocument(pdfReader)
            val acroForm = PdfAcroForm.getAcroForm(pdfDocument, false)
            
            if (acroForm != null) {
                val fields = acroForm.formFields
                
                fields.forEach { (fieldName, field) ->
                    val fieldMapping = extractFieldInfo(fieldName, field, documentId)
                    if (fieldMapping != null) {
                        fieldMappings.add(fieldMapping)
                    }
                }
            }
            
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        fieldMappings
    }
    
    /**
     * Extract field information and create FieldMapping
     */
    private fun extractFieldInfo(
        fieldName: String,
        field: PdfFormField,
        documentId: Long
    ): FieldMapping? {
        try {
            val fieldType = determineFieldType(field)
            val profileKey = mapFieldNameToProfileKey(fieldName)
            val confidence = calculateConfidence(fieldName, profileKey)
            
            return FieldMapping(
                documentId = documentId,
                fieldName = fieldName,
                fieldType = fieldType,
                fieldPage = 0, // Will be determined later if needed
                profileKey = profileKey,
                confidenceScore = confidence,
                confidenceLevel = getConfidenceLevel(confidence),
                isRequired = isFieldRequired(field),
                isSensitive = isSensitiveField(profileKey),
                needsReview = confidence < 0.9f || isSensitiveField(profileKey)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Determine field type from PDF field
     */
    private fun determineFieldType(field: PdfFormField): FieldType {
        val fieldName = field.fieldName?.toString() ?: ""
        
        return when {
            fieldName.contains("signature", ignoreCase = true) -> FieldType.SIGNATURE
            fieldName.contains("date", ignoreCase = true) -> FieldType.DATE
            fieldName.contains("check", ignoreCase = true) -> FieldType.CHECKBOX
            else -> FieldType.TEXT
        }
    }
    
    /**
     * Map PDF field name to profile data key
     * Uses fuzzy matching and common patterns
     */
    private fun mapFieldNameToProfileKey(fieldName: String): String {
        val lowerFieldName = fieldName.lowercase()
        
        return when {
            // Name fields
            lowerFieldName.contains("first") && lowerFieldName.contains("name") -> "firstName"
            lowerFieldName.contains("middle") && lowerFieldName.contains("name") -> "middleName"
            lowerFieldName.contains("last") && lowerFieldName.contains("name") -> "lastName"
            lowerFieldName.contains("full") && lowerFieldName.contains("name") -> "fullName"
            lowerFieldName.matches(Regex(".*\\bname\\b.*")) -> "fullName"
            
            // Contact fields
            lowerFieldName.contains("email") || lowerFieldName.contains("e-mail") -> "email"
            lowerFieldName.contains("phone") || lowerFieldName.contains("telephone") -> "phoneNumber"
            lowerFieldName.contains("mobile") || lowerFieldName.contains("cell") -> "phoneNumber"
            
            // Address fields
            lowerFieldName.contains("address") && lowerFieldName.contains("1") -> "addressLine1"
            lowerFieldName.contains("address") && lowerFieldName.contains("2") -> "addressLine2"
            lowerFieldName.contains("address") -> "addressLine1"
            lowerFieldName.contains("city") -> "city"
            lowerFieldName.contains("state") || lowerFieldName.contains("province") -> "state"
            lowerFieldName.contains("zip") || lowerFieldName.contains("postal") -> "zipCode"
            lowerFieldName.contains("country") -> "country"
            
            // Identification fields
            lowerFieldName.contains("ssn") || lowerFieldName.contains("social") -> "idNumber"
            lowerFieldName.contains("passport") -> "passportNumber"
            lowerFieldName.contains("driver") || lowerFieldName.contains("license") -> "driverLicenseNumber"
            lowerFieldName.contains("tax") && lowerFieldName.contains("id") -> "taxNumber"
            
            // Date fields
            lowerFieldName.contains("birth") || lowerFieldName.contains("dob") -> "dateOfBirth"
            lowerFieldName.contains("date") -> "dateOfBirth"
            
            // Other
            lowerFieldName.contains("gender") || lowerFieldName.contains("sex") -> "gender"
            lowerFieldName.contains("employer") || lowerFieldName.contains("company") -> "employer"
            lowerFieldName.contains("occupation") || lowerFieldName.contains("job") -> "occupation"
            lowerFieldName.contains("signature") -> "signaturePath"
            
            else -> "unknown"
        }
    }
    
    /**
     * Calculate confidence score for field mapping
     */
    private fun calculateConfidence(fieldName: String, profileKey: String): Float {
        if (profileKey == "unknown") return 0.3f
        
        val lowerFieldName = fieldName.lowercase()
        val keyWords = profileKey.split(Regex("(?=[A-Z])")).map { it.lowercase() }
        
        var confidence = 0.5f
        
        // Exact keyword match
        keyWords.forEach { keyword ->
            if (lowerFieldName.contains(keyword)) {
                confidence += 0.2f
            }
        }
        
        // Position-based boost
        if (lowerFieldName.startsWith(keyWords.firstOrNull() ?: "")) {
            confidence += 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Get confidence level from score
     */
    private fun getConfidenceLevel(score: Float): ConfidenceLevel {
        return when {
            score >= 0.9f -> ConfidenceLevel.HIGH
            score >= 0.7f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
    
    /**
     * Check if field is required
     */
    private fun isFieldRequired(field: PdfFormField): Boolean {
        return field.getFieldFlag(PdfFormField.FF_REQUIRED)
    }
    
    /**
     * Check if profile key represents sensitive data
     */
    private fun isSensitiveField(profileKey: String): Boolean {
        val sensitiveKeys = setOf(
            "idNumber", "passportNumber", "driverLicenseNumber",
            "taxNumber", "dateOfBirth"
        )
        return profileKey in sensitiveKeys
    }
}
