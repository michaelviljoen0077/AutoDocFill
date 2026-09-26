package com.autodocfill.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodocfill.app.data.model.Profile
import com.autodocfill.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile management
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun createProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                profileRepository.createProfile(profile)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Profile created successfully"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to create profile"
                    ) 
                }
            }
        }
    }
    
    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                profileRepository.updateProfile(profile)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Profile updated successfully"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to update profile"
                    ) 
                }
            }
        }
    }
    
    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(profile)
                _uiState.update { 
                    it.copy(successMessage = "Profile deleted successfully") 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = e.message ?: "Failed to delete profile") 
                }
            }
        }
    }
    
    fun setActiveProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileRepository.setActiveProfile(profile.id)
                _uiState.update { 
                    it.copy(successMessage = "${profile.profileName} is now active") 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = e.message ?: "Failed to set active profile") 
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

/**
 * UI state for Profile screen
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
