package com.autodocfill.app.domain.pdf

import android.content.Context
import com.autodocfill.app.data.model.FieldMapping
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF processor for filling and exporting PDF documents
 */
@Singleton
class PdfProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Fill PDF fields with values from field mappings
     */
    suspend fun fillPdfFields(
        originalPdfPath: String,
        outputPdfPath: String,
        fieldMappings: List<FieldMapping>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val originalFile = File(originalPdfPath)
            val outputFile = File(outputPdfPath)
            
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            val pdfReader = PdfReader(originalFile)
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfReader, pdfWriter)
            val acroForm = PdfAcroForm.getAcroForm(pdfDocument, true)
            
            if (acroForm != null) {
                // Fill each field
                fieldMappings.forEach { mapping ->
                    if (mapping.finalValue.isNotEmpty()) {
                        val field = acroForm.getField(mapping.fieldName)
                        if (field != null) {
                            fillField(field, mapping)
                        }
                    }
                }
                
                // Flatten form to prevent further editing (optional)
                // acroForm.flattenFields()
            }
            
            pdfDocument.close()
            true
            
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Fill individual field based on type
     */
    private fun fillField(field: PdfFormField, mapping: FieldMapping) {
        try {
            when (mapping.fieldType) {
                com.autodocfill.app.data.model.FieldType.TEXT,
                com.autodocfill.app.data.model.FieldType.DATE -> {
                    field.setValue(mapping.finalValue)
                }
                com.autodocfill.app.data.model.FieldType.CHECKBOX -> {
                    if (mapping.finalValue.equals("yes", ignoreCase = true) ||
                        mapping.finalValue.equals("true", ignoreCase = true)) {
                        field.setValue("Yes")
                    }
                }
                com.autodocfill.app.data.model.FieldType.RADIO_BUTTON -> {
                    field.setValue(mapping.finalValue)
                }
                else -> {
                    field.setValue(mapping.finalValue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if PDF is fillable (has AcroForm)
     */
    suspend fun isPdfFillable(pdfPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfFile = File(pdfPath)
            val pdfReader = PdfReader(pdfFile)
            val pdfDocument = PdfDocument(pdfReader)
            val acroForm = PdfAcroForm.getAcroForm(pdfDocument, false)
            val isFillable = acroForm != null && acroForm.formFields.isNotEmpty()
            pdfDocument.close()
            isFillable
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get page count from PDF
     */
    suspend fun getPageCount(pdfPath: String): Int = withContext(Dispatchers.IO) {
        try {
            val pdfFile = File(pdfPath)
            val pdfReader = PdfReader(pdfFile)
            val pdfDocument = PdfDocument(pdfReader)
            val count = pdfDocument.numberOfPages
            pdfDocument.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    /**
     * Get file size
     */
    suspend fun getFileSize(pdfPath: String): Long = withContext(Dispatchers.IO) {
        try {
            File(pdfPath).length()
        } catch (e: Exception) {
            0L
        }
    }
}
