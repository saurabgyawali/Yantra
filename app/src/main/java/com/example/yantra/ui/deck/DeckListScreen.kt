package com.example.yantra.ui.deck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.yantra.data.model.Deck
import com.example.yantra.ui.flashcard.FlashcardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeckListScreen(
    navController: NavHostController,
    vm: DeckViewModel = viewModel()
) {
    val decks by vm.decks.collectAsState()

    // Flashcard VM just to know how many cards are due (across all decks)
    val flashVm: FlashcardViewModel = viewModel()
    val dueCards by flashVm.dueCount.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    var actionDeck by remember { mutableStateOf<Deck?>(null) }

    LaunchedEffect(Unit) {
        vm.loadDecks()
        flashVm.refreshStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decks") }
            )
        },
        floatingActionButton = {
            // Stack: Review FAB (if there are due cards) above the + FAB
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (dueCards > 0) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("review") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Review today's cards"
                            )
                        },
                        text = {
                            Text("Review $dueCards due")
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create deck"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header (replaces Back button)
            Text(
                text = "Your decks",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap a deck to open it. Long press for review & delete options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            if (decks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No decks yet.\nTap + to create your first deck.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(decks) { deck ->
                        DeckListItem(
                            deck = deck,
                            onClick = {
                                navController.navigate("deck/${deck.id}/cards")
                            },
                            onLongClick = {
                                actionDeck = deck
                            }
                        )
                    }
                }
            }
        }
    }

    // New deck dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New deck") },
            text = {
                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("Deck name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.createDeck(newDeckName)
                        newDeckName = ""
                        showCreateDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newDeckName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Long-press actions: Open / Review / Delete / Cancel
    actionDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { actionDeck = null },
            title = { Text(deck.name) },
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
                            actionDeck = null
                            navController.navigate("deck/${deck.id}/cards")
                        }
                    ) {
                        Text("Open cards")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            actionDeck = null
                            navController.navigate("deck/${deck.id}/review")
                        }
                    ) {
                        Text("Review this deck")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        onClick = {
                            vm.deleteDeck(deck)
                            actionDeck = null
                        }
                    ) {
                        Text("Delete deck")
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { actionDeck = null }
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
private fun DeckListItem(
    deck: Deck,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primaryContainer
    val accentTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular "avatar" with first letter of deck
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = deck.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentTextColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Tap to open • Long press for options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
