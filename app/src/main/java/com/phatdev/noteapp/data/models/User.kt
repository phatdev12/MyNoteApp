package com.phatdev.noteapp.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class User(
    @DocumentId
    var id: String = "",
    var email: String = "",
    var fullName: String = "",
    var profileImageUrl: String = "",
    @ServerTimestamp
    var createdAt: Date? = null,
    @ServerTimestamp
    var updatedAt: Date? = null
)
