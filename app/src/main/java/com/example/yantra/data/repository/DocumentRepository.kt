package com.example.yantra.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.IOException

class DocumentRepository(private val context: Context) {

    // Folder inside internal storage: /data/data/your.app.package/files/pdfs
    private val pdfDir: File = File(context.filesDir, "pdfs").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Import a PDF from a SAF Uri (file picked by user).
     * Copies it into our app's private "pdfs" directory and returns the File.
     */
    @Throws(IOException::class)
    fun importPdfFromUri(uri: Uri, displayName: String? = null): File {
        // Try to use display name if provided, otherwise fall back to timestamp.
        val safeName = (displayName ?: "document_${System.currentTimeMillis()}.pdf")
            .replace("/", "_")

        val destFile = File(pdfDir, safeName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Unable to open input stream for uri: $uri")

        return destFile
    }

    /**
     * List all imported PDFs.
     */
    fun listLocalPdfs(): List<File> {
        val files = pdfDir.listFiles { file ->
            file.isFile && file.extension.equals("pdf", ignoreCase = true)
        } ?: emptyArray()

        return files.sortedBy { it.name.lowercase() }
    }

    /**
     * Open a PDFRenderer for a given file.
     */
    @Throws(IOException::class)
    fun openPdfFromFile(file: File): PdfRenderer {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return PdfRenderer(pfd)
    }
}

