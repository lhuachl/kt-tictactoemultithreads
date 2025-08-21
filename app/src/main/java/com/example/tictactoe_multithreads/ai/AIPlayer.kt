package com.example.tictactoe_multithreads.ai

import com.example.tictactoe_multithreads.data.Player
import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * Clase abstracta base para diferentes niveles de IA
 */
abstract class AIPlayer(protected val aiPlayer: Player) {
    /**
     * Calcula el mejor movimiento para la IA
     * @param board Estado actual del tablero
     * @return Pair con la posición (row, col) del mejor movimiento, o null si no hay movimientos
     */
    abstract suspend fun calculateBestMove(board: Array<Array<String>>): Pair<Int, Int>?
    
    /**
     * Obtiene todas las posiciones disponibles en el tablero
     */
    protected fun getAvailablePositions(board: Array<Array<String>>): List<Pair<Int, Int>> {
        val positions = mutableListOf<Pair<Int, Int>>()
        for (row in board.indices) {
            for (col in board[row].indices) {
                if (board[row][col].isEmpty()) {
                    positions.add(Pair(row, col))
                }
            }
        }
        return positions
    }
    
    /**
     * Verifica si hay un ganador en el tablero
     */
    protected fun checkWinner(board: Array<Array<String>>): String? {
        val size = board.size
        
        // Verificar filas
        for (row in 0 until size) {
            if (board[row][0].isNotEmpty() && 
                board[row][0] == board[row][1] && 
                board[row][1] == board[row][2]) {
                return board[row][0]
            }
        }
        
        // Verificar columnas
        for (col in 0 until size) {
            if (board[0][col].isNotEmpty() && 
                board[0][col] == board[1][col] && 
                board[1][col] == board[2][col]) {
                return board[0][col]
            }
        }
        
        // Verificar diagonal principal
        if (board[0][0].isNotEmpty() && 
            board[0][0] == board[1][1] && 
            board[1][1] == board[2][2]) {
            return board[0][0]
        }
        
        // Verificar diagonal secundaria
        if (board[0][2].isNotEmpty() && 
            board[0][2] == board[1][1] && 
            board[1][1] == board[2][0]) {
            return board[0][2]
        }
        
        return null
    }
    
    /**
     * Verifica si el tablero está lleno
     */
    protected fun isBoardFull(board: Array<Array<String>>): Boolean {
        return getAvailablePositions(board).isEmpty()
    }
    
    /**
     * Crea una copia del tablero
     */
    protected fun copyBoard(board: Array<Array<String>>): Array<Array<String>> {
        return Array(board.size) { row ->
            Array(board[row].size) { col ->
                board[row][col]
            }
        }
    }
}

/**
 * Nivel 1: IA que hace movimientos completamente aleatorios
 */
class RandomAI(aiPlayer: Player) : AIPlayer(aiPlayer) {
    
    override suspend fun calculateBestMove(board: Array<Array<String>>): Pair<Int, Int>? = withContext(Dispatchers.Default) {
        delay(Random.nextLong(500, 1500)) // Simular tiempo de "pensamiento"
        
        val availablePositions = getAvailablePositions(board)
        if (availablePositions.isEmpty()) {
            return@withContext null
        }
        
        // Seleccionar posición aleatoria
        val randomIndex = Random.nextInt(availablePositions.size)
        availablePositions[randomIndex]
    }
}

/**
 * Nivel 2: IA con algoritmo básico de defensa y ataque
 */
class DefensiveAI(aiPlayer: Player) : AIPlayer(aiPlayer) {
    private val humanPlayer = if (aiPlayer == Player.X) Player.O else Player.X
    
    override suspend fun calculateBestMove(board: Array<Array<String>>): Pair<Int, Int>? = withContext(Dispatchers.Default) {
        delay(Random.nextLong(300, 800)) // Simular tiempo de "pensamiento"
        
        val availablePositions = getAvailablePositions(board)
        if (availablePositions.isEmpty()) {
            return@withContext null
        }
        
        // 1. Intentar ganar
        val winningMove = findWinningMove(board, aiPlayer.symbol)
        if (winningMove != null) {
            return@withContext winningMove
        }
        
        // 2. Bloquear al oponente
        val blockingMove = findWinningMove(board, humanPlayer.symbol)
        if (blockingMove != null) {
            return@withContext blockingMove
        }
        
        // 3. Tomar el centro si está disponible
        if (board[1][1].isEmpty()) {
            return@withContext Pair(1, 1)
        }
        
        // 4. Tomar una esquina
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val availableCorners = corners.filter { (row, col) -> board[row][col].isEmpty() }
        if (availableCorners.isNotEmpty()) {
            return@withContext availableCorners[Random.nextInt(availableCorners.size)]
        }
        
        // 5. Movimiento aleatorio
        availablePositions[Random.nextInt(availablePositions.size)]
    }
    
    private fun findWinningMove(board: Array<Array<String>>, player: String): Pair<Int, Int>? {
        for (position in getAvailablePositions(board)) {
            val testBoard = copyBoard(board)
            testBoard[position.first][position.second] = player
            
            if (checkWinner(testBoard) == player) {
                return position
            }
        }
        return null
    }
}

/**
 * Nivel 3: IA con algoritmo Minimax usando múltiples hilos
 */
class MinimaxAI(aiPlayer: Player) : AIPlayer(aiPlayer) {
    private val humanPlayer = if (aiPlayer == Player.X) Player.O else Player.X
    private val maxDepth = 9 // Profundidad máxima para el minimax
    
    override suspend fun calculateBestMove(board: Array<Array<String>>): Pair<Int, Int>? = withContext(Dispatchers.Default) {
        val availablePositions = getAvailablePositions(board)
        if (availablePositions.isEmpty()) {
            return@withContext null
        }
        
        // Si solo hay una posición disponible, devolverla inmediatamente
        if (availablePositions.size == 1) {
            return@withContext availablePositions[0]
        }
        
        // Usar múltiples hilos para evaluar movimientos en paralelo
        val moveEvaluations = availablePositions.map { position ->
            async {
                val testBoard = copyBoard(board)
                testBoard[position.first][position.second] = aiPlayer.symbol
                val score = minimax(testBoard, 0, false, Int.MIN_VALUE, Int.MAX_VALUE)
                Pair(position, score)
            }
        }
        
        // Esperar a que todos los hilos terminen y encontrar el mejor movimiento
        val results = moveEvaluations.awaitAll()
        val bestMove = results.maxByOrNull { it.second }
        
        bestMove?.first
    }
    
    /**
     * Algoritmo Minimax con poda Alpha-Beta
     */
    private fun minimax(
        board: Array<Array<String>>, 
        depth: Int, 
        isMaximizing: Boolean, 
        alpha: Int, 
        beta: Int
    ): Int {
        val winner = checkWinner(board)
        
        // Casos base
        when {
            winner == aiPlayer.symbol -> return 10 - depth
            winner == humanPlayer.symbol -> return depth - 10
            isBoardFull(board) || depth >= maxDepth -> return 0
        }
        
        val availablePositions = getAvailablePositions(board)
        var currentAlpha = alpha
        var currentBeta = beta
        
        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            
            for (position in availablePositions) {
                val testBoard = copyBoard(board)
                testBoard[position.first][position.second] = aiPlayer.symbol
                
                val eval = minimax(testBoard, depth + 1, false, currentAlpha, currentBeta)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                
                // Poda Alpha-Beta
                if (currentBeta <= currentAlpha) {
                    break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            
            for (position in availablePositions) {
                val testBoard = copyBoard(board)
                testBoard[position.first][position.second] = humanPlayer.symbol
                
                val eval = minimax(testBoard, depth + 1, true, currentAlpha, currentBeta)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                
                // Poda Alpha-Beta
                if (currentBeta <= currentAlpha) {
                    break
                }
            }
            return minEval
        }
    }
}

/**
 * Enumeración para los diferentes niveles de dificultad
 */
enum class DifficultyLevel(val displayName: String) {
    EASY("Fácil - Aleatorio"),
    MEDIUM("Medio - Defensivo"),
    HARD("Difícil - Minimax")
}

/**
 * Factory para crear instancias de IA según el nivel de dificultad
 */
object AIPlayerFactory {
    fun createAI(level: DifficultyLevel, aiPlayer: Player): AIPlayer {
        return when (level) {
            DifficultyLevel.EASY -> RandomAI(aiPlayer)
            DifficultyLevel.MEDIUM -> DefensiveAI(aiPlayer)
            DifficultyLevel.HARD -> MinimaxAI(aiPlayer)
        }
    }
}