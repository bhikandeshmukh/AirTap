package com.bhikan.airtap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String? = null,
    val extension: String? = null
)

@Serializable
data class FileListResponse(
    val currentPath: String,
    val parentPath: String?,
    val files: List<FileItem>,
    val totalCount: Int
)

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filePath: String? = null
)

@Serializable
data class DeleteResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class ApiError(
    val error: String,
    val code: Int
)
