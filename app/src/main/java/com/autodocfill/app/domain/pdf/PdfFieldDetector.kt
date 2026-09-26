package com.autodocfill.app.domain.pdf

import android.util.Log
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfButtonFormField
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
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
class PdfFieldDetector @Inject constructor() {

    /**
     * Detect and extract all fillable fields from a PDF
     */
    suspend fun detectFields(pdfPath: String, documentId: Long): List<FieldMapping> = withContext(Dispatchers.IO) {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) {
            return@withContext emptyList()
        }

        try {
            PdfDocument(PdfReader(pdfFile)).use { pdfDocument ->
                val acroForm = PdfAcroForm.getAcroForm(pdfDocument, false)
                    ?: return@withContext emptyList()

                acroForm.formFields.mapNotNull { (fieldName, field) ->
                    extractFieldInfo(pdfDocument, fieldName, field, documentId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect fields in $pdfPath", e)
            emptyList()
        }
    }

    /**
     * Extract field information and create FieldMapping
     */
    private fun extractFieldInfo(
        pdfDocument: PdfDocument,
        fieldName: String,
        field: PdfFormField,
        documentId: Long
    ): FieldMapping? {
        return try {
            // Parent nodes in the field hierarchy have no type of their own; only fill terminal fields
            val fieldType = determineFieldType(field) ?: return null

            val profileKey = if (fieldType == FieldType.SIGNATURE) {
                FieldMatcher.SIGNATURE_KEY
            } else {
                FieldMatcher.mapToProfileKey(fieldName)
            }
            val confidence = FieldMatcher.calculateConfidence(fieldName, profileKey)
            val isSensitive = FieldMatcher.isSensitive(profileKey)
            val widget = field.widgets.firstOrNull()
            val pageNumber = widget?.page?.let { pdfDocument.getPageNumber(it) } ?: 1
            val fieldRect = widget?.rectangle?.toRectangle()?.let {
                """{"x":${it.x},"y":${it.y},"width":${it.width},"height":${it.height}}"""
            }

            FieldMapping(
                documentId = documentId,
                fieldName = fieldName,
                fieldType = fieldType,
                fieldPage = (pageNumber - 1).coerceAtLeast(0),
                fieldRect = fieldRect,
                profileKey = profileKey,
                confidenceScore = confidence,
                confidenceLevel = FieldMatcher.confidenceLevel(confidence),
                isRequired = field.getFieldFlag(PdfFormField.FF_REQUIRED),
                isReadOnly = field.getFieldFlag(PdfFormField.FF_READ_ONLY),
                isSensitive = isSensitive,
                needsReview = confidence < 0.9f || isSensitive
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping field $fieldName", e)
            null
        }
    }

    /**
     * Determine field type from the PDF field dictionary, or null for
     * non-terminal fields and push buttons that cannot hold a value.
     */
    private fun determineFieldType(field: PdfFormField): FieldType? {
        return when (field.formType) {
            PdfName.Sig -> FieldType.SIGNATURE
            PdfName.Ch -> FieldType.DROPDOWN
            PdfName.Btn -> when {
                field is PdfButtonFormField && field.isPushButton -> null
                field is PdfButtonFormField && field.isRadio -> FieldType.RADIO_BUTTON
                else -> FieldType.CHECKBOX
            }
            PdfName.Tx -> {
                val name = field.fieldName?.toUnicodeString() ?: ""
                if (name.contains("date", ignoreCase = true)) FieldType.DATE else FieldType.TEXT
            }
            else -> null
        }
    }

    private companion object {
        const val TAG = "PdfFieldDetector"
    }
}
