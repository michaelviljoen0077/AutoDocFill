package com.autodocfill.app.domain.signature

import android.content.Context
import android.graphics.Bitmap
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Places signature and date on PDF documents
 */
@Singleton
class SignaturePlacer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Add signature and date to PDF
     */
    suspend fun addSignatureToPdf(
        inputPdfPath: String,
        outputPdfPath: String,
        signatureBitmap: Bitmap,
        pageNumber: Int = 1, // Page to sign (1-indexed)
        x: Float = 100f,
        y: Float = 100f,
        width: Float = 200f,
        height: Float = 100f
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputPdfPath)
            val outputFile = File(outputPdfPath)
            
            outputFile.parentFile?.mkdirs()
            
            val pdfReader = PdfReader(inputFile)
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfReader, pdfWriter)
            val document = Document(pdfDocument)
            
            // Convert bitmap to byte array
            val stream = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            
            // Create image from bitmap
            val imageData = ImageDataFactory.create(byteArray)
            val image = Image(imageData)
            image.setFixedPosition(pageNumber, x, y)
            image.scaleToFit(width, height)
            
            document.add(image)
            
            // Add date stamp below signature
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val dateString = dateFormat.format(Date())
            val dateParagraph = Paragraph("Date: $dateString")
            dateParagraph.setFixedPosition(pageNumber, x, y - 20f, width)
            dateParagraph.setFontSize(10f)
            
            document.add(dateParagraph)
            
            document.close()
            true
            
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Find signature field positions in PDF
     */
    suspend fun findSignaturePositions(pdfPath: String): List<SignaturePosition> = withContext(Dispatchers.IO) {
        val positions = mutableListOf<SignaturePosition>()
        
        try {
            val pdfFile = File(pdfPath)
            val pdfReader = PdfReader(pdfFile)
            val pdfDocument = PdfDocument(pdfReader)
            
            // In a real implementation, we would detect signature fields
            // For now, return a default position on the last page
            val lastPage = pdfDocument.numberOfPages
            positions.add(
                SignaturePosition(
                    page = lastPage,
                    x = 100f,
                    y = 100f,
                    width = 200f,
                    height = 100f
                )
            )
            
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        positions
    }
}

/**
 * Data class for signature position on PDF
 */
data class SignaturePosition(
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
