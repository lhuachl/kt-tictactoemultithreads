package com.example.tictactoe_multithreads.game

import com.example.tictactoe_multithreads.ai.AIPlayer
import com.example.tictactoe_multithreads.ai.AIPlayerFactory
import com.example.tictactoe_multithreads.ai.DifficultyLevel
import com.example.tictactoe_multithreads.data.GameResult
import com.example.tictactoe_multithreads.data.Player
import kotlinx.coroutines.*

/**
 * Controlador para manejar juegos contra la IA con multithreading
 */
class AIGameController(
    private val humanPlayer: Player = Player.X,
    private var difficultyLevel: DifficultyLevel = DifficultyLevel.EASY
) {
    private val aiPlayer = if (humanPlayer == Player.X) Player.O else Player.X
    private var currentAI: AIPlayer = AIPlayerFactory.createAI(difficultyLevel, aiPlayer)
    private var gameScope: CoroutineScope? = null
    
    interface AIGameListener {
        /**
         * Llamado cuando la IA está calculando su movimiento
         */
        fun onAIThinking()
        
        /**
         * Llamado cuando la IA ha hecho su movimiento
         * @param row Fila del movimiento
         * @param col Columna del movimiento
         */
        fun onAIMoveCompleted(row: Int, col: Int)
        
        /**
         * Llamado cuando hay un error en el cálculo de la IA
         */
        fun onAIError(error: String)
        
        /**
         * Llamado para actualizar el progreso del cálculo de la IA
         * @param message Mensaje de progreso
         */
        fun onAIProgressUpdate(message: String)
    }
    
    private var listener: AIGameListener? = null
    
    /**
     * Establece el listener para eventos de la IA
     */
    fun setAIGameListener(listener: AIGameListener) {
        this.listener = listener
    }
    
    /**
     * Cambia el nivel de dificultad
     */
    fun setDifficultyLevel(level: DifficultyLevel) {
        difficultyLevel = level
        currentAI = AIPlayerFactory.createAI(level, aiPlayer)
    }
    
    /**
     * Obtiene el nivel de dificultad actual
     */
    fun getDifficultyLevel(): DifficultyLevel = difficultyLevel
    
    /**
     * Verifica si es el turno de la IA
     */
    fun isAITurn(currentPlayer: Player): Boolean {
        return currentPlayer == aiPlayer
    }
    
    /**
     * Obtiene el jugador IA
     */
    fun getAIPlayer(): Player = aiPlayer
    
    /**
     * Obtiene el jugador humano
     */
    fun getHumanPlayer(): Player = humanPlayer
    
    /**
     * Hace que la IA calcule y ejecute su movimiento
     */
    fun makeAIMove(game: TicTacToeGame, callback: (Int, Int) -> Unit) {
        // Cancelar cualquier cálculo anterior
        gameScope?.cancel()
        
        // Crear nuevo scope para este cálculo
        gameScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        gameScope?.launch {
            try {
                listener?.onAIThinking()
                
                // Actualizar progreso según el nivel de dificultad
                when (difficultyLevel) {
                    DifficultyLevel.EASY -> {
                        listener?.onAIProgressUpdate("Generando movimiento aleatorio...")
                    }
                    DifficultyLevel.MEDIUM -> {
                        listener?.onAIProgressUpdate("Analizando posiciones defensivas...")
                    }
                    DifficultyLevel.HARD -> {
                        listener?.onAIProgressUpdate("Calculando árbol de decisiones...")
                        delay(500) // Dar tiempo para mostrar el mensaje
                        listener?.onAIProgressUpdate("Evaluando con algoritmo Minimax...")
                    }
                }
                
                // Convertir el tablero del juego a matriz para la IA
                val board = convertGameBoardToMatrix(game)
                
                // Calcular el mejor movimiento usando la IA
                val bestMove = withContext(Dispatchers.Default) {
                    currentAI.calculateBestMove(board)
                }
                
                if (bestMove != null) {
                    val (row, col) = bestMove
                    
                    // Ejecutar el movimiento en el hilo principal
                    withContext(Dispatchers.Main) {
                        listener?.onAIMoveCompleted(row, col)
                        callback(row, col)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        listener?.onAIError("No se encontraron movimientos disponibles")
                    }
                }
                
            } catch (e: CancellationException) {
                // El cálculo fue cancelado, no hacer nada
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener?.onAIError("Error en el cálculo de la IA: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Convierte el tablero del juego a una matriz para la IA
     */
    private fun convertGameBoardToMatrix(game: TicTacToeGame): Array<Array<String>> {
        return Array(TicTacToeGame.BOARD_SIZE) { row ->
            Array(TicTacToeGame.BOARD_SIZE) { col ->
                game.getCellValue(row, col)
            }
        }
    }
    
    /**
     * Cancela cualquier cálculo de IA en progreso
     */
    fun cancelAICalculation() {
        gameScope?.cancel()
        gameScope = null
    }
    
    /**
     * Limpia recursos cuando ya no se necesita el controlador
     */
    fun cleanup() {
        cancelAICalculation()
        listener = null
    }
    
    /**
     * Obtiene estadísticas del rendimiento de la IA según el nivel
     */
    fun getAIPerformanceStats(): AIPerformanceStats {
        return when (difficultyLevel) {
            DifficultyLevel.EASY -> AIPerformanceStats(
                level = "Fácil",
                description = "Movimientos completamente aleatorios",
                averageThinkingTime = "0.5-1.5 segundos",
                winProbability = "Baja (~10-20%)",
                algorithm = "Selección aleatoria"
            )
            DifficultyLevel.MEDIUM -> AIPerformanceStats(
                level = "Medio",
                description = "Estrategia defensiva básica",
                averageThinkingTime = "0.3-0.8 segundos",
                winProbability = "Media (~40-60%)",
                algorithm = "Lógica heurística (ganar/bloquear)"
            )
            DifficultyLevel.HARD -> AIPerformanceStats(
                level = "Difícil",
                description = "Algoritmo Minimax optimizado",
                averageThinkingTime = "0.5-2.0 segundos",
                winProbability = "Alta (~80-90%)",
                algorithm = "Minimax con poda Alpha-Beta"
            )
        }
    }
}

/**
 * Clase de datos para estadísticas de rendimiento de la IA
 */
data class AIPerformanceStats(
    val level: String,
    val description: String,
    val averageThinkingTime: String,
    val winProbability: String,
    val algorithm: String
)