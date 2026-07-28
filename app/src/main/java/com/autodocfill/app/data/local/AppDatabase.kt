package com.autodocfill.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autodocfill.app.data.local.dao.*
import com.autodocfill.app.data.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Room database with SQLCipher encryption
 * All data stored locally is encrypted at rest
 */
@Database(
    entities = [
        Profile::class,
        Document::class,
        FieldMapping::class,
        AuditLog::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun profileDao(): ProfileDao
    abstract fun documentDao(): DocumentDao
    abstract fun fieldMappingDao(): FieldMappingDao
    abstract fun auditLogDao(): AuditLogDao
    
    companion object {
        private const val DATABASE_NAME = "autodocfill_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context, passphrase: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context, passphrase: String): AppDatabase {
            // Create SQLCipher factory with passphrase
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
            
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // For development; use migrations in production
                .build()
        }
        
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
