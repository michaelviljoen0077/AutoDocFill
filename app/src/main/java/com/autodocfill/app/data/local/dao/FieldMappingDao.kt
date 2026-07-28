package com.autodocfill.app.data.local.dao

import androidx.room.*
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.ConfidenceLevel
import kotlinx.coroutines.flow.Flow

/**
 * DAO for FieldMapping operations
 */
@Dao
interface FieldMappingDao {
    
    @Query("SELECT * FROM field_mappings WHERE documentId = :documentId ORDER BY fieldPage, fieldName")
    fun getFieldMappingsByDocument(documentId: Long): Flow<List<FieldMapping>>
    
    @Query("SELECT * FROM field_mappings WHERE documentId = :documentId")
    suspend fun getFieldMappingsByDocumentSync(documentId: Long): List<FieldMapping>
    
    @Query("SELECT * FROM field_mappings WHERE id = :mappingId")
    suspend fun getFieldMappingById(mappingId: Long): FieldMapping?
    
    @Query("SELECT * FROM field_mappings WHERE documentId = :documentId AND needsReview = 1")
    fun getFieldsNeedingReview(documentId: Long): Flow<List<FieldMapping>>
    
    @Query("SELECT * FROM field_mappings WHERE documentId = :documentId AND isRequired = 1 AND finalValue = ''")
    suspend fun getUnfilledRequiredFields(documentId: Long): List<FieldMapping>
    
    @Query("SELECT * FROM field_mappings WHERE documentId = :documentId AND confidenceLevel = :level")
    fun getFieldsByConfidence(documentId: Long, level: ConfidenceLevel): Flow<List<FieldMapping>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldMapping(fieldMapping: FieldMapping): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldMappings(fieldMappings: List<FieldMapping>)
    
    @Update
    suspend fun updateFieldMapping(fieldMapping: FieldMapping)
    
    @Delete
    suspend fun deleteFieldMapping(fieldMapping: FieldMapping)
    
    @Query("UPDATE field_mappings SET finalValue = :value, isAutofilled = 1, updatedAt = :timestamp WHERE id = :mappingId")
    suspend fun updateFieldValue(mappingId: Long, value: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE field_mappings SET needsReview = 0 WHERE id = :mappingId")
    suspend fun markAsReviewed(mappingId: Long)
    
    @Query("DELETE FROM field_mappings WHERE documentId = :documentId")
    suspend fun deleteFieldMappingsByDocument(documentId: Long)
    
    @Query("SELECT COUNT(*) FROM field_mappings WHERE documentId = :documentId AND isAutofilled = 1")
    suspend fun getAutofilledFieldCount(documentId: Long): Int
}
