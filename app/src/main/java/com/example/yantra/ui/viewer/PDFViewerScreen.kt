package com.example.yantra.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.yantra.data.repository.DocumentRepository
import com.example.yantra.ui.flashcard.FlashcardViewModel
import com.example.yantra.ui.deck.DeckViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    navController: NavHostController,
    fileName: String
) {
    val context = LocalContext.current
    val repo = remember { DocumentRepository(context) }
    val flashVm: FlashcardViewModel = viewModel()
    val deckVm: DeckViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val decks by deckVm.decks.collectAsState()

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageIndex by remember { mutableStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Dialog / flashcard creation state
    var showDialog by remember { mutableStateOf(false) }
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    var isBackEditable by remember { mutableStateOf(false) }

    // Deck selection state
    var selectedDeckId by remember { mutableStateOf<Long?>(null) }
    var deckMenuExpanded by remember { mutableStateOf(false) }

    // Highlight / selection state (in viewer coordinates)
    var viewerSize by remember { mutableStateOf(IntSize.Zero) }
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }

    val hasSelection by remember(selectionStart, selectionEnd) {
        mutableStateOf(
            selectionStart != null &&
                    selectionEnd != null &&
                    abs(selectionEnd!!.x - selectionStart!!.x) > 8f &&
                    abs(selectionEnd!!.y - selectionStart!!.y) > 8f
        )
    }

    // Load decks and PDF once
    LaunchedEffect(fileName) {
        deckVm.loadDecks()

        pdfRenderer?.close()

        val pdfDir = File(context.filesDir, "pdfs")
        val file = File(pdfDir, fileName)

        if (file.exists()) {
            val renderer = withContext(Dispatchers.IO) {
                repo.openPdfFromFile(file)
            }
            pdfRenderer = renderer
            pageIndex = 0
            pageBitmap = renderPage(renderer, 0)

            // reset highlight on new doc
            selectionStart = null
            selectionEnd = null
        } else {
            pdfRenderer = null
            pageBitmap = null
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viewer: $fileName") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // PDF preview with drag-to-highlight overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val highlightFillColor =
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                val highlightStrokeColor =
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

                // 1) The PDF image (bottom layer)
                if (pageBitmap != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            ImageView(context).apply {
                                // We assume FIT_CENTER / ContentScale.Fit behavior
                                // (default scaleType), and mirror that in cropSelectionToBitmap
                            }
                        },
                        update = { imageView ->
                            imageView.setImageBitmap(pageBitmap)
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to open document.",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // 2) Transparent overlay for drag + tap + highlight (top layer)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .onSizeChanged { size ->
                            viewerSize = size
                        }
                        // Tap to clear selection
                        .pointerInput(pageBitmap) {
                            detectTapGestures(
                                onTap = {
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            )
                        }
                        // Drag to create/update selection
                        .pointerInput(pageBitmap) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    selectionStart = offset
                                    selectionEnd = offset
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val start = selectionStart ?: change.position
                                    val currentEnd = selectionEnd ?: start
                                    val newEnd = currentEnd + dragAmount
                                    selectionStart = start
                                    selectionEnd = newEnd
                                },
                                onDragEnd = {
                                    // keep selection after finger lifts
                                },
                                onDragCancel = {
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            )
                        }
                ) {
                    if (hasSelection && selectionStart != null && selectionEnd != null) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val start = selectionStart!!
                            val end = selectionEnd!!

                            val left = min(start.x, end.x)
                            val top = min(start.y, end.y)
                            val right = max(start.x, end.x)
                            val bottom = max(start.y, end.y)

                            val width = right - left
                            val height = bottom - top

                            if (width > 0f && height > 0f) {
                                drawRect(
                                    color = highlightFillColor,
                                    topLeft = Offset(left, top),
                                    size = Size(width, height)
                                )
                                drawRect(
                                    color = highlightStrokeColor,
                                    topLeft = Offset(left, top),
                                    size = Size(width, height),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2f
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Hint
            Text(
                text = "Tip: drag over the page to highlight the part you want to scan. Tap to clear.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pdfRenderer?.let { r ->
                                if (pageIndex > 0) {
                                    pageIndex--
                                    pageBitmap = renderPage(r, pageIndex)
                                    // reset selection when page changes
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            }
                        },
                        enabled = pdfRenderer != null && pageIndex > 0
                    ) {
                        Text("Previous")
                    }

                    Button(
                        modifier = Modifier.weight(1.3f),
                        onClick = {
                            val bmp = pageBitmap
                            if (bmp != null) {
                                scope.launch {
                                    val bitmapForOcr =
                                        if (
                                            hasSelection &&
                                            viewerSize.width > 0 &&
                                            viewerSize.height > 0 &&
                                            selectionStart != null &&
                                            selectionEnd != null
                                        ) {
                                            cropSelectionToBitmap(
                                                source = bmp,
                                                viewerSize = viewerSize,
                                                start = selectionStart!!,
                                                end = selectionEnd!!
                                            ) ?: bmp
                                        } else {
                                            bmp
                                        }

                                    val text = recognizeText(bitmapForOcr).trim()
                                    backText = text
                                    frontText = ""
                                    isBackEditable = false
                                    selectedDeckId = null
                                    deckMenuExpanded = false
                                    showDialog = true
                                }
                            }
                        },
                        enabled = pageBitmap != null
                    ) {
                        Text(
                            text = if (hasSelection) "Scan area" else "Scan page"
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pdfRenderer?.let { r ->
                                if (pageIndex < r.pageCount - 1) {
                                    pageIndex++
                                    pageBitmap = renderPage(r, pageIndex)
                                    // reset selection when page changes
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            }
                        },
                        enabled = pdfRenderer?.let { pageIndex < it.pageCount - 1 } == true
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }

    // Dialog: Create flashcard from OCR
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create Flashcard") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // FRONT
                    Text(
                        text = "Front (your question / prompt)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. What is the key idea here?") }
                    )

                    Spacer(Modifier.height(16.dp))

                    // DECK SELECTOR
                    Text(
                        text = "Deck",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = deckMenuExpanded,
                        onExpandedChange = { deckMenuExpanded = !deckMenuExpanded }
                    ) {
                        val selectedName = decks.firstOrNull { it.id == selectedDeckId }?.name
                            ?: "No deck (default)"

                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = deckMenuExpanded
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = deckMenuExpanded,
                            onDismissRequest = { deckMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No deck (default)") },
                                onClick = {
                                    selectedDeckId = null
                                    deckMenuExpanded = false
                                }
                            )
                            decks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text(deck.name) },
                                    onClick = {
                                        selectedDeckId = deck.id
                                        deckMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // BACK
                    Text(
                        text = "Back (recognized text)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))

                    if (isBackEditable) {
                        OutlinedTextField(
                            value = backText,
                            onValueChange = { backText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp),
                            singleLine = false
                        )
                    } else {
                        Surface(
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = if (backText.isBlank()) "(No text recognized)" else backText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = { isBackEditable = !isBackEditable }) {
                        Text(if (isBackEditable) "Done editing back" else "Edit back text")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalBack = backText.trim()
                        if (finalBack.isNotBlank()) {
                            val finalFront = frontText.ifBlank { finalBack.take(80) }
                            flashVm.insert(
                                front = finalFront,
                                back = finalBack,
                                deckId = selectedDeckId
                            )
                        }
                        frontText = ""
                        backText = ""
                        isBackEditable = false
                        selectedDeckId = null
                        deckMenuExpanded = false
                        showDialog = false
                    }
                ) {
                    Text("Save as Flashcard")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---- Helpers ----

/**
 * Map the drag rectangle in the viewer to a crop in the bitmap,
 * assuming ImageView scaleType behaves like ContentScale.Fit (FIT_CENTER).
 */
private fun cropSelectionToBitmap(
    source: Bitmap,
    viewerSize: IntSize,
    start: Offset,
    end: Offset
): Bitmap? {
    if (viewerSize.width == 0 || viewerSize.height == 0) return null

    val viewWidth = viewerSize.width.toFloat()
    val viewHeight = viewerSize.height.toFloat()
    val bmpWidth = source.width.toFloat()
    val bmpHeight = source.height.toFloat()

    if (viewWidth <= 0f || viewHeight <= 0f || bmpWidth <= 0f || bmpHeight <= 0f) {
        return null
    }

    // Selection in view coordinates
    val leftV = min(start.x, end.x)
    val topV = min(start.y, end.y)
    val rightV = max(start.x, end.x)
    val bottomV = max(start.y, end.y)

    val selWidth = rightV - leftV
    val selHeight = bottomV - topV
    if (selWidth <= 0f || selHeight <= 0f) return null

    // Aspect ratios
    val viewAspect = viewWidth / viewHeight
    val bmpAspect = bmpWidth / bmpHeight

    val scale: Float
    val offsetX: Float
    val offsetY: Float

    if (viewAspect > bmpAspect) {
        // View is "wider" -> image height fills view; horizontal letterboxing
        scale = viewHeight / bmpHeight
        val imageWidth = bmpWidth * scale
        offsetX = (viewWidth - imageWidth) / 2f
        offsetY = 0f
    } else {
        // View is "taller" -> image width fills view; vertical letterboxing
        scale = viewWidth / bmpWidth
        val imageHeight = bmpHeight * scale
        offsetX = 0f
        offsetY = (viewHeight - imageHeight) / 2f
    }

    fun mapX(viewX: Float): Float =
        ((viewX - offsetX) / scale).coerceIn(0f, bmpWidth)

    fun mapY(viewY: Float): Float =
        ((viewY - offsetY) / scale).coerceIn(0f, bmpHeight)

    val leftF = mapX(leftV)
    val topF = mapY(topV)
    val rightF = mapX(rightV)
    val bottomF = mapY(bottomV)

    val cropLeft = leftF.toInt().coerceIn(0, source.width - 1)
    val cropTop = topF.toInt().coerceIn(0, source.height - 1)
    val cropRight = rightF.toInt().coerceIn(cropLeft + 1, source.width)
    val cropBottom = bottomF.toInt().coerceIn(cropTop + 1, source.height)

    val w = cropRight - cropLeft
    val h = cropBottom - cropTop
    if (w <= 0 || h <= 0) return null

    return Bitmap.createBitmap(source, cropLeft, cropTop, w, h)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val task = recognizer.process(image)
    return@withContext try {
        val result = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
            task
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) {} }
                .addOnFailureListener { _ -> cont.resume("") {} }
        }
        result
    } catch (e: Exception) {
        ""
    }
}

fun renderPage(renderer: PdfRenderer, index: Int): Bitmap {
    val page = renderer.openPage(index)
    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()
    return bitmap
}
