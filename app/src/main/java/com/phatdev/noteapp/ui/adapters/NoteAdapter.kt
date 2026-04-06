package com.phatdev.noteapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.databinding.ItemNoteBinding
import com.phatdev.noteapp.utils.DateUtils

class NoteAdapter(
    private val notes: List<Note>,
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.apply {
                tvTitle.text = note.title.ifEmpty { "Không có tiêu đề" }
                tvDescription.text = note.description.ifEmpty { "Không có nội dung" }
                tvDate.text = DateUtils.getTimeAgo(note.updatedAt)

                if (note.thumbnailUrl.isNotEmpty()) {
                    cardThumbnail.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(note.thumbnailUrl)
                        .centerCrop()
                        .into(ivImage)
                } else {
                    cardThumbnail.visibility = View.GONE
                }

                root.setOnClickListener {
                    onNoteClick(note)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size
}
