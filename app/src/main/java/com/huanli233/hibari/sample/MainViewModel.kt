package com.huanli233.hibari.sample

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>(emptyList())
    val notes: LiveData<List<Note>> = _notes

    init {
        loadInitialNotes()
    }

    private fun loadInitialNotes() {
        _notes.value = listOf(
            Note(title = "Welcome to Material Notes", content = "This is a demo app showcasing Material 3 components with Android Views and Kotlin DSL."),
            Note(title = "Edge to Edge", content = "The UI gracefully handles system bars for an immersive experience."),
            Note(title = "How to add a new note?", content = "Just tap the floating action button at the bottom right corner!")
        ).sortedByDescending { it.timestamp }
    }

    fun saveNote(note: Note) {
        val currentList = _notes.value?.toMutableList() ?: mutableListOf()
        val existingIndex = currentList.indexOfFirst { it.id == note.id }

        if (existingIndex != -1) {
            currentList[existingIndex] = note
        } else {
            currentList.add(0, note)
        }
        _notes.value = currentList.sortedByDescending { it.timestamp }
    }
}