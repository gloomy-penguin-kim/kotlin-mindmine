package com.kim.minemind.domain


class GameSession(initial: Board) {

    private val history = ArrayDeque<Board>()

    var board: Board = initial
        private set

    // ---------------------------
    // Apply move helpers
    // ---------------------------

    fun reveal(id: Int) {
        push()
        board = board.reveal(id)
    }

    fun toggleFlag(id: Int) {
        push()
        board = board.toggleFlag(id)
    }

    fun replace(newBoard: Board) {
        push()
        board = newBoard
    }

    // ---------------------------
    // Undo
    // ---------------------------

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        board = history.removeLast()
        return true
    }

    fun clearHistory() {
        history.clear()
    }

    private fun push() {
        history.addLast(board)
    }
}
