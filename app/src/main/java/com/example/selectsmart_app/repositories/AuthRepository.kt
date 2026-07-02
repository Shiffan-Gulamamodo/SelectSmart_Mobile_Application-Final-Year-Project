package com.example.selectsmart_app.repositories

import com.example.selectsmart_app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// This repository class is used to handle Firebase Authentication operations
class AuthRepository {

    // Firebase Authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Firestore database instance
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Used for authenticating user login
    suspend fun login(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            // sign in user using email and password
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            // returns authenticated Firebase user
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Used for registering new users
    suspend fun register(user: User, pass: String): Result<FirebaseUser?> {
        return try {
            // creates user account using email and password
            val result = auth.createUserWithEmailAndPassword(user.userEmail, pass).await()
            val firebaseUser = result.user
            //Checks for user successful registration
            if (firebaseUser != null) {
                // Create user data map for Firestore
                val userData = hashMapOf(
                    "UserId" to firebaseUser.uid,
                    "UserEmail" to user.userEmail,
                    "UserFirstName" to user.userFirstName,
                    "UserLastName" to user.userLastName,
                    "UserCreatedAt" to FieldValue.serverTimestamp()
                )
                // stores user data inside Firestore Users collection
                firestore.collection("Users").document(firebaseUser.uid).set(userData).await()
                // returns registered Firebase user
                Result.success(firebaseUser)
            } else {
                // validation for failed registration
                Result.failure(Exception("Registration failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Used to send password reset email
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // sends Firebase password reset email
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Used for confirming password reset
    suspend fun confirmPasswordReset(code: String, newPass: String): Result<Unit> {
        return try {
            // Confirm password reset using verification code
            auth.confirmPasswordReset(code, newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // returns currently authenticated Firebase user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Logs out currently authenticated user
    fun logout() {
        auth.signOut()
    }
}
