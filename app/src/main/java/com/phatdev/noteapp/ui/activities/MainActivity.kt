package com.phatdev.noteapp.ui.activities

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.data.repository.FirebaseRepository
import com.phatdev.noteapp.databinding.ActivityMainBinding
import com.phatdev.noteapp.ui.adapters.NoteAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: FirebaseRepository
    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()
    private val filteredList = mutableListOf<Note>()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            repository = FirebaseRepository(this)
            setupRecyclerView()
            setupListeners()
            setupSearch()
            loadUserName()
            loadNotes()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error loading app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            loadNotes()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(filteredList) { note ->
            openNoteDetail(note)
        }
        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterNotes(s?.toString() ?: "")
            }
        })
    }

    private fun filterNotes(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(notesList)
        } else {
            val lowerQuery = query.lowercase()
            notesList.forEach { note ->
                if (note.title.lowercase().contains(lowerQuery) ||
                    note.description.lowercase().contains(lowerQuery)) {
                    filteredList.add(note)
                }
            }
        }
        noteAdapter.notifyDataSetChanged()
        updateEmptyState(query)
    }

    private fun setupListeners() {
        binding.fabAddNote.setOnClickListener {
            startActivity(Intent(this, NoteDetailActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            try {
                repository.logout()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
            }
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.navHome.setOnClickListener {
            // Already on home
        }
    }

    private fun loadUserName() {
        val userId = repository.getCurrentUserId() ?: return
        lifecycleScope.launch {
            val result = repository.getUserProfile(userId)
            result.onSuccess { user ->
                binding.tvUserName.text = if (user.fullName.isNotEmpty()) user.fullName else "My Notes"
            }
        }
    }

    private fun loadNotes() {
        val userId = repository.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "User not logged in, redirecting to login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        Log.d(TAG, "Loading notes for user: $userId")
        
        lifecycleScope.launch {
            try {
                val result = repository.getNotesByUserId(userId)
                result.onSuccess { notes ->
                    Log.d(TAG, "Loaded ${notes.size} notes")
                    notesList.clear()
                    notesList.addAll(notes)
                    // Apply current search filter
                    filterNotes(binding.etSearch.text?.toString() ?: "")
                }
                result.onFailure { e ->
                    Log.e(TAG, "Failed to load notes: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Lỗi tải ghi chú: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateEmptyState("")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading notes: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState("")
            }
        }
    }

    private fun updateEmptyState(query: String = "") {
        if (filteredList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvNotes.visibility = View.GONE
            binding.tvEmptyState.text = if (query.isNotEmpty()) {
                "Không tìm thấy ghi chú"
            } else {
                "Chưa có ghi chú"
            }
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvNotes.visibility = View.VISIBLE
        }
    }

    private fun openNoteDetail(note: Note) {
        val intent = Intent(this, NoteDetailActivity::class.java).apply {
            putExtra("note_id", note.id)
        }
        startActivity(intent)
    }
}
