package com.autodocfill.app.data.local.dao

import androidx.room.*
import com.autodocfill.app.data.model.Profile
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Profile operations
 */
@Dao
interface ProfileDao {
    
    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<Profile>>
    
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    fun getProfileById(profileId: Long): Flow<Profile?>
    
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileByIdSync(profileId: Long): Profile?
    
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<Profile?>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: Profile): Long
    
    @Update
    suspend fun updateProfile(profile: Profile)
    
    @Delete
    suspend fun deleteProfile(profile: Profile)
    
    @Query("UPDATE profiles SET isActive = 0 WHERE id = :profileId")
    suspend fun deactivateProfile(profileId: Long)
    
    @Query("UPDATE profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun activateProfile(profileId: Long)
    
    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()
    
    @Query("SELECT COUNT(*) FROM profiles WHERE isActive = 1")
    suspend fun getActiveProfileCount(): Int

    /**
     * Make [profileId] the only active profile (the one used for autofill).
     */
    @Query("UPDATE profiles SET isActive = CASE WHEN id = :profileId THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(profileId: Long)
    
    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long)
}
