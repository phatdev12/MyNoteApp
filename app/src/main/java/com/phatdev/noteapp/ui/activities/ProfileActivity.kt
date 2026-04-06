package com.phatdev.noteapp.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.phatdev.noteapp.data.repository.CloudinaryRepository
import com.phatdev.noteapp.data.repository.FirebaseRepository
import com.phatdev.noteapp.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var repository: FirebaseRepository
    private lateinit var cloudinaryRepository: CloudinaryRepository
    private var selectedAvatarBitmap: Bitmap? = null
    private var currentAvatarUrl: String? = null

    companion object {
        private const val TAG = "ProfileActivity"
        private const val PICK_IMAGE_REQUEST = 2001
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FirebaseRepository(this)
        cloudinaryRepository = CloudinaryRepository(this)

        setupListeners()
        loadUserProfile()
        loadNotesStats()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnChangeAvatar.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.ivAvatar.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            repository.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun loadUserProfile() {
        val userId = repository.getCurrentUserId() ?: return

        lifecycleScope.launch {
            val result = repository.getUserProfile(userId)
            result.onSuccess { user ->
                binding.etFullName.setText(user.fullName)
                binding.etEmail.setText(user.email)
                currentAvatarUrl = user.profileImageUrl

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(user.profileImageUrl)
                        .circleCrop()
                        .into(binding.ivAvatar)
                }
            }
            result.onFailure { e ->
                Log.e(TAG, "Failed to load profile", e)
            }
        }
    }

    private fun loadNotesStats() {
        val userId = repository.getCurrentUserId() ?: return

        lifecycleScope.launch {
            val result = repository.getNotesByUserId(userId)
            result.onSuccess { notes ->
                binding.tvNotesCount.text = notes.size.toString()
                val withImages = notes.count { it.imageUrl.isNotEmpty() }
                binding.tvImagesCount.text = withImages.toString()
            }
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            val imageUri = data?.data
            if (imageUri != null) {
                try {
                    @Suppress("DEPRECATION")
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    selectedAvatarBitmap = bitmap
                    Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(binding.ivAvatar)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image", e)
                    Toast.makeText(this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Vui lòng nhập tên"
            return
        }

        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                var avatarUrl = currentAvatarUrl ?: ""

                if (selectedAvatarBitmap != null) {
                    val fileName = "avatar_${repository.getCurrentUserId()}_${System.currentTimeMillis()}"
                    val uploadResult = cloudinaryRepository.uploadBitmap(selectedAvatarBitmap!!, fileName)
                    if (uploadResult.isSuccess) {
                        avatarUrl = uploadResult.getOrThrow()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show()
                    }
                }

                val userId = repository.getCurrentUserId()
                if (userId != null) {
                    val result = repository.updateUserProfile(userId, fullName, avatarUrl)
                    result.onSuccess {
                        Toast.makeText(this@ProfileActivity, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    result.onFailure { e ->
                        Toast.makeText(this@ProfileActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
                Toast.makeText(this@ProfileActivity, "Lỗi lưu hồ sơ", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
            }
        }
    }
}
