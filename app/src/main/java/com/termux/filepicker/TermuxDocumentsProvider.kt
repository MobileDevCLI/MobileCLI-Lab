package com.termux.filepicker

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * DocumentsProvider for accessing MobileCLI files from other apps.
 * This allows file managers and other apps to browse ~/storage and $PREFIX.
 * Matches real Termux's TermuxDocumentsProvider.
 */
class TermuxDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val TAG = "TermuxDocumentsProvider"

        // Root IDs
        private const val ROOT_ID_HOME = "home"
        private const val ROOT_ID_PREFIX = "prefix"

        // Default columns for roots
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID
        )

        // Default columns for documents
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    private lateinit var homeDir: File
    private lateinit var prefixDir: File

    override fun onCreate(): Boolean {
        val filesDir = context?.filesDir ?: return false
        homeDir = File(filesDir, "home")
        prefixDir = File(filesDir, "usr")
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        // Home directory root
        result.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_HOME)
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
            add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
                DocumentsContract.Root.FLAG_LOCAL_ONLY
            )
            add(DocumentsContract.Root.COLUMN_ICON, android.R.drawable.ic_menu_myplaces)
            add(DocumentsContract.Root.COLUMN_TITLE, "MobileCLI Home")
            add(DocumentsContract.Root.COLUMN_SUMMARY, "~/")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID_HOME)
        }

        // Prefix directory root
        result.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_PREFIX)
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
            add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
                DocumentsContract.Root.FLAG_LOCAL_ONLY
            )
            add(DocumentsContract.Root.COLUMN_ICON, android.R.drawable.ic_menu_compass)
            add(DocumentsContract.Root.COLUMN_TITLE, "MobileCLI Prefix")
            add(DocumentsContract.Root.COLUMN_SUMMARY, "\$PREFIX")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID_PREFIX)
        }

        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val file = getFileForDocumentId(documentId ?: return result)
        addFileRow(result, documentId, file)
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocumentId(parentDocumentId ?: return result)

        parent.listFiles()?.forEach { file ->
            val docId = getDocumentIdForFile(file)
            addFileRow(result, docId, file)
        }

        return result
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocumentId(documentId ?: throw FileNotFoundException())
        val accessMode = ParcelFileDescriptor.parseMode(mode ?: "r")
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun createDocument(
        parentDocumentId: String?,
        mimeType: String?,
        displayName: String?
    ): String {
        val parent = getFileForDocumentId(parentDocumentId ?: throw FileNotFoundException())
        val newFile = File(parent, displayName ?: "new_file")

        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            newFile.mkdirs()
        } else {
            newFile.createNewFile()
        }

        return getDocumentIdForFile(newFile)
    }

    override fun deleteDocument(documentId: String?) {
        val file = getFileForDocumentId(documentId ?: throw FileNotFoundException())
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    override fun renameDocument(documentId: String?, displayName: String?): String {
        val file = getFileForDocumentId(documentId ?: throw FileNotFoundException())
        val newFile = File(file.parentFile, displayName ?: file.name)
        file.renameTo(newFile)
        return getDocumentIdForFile(newFile)
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        val parent = getFileForDocumentId(parentDocumentId ?: return false)
        val child = getFileForDocumentId(documentId ?: return false)
        return child.absolutePath.startsWith(parent.absolutePath)
    }

    private fun getFileForDocumentId(documentId: String): File {
        return when {
            documentId == ROOT_ID_HOME -> homeDir
            documentId == ROOT_ID_PREFIX -> prefixDir
            documentId.startsWith("$ROOT_ID_HOME/") ->
                File(homeDir, documentId.removePrefix("$ROOT_ID_HOME/"))
            documentId.startsWith("$ROOT_ID_PREFIX/") ->
                File(prefixDir, documentId.removePrefix("$ROOT_ID_PREFIX/"))
            else -> throw FileNotFoundException("Unknown document ID: $documentId")
        }
    }

    private fun getDocumentIdForFile(file: File): String {
        return when {
            file.absolutePath.startsWith(homeDir.absolutePath) -> {
                val relativePath = file.absolutePath.removePrefix(homeDir.absolutePath)
                if (relativePath.isEmpty()) ROOT_ID_HOME
                else "$ROOT_ID_HOME$relativePath"
            }
            file.absolutePath.startsWith(prefixDir.absolutePath) -> {
                val relativePath = file.absolutePath.removePrefix(prefixDir.absolutePath)
                if (relativePath.isEmpty()) ROOT_ID_PREFIX
                else "$ROOT_ID_PREFIX$relativePath"
            }
            else -> file.absolutePath
        }
    }

    private fun addFileRow(cursor: MatrixCursor, documentId: String, file: File) {
        val mimeType = if (file.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            getMimeType(file)
        }

        var flags = 0
        if (file.isDirectory) {
            flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        }
        if (file.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        }

        cursor.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
            add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(DocumentsContract.Document.COLUMN_FLAGS, flags)
            add(DocumentsContract.Document.COLUMN_SIZE, file.length())
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType ?: "application/octet-stream"
    }
}
