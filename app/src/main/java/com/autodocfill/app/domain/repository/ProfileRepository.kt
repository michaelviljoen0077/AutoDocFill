package com.autodocfill.app.domain.repository

import com.autodocfill.app.data.local.dao.ProfileDao
import com.autodocfill.app.data.model.Profile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Profile operations
 * Handles data access logic for user profiles
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    
    fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles()
    }
    
    fun getProfileById(profileId: Long): Flow<Profile?> {
        return profileDao.getProfileById(profileId)
    }
    
    suspend fun getProfileByIdSync(profileId: Long): Profile? {
        return profileDao.getProfileByIdSync(profileId)
    }
    
    fun getActiveProfile(): Flow<Profile?> {
        return profileDao.getActiveProfile()
    }
    
    suspend fun createProfile(profile: Profile): Long {
        val now = System.currentTimeMillis()
        val updatedProfile = profile.copy(
            createdAt = now,
            updatedAt = now,
            // The first profile becomes the active one; later ones must be activated explicitly
            isActive = profileDao.getActiveProfileCount() == 0
        )
        return profileDao.insertProfile(updatedProfile)
    }
    
    suspend fun updateProfile(profile: Profile) {
        val updatedProfile = profile.copy(
            updatedAt = System.currentTimeMillis()
        )
        profileDao.updateProfile(updatedProfile)
    }
    
    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }
    
    suspend fun activateProfile(profileId: Long) {
        profileDao.activateProfile(profileId)
    }
    
    suspend fun deactivateProfile(profileId: Long) {
        profileDao.deactivateProfile(profileId)
    }
    
    suspend fun setActiveProfile(profileId: Long) {
        profileDao.setActiveProfile(profileId)
    }
    
    suspend fun getActiveProfileCount(): Int {
        return profileDao.getActiveProfileCount()
    }
}
