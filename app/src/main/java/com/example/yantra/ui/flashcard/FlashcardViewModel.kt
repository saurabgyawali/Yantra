package com.example.yantra.ui.flashcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.yantra.data.model.Flashcard
import com.example.yantra.data.repository.FlashcardRepository
import com.example.yantra.data.srs.Rating
import com.example.yantra.data.srs.updateCardSRS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlashcardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FlashcardRepository(app)

    private val _cards = MutableStateFlow<List<Flashcard>>(emptyList())
    val cards = _cards.asStateFlow()

    // Simple stats
    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    private val _dueCount = MutableStateFlow(0)
    val dueCount = _dueCount.asStateFlow()

    // null = "all decks"
    private val _selectedDeckId = MutableStateFlow<Long?>(null)
    val selectedDeckId = _selectedDeckId.asStateFlow()

    fun setSelectedDeck(deckId: Long?) {
        _selectedDeckId.value = deckId
    }

    // ───── Loading (all decks) ─────

    fun loadAll() {
        viewModelScope.launch {
            _selectedDeckId.value = null
            _cards.value = repo.getAll()
            refreshStatsInternal()
        }
    }

    fun loadDueCards() {
        viewModelScope.launch {
            _selectedDeckId.value = null
            val now = System.currentTimeMillis()
            _cards.value = repo.getDue(now)
            refreshStatsInternal()
        }
    }

    // ───── Loading (per deck) ─────

    fun loadAllForDeck(deckId: Long) {
        viewModelScope.launch {
            _selectedDeckId.value = deckId
            _cards.value = repo.getAllForDeck(deckId)
            refreshStatsForDeck(deckId)
        }
    }

    fun loadDueForDeck(deckId: Long) {
        viewModelScope.launch {
            _selectedDeckId.value = deckId
            val now = System.currentTimeMillis()
            _cards.value = repo.getDueForDeck(deckId, now)
            refreshStatsForDeck(deckId)
        }
    }

    // ───── Insert / Update / Delete ─────

    // Default insert (no deck → deckId = 0L)
    fun insert(front: String, back: String = "") {
        insert(front, back, deckId = null)
    }

    // Insert with explicit deck
    fun insert(front: String, back: String, deckId: Long?) {
        viewModelScope.launch {
            repo.insert(
                Flashcard(
                    front = front,
                    back = back,
                    deckId = deckId ?: 0L
                )
            )

            val currentDeck = _selectedDeckId.value
            if (currentDeck == null) {
                loadAll()
            } else {
                loadAllForDeck(currentDeck)
            }
        }
    }

    fun update(card: Flashcard) {
        viewModelScope.launch {
            repo.update(card)

            val currentDeck = _selectedDeckId.value
            if (currentDeck == null) {
                loadAll()
            } else {
                loadAllForDeck(currentDeck)
            }
        }
    }

    fun delete(card: Flashcard) {
        viewModelScope.launch {
            repo.delete(card)

            val currentDeck = _selectedDeckId.value
            if (currentDeck == null) {
                loadAll()
            } else {
                loadAllForDeck(currentDeck)
            }
        }
    }

    fun submitReview(card: Flashcard, rating: Rating) {
        viewModelScope.launch {
            val updated = updateCardSRS(card, rating)
            repo.update(updated)

            val now = System.currentTimeMillis()
            val currentDeck = _selectedDeckId.value

            if (currentDeck == null) {
                _cards.value = repo.getDue(now)
                refreshStatsInternal()
            } else {
                _cards.value = repo.getDueForDeck(currentDeck, now)
                refreshStatsForDeck(currentDeck)
            }
        }
    }

    // Stats for Home
    fun refreshStats() {
        viewModelScope.launch {
            refreshStatsInternal()
        }
    }

    private suspend fun refreshStatsInternal() {
        val now = System.currentTimeMillis()
        _totalCount.value = repo.countAll()
        _dueCount.value = repo.countDue(now)
    }

    private suspend fun refreshStatsForDeck(deckId: Long) {
        val now = System.currentTimeMillis()
        _totalCount.value = repo.countAllForDeck(deckId)
        _dueCount.value = repo.countDueForDeck(deckId, now)
    }
}
