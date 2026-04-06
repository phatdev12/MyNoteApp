package com.phatdev.noteapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FirebaseRepository(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    companion object {
        private const val TAG = "FirebaseRepository"
    }

    suspend fun register(email: String, password: String, fullName: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")
            
            val user = User(
                id = userId,
                email = email,
                fullName = fullName
            )
            firestore.collection("users").document(userId).set(user).await()
            
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            Log.d(TAG, "Attempting login for: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")
            Log.d(TAG, "Login successful for userId: $userId")
            Result.success(userId)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("network") == true -> "Lỗi kết nối mạng"
                e.message?.contains("password") == true -> "Sai mật khẩu"
                e.message?.contains("no user record") == true -> "Email chưa được đăng ký"
                e.message?.contains("invalid") == true -> "Email hoặc mật khẩu không hợp lệ"
                else -> e.message ?: "Đăng nhập thất bại"
            }
            Log.e(TAG, "Login failed: $errorMsg", e)
            Result.failure(Exception(errorMsg))
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
        }
    }

    fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return try {
            auth.currentUser != null
        } catch (e: Exception) {
            false
        }
    }


    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, fullName: String, profileImageUrl: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "fullName" to fullName,
                "profileImageUrl" to profileImageUrl
            )
            firestore.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile", e)
            Result.failure(e)
        }
    }

    suspend fun createNote(note: Note): Result<String> {
        return try {
            val noteData = hashMapOf(
                "title" to note.title,
                "description" to note.description,
                "userId" to note.userId,
                "imageUrl" to note.imageUrl,
                "thumbnailUrl" to note.thumbnailUrl,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            val docRef = firestore.collection("notes").add(noteData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create note", e)
            Result.failure(e)
        }
    }

    suspend fun updateNote(noteId: String, note: Note): Result<Unit> {
        return try {
            val noteData = hashMapOf(
                "title" to note.title,
                "description" to note.description,
                "userId" to note.userId,
                "imageUrl" to note.imageUrl,
                "thumbnailUrl" to note.thumbnailUrl,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("notes").document(noteId).update(noteData as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update note", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            firestore.collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete note", e)
            Result.failure(e)
        }
    }

    suspend fun getNoteById(noteId: String): Result<Note> {
        return try {
            val snapshot = firestore.collection("notes").document(noteId).get().await()
            val note = snapshot.toObject(Note::class.java)
            if (note != null) {
                note.id = snapshot.id
                Result.success(note)
            } else {
                Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get note", e)
            Result.failure(e)
        }
    }

    suspend fun getNotesByUserId(userId: String): Result<List<Note>> {
        return try {
            val snapshot = try {
                firestore.collection("notes")
                    .whereEqualTo("userId", userId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // Nếu lỗi index, fallback query không orderBy
                Log.w(TAG, "Query với orderBy thất bại, thử không orderBy: ${e.message}")
                firestore.collection("notes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
            }
            
            val notes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Note::class.java)?.apply {
                    id = doc.id
                }
            }.sortedByDescending { it.updatedAt }
            
            Result.success(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notes: ${e.message}", e)
            Result.failure(e)
        }
    }

}
