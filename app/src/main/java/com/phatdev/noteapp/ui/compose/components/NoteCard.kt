package com.phatdev.noteapp.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.phatdev.noteapp.data.models.Note
import com.phatdev.noteapp.ui.compose.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = note.title.ifEmpty { "Tiêu đề" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.description.ifEmpty { "Nội dung ghi chú..." },
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (14 * 1.3).sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = note.updatedAt?.let { formatDate(it) } ?: "Vừa xong",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }

        if (note.thumbnailUrl.isNotEmpty()) {
            AsyncImage(
                model = note.thumbnailUrl,
                contentDescription = "Note image",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun formatDate(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = diffMillis / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffMinutes < 1 -> "Vừa xong"
        diffMinutes < 60 -> "$diffMinutes phút trước"
        diffHours < 24 -> "$diffHours giờ trước"
        diffDays < 7 -> "$diffDays ngày trước"
        else -> {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
