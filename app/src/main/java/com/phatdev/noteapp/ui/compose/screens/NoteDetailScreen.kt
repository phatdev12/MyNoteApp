package com.phatdev.noteapp.ui.compose.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phatdev.noteapp.ui.compose.theme.*

@Composable
fun NoteDetailScreen(
    noteId: String?,
    initialTitle: String = "",
    initialDescription: String = "",
    initialImageUrl: String = "",
    isLoading: Boolean = false,
    isSaving: Boolean = false,
    onSave: (title: String, description: String, imageUri: Uri?) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialTitle, initialDescription) {
        title = initialTitle
        description = initialDescription
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    val isNewNote = noteId == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header - height 56dp, paddingHorizontal 16dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // btnBack - "← Quay lại", padding 8dp
            Text(
                text = "← Quay lại",
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!isNewNote) {
                Text(
                    text = "Xóa",
                    color = ErrorColor,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { showDeleteDialog = true }
                        .padding(8.dp)
                )
            }
        }

        HorizontalDivider(color = BorderLight, thickness = 1.dp)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(TextPrimary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Tiêu đề",
                                    color = TextTertiary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // etDescription - marginTop 16dp, 16sp, minLines 8, hint "Viết nội dung ghi chú...", lineSpacing 1.4
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = (16 * 1.4).sp
                    ),
                    cursorBrush = SolidColor(TextPrimary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Viết nội dung ghi chú...",
                                    color = TextTertiary,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (selectedImageUri != null || initialImageUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AsyncImage(
                        model = selectedImageUri ?: initialImageUrl,
                        contentDescription = "Note image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // btnUploadImage - marginTop 16dp, height 48dp, bg_input_field
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(BackgroundInput, RoundedCornerShape(12.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Upload Image",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Divider
        HorizontalDivider(color = BorderLight, thickness = 1.dp)

        // Save Button - padding 16dp, height 52dp, cornerRadius 8dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = { onSave(title, description, selectedImageUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = Color.White),
                enabled = title.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu", fontSize = 16.sp)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            title = { Text("Xóa ghi chú", color = TextPrimary) },
            text = { Text("Bạn có chắc chắn muốn xóa ghi chú này?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Xóa", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = TextPrimary)
                }
            }
        )
    }
}
