package com.example.yantra.ui.flashcard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.yantra.data.model.Flashcard
import com.example.yantra.ui.deck.DeckViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlashcardListScreen(
    navController: NavHostController,
    deckId: Long? = null,
    vm: FlashcardViewModel = viewModel()
) {
    val cards by vm.cards.collectAsState()

    // Long press action card (Edit / Delete / Move / Cancel)
    var actionCard by remember { mutableStateOf<Flashcard?>(null) }

    // Edit dialog state
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }
    var editFront by remember { mutableStateOf("") }
    var editBack by remember { mutableStateOf("") }

    // Move-to-deck dialog state
    val deckVm: DeckViewModel = viewModel()
    val decks by deckVm.decks.collectAsState()
    var moveCard by remember { mutableStateOf<Flashcard?>(null) }
    var moveDeckMenuExpanded by remember { mutableStateOf(false) }
    var selectedMoveDeckId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(deckId) {
        deckVm.loadDecks()
        if (deckId == null) {
            vm.loadAll()
        } else {
            vm.loadAllForDeck(deckId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (deckId == null) {
                            "My Flashcards"
                        } else {
                            "Deck Flashcards"
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flashcards yet.\nScan a PDF page to create one!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cards) { card ->
                            FlashcardListItem(
                                card = card,
                                onLongPress = { selected ->
                                    actionCard = selected
                                }
                            )
                        }
                    }
                }
            }

            // Long-press action dialog
            actionCard?.let { current ->
                AlertDialog(
                    onDismissRequest = { actionCard = null },
                    title = {
                        Text(
                            text = "Flashcard options",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingCard = current
                                    editFront = current.front
                                    editBack = current.back
                                    actionCard = null
                                }
                            ) {
                                Text("Edit")
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    moveCard = current
                                    selectedMoveDeckId = null
                                    moveDeckMenuExpanded = false
                                    actionCard = null
                                }
                            ) {
                                Text("Move to deck")
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                onClick = {
                                    vm.delete(current)
                                    actionCard = null
                                }
                            ) {
                                Text("Delete")
                            }

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { actionCard = null }
                            ) {
                                Text("Cancel")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }

            // Edit dialog (front/back)
            editingCard?.let { original ->
                AlertDialog(
                    onDismissRequest = { editingCard = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.update(
                                    original.copy(
                                        front = editFront.trim(),
                                        back = editBack.trim()
                                    )
                                )
                                editingCard = null
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingCard = null }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Edit Flashcard") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editFront,
                                onValueChange = { editFront = it },
                                label = { Text("Front (question)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editBack,
                                onValueChange = { editBack = it },
                                label = { Text("Back (answer)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            // Move-to-deck dialog
            moveCard?.let { cardToMove ->
                AlertDialog(
                    onDismissRequest = {
                        moveCard = null
                        selectedMoveDeckId = null
                        moveDeckMenuExpanded = false
                    },
                    title = { Text("Move to deck") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Choose a deck for this card:",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            ExposedDropdownMenuBox(
                                expanded = moveDeckMenuExpanded,
                                onExpandedChange = { moveDeckMenuExpanded = !moveDeckMenuExpanded }
                            ) {
                                val selectedName =
                                    decks.firstOrNull { it.id == selectedMoveDeckId }?.name
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
                                            expanded = moveDeckMenuExpanded
                                        )
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = moveDeckMenuExpanded,
                                    onDismissRequest = { moveDeckMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("No deck (default)") },
                                        onClick = {
                                            selectedMoveDeckId = null
                                            moveDeckMenuExpanded = false
                                        }
                                    )

                                    decks.forEach { deck ->
                                        DropdownMenuItem(
                                            text = { Text(deck.name) },
                                            onClick = {
                                                selectedMoveDeckId = deck.id
                                                moveDeckMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val newDeckId = selectedMoveDeckId ?: 0L
                                vm.update(
                                    cardToMove.copy(deckId = newDeckId)
                                )
                                moveCard = null
                                selectedMoveDeckId = null
                                moveDeckMenuExpanded = false
                            }
                        ) {
                            Text("Move")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                moveCard = null
                                selectedMoveDeckId = null
                                moveDeckMenuExpanded = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlashcardListItem(
    card: Flashcard,
    onLongPress: (Flashcard) -> Unit
) {
    var showBack by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showBack = !showBack },   // tap to flip
                onLongClick = { onLongPress(card) }   // long press → actions dialog
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (!showBack) "Front" else "Back",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (!showBack) card.front else card.back.ifBlank { "(empty)" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
