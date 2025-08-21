package com.example.tictactoe_multithreads.game

import com.example.tictactoe_multithreads.data.GameResult
import com.example.tictactoe_multithreads.data.Player

/**
 * Clase que maneja la lógica del juego Tic Tac Toe
 */
class TicTacToeGame {
    companion object {
        const val BOARD_SIZE = 3
        const val TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE
    }

    private val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { "" } }
    private var currentPlayer = Player.X
    private var movesCount = 0
    private var gameResult = GameResult.ONGOING

    /**
     * Realiza un movimiento en la posición especificada
     * @param row Fila del tablero (0-2)
     * @param col Columna del tablero (0-2)
     * @return true si el movimiento fue válido, false en caso contrario
     */
    fun makeMove(row: Int, col: Int): Boolean {
        if (row !in 0 until BOARD_SIZE || col !in 0 until BOARD_SIZE) {
            return false
        }

        if (board[row][col].isNotEmpty() || gameResult != GameResult.ONGOING) {
            return false
        }

        board[row][col] = currentPlayer.symbol
        movesCount++

        gameResult = checkGameResult()
        
        if (gameResult == GameResult.ONGOING) {
            currentPlayer = currentPlayer.next()
        }

        return true
    }

    /**
     * Verifica el resultado actual del juego
     */
    private fun checkGameResult(): GameResult {
        // Verificar filas
        for (row in 0 until BOARD_SIZE) {
            if (board[row][0].isNotEmpty() && 
                board[row][0] == board[row][1] && 
                board[row][1] == board[row][2]) {
                return if (board[row][0] == Player.X.symbol) {
                    GameResult.PLAYER_X_WINS
                } else {
                    GameResult.PLAYER_O_WINS
                }
            }
        }

        // Verificar columnas
        for (col in 0 until BOARD_SIZE) {
            if (board[0][col].isNotEmpty() && 
                board[0][col] == board[1][col] && 
                board[1][col] == board[2][col]) {
                return if (board[0][col] == Player.X.symbol) {
                    GameResult.PLAYER_X_WINS
                } else {
                    GameResult.PLAYER_O_WINS
                }
            }
        }

        // Verificar diagonal principal
        if (board[0][0].isNotEmpty() && 
            board[0][0] == board[1][1] && 
            board[1][1] == board[2][2]) {
            return if (board[0][0] == Player.X.symbol) {
                GameResult.PLAYER_X_WINS
            } else {
                GameResult.PLAYER_O_WINS
            }
        }

        // Verificar diagonal secundaria
        if (board[0][2].isNotEmpty() && 
            board[0][2] == board[1][1] && 
            board[1][1] == board[2][0]) {
            return if (board[0][2] == Player.X.symbol) {
                GameResult.PLAYER_X_WINS
            } else {
                GameResult.PLAYER_O_WINS
            }
        }

        // Verificar empate
        if (movesCount == TOTAL_CELLS) {
            return GameResult.DRAW
        }

        return GameResult.ONGOING
    }

    /**
     * Reinicia el juego
     */
    fun resetGame() {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                board[row][col] = ""
            }
        }
        currentPlayer = Player.X
        movesCount = 0
        gameResult = GameResult.ONGOING
    }

    /**
     * Fuerza el final del juego por tiempo agotado
     */
    fun forceTimeUp() {
        gameResult = GameResult.TIME_UP
    }

    /**
     * Obtiene el símbolo en una posición específica del tablero
     */
    fun getCellValue(row: Int, col: Int): String {
        return if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            board[row][col]
        } else {
            ""
        }
    }

    /**
     * Obtiene el jugador actual
     */
    fun getCurrentPlayer(): Player = currentPlayer

    /**
     * Obtiene el resultado actual del juego
     */
    fun getGameResult(): GameResult = gameResult

    /**
     * Obtiene el número de movimientos realizados
     */
    fun getMovesCount(): Int = movesCount

    /**
     * Verifica si el juego ha terminado
     */
    fun isGameFinished(): Boolean = gameResult != GameResult.ONGOING
}