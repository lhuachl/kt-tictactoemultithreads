package com.example.tictactoe_multithreads.data

/**
 * Clase de datos para manejar las estadísticas del juego
 */
data class GameStatistics(
    var playerXWins: Int = 0,
    var playerOWins: Int = 0,
    var draws: Int = 0,
    var totalGames: Int = 0,
    var totalTimeElapsed: Long = 0L
) {
    /**
     * Calcula el tiempo promedio por juego
     */
    fun getAverageTime(): Int {
        return if (totalGames > 0) {
            (totalTimeElapsed / totalGames).toInt()
        } else {
            0
        }
    }

    /**
     * Registra el resultado de un juego
     */
    fun recordGame(winner: GameResult, timeElapsed: Long) {
        when (winner) {
            GameResult.PLAYER_X_WINS -> playerXWins++
            GameResult.PLAYER_O_WINS -> playerOWins++
            GameResult.DRAW -> draws++
            GameResult.TIME_UP -> draws++
            GameResult.ONGOING -> {
                // El juego aún está en curso, no registrar resultado
                return
            }
        }
        totalGames++
        totalTimeElapsed += timeElapsed
    }

    /**
     * Reinicia todas las estadísticas
     */
    fun reset() {
        playerXWins = 0
        playerOWins = 0
        draws = 0
        totalGames = 0
        totalTimeElapsed = 0L
    }
}

/**
 * Enumeración para los posibles resultados del juego
 */
enum class GameResult {
    PLAYER_X_WINS,
    PLAYER_O_WINS,
    DRAW,
    TIME_UP,
    ONGOING
}

/**
 * Enumeración para los jugadores
 */
enum class Player(val symbol: String) {
    X("X"),
    O("O");
    
    fun next(): Player = if (this == X) O else X
}