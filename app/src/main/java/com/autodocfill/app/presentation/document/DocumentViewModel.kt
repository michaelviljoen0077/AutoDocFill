package com.autodocfill.app.presentation.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodocfill.app.data.model.*
import com.autodocfill.app.domain.pdf.*
import com.autodocfill.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Document processing
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val fieldMappingRepository: FieldMappingRepository,
    private val profileRepository: ProfileRepository,
    private val auditLogRepository: AuditLogRepository,
    private val pdfFieldDetector: PdfFieldDetector,
    private val autofillEngine: AutofillEngine,
    private val pdfProcessor: PdfProcessor,
    private val ocrEngine: OcrEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()
    
    val documents: StateFlow<List<Document>> = documentRepository.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Process uploaded PDF document
     */
    fun processDocument(pdfPath: String, documentName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, processingStep = "Analyzing PDF...") }
                
                // Check if fillable
                val isFillable = pdfProcessor.isPdfFillable(pdfPath)
                val pageCount = pdfProcessor.getPageCount(pdfPath)
                val fileSize = pdfProcessor.getFileSize(pdfPath)
                
                // Create document record
                val document = Document(
                    documentName = documentName,
                    originalFileName = documentName,
                    originalPdfPath = pdfPath,
                    isFillable = isFillable,
                    requiresOcr = !isFillable,
                    pageCount = pageCount,
                    fileSizeBytes = fileSize,
                    status = DocumentStatus.PROCESSING
                )
                
                val documentId = documentRepository.createDocument(document)
                
                // Log upload
                auditLogRepository.logAction(
                    documentId = documentId,
                    action = AuditAction.DOCUMENT_UPLOADED,
                    description = "Document uploaded: $documentName"
                )
                
                // Detect fields
                _uiState.update { it.copy(processingStep = "Detecting fields...") }
                
                val fieldMappings = if (isFillable) {
                    pdfFieldDetector.detectFields(pdfPath, documentId)
                } else {
                    ocrEngine.processScannedPdf(pdfPath, documentId)
                }
                
                if (fieldMappings.isNotEmpty()) {
                    fieldMappingRepository.createFieldMappings(fieldMappings)
                    
                    documentRepository.updateDocument(
                        document.copy(
                            id = documentId,
                            detectedFieldsCount = fieldMappings.size,
                            status = DocumentStatus.READY_TO_FILL
                        )
                    )
                    
                    auditLogRepository.logAction(
                        documentId = documentId,
                        action = AuditAction.DOCUMENT_ANALYZED,
                        description = "Detected ${fieldMappings.size} fields"
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        currentDocumentId = documentId,
                        successMessage = "Document processed: ${fieldMappings.size} fields detected"
                    ) 
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        errorMessage = e.message ?: "Failed to process document"
                    ) 
                }
            }
        }
    }
    
    /**
     * Autofill document with profile data
     */
    fun autofillDocument(documentId: Long, profileId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, processingStep = "Autofilling fields...") }
                
                val profile = profileRepository.getProfileByIdSync(profileId)
                val fieldMappings = fieldMappingRepository.getFieldMappingsByDocumentSync(documentId)
                
                if (profile != null && fieldMappings.isNotEmpty()) {
                    val filledMappings = autofillEngine.autofillFields(fieldMappings, profile)
                    
                    filledMappings.forEach { mapping ->
                        fieldMappingRepository.updateFieldMapping(mapping)
                    }
                    
                    val filledCount = filledMappings.count { it.isAutofilled }
                    documentRepository.updateFilledFieldsCount(documentId, filledCount)
                    documentRepository.updateDocumentStatus(documentId, DocumentStatus.FILLED)
                    
                    auditLogRepository.logAction(
                        documentId = documentId,
                        profileId = profileId,
                        action = AuditAction.DOCUMENT_AUTOFILLED,
                        description = "Autofilled $filledCount of ${fieldMappings.size} fields"
                    )
                    
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            processingStep = null,
                            successMessage = "Autofilled $filledCount fields"
                        ) 
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        errorMessage = e.message ?: "Failed to autofill document"
                    ) 
                }
            }
        }
    }
    
    /**
     * Export filled PDF
     */
    fun exportDocument(documentId: Long, outputPath: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, processingStep = "Exporting PDF...") }
                
                val document = documentRepository.getDocumentByIdSync(documentId)
                val fieldMappings = fieldMappingRepository.getFieldMappingsByDocumentSync(documentId)
                
                if (document != null) {
                    val success = pdfProcessor.fillPdfFields(
                        document.originalPdfPath,
                        outputPath,
                        fieldMappings
                    )
                    
                    if (success) {
                        documentRepository.updateDocument(
                            document.copy(
                                exportedPdfPath = outputPath,
                                status = DocumentStatus.EXPORTED,
                                exportedAt = System.currentTimeMillis()
                            )
                        )
                        
                        auditLogRepository.logAction(
                            documentId = documentId,
                            action = AuditAction.DOCUMENT_EXPORTED,
                            description = "PDF exported to $outputPath"
                        )
                        
                        _uiState.update { 
                            it.copy(
                                isProcessing = false,
                                processingStep = null,
                                successMessage = "Document exported successfully"
                            ) 
                        }
                    } else {
                        throw Exception("Failed to export PDF")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        errorMessage = e.message ?: "Failed to export document"
                    ) 
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
    
    fun viewDocument(document: Document) {
        // Open PDF viewer - will implement when PDF viewer screen is ready
        _uiState.update { 
            it.copy(successMessage = "Opening ${document.documentName}...") 
        }
    }
    
    fun autofillDocument(document: Document) {
        viewModelScope.launch {
            val activeProfile = profileRepository.getActiveProfile().firstOrNull()
            if (activeProfile != null) {
                autofillDocument(document.id, activeProfile.id)
            } else {
                _uiState.update { 
                    it.copy(errorMessage = "No active profile. Please create and activate a profile first.") 
                }
            }
        }
    }
    
    fun exportDocument(document: Document) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val outputPath = document.originalPdfPath.replace(".pdf", "_filled_$timestamp.pdf")
            exportDocument(document.id, outputPath)
        }
    }
    
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
                _uiState.update { 
                    it.copy(successMessage = "${document.documentName} deleted") 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to delete document: ${e.message}") 
                }
            }
        }
    }
    
    fun retryProcessing(document: Document) {
        processDocument(document.originalPdfPath, document.documentName)
    }
}

/**
 * UI state for Document screen
 */
data class DocumentUiState(
    val isProcessing: Boolean = false,
    val processingStep: String? = null,
    val currentDocumentId: Long? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
