package com.autodocfill.app.presentation.document

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodocfill.app.data.model.*
import com.autodocfill.app.domain.pdf.*
import com.autodocfill.app.domain.repository.*
import com.autodocfill.app.domain.signature.SignaturePlacer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Document processing
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val fieldMappingRepository: FieldMappingRepository,
    private val profileRepository: ProfileRepository,
    private val auditLogRepository: AuditLogRepository,
    private val pdfFieldDetector: PdfFieldDetector,
    private val autofillEngine: AutofillEngine,
    private val pdfProcessor: PdfProcessor,
    private val ocrEngine: OcrEngine,
    private val signaturePlacer: SignaturePlacer
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _events = Channel<DocumentEvent>(Channel.BUFFERED)
    val events: Flow<DocumentEvent> = _events.receiveAsFlow()

    val documents: StateFlow<List<Document>> = documentRepository.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val pdfDir get() = File(context.filesDir, "pdfs")
    private val exportDir get() = File(context.filesDir, "exports")

    fun getFieldMappings(documentId: Long): Flow<List<FieldMapping>> =
        fieldMappingRepository.getFieldMappingsByDocument(documentId)

    /**
     * Copy a PDF picked by the user into private storage and process it
     */
    fun importPdf(uri: Uri) {
        runTask("Importing PDF...", "Failed to import PDF") {
            val displayName = queryDisplayName(uri) ?: "document.pdf"
            val storedFile = withContext(Dispatchers.IO) {
                pdfDir.mkdirs()
                val file = File(pdfDir, "document_${System.currentTimeMillis()}.pdf")
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open the selected file")
                input.use { source -> file.outputStream().use { source.copyTo(it) } }
                file
            }
            processNewDocument(storedFile.absolutePath, displayName)
        }
    }

    private suspend fun processNewDocument(pdfPath: String, originalFileName: String) {
        val document = Document(
            documentName = originalFileName.removeSuffix(".pdf").removeSuffix(".PDF"),
            originalFileName = originalFileName,
            originalPdfPath = pdfPath,
            fileSizeBytes = pdfProcessor.getFileSize(pdfPath),
            status = DocumentStatus.PROCESSING
        )
        val documentId = documentRepository.createDocument(document)

        auditLogRepository.logAction(
            documentId = documentId,
            action = AuditAction.DOCUMENT_UPLOADED,
            description = "Document uploaded: $originalFileName"
        )

        analyzeDocument(document.copy(id = documentId))
    }

    /**
     * Detect fields in a stored document and save them. Sets the document to
     * READY_TO_FILL, or ERROR when nothing usable was found.
     */
    private suspend fun analyzeDocument(document: Document) {
        _uiState.update { it.copy(processingStep = "Detecting fields...") }

        try {
            val isFillable = pdfProcessor.isPdfFillable(document.originalPdfPath)
            val pageCount = pdfProcessor.getPageCount(document.originalPdfPath)

            if (!isFillable) {
                _uiState.update { it.copy(processingStep = "Running text recognition...") }
            }
            val fieldMappings = if (isFillable) {
                pdfFieldDetector.detectFields(document.originalPdfPath, document.id)
            } else {
                ocrEngine.processScannedPdf(document.originalPdfPath, document.id)
            }

            fieldMappingRepository.deleteFieldMappingsByDocument(document.id)
            fieldMappingRepository.createFieldMappings(fieldMappings)

            documentRepository.updateDocument(
                document.copy(
                    isFillable = isFillable,
                    requiresOcr = !isFillable,
                    pageCount = pageCount,
                    detectedFieldsCount = fieldMappings.size,
                    filledFieldsCount = 0,
                    requiredFieldsCount = fieldMappings.count { it.isRequired },
                    status = if (fieldMappings.isEmpty()) DocumentStatus.ERROR else DocumentStatus.READY_TO_FILL,
                    lastModifiedAt = System.currentTimeMillis()
                )
            )

            auditLogRepository.logAction(
                documentId = document.id,
                action = AuditAction.DOCUMENT_ANALYZED,
                description = "Detected ${fieldMappings.size} fields"
            )

            if (fieldMappings.isEmpty()) {
                showError("No form fields were detected in ${document.documentName}")
            } else {
                showSuccess("${fieldMappings.size} fields detected")
            }
        } catch (e: Exception) {
            documentRepository.updateDocumentStatus(document.id, DocumentStatus.ERROR)
            throw e
        }
    }

    fun retryProcessing(document: Document) {
        runTask("Analyzing PDF...", "Failed to process document") {
            documentRepository.updateDocumentStatus(document.id, DocumentStatus.PROCESSING)
            analyzeDocument(document)
        }
    }

    /**
     * Autofill document with the active profile
     */
    fun autofillDocument(document: Document) {
        runTask("Autofilling fields...", "Failed to autofill document") {
            val profile = profileRepository.getActiveProfile().first()
            if (profile == null) {
                showError("No active profile. Create a profile first, then set it as active.")
                return@runTask
            }

            val fieldMappings = fieldMappingRepository.getFieldMappingsByDocumentSync(document.id)
            if (fieldMappings.isEmpty()) {
                showError("This document has no fields to fill")
                return@runTask
            }

            val filledMappings = autofillEngine.autofillFields(fieldMappings, profile)
            filledMappings.forEach { fieldMappingRepository.updateFieldMapping(it) }

            val autofilledCount = filledMappings.count { it.isAutofilled }
            documentRepository.updateDocument(
                document.copy(
                    profileId = profile.id,
                    filledFieldsCount = filledMappings.count { it.finalValue.isNotBlank() },
                    status = DocumentStatus.FILLED,
                    lastModifiedAt = System.currentTimeMillis()
                )
            )

            auditLogRepository.logAction(
                documentId = document.id,
                profileId = profile.id,
                action = AuditAction.DOCUMENT_AUTOFILLED,
                description = "Autofilled $autofilledCount of ${fieldMappings.size} fields using ${profile.profileName}"
            )

            showSuccess("Autofilled $autofilledCount of ${fieldMappings.size} fields")
        }
    }

    /**
     * Save a manual edit to a single field
     */
    fun updateField(updated: FieldMapping) {
        viewModelScope.launch {
            try {
                val previous = fieldMappingRepository.getFieldMappingById(updated.id)
                fieldMappingRepository.updateFieldMapping(updated.copy(needsReview = false))

                if (previous?.finalValue != updated.finalValue) {
                    auditLogRepository.logAction(
                        documentId = updated.documentId,
                        action = AuditAction.FIELD_EDITED,
                        description = "Edited field ${updated.fieldName}",
                        fieldName = updated.fieldName,
                        // Values are personal data; don't copy sensitive ones into the log
                        oldValue = previous?.finalValue?.takeUnless { updated.isSensitive },
                        newValue = updated.finalValue.takeUnless { updated.isSensitive }
                    )
                }

                val filledCount = fieldMappingRepository.getFieldMappingsByDocumentSync(updated.documentId)
                    .count { it.finalValue.isNotBlank() }
                documentRepository.updateFilledFieldsCount(updated.documentId, filledCount)
            } catch (e: Exception) {
                showError("Failed to save field: ${e.message}")
            }
        }
    }

    /**
     * Write the field values into a copy of the PDF and offer to share it
     */
    fun exportDocument(document: Document) {
        runTask("Exporting PDF...", "Failed to export document") {
            val outputFile = writeFilledPdf(document) ?: return@runTask

            documentRepository.updateDocument(
                document.copy(
                    filledPdfPath = outputFile.absolutePath,
                    exportedPdfPath = outputFile.absolutePath,
                    status = DocumentStatus.EXPORTED,
                    exportedAt = System.currentTimeMillis(),
                    lastModifiedAt = System.currentTimeMillis()
                )
            )

            auditLogRepository.logAction(
                documentId = document.id,
                profileId = document.profileId,
                action = AuditAction.DOCUMENT_EXPORTED,
                description = "Exported ${outputFile.name}"
            )

            showSuccess("Document exported")
            _events.send(DocumentEvent.SharePdf(outputFile, document.documentName))
        }
    }

    /**
     * Fill the PDF, stamp the signature onto it and offer to share it
     */
    fun signDocument(document: Document, signature: Bitmap) {
        runTask("Signing PDF...", "Failed to sign document") {
            val filledFile = writeFilledPdf(document) ?: return@runTask
            val signedFile = File(exportDir, "${safeFileName(document.documentName)}_signed_${System.currentTimeMillis()}.pdf")

            val position = signaturePosition(document)
            val success = signaturePlacer.addSignatureToPdf(
                inputPdfPath = filledFile.absolutePath,
                outputPdfPath = signedFile.absolutePath,
                signatureBitmap = signature,
                pageNumber = position.page,
                x = position.x,
                y = position.y,
                width = position.width,
                height = position.height
            )
            withContext(Dispatchers.IO) { filledFile.delete() }
            if (!success) {
                showError("Failed to add signature to the PDF")
                return@runTask
            }

            documentRepository.updateDocument(
                document.copy(
                    exportedPdfPath = signedFile.absolutePath,
                    status = DocumentStatus.SIGNED,
                    exportedAt = System.currentTimeMillis(),
                    lastModifiedAt = System.currentTimeMillis()
                )
            )

            auditLogRepository.logAction(
                documentId = document.id,
                profileId = document.profileId,
                action = AuditAction.DOCUMENT_SIGNED,
                description = "Signed ${signedFile.name}"
            )

            showSuccess("Document signed")
            _events.send(DocumentEvent.SharePdf(signedFile, document.documentName))
        }
    }

    /**
     * Open the most recent output of a document (or the original) in a PDF viewer
     */
    fun viewDocument(document: Document) {
        viewModelScope.launch {
            val file = listOfNotNull(document.exportedPdfPath, document.originalPdfPath)
                .map { File(it) }
                .firstOrNull { it.exists() }
            if (file == null) {
                showError("PDF file is missing")
            } else {
                _events.send(DocumentEvent.OpenPdf(file))
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
                withContext(Dispatchers.IO) {
                    listOfNotNull(document.originalPdfPath, document.filledPdfPath, document.exportedPdfPath)
                        .forEach { File(it).delete() }
                }
                showSuccess("${document.documentName} deleted")
            } catch (e: Exception) {
                showError("Failed to delete document: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    private suspend fun writeFilledPdf(document: Document): File? {
        if (!document.isFillable) {
            showError("Scanned PDFs have no form fields to write into, so they can't be exported yet")
            return null
        }

        val outputFile = File(exportDir, "${safeFileName(document.documentName)}_filled_${System.currentTimeMillis()}.pdf")
        val fieldMappings = fieldMappingRepository.getFieldMappingsByDocumentSync(document.id)
        pdfProcessor.fillPdfFields(document.originalPdfPath, outputFile.absolutePath, fieldMappings)
            ?: throw IllegalStateException("Could not write the filled PDF")
        return outputFile
    }

    /**
     * Use the PDF's own signature field if one was detected, otherwise the bottom of the last page
     */
    private suspend fun signaturePosition(document: Document): SignatureBox {
        val signatureField = fieldMappingRepository.getFieldMappingsByDocumentSync(document.id)
            .firstOrNull { it.fieldType == FieldType.SIGNATURE && it.fieldRect != null }

        signatureField?.fieldRect?.let { rectJson ->
            runCatching {
                val rect = JSONObject(rectJson)
                return SignatureBox(
                    page = signatureField.fieldPage + 1,
                    x = rect.getDouble("x").toFloat(),
                    y = rect.getDouble("y").toFloat(),
                    width = rect.getDouble("width").toFloat(),
                    height = rect.getDouble("height").toFloat()
                )
            }
        }

        return SignatureBox(page = document.pageCount.coerceAtLeast(1), x = 72f, y = 72f, width = 200f, height = 80f)
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(60).ifEmpty { "document" }

    /**
     * Run a long operation with the processing overlay shown, reporting failures as an error message
     */
    private fun runTask(step: String, errorMessage: String, block: suspend () -> Unit) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingStep = step) }
            try {
                block()
            } catch (e: Exception) {
                showError(e.message ?: errorMessage)
            } finally {
                _uiState.update { it.copy(isProcessing = false, processingStep = null) }
            }
        }
    }

    private fun showSuccess(message: String) {
        _uiState.update { it.copy(successMessage = message, errorMessage = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }

    private data class SignatureBox(val page: Int, val x: Float, val y: Float, val width: Float, val height: Float)
}

/**
 * One-off actions the screen must perform (they need an Activity to start)
 */
sealed class DocumentEvent {
    data class OpenPdf(val file: File) : DocumentEvent()
    data class SharePdf(val file: File, val title: String) : DocumentEvent()
}

/**
 * UI state for Document screen
 */
data class DocumentUiState(
    val isProcessing: Boolean = false,
    val processingStep: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
