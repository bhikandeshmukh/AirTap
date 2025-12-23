package com.bhikan.airtap.data.repository

import com.bhikan.airtap.data.model.FileItem
import com.bhikan.airtap.data.model.FileListResponse
import java.io.File
import java.io.InputStream

interface FileRepository {
    suspend fun listFiles(path: String): FileListResponse
    suspend fun getFile(path: String): File?
    suspend fun saveFile(path: String, fileName: String, inputStream: InputStream): Result<String>
    suspend fun deleteFile(path: String): Result<Unit>
    suspend fun createDirectory(path: String, name: String): Result<String>
    fun getStorageRoots(): List<FileItem>
    fun getMimeType(file: File): String?
}
