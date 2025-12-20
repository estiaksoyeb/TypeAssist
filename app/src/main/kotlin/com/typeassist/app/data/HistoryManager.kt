package com.typeassist.app.data

import java.util.concurrent.CopyOnWriteArrayList

data class HistoryItem(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {
    private val history = CopyOnWriteArrayList<HistoryItem>()
    private const val EXPIRATION_TIME_MS = 2 * 60 * 1000 // 2 minutes

    fun add(text: String) {
        history.add(0, HistoryItem(text))
        cleanup()
    }

    fun getHistory(): List<HistoryItem> {
        cleanup()
        return history.toList()
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        history.removeIf { now - it.timestamp > EXPIRATION_TIME_MS }
    }
}
