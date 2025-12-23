package com.bhikan.airtap.data.repository

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import com.bhikan.airtap.data.model.FileItem
import com.bhikan.airtap.data.model.FileListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    private val context: Context
) : FileRepository {

    private val rootPath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    override suspend fun listFiles(path: String): FileListResponse = withContext(Dispatchers.IO) {
        val targetPath = if (path.isEmpty() || path == "/") rootPath else path
        val directory = File(targetPath)

        if (!directory.exists() || !directory.isDirectory) {
            return@withContext FileListResponse(
                currentPath = targetPath,
                parentPath = null,
                files = emptyList(),
                totalCount = 0
            )
        }

        val files = directory.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { it.toFileItem() }
            ?: emptyList()

        val parentPath = if (targetPath != rootPath) {
            directory.parentFile?.absolutePath
        } else null

        FileListResponse(
            currentPath = targetPath,
            parentPath = parentPath,
            files = files,
            totalCount = files.size
        )
    }

    override suspend fun getFile(path: String): File? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists() && file.isFile && isPathAllowed(path)) {
            file
        } else null
    }

    override suspend fun saveFile(
        path: String,
        fileName: String,
        inputStream: InputStream
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(if (path.isEmpty()) rootPath else path)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, fileName)
            targetFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!isPathAllowed(path)) {
                return@withContext Result.failure(SecurityException("Access denied"))
            }

            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDirectory(path: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parentDir = File(if (path.isEmpty()) rootPath else path)
                val newDir = File(parentDir, name)

                if (newDir.exists()) {
                    return@withContext Result.failure(Exception("Directory already exists"))
                }

                if (newDir.mkdirs()) {
                    Result.success(newDir.absolutePath)
                } else {
                    Result.failure(Exception("Failed to create directory"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun getStorageRoots(): List<FileItem> {
        val roots = mutableListOf<FileItem>()

        val internal = Environment.getExternalStorageDirectory()
        if (internal.exists()) {
            roots.add(
                FileItem(
                    name = "Internal Storage",
                    path = internal.absolutePath,
                    isDirectory = true,
                    size = internal.totalSpace,
                    lastModified = internal.lastModified()
                )
            )
        }

        context.getExternalFilesDirs(null).forEachIndexed { index, file ->
            if (index > 0 && file != null) {
                val sdRoot = file.parentFile?.parentFile?.parentFile?.parentFile
                if (sdRoot != null && sdRoot.exists()) {
                    roots.add(
                        FileItem(
                            name = "SD Card $index",
                            path = sdRoot.absolutePath,
                            isDirectory = true,
                            size = sdRoot.totalSpace,
                            lastModified = sdRoot.lastModified()
                        )
                    )
                }
            }
        }

        return roots
    }

    override fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "json" -> "application/json"
                "js" -> "application/javascript"
                "css" -> "text/css"
                "html", "htm" -> "text/html"
                "txt", "log" -> "text/plain"
                "md" -> "text/markdown"
                "xml" -> "application/xml"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "apk" -> "application/vnd.android.package-archive"
                else -> "application/octet-stream"
            }
    }

    private fun File.toFileItem(): FileItem {
        return FileItem(
            name = name,
            path = absolutePath,
            isDirectory = isDirectory,
            size = if (isFile) length() else 0,
            lastModified = lastModified(),
            mimeType = if (isFile) getMimeType(this) else null,
            extension = if (isFile) extension.lowercase().ifEmpty { null } else null
        )
    }

    private fun isPathAllowed(path: String): Boolean {
        val normalizedPath = File(path).canonicalPath
        val allowedRoots = listOf(
            Environment.getExternalStorageDirectory().canonicalPath
        )
        return allowedRoots.any { normalizedPath.startsWith(it) }
    }
}
