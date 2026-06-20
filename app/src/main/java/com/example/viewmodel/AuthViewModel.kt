package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.UserEntity
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val user: UserEntity) : AuthState
    data class VerificationRequired(val email: String) : AuthState
    data class VerificationSuccess(val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(private val repository: MusicRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _simulatedOtp = MutableStateFlow<String?>(null)
    val simulatedOtp: StateFlow<String?> = _simulatedOtp.asStateFlow()

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { error ->
                    val message = error.localizedMessage ?: "Unknown Error occurred"
                    if (message.contains("email_not_verified")) {
                        _authState.value = AuthState.VerificationRequired(email)
                    } else {
                        _authState.value = AuthState.Error(message)
                    }
                }
        }
    }

    fun signup(name: String, username: String, email: String, image: String, password: String, role: String = "User") {
        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill all required fields")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signup(name, username, email, image, role, password)
                .onSuccess { user ->
                    _authState.value = AuthState.VerificationRequired(user.email)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Sign up failed")
                }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.verifyOtp(email, otp)
                .onSuccess {
                    _authState.value = AuthState.VerificationSuccess(email)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Verification failed")
                }
        }
    }

    fun loadSimulatedOtp(email: String) {
        viewModelScope.launch {
            _simulatedOtp.value = repository.getSimulatedOtp(email)
        }
    }

    fun loginWithGoogle(email: String, name: String, image: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.loginWithGoogle(email, name, image)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Google Sign-In failed")
                }
        }
    }

    fun updateProfile(name: String, username: String, image: String) {
        viewModelScope.launch {
            repository.updateProfile(name, username, image)
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    class Factory(private val repository: MusicRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
