package com.example.yantra.ui.deck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.yantra.data.model.Deck
import com.example.yantra.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeckViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DeckRepository(app)

    private val _decks = MutableStateFlow<List<Deck>>(emptyList())
    val decks = _decks.asStateFlow()

    fun loadDecks() {
        viewModelScope.launch {
            _decks.value = repo.getAll()
        }
    }

    fun createDeck(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repo.insert(Deck(name = trimmed))
            loadDecks()
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            repo.delete(deck)
            loadDecks()
        }
    }

    fun renameDeck(deck: Deck, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repo.update(deck.copy(name = trimmed))
            loadDecks()
        }
    }
}
