package com.autodocfill.app.domain.signature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signature manager for capturing and storing signatures
 */
@Singleton
class SignatureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val signatureDir = File(context.filesDir, "signatures")
    
    init {
        signatureDir.mkdirs()
    }
    
    /**
     * Save signature bitmap to file
     */
    suspend fun saveSignature(bitmap: Bitmap, filename: String = "signature_${System.currentTimeMillis()}.png"): String = withContext(Dispatchers.IO) {
        val file = File(signatureDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }
    
    /**
     * Load signature from file
     */
    suspend fun loadSignature(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            android.graphics.BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete signature file
     */
    suspend fun deleteSignature(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create blank signature bitmap
     */
    fun createBlankSignatureBitmap(width: Int = 800, height: Int = 400): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap
    }
}
