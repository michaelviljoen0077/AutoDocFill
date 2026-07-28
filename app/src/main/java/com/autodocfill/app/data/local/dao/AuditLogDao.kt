package com.autodocfill.app.data.local.dao

import androidx.room.*
import com.autodocfill.app.data.model.AuditLog
import com.autodocfill.app.data.model.AuditAction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for AuditLog operations (append-only)
 */
@Dao
interface AuditLogDao {
    
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<AuditLog>>
    
    @Query("SELECT * FROM audit_logs WHERE documentId = :documentId ORDER BY timestamp DESC")
    fun getLogsByDocument(documentId: Long): Flow<List<AuditLog>>
    
    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC LIMIT 50")
    fun getLogsByAction(action: AuditAction): Flow<List<AuditLog>>
    
    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<AuditLog>>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: AuditLog): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLogs(logs: List<AuditLog>)
    
    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getLogCount(): Int
    
    @Query("DELETE FROM audit_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
    
    // No update or delete for individual logs (append-only design)
}
