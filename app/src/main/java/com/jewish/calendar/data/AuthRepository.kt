package com.jewish.calendar.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jewish.calendar.model.UserModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser
    val isLoggedIn get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<UserModel> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = fetchUserProfile() ?: return Result.failure(Exception("פרופיל משתמש לא נמצא"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        gender: String
    ): Result<UserModel> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("שגיאה ביצירת המשתמש")

            val userModel = UserModel(uid = uid, displayName = displayName, email = email, gender = gender)
            db.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "gender" to gender
                )
            ).await()

            Result.success(userModel)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun fetchUserProfile(): UserModel? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserModel(
                    uid = uid,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun signOut() = auth.signOut()

    private fun mapAuthException(e: Exception): Exception {
        val msg = e.message ?: ""
        return when {
            msg.contains("INVALID_EMAIL") || msg.contains("invalid-email") ->
                Exception("כתובת האימייל אינה תקינה")
            msg.contains("WRONG_PASSWORD") || msg.contains("wrong-password") || msg.contains("invalid-credential") ->
                Exception("סיסמה שגויה")
            msg.contains("USER_NOT_FOUND") || msg.contains("user-not-found") ->
                Exception("משתמש לא קיים")
            msg.contains("EMAIL_ALREADY_IN_USE") || msg.contains("email-already-in-use") ->
                Exception("כתובת האימייל כבר בשימוש")
            msg.contains("WEAK_PASSWORD") || msg.contains("weak-password") ->
                Exception("הסיסמה חלשה מדי - לפחות 6 תווים")
            msg.contains("NETWORK_ERROR") || msg.contains("network") ->
                Exception("שגיאת רשת - בדוק חיבור לאינטרנט")
            else -> Exception("שגיאה: $msg")
        }
    }
}
