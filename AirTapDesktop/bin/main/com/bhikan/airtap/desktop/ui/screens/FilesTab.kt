package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.api.AirTapClient
import com.bhikan.airtap.desktop.api.FileItem
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun FilesTab(client: AirTapClient) {
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadFiles(path: String) {
        scope.launch {
            isLoading = true
            try {
                val response = client.getFiles(path)
                currentPath = response.currentPath
                files = response.files
            } catch (e: Exception) {
                files = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadFiles("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Files", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { loadFiles(currentPath) }) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
                Button(onClick = {
                    val dialog = FileDialog(null as Frame?, "Select file to upload", FileDialog.LOAD)
                    dialog.isVisible = true
                    dialog.file?.let { fileName ->
                        val file = File(dialog.directory, fileName)
                        scope.launch {
                            client.uploadFile(currentPath, file)
                            loadFiles(currentPath)
                        }
                    }
                }) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breadcrumb
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { loadFiles("") }) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Home")
            }
            
            currentPath.split("/").filter { it.isNotEmpty() }.fold("") { acc, part ->
                val newPath = if (acc.isEmpty()) "/$part" else "$acc/$part"
                Text(" / ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                TextButton(onClick = { loadFiles(newPath) }) {
                    Text(part)
                }
                newPath
            }
        }

        // Go Up Button
        if (currentPath.isNotEmpty()) {
            TextButton(onClick = {
                val parentPath = currentPath.substringBeforeLast("/", "")
                loadFiles(parentPath)
            }) {
                Icon(Icons.Default.ArrowUpward, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Go Up")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // File List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files) { file ->
                    FileItemRow(
                        file = file,
                        onOpen = {
                            if (file.isDirectory) {
                                loadFiles(file.path)
                            } else {
                                // Download file
                                val dialog = FileDialog(null as Frame?, "Save file", FileDialog.SAVE)
                                dialog.file = file.name
                                dialog.isVisible = true
                                dialog.file?.let { fileName ->
                                    val destination = File(dialog.directory, fileName)
                                    scope.launch {
                                        client.downloadFile(file.path, destination)
                                    }
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                client.deleteFile(file.path)
                                loadFiles(currentPath)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: FileItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.extension),
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium)
                if (!file.isDirectory) {
                    Text(
                        formatFileSize(file.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getFileIcon(extension: String) = when (extension.lowercase()) {
    "jpg", "jpeg", "png", "gif", "webp" -> Icons.Default.Image
    "mp4", "mkv", "avi", "mov" -> Icons.Default.VideoFile
    "mp3", "wav", "flac", "aac" -> Icons.Default.AudioFile
    "pdf" -> Icons.Default.PictureAsPdf
    "zip", "rar", "7z" -> Icons.Default.FolderZip
    "apk" -> Icons.Default.Android
    else -> Icons.Default.InsertDriveFile
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
