package com.example.traverse2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traverse2.data.api.TraverseApi
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.data.model.LoginRequest
import com.example.traverse2.data.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    
    private val api: TraverseApi = RetrofitClient.api
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    // TODO: Save token to DataStore
                    _uiState.value = AuthUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorMessage(errorBody) ?: "Login failed"
                    _uiState.value = AuthUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }
    
    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        
        if (username.length < 3 || username.length > 20) {
            _uiState.value = AuthUiState.Error("Username must be 3-20 characters")
            return
        }
        
        if (password.length < 8) {
            _uiState.value = AuthUiState.Error("Password must be at least 8 characters")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.register(RegisterRequest(username, email, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    // TODO: Save token to DataStore
                    _uiState.value = AuthUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorMessage(errorBody) ?: "Registration failed"
                    _uiState.value = AuthUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }
    
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
    
    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody == null) return null
        // Simple parsing - in production use proper JSON parsing
        return try {
            if (errorBody.contains("\"error\"")) {
                val start = errorBody.indexOf("\"error\":\"") + 9
                val end = errorBody.indexOf("\"", start)
                if (start > 8 && end > start) {
                    errorBody.substring(start, end)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
