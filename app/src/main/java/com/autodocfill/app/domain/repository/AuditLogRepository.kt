package com.autodocfill.app.domain.repository

import com.autodocfill.app.data.local.dao.AuditLogDao
import com.autodocfill.app.data.model.AuditAction
import com.autodocfill.app.data.model.AuditLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AuditLog operations
 */
@Singleton
class AuditLogRepository @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    
    fun getRecentLogs(): Flow<List<AuditLog>> {
        return auditLogDao.getRecentLogs()
    }
    
    fun getLogsByDocument(documentId: Long): Flow<List<AuditLog>> {
        return auditLogDao.getLogsByDocument(documentId)
    }
    
    fun getLogsByAction(action: AuditAction): Flow<List<AuditLog>> {
        return auditLogDao.getLogsByAction(action)
    }
    
    fun getLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<AuditLog>> {
        return auditLogDao.getLogsByTimeRange(startTime, endTime)
    }
    
    suspend fun logAction(
        documentId: Long,
        action: AuditAction,
        description: String,
        profileId: Long? = null,
        fieldName: String? = null,
        oldValue: String? = null,
        newValue: String? = null,
        metadata: String? = null
    ): Long {
        val log = AuditLog(
            documentId = documentId,
            profileId = profileId,
            action = action,
            actionDescription = description,
            fieldName = fieldName,
            oldValue = oldValue,
            newValue = newValue,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        return auditLogDao.insertLog(log)
    }
    
    suspend fun getLogCount(): Int {
        return auditLogDao.getLogCount()
    }
    
    suspend fun deleteOldLogs(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        auditLogDao.deleteOldLogs(cutoffTime)
    }
}
