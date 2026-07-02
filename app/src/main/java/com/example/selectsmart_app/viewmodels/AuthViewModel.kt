package com.example.selectsmart_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.User
import com.example.selectsmart_app.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

// AuthViewModel handles the authentication logic between the UI and the repository.
// It is part of the MVVM architecture and connects Login/Register/Forgot Password screens
// to AuthRepository, which communicates with Firebase Authentication.
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    // Private MutableLiveData used inside the ViewModel to store the login result.
    // It stores either a successful FirebaseUser result or an error.
    private val _loginResult = MutableLiveData<Result<FirebaseUser?>>()
    val loginResult: LiveData<Result<FirebaseUser?>> = _loginResult

    private val _registerResult = MutableLiveData<Result<FirebaseUser?>>()
    val registerResult: LiveData<Result<FirebaseUser?>> = _registerResult

    private val _resetEmailResult = MutableLiveData<Result<Unit>?>()
    val resetEmailResult: LiveData<Result<Unit>?> = _resetEmailResult

    private val _confirmResetResult = MutableLiveData<Result<Unit>>()
    val confirmResetResult: LiveData<Result<Unit>> = _confirmResetResult

    // Function used to log in user
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            // Calls the login function from AuthRepository.
            val result = repository.login(email, pass)
            // Updates loginResult so the LoginFragment can react to success or failure.
            _loginResult.postValue(result)
        }
    }

    // Function used to register a new user
    fun register(user: User, pass: String) {
        viewModelScope.launch {
            // Calls the register function from AuthRepository.
            val result = repository.register(user, pass)
            // Updates registerResult so the RegisterFragment can show the correct result.
            _registerResult.postValue(result)
        }
    }

    // Function used to send a password reset email
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            _resetEmailResult.postValue(result)
        }
    }

    // Function used to reset the password
    fun resetState() {
        _resetEmailResult.value = null
    }

    // Function used to confirm the password reset
    fun confirmPasswordReset(code: String, newPass: String) {
        viewModelScope.launch {
            val result = repository.confirmPasswordReset(code, newPass)
            _confirmResetResult.postValue(result)
        }
    }

}
