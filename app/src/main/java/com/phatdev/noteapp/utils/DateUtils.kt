package com.phatdev.noteapp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatDate(date: Date?): String {
        if (date == null) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatDateShort(date: Date?): String {
        if (date == null) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    fun getTimeAgo(date: Date?): String {
        if (date == null) return ""
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> formatDate(date)
        }
    }
}
