package com.autodocfill.app.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.autodocfill.app.data.model.FieldMapping
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR engine for scanned PDFs using ML Kit
 * Extracts text from non-fillable (scanned) PDF documents
 */
@Singleton
class OcrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Process scanned PDF and extract text using OCR
     */
    suspend fun processScannedPdf(pdfPath: String, documentId: Long): List<FieldMapping> = withContext(Dispatchers.IO) {
        val fieldMappings = mutableListOf<FieldMapping>()
        
        try {
            val pdfFile = File(pdfPath)
            if (!pdfFile.exists()) {
                return@withContext emptyList()
            }
            
            val parcelFileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            // Process each page
            for (pageIndex in 0 until pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                
                // Render page to bitmap
                val bitmap = Bitmap.createBitmap(
                    page.width * 2, // Higher resolution for better OCR
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Extract text using ML Kit
                val extractedText = extractTextFromBitmap(bitmap)
                
                // Parse extracted text to find potential fields
                val pageMappings = parseExtractedText(extractedText, documentId, pageIndex)
                fieldMappings.addAll(pageMappings)
                
                page.close()
                bitmap.recycle()
            }
            
            pdfRenderer.close()
            parcelFileDescriptor.close()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        fieldMappings
    }
    
    /**
     * Extract text from bitmap using ML Kit OCR
     */
    private suspend fun extractTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    /**
     * Parse extracted text to identify potential form fields
     * Looks for common patterns like "Name: _____" or "Email: _____"
     */
    private fun parseExtractedText(
        text: String,
        documentId: Long,
        pageIndex: Int
    ): List<FieldMapping> {
        val fieldMappings = mutableListOf<FieldMapping>()
        val lines = text.split("\n")
        
        // Common field patterns
        val fieldPatterns = mapOf(
            Regex("(?i)name\\s*[:_]?\\s*$") to "fullName",
            Regex("(?i)first\\s*name\\s*[:_]?\\s*$") to "firstName",
            Regex("(?i)last\\s*name\\s*[:_]?\\s*$") to "lastName",
            Regex("(?i)email\\s*[:_]?\\s*$") to "email",
            Regex("(?i)phone\\s*[:_]?\\s*$") to "phoneNumber",
            Regex("(?i)address\\s*[:_]?\\s*$") to "addressLine1",
            Regex("(?i)city\\s*[:_]?\\s*$") to "city",
            Regex("(?i)state\\s*[:_]?\\s*$") to "state",
            Regex("(?i)zip\\s*[:_]?\\s*$") to "zipCode",
            Regex("(?i)date\\s*of\\s*birth\\s*[:_]?\\s*$") to "dateOfBirth",
            Regex("(?i)signature\\s*[:_]?\\s*$") to "signaturePath"
        )
        
        lines.forEachIndexed { index, line ->
            fieldPatterns.forEach { (pattern, profileKey) ->
                if (pattern.containsMatchIn(line)) {
                    val fieldMapping = FieldMapping(
                        documentId = documentId,
                        fieldName = line.trim(),
                        fieldType = com.autodocfill.app.data.model.FieldType.TEXT,
                        fieldPage = pageIndex,
                        profileKey = profileKey,
                        confidenceScore = 0.7f, // OCR has lower confidence
                        confidenceLevel = com.autodocfill.app.data.model.ConfidenceLevel.MEDIUM,
                        needsReview = true
                    )
                    fieldMappings.add(fieldMapping)
                }
            }
        }
        
        return fieldMappings
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        textRecognizer.close()
    }
}
