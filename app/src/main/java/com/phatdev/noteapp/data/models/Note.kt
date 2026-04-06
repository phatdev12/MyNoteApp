package com.phatdev.noteapp.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Note(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var userId: String = "",
    var imageUrl: String = "",
    var thumbnailUrl: String = "",
    @ServerTimestamp
    var createdAt: Date? = null,
    @ServerTimestamp
    var updatedAt: Date? = null
)
