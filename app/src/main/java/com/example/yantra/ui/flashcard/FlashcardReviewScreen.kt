package com.example.yantra.ui.flashcard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.yantra.data.srs.Rating
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardReviewScreen(
    navController: NavHostController,
    deckId: Long? = null // still here so your NavGraph compiles
) {
    val vm: FlashcardViewModel = viewModel()

    // Load due cards (later you can use deckId to filter if you want)
    LaunchedEffect(deckId) {
        vm.loadDueCards()
    }

    val cards by vm.cards.collectAsState()

    // Local UI state: whether we are showing the answer
    var isAnswerVisible by remember { mutableStateOf(false) }

    // Swipe animation helpers
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Screen width/height in px (used to fling card off-screen)
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // When the "current card" changes, hide the answer and reset offsets
    LaunchedEffect(cards.firstOrNull()?.id) {
        isAnswerVisible = false
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review Flashcards") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (cards.isEmpty()) {

                Text(
                    "No flashcards due for review.\nYou're all caught up 🎉",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Back")
                }

            } else {

                val card = cards.first()
                val total = cards.size

                // Small progress indicator
                Text(
                    text = "Card 1 of $total",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.height(12.dp))

                // Swipe thresholds in px (how far to drag to trigger rating)
                val swipeThresholdPx = with(density) { 80.dp.toPx() }

                // The actual card you tap to flip AND swipe to rate
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        // Apply both X and Y offsets for animation
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(),
                                offsetY.value.roundToInt()
                            )
                        }
                        // Tap to flip question/answer
                        .clickable {
                            isAnswerVisible = !isAnswerVisible
                        }
                        // Swipe in any direction (only when answer is visible)
                        .pointerInput(card.id, isAnswerVisible) {
                            if (!isAnswerVisible) return@pointerInput

                            detectDragGestures(
                                onDragEnd = {
                                    val x = offsetX.value
                                    val y = offsetY.value

                                    // Decide primary direction based on which axis moved more
                                    val absX = abs(x)
                                    val absY = abs(y)

                                    when {
                                        // Horizontal swipe dominates
                                        absX > absY && absX > swipeThresholdPx -> {
                                            if (x > 0f) {
                                                // RIGHT -> EASY
                                                scope.launch {
                                                    offsetX.animateTo(
                                                        targetValue = screenWidthPx * 1.2f,
                                                        animationSpec = tween(durationMillis = 200)
                                                    )
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    vm.submitReview(card, Rating.EASY)
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            } else {
                                                // LEFT -> AGAIN
                                                scope.launch {
                                                    offsetX.animateTo(
                                                        targetValue = -screenWidthPx * 1.2f,
                                                        animationSpec = tween(durationMillis = 200)
                                                    )
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    vm.submitReview(card, Rating.AGAIN)
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            }
                                        }

                                        // Vertical swipe dominates
                                        absY >= absX && absY > swipeThresholdPx -> {
                                            if (y < 0f) {
                                                // UP -> GOOD
                                                scope.launch {
                                                    offsetY.animateTo(
                                                        targetValue = -screenHeightPx * 1.2f,
                                                        animationSpec = tween(durationMillis = 200)
                                                    )
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    vm.submitReview(card, Rating.GOOD)
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            } else {
                                                // Downward swipe not mapped → snap back
                                                scope.launch {
                                                    offsetX.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = tween(durationMillis = 150)
                                                    )
                                                    offsetY.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = tween(durationMillis = 150)
                                                    )
                                                }
                                            }
                                        }

                                        // Not far enough: just snap back
                                        else -> {
                                            scope.launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(durationMillis = 150)
                                                )
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(durationMillis = 150)
                                                )
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val (dx, dy) = dragAmount
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dx)
                                        offsetY.snapTo(offsetY.value + dy)
                                    }
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (!isAnswerVisible) "Question" else "Answer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (!isAnswerVisible) card.front else card.back.ifBlank { "(empty)" },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!isAnswerVisible) {
                    Text(
                        text = "Tap the card to reveal the answer.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Only show SRS rating once the answer is visible
                    Text(
                        text = "Swipe left = Hard, up = Good, right = Easy.\nOr use the buttons below.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            vm.submitReview(card, Rating.AGAIN)
                        }) {
                            Text("Hard")
                        }

                        Button(onClick = {
                            vm.submitReview(card, Rating.GOOD)
                        }) {
                            Text("Good")
                        }

                        Button(onClick = {
                            vm.submitReview(card, Rating.EASY)
                        }) {
                            Text("Easy")
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Exit Review")
                }
            }
        }
    }
}
