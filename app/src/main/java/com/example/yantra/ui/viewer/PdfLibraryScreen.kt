package com.example.yantra.ui.viewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.yantra.data.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfItem(
    val file: File,
    val name: String,
    val importedAt: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfLibraryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { DocumentRepository(context) }
    val scope = rememberCoroutineScope()

    var documents by remember { mutableStateOf<List<PdfItem>>(emptyList()) }

    // For long-press action dialog
    var actionDialogItem by remember { mutableStateOf<PdfItem?>(null) }

    // Load documents once when the screen opens
    LaunchedEffect(Unit) {
        loadDocuments(repo) { documents = it }
    }

    // File picker (+) for PDFs
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    // 1) Get display name from Uri
                    val displayName = getFileName(context, uri)

                    // 2) Import into app storage
                    withContext(Dispatchers.IO) {
                        repo.importPdfFromUri(uri, displayName)
                    }

                    // 3) Reload list
                    loadDocuments(repo) { documents = it }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your PDFs") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add PDF")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (documents.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "No documents yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap the + button to import a PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents) { item ->
                        PdfListCard(
                            item = item,
                            onClick = {
                                val encodedName = Uri.encode(item.file.name)
                                navController.navigate("pdfReader/$encodedName")
                            },
                            onLongClick = {
                                actionDialogItem = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Long-press dialog: Open / Delete / Cancel with big buttons
    actionDialogItem?.let { selected ->
        AlertDialog(
            onDismissRequest = { actionDialogItem = null },
            title = {
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OPEN
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val encodedName = Uri.encode(selected.file.name)
                            actionDialogItem = null
                            navController.navigate("pdfReader/$encodedName")
                        }
                    ) {
                        Text("Open")
                    }

                    // DELETE
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    selected.file.delete()
                                }
                                loadDocuments(repo) { documents = it }
                                actionDialogItem = null
                            }
                        }
                    ) {
                        Text("Delete")
                    }

                    // CANCEL
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { actionDialogItem = null }
                    ) {
                        Text("Cancel")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfListCard(
    item: PdfItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple "PDF" pill avatar instead of an icon that needs extra deps
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Imported on ${formatDate(item.importedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper: load documents from repo and convert to PdfItem list
private suspend fun loadDocuments(
    repo: DocumentRepository,
    onLoaded: (List<PdfItem>) -> Unit
) {
    withContext(Dispatchers.IO) {
        val files = repo.listLocalPdfs()
        val items = files.map { file ->
            PdfItem(
                file = file,
                name = file.name,
                importedAt = file.lastModified()
            )
        }
        withContext(Dispatchers.Main) {
            onLoaded(items)
        }
    }
}

// Helper: read display name from Uri
private fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1 && it.moveToFirst()) {
            return it.getString(index)
        }
    }
    return null
}

// Helper: simple date formatter for subtitle
private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "unknown"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
