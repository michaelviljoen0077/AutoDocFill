package com.autodocfill.app.domain.repository

import com.autodocfill.app.data.local.dao.DocumentDao
import com.autodocfill.app.data.model.Document
import com.autodocfill.app.data.model.DocumentStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Document operations
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {
    
    fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments()
    }
    
    fun getDocumentById(documentId: Long): Flow<Document?> {
        return documentDao.getDocumentById(documentId)
    }
    
    suspend fun getDocumentByIdSync(documentId: Long): Document? {
        return documentDao.getDocumentByIdSync(documentId)
    }
    
    fun getDocumentsByStatus(status: DocumentStatus): Flow<List<Document>> {
        return documentDao.getDocumentsByStatus(status)
    }
    
    fun getDocumentsByProfile(profileId: Long): Flow<List<Document>> {
        return documentDao.getDocumentsByProfile(profileId)
    }
    
    suspend fun createDocument(document: Document): Long {
        return documentDao.insertDocument(document)
    }
    
    suspend fun updateDocument(document: Document) {
        documentDao.updateDocument(document)
    }
    
    suspend fun updateDocumentStatus(documentId: Long, status: DocumentStatus) {
        documentDao.updateDocumentStatus(documentId, status)
    }
    
    suspend fun updateFilledFieldsCount(documentId: Long, count: Int) {
        documentDao.updateFilledFieldsCount(documentId, count)
    }
    
    suspend fun deleteDocument(document: Document) {
        documentDao.deleteDocument(document)
    }
    
    suspend fun deleteDocumentById(documentId: Long) {
        documentDao.deleteDocumentById(documentId)
    }
    
    suspend fun getDocumentCount(): Int {
        return documentDao.getDocumentCount()
    }
}
