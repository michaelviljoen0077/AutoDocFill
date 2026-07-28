package com.autodocfill.app.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.autodocfill.app.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val PREFS_NAME = "autodocfill_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    
    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): android.content.SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptedPrefs: android.content.SharedPreferences
    ): AppDatabase {
        // Get or create database passphrase
        val passphrase = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
            ?: generatePassphrase().also {
                encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, it).apply()
            }
        
        return AppDatabase.getInstance(context, passphrase)
    }
    
    @Provides
    fun provideProfileDao(database: AppDatabase) = database.profileDao()
    
    @Provides
    fun provideDocumentDao(database: AppDatabase) = database.documentDao()
    
    @Provides
    fun provideFieldMappingDao(database: AppDatabase) = database.fieldMappingDao()
    
    @Provides
    fun provideAuditLogDao(database: AppDatabase) = database.auditLogDao()
    
    private fun generatePassphrase(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..32)
            .map { chars.random() }
            .joinToString("")
    }
}
