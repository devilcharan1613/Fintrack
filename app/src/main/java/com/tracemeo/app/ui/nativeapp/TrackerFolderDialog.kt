package com.tracemeo.app.ui.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracemeo.app.data.local.TrackerFolderEntity

@Composable
fun TrackerFolderDialog(
    folders: List<TrackerFolderEntity>,
    palette: ThemePalette,
    onDismiss: () -> Unit,
    onAddFolder: (folderName: String, icon: String) -> Unit,
    onDeleteFolder: (TrackerFolderEntity) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📂") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text("📂 Organize Tracker Folders", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                folders.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${folder.icon} ${folder.folderName}", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(
                            "✕",
                            color = palette.secondary,
                            modifier = Modifier.clickable { onDeleteFolder(folder) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("Icon") },
                        singleLine = true,
                        modifier = Modifier.width(72.dp)
                    )
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("New Folder Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onAddFolder(folderName.trim(), icon.ifBlank { "📂" })
                        folderName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
            ) {
                Text("+ Add Folder", color = palette.background, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = palette.textSecondary) }
        }
    )
}
