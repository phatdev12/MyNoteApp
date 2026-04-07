package com.phatdev.noteapp.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.data.repository.CloudinaryRepository
import com.phatdev.noteapp.data.repository.FirebaseRepository
import com.phatdev.noteapp.ui.compose.screens.*
import com.phatdev.noteapp.ui.compose.theme.NoteAppTheme
import kotlinx.coroutines.launch

class ComposeMainActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var cloudinaryRepository: CloudinaryRepository

    private val refreshTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(this)
        cloudinaryRepository = CloudinaryRepository(this)

        setContent {
            NoteAppTheme {
                var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
                var userName by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(true) }
                val refreshKey by refreshTrigger

                LaunchedEffect(refreshKey) {
                    loadData { loadedNotes, loadedName ->
                        notes = loadedNotes
                        userName = loadedName
                        isLoading = false
                    }
                }

                HomeScreen(
                    notes = notes,
                    userName = userName,
                    isLoading = isLoading,
                    onNoteClick = { note ->
                        startActivity(Intent(this@ComposeMainActivity, ComposeNoteDetailActivity::class.java).apply {
                            putExtra("note_id", note.id)
                        })
                    },
                    onAddNoteClick = {
                        startActivity(Intent(this@ComposeMainActivity, ComposeNoteDetailActivity::class.java))
                    },
                    onLogoutClick = {
                        repository.logout()
                        startActivity(Intent(this@ComposeMainActivity, ComposeLoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger refresh khi quay lại activity
        refreshTrigger.intValue++
    }

    private fun loadData(onLoaded: (List<Note>, String) -> Unit) {
        val userId = repository.getCurrentUserId()
        if (userId == null) {
            startActivity(Intent(this, ComposeLoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            var loadedNotes = emptyList<Note>()
            var loadedName = ""

            repository.getNotesByUserId(userId).onSuccess { notes ->
                loadedNotes = notes
            }

            repository.getUserProfile(userId).onSuccess { user ->
                loadedName = user.fullName
            }

            onLoaded(loadedNotes, loadedName)
        }
    }
}

class ComposeLoginActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(this)

        if (repository.getCurrentUserId() != null) {
            startActivity(Intent(this, ComposeMainActivity::class.java))
            finish()
            return
        }

        setContent {
            NoteAppTheme {
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LoginScreen(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogin = { email, password ->
                        isLoading = true
                        errorMessage = null
                        lifecycleScope.launch {
                            repository.login(email, password)
                                .onSuccess {
                                    startActivity(Intent(this@ComposeLoginActivity, ComposeMainActivity::class.java))
                                    finish()
                                }
                                .onFailure { e ->
                                    errorMessage = e.message ?: "Đăng nhập thất bại"
                                    isLoading = false
                                }
                        }
                    },
                    onNavigateToRegister = {
                        startActivity(Intent(this@ComposeLoginActivity, ComposeRegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

class ComposeRegisterActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(this)

        setContent {
            NoteAppTheme {
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                RegisterScreen(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onRegister = { email, password, fullName ->
                        isLoading = true
                        errorMessage = null
                        lifecycleScope.launch {
                            repository.register(email, password, fullName)
                                .onSuccess {
                                    Toast.makeText(this@ComposeRegisterActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@ComposeRegisterActivity, ComposeMainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                    finish()
                                }
                                .onFailure { e ->
                                    errorMessage = e.message ?: "Đăng ký thất bại"
                                    isLoading = false
                                }
                        }
                    },
                    onNavigateToLogin = { finish() }
                )
            }
        }
    }
}

class ComposeNoteDetailActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var cloudinaryRepository: CloudinaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(this)
        cloudinaryRepository = CloudinaryRepository(this)

        val noteId = intent.getStringExtra("note_id")

        setContent {
            NoteAppTheme {
                var note by remember { mutableStateOf<Note?>(null) }
                var isLoading by remember { mutableStateOf(noteId != null) }
                var isSaving by remember { mutableStateOf(false) }

                LaunchedEffect(noteId) {
                    noteId?.let { id ->
                        repository.getNoteById(id).onSuccess { loadedNote ->
                            note = loadedNote
                            isLoading = false
                        }.onFailure {
                            Toast.makeText(this@ComposeNoteDetailActivity, "Không tìm thấy ghi chú", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }

                NoteDetailScreen(
                    noteId = noteId,
                    initialTitle = note?.title ?: "",
                    initialDescription = note?.description ?: "",
                    initialImageUrl = note?.imageUrl ?: "",
                    isLoading = isLoading,
                    isSaving = isSaving,
                    onSave = { title, description, imageUri ->
                        isSaving = true
                        lifecycleScope.launch {
                            saveNote(noteId, title, description, imageUri, note?.imageUrl, note?.thumbnailUrl)
                            isSaving = false
                            finish()
                        }
                    },
                    onDelete = {
                        lifecycleScope.launch {
                            noteId?.let { id ->
                                repository.deleteNote(id).onSuccess {
                                    Toast.makeText(this@ComposeNoteDetailActivity, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show()
                                    finish()
                                }.onFailure { e ->
                                    Toast.makeText(this@ComposeNoteDetailActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private suspend fun saveNote(
        noteId: String?,
        title: String,
        description: String,
        imageUri: Uri?,
        currentImageUrl: String?,
        currentThumbnailUrl: String?
    ) {
        val userId = repository.getCurrentUserId() ?: return

        var imageUrl = currentImageUrl ?: ""
        var thumbnailUrl = currentThumbnailUrl ?: ""

        // Upload new image if selected
        imageUri?.let { uri ->
            cloudinaryRepository.uploadImage(uri).onSuccess { uploadedUrl ->
                imageUrl = uploadedUrl
                thumbnailUrl = cloudinaryRepository.getThumbnailUrl(uploadedUrl)
            }.onFailure { e ->
                Toast.makeText(this, "Lỗi upload ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val noteToSave = Note(
            id = noteId ?: "",
            title = title,
            description = description,
            userId = userId,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl
        )

        if (noteId == null) {
            repository.createNote(noteToSave).onSuccess {
                Toast.makeText(this, "Đã tạo ghi chú", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            repository.updateNote(noteId, noteToSave).onSuccess {
                Toast.makeText(this, "Đã cập nhật ghi chú", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
