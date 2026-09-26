package com.autodocfill.app.domain.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.autodocfill.app.data.model.ConfidenceLevel
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
class OcrEngine @Inject constructor() {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Process scanned PDF and extract text using OCR
     */
    suspend fun processScannedPdf(pdfPath: String, documentId: Long): List<FieldMapping> = withContext(Dispatchers.IO) {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) {
            return@withContext emptyList()
        }

        val fieldMappings = mutableListOf<FieldMapping>()
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
                PdfRenderer(fileDescriptor).use { pdfRenderer ->
                    for (pageIndex in 0 until pdfRenderer.pageCount) {
                        val extractedText = pdfRenderer.openPage(pageIndex).use { page ->
                            // Render at 2x for better OCR accuracy, on a white background
                            // (PDF pages are transparent by default)
                            val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                            )
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                extractTextFromBitmap(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        fieldMappings.addAll(parseExtractedText(extractedText, documentId, pageIndex))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed for $pdfPath", e)
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
            Log.e(TAG, "Text recognition failed", e)
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
        // Only lines that look like a label waiting for a value, e.g. "First Name: ____"
        val labelPattern = Regex("^(.{2,40}?)\\s*[:_]+[\\s_.]*$")

        return text.lines()
            .map { it.trim() }
            .mapNotNull { line -> labelPattern.find(line)?.groupValues?.get(1)?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { label ->
                val profileKey = FieldMatcher.mapToProfileKey(label)
                if (profileKey == FieldMatcher.UNKNOWN_KEY) return@mapNotNull null
                FieldMapping(
                    documentId = documentId,
                    fieldName = label,
                    fieldType = if (profileKey == FieldMatcher.SIGNATURE_KEY) FieldType.SIGNATURE else FieldType.TEXT,
                    fieldPage = pageIndex,
                    profileKey = profileKey,
                    confidenceScore = 0.7f, // OCR has lower confidence
                    confidenceLevel = ConfidenceLevel.MEDIUM,
                    isSensitive = FieldMatcher.isSensitive(profileKey),
                    needsReview = true
                )
            }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        textRecognizer.close()
    }

    private companion object {
        const val TAG = "OcrEngine"
    }
}
