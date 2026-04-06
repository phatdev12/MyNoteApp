package com.phatdev.noteapp.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.data.repository.CloudinaryRepository
import com.phatdev.noteapp.data.repository.FirebaseRepository
import com.phatdev.noteapp.databinding.ActivityNoteDetailBinding
import com.phatdev.noteapp.utils.ImageUtils
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var repository: FirebaseRepository
    private lateinit var cloudinaryRepository: CloudinaryRepository
    private var currentNote: Note? = null
    private var selectedImageBitmap: Bitmap? = null
    private var currentImageUrl: String? = null
    private var pendingAction: (() -> Unit)? = null

    companion object {
        private const val TAG = "NoteDetailActivity"
        private const val PICK_IMAGE_REQUEST = 1001
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAction?.invoke()
        } else {
            Toast.makeText(this, "Cần cấp quyền để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
        }
        pendingAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityNoteDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            repository = FirebaseRepository(this)
            cloudinaryRepository = CloudinaryRepository(this)
            setupListeners()
            loadNote()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error loading note", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveNote()
        }

        binding.btnDelete.setOnClickListener {
            deleteNote()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnUploadImage.setOnClickListener {
            openGallery()
        }
    }

    private fun loadNote() {
        val noteId = intent.getStringExtra("note_id")
        if (noteId != null) {
            lifecycleScope.launch {
                try {
                    val result = repository.getNoteById(noteId)
                    result.onSuccess { note ->
                        currentNote = note
                        displayNote(note)
                    }
                    result.onFailure { e ->
                        Log.e(TAG, "Failed to load note", e)
                        Toast.makeText(this@NoteDetailActivity, "Failed to load note", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading note", e)
                }
            }
        } else {
            binding.btnDelete.isEnabled = false
        }
    }

    private fun displayNote(note: Note) {
        binding.etTitle.setText(note.title)
        binding.etDescription.setText(note.description)
        currentImageUrl = note.imageUrl
        if (note.imageUrl.isNotEmpty()) {
            try {
                binding.cardImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(note.imageUrl)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.transparent)
                    .into(binding.ivNoteImage)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image", e)
            }
        }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }

        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                var imageUrl = currentImageUrl ?: ""
                var thumbnailUrl = currentNote?.thumbnailUrl ?: ""
                if (selectedImageBitmap != null) {
                    val fileName = ImageUtils.generateFileName()

                    val uploadResult = cloudinaryRepository.uploadBitmap(selectedImageBitmap!!, fileName)
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrThrow()

                        thumbnailUrl = cloudinaryRepository.getThumbnailUrl(imageUrl, 200)
                        Log.d(TAG, "Image uploaded: $imageUrl")
                        Log.d(TAG, "Thumbnail URL: $thumbnailUrl")
                    } else {
                        val error = uploadResult.exceptionOrNull()?.message ?: "Upload thất bại"
                        Log.e(TAG, "Failed to upload image: $error")
                        Toast.makeText(this@NoteDetailActivity, "Lỗi upload ảnh: $error", Toast.LENGTH_LONG).show()
                        binding.btnSave.isEnabled = true
                        return@launch
                    }
                }

                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    Toast.makeText(this@NoteDetailActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    return@launch
                }

                val note = Note(
                    id = currentNote?.id ?: "",
                    title = title,
                    description = description,
                    userId = userId,
                    imageUrl = imageUrl,
                    thumbnailUrl = thumbnailUrl
                )

                val result = if (currentNote != null) {
                    repository.updateNote(currentNote!!.id, note)
                } else {
                    val createResult = repository.createNote(note)
                    createResult.map { }
                }

                result.onSuccess {
                    Toast.makeText(this@NoteDetailActivity, "Note saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                result.onFailure { e ->
                    Log.e(TAG, "Failed to save note", e)
                    Toast.makeText(this@NoteDetailActivity, "Failed to save note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving note", e)
                Toast.makeText(this@NoteDetailActivity, "Error saving note", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun deleteNote() {
        if (currentNote == null) return

        lifecycleScope.launch {
            try {
                val result = repository.deleteNote(currentNote!!.id)
                result.onSuccess {
                    Toast.makeText(this@NoteDetailActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                result.onFailure { e ->
                    Log.e(TAG, "Failed to delete note", e)
                    Toast.makeText(this@NoteDetailActivity, "Failed to delete note", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting note", e)
            }
        }
    }

    private fun openGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = { openGallery() }
            requestPermissionLauncher.launch(permission)
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery", e)
            Toast.makeText(this, "Không thể mở thư viện ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            try {
                val imageUri = data?.data
                if (imageUri != null) {
                    @Suppress("DEPRECATION")
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    selectedImageBitmap = bitmap
                    binding.cardImage.visibility = View.VISIBLE
                    binding.ivNoteImage.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
