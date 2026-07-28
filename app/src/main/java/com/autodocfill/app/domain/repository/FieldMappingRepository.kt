package com.autodocfill.app.domain.repository

import com.autodocfill.app.data.local.dao.FieldMappingDao
import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.ConfidenceLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for FieldMapping operations
 */
@Singleton
class FieldMappingRepository @Inject constructor(
    private val fieldMappingDao: FieldMappingDao
) {
    
    fun getFieldMappingsByDocument(documentId: Long): Flow<List<FieldMapping>> {
        return fieldMappingDao.getFieldMappingsByDocument(documentId)
    }
    
    suspend fun getFieldMappingsByDocumentSync(documentId: Long): List<FieldMapping> {
        return fieldMappingDao.getFieldMappingsByDocumentSync(documentId)
    }
    
    suspend fun getFieldMappingById(mappingId: Long): FieldMapping? {
        return fieldMappingDao.getFieldMappingById(mappingId)
    }
    
    fun getFieldsNeedingReview(documentId: Long): Flow<List<FieldMapping>> {
        return fieldMappingDao.getFieldsNeedingReview(documentId)
    }
    
    suspend fun getUnfilledRequiredFields(documentId: Long): List<FieldMapping> {
        return fieldMappingDao.getUnfilledRequiredFields(documentId)
    }
    
    fun getFieldsByConfidence(documentId: Long, level: ConfidenceLevel): Flow<List<FieldMapping>> {
        return fieldMappingDao.getFieldsByConfidence(documentId, level)
    }
    
    suspend fun createFieldMapping(fieldMapping: FieldMapping): Long {
        return fieldMappingDao.insertFieldMapping(fieldMapping)
    }
    
    suspend fun createFieldMappings(fieldMappings: List<FieldMapping>) {
        fieldMappingDao.insertFieldMappings(fieldMappings)
    }
    
    suspend fun updateFieldMapping(fieldMapping: FieldMapping) {
        val updated = fieldMapping.copy(updatedAt = System.currentTimeMillis())
        fieldMappingDao.updateFieldMapping(updated)
    }
    
    suspend fun updateFieldValue(mappingId: Long, value: String) {
        fieldMappingDao.updateFieldValue(mappingId, value)
    }
    
    suspend fun markAsReviewed(mappingId: Long) {
        fieldMappingDao.markAsReviewed(mappingId)
    }
    
    suspend fun deleteFieldMappingsByDocument(documentId: Long) {
        fieldMappingDao.deleteFieldMappingsByDocument(documentId)
    }
    
    suspend fun getAutofilledFieldCount(documentId: Long): Int {
        return fieldMappingDao.getAutofilledFieldCount(documentId)
    }
}
