package com.example.yantra.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.yantra.ui.home.HomeScreen
import com.example.yantra.ui.viewer.PdfLibraryScreen
import com.example.yantra.ui.viewer.PDFViewerScreen
import com.example.yantra.ui.flashcard.FlashcardListScreen
import com.example.yantra.ui.flashcard.FlashcardReviewScreen
import com.example.yantra.ui.deck.DeckListScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen(navController) }

        // PDF library screen
        composable("pdfViewer") { PdfLibraryScreen(navController) }

        // Actual PDF viewer for a specific file
        composable(
            route = "pdfReader/{fileName}",
            arguments = listOf(
                navArgument("fileName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName") ?: return@composable
            PDFViewerScreen(navController, fileName)
        }

        // All flashcards (no deck filter)
        composable("flashcards") { FlashcardListScreen(navController) }

        // Review all due cards (no deck filter)
        composable("review") { FlashcardReviewScreen(navController) }

        // Deck list
        composable("decks") { DeckListScreen(navController) }

        // Deck-specific cards
        composable(
            route = "deck/{deckId}/cards",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            FlashcardListScreen(navController, deckId = deckId)
        }

        // Deck-specific review
        composable(
            route = "deck/{deckId}/review",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            FlashcardReviewScreen(navController, deckId = deckId)
        }
    }
}
