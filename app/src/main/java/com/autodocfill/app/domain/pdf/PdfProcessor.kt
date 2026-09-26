package com.autodocfill.app.domain.pdf

import android.util.Log
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF processor for filling and exporting PDF documents
 */
@Singleton
class PdfProcessor @Inject constructor() {

    /**
     * Fill PDF fields with values from field mappings and write the result to [outputPdfPath].
     *
     * @return the number of fields written, or null if the PDF could not be written
     */
    suspend fun fillPdfFields(
        originalPdfPath: String,
        outputPdfPath: String,
        fieldMappings: List<FieldMapping>
    ): Int? = withContext(Dispatchers.IO) {
        val originalFile = File(originalPdfPath)
        val outputFile = File(outputPdfPath)
        require(originalFile.canonicalPath != outputFile.canonicalPath) {
            "Output path must differ from the original PDF"
        }
        outputFile.parentFile?.mkdirs()

        try {
            var filledCount = 0
            PdfDocument(PdfReader(originalFile), PdfWriter(outputFile)).use { pdfDocument ->
                val acroForm = PdfAcroForm.getAcroForm(pdfDocument, false)
                if (acroForm != null) {
                    fieldMappings
                        .filter { it.finalValue.isNotEmpty() && !it.isReadOnly }
                        .forEach { mapping ->
                            val field = acroForm.getField(mapping.fieldName)
                            if (field != null && fillField(field, mapping)) {
                                filledCount++
                            }
                        }
                }
            }
            filledCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fill $originalPdfPath", e)
            outputFile.delete()
            null
        }
    }

    /**
     * Fill individual field based on type
     */
    private fun fillField(field: PdfFormField, mapping: FieldMapping): Boolean {
        return try {
            when (mapping.fieldType) {
                FieldType.CHECKBOX -> {
                    val checked = mapping.finalValue.lowercase() in setOf("yes", "true", "1", "on", "x")
                    // A checkbox's "on" value is whatever appearance state the form defines (often "Yes" or "On")
                    val onState = field.appearanceStates.firstOrNull { it != "Off" } ?: "Yes"
                    field.setValue(if (checked) onState else "Off")
                }
                FieldType.SIGNATURE -> return false // Signatures are drawn by SignaturePlacer
                else -> field.setValue(mapping.finalValue)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not fill field ${mapping.fieldName}", e)
            false
        }
    }

    /**
     * Check if PDF is fillable (has AcroForm fields)
     */
    suspend fun isPdfFillable(pdfPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            PdfDocument(PdfReader(File(pdfPath))).use { pdfDocument ->
                val acroForm = PdfAcroForm.getAcroForm(pdfDocument, false)
                acroForm != null && acroForm.formFields.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read $pdfPath", e)
            false
        }
    }

    /**
     * Get page count from PDF
     */
    suspend fun getPageCount(pdfPath: String): Int = withContext(Dispatchers.IO) {
        try {
            PdfDocument(PdfReader(File(pdfPath))).use { it.numberOfPages }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read $pdfPath", e)
            0
        }
    }

    /**
     * Get file size
     */
    suspend fun getFileSize(pdfPath: String): Long = withContext(Dispatchers.IO) {
        File(pdfPath).length()
    }

    private companion object {
        const val TAG = "PdfProcessor"
    }
}
