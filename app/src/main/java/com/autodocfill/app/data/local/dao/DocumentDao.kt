package com.autodocfill.app.data.local.dao

import androidx.room.*
import com.autodocfill.app.data.model.Document
import com.autodocfill.app.data.model.DocumentStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Document operations
 */
@Dao
interface DocumentDao {
    
    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>
    
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocumentById(documentId: Long): Flow<Document?>
    
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentByIdSync(documentId: Long): Document?
    
    @Query("SELECT * FROM documents WHERE status = :status ORDER BY uploadedAt DESC")
    fun getDocumentsByStatus(status: DocumentStatus): Flow<List<Document>>
    
    @Query("SELECT * FROM documents WHERE profileId = :profileId ORDER BY uploadedAt DESC")
    fun getDocumentsByProfile(profileId: Long): Flow<List<Document>>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(document: Document): Long
    
    @Update
    suspend fun updateDocument(document: Document)
    
    @Delete
    suspend fun deleteDocument(document: Document)
    
    @Query("UPDATE documents SET status = :status, lastModifiedAt = :timestamp WHERE id = :documentId")
    suspend fun updateDocumentStatus(documentId: Long, status: DocumentStatus, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE documents SET filledFieldsCount = :count, lastModifiedAt = :timestamp WHERE id = :documentId")
    suspend fun updateFilledFieldsCount(documentId: Long, count: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)
    
    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int
}
