package com.autodocfill.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only audit log for tracking all document actions
 * Provides accountability and version history
 */
@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("documentId"), Index("profileId"), Index("timestamp")]
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Associations
    val documentId: Long,
    val profileId: Long? = null,
    
    // Action details
    val action: AuditAction,
    val actionDescription: String,
    
    // Field-level changes (for field edits)
    val fieldName: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    
    // User/system context
    val userId: String = "local_user", // For future multi-user support
    val deviceId: String = "",
    val appVersion: String = "",
    
    // Timestamp (immutable)
    val timestamp: Long = System.currentTimeMillis(),
    
    // Additional metadata (JSON)
    val metadata: String? = null
)

/**
 * Types of auditable actions
 */
enum class AuditAction {
    // Document actions
    DOCUMENT_UPLOADED,
    DOCUMENT_ANALYZED,
    DOCUMENT_AUTOFILLED,
    DOCUMENT_REVIEWED,
    DOCUMENT_SIGNED,
    DOCUMENT_EXPORTED,
    DOCUMENT_DELETED,
    
    // Field actions
    FIELD_DETECTED,
    FIELD_MAPPED,
    FIELD_AUTOFILLED,
    FIELD_EDITED,
    FIELD_VALIDATED,
    
    // Profile actions
    PROFILE_CREATED,
    PROFILE_UPDATED,
    PROFILE_DELETED,
    PROFILE_SELECTED,
    
    // Security actions
    BIOMETRIC_AUTH_SUCCESS,
    BIOMETRIC_AUTH_FAILED,
    SENSITIVE_FIELD_APPROVED,
    
    // System actions
    APP_OPENED,
    APP_CLOSED,
    ERROR_OCCURRED
}
