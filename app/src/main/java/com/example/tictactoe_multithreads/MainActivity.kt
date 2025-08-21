package com.example.tictactoe_multithreads

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tictactoe_multithreads.ai.DifficultyLevel
import com.example.tictactoe_multithreads.data.GameResult
import com.example.tictactoe_multithreads.data.GameStatistics
import com.example.tictactoe_multithreads.data.Player
import com.example.tictactoe_multithreads.game.AIGameController
import com.example.tictactoe_multithreads.game.TicTacToeGame
import com.example.tictactoe_multithreads.timer.GameTimer

/**
 * Actividad principal que maneja la interfaz y lógica del juego Tic Tac Toe
 * Implementa multithreading para el temporizador y IA con diferentes niveles
 */
class MainActivity : AppCompatActivity(), GameTimer.GameTimerListener, AIGameController.AIGameListener {
    
    // Componentes de la interfaz
    private lateinit var tvCurrentPlayer: TextView
    private lateinit var tvTimer: TextView
    private lateinit var gridBoard: GridLayout
    private lateinit var btnNewGame: Button
    private lateinit var btnResetStats: Button
    private lateinit var btnPlayerVsPlayer: Button
    private lateinit var btnPlayerVsAI: Button
    private lateinit var llDifficultySelector: LinearLayout
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var tvAIStatus: TextView
    private lateinit var tvPlayerXWins: TextView
    private lateinit var tvPlayerOWins: TextView
    private lateinit var tvDraws: TextView
    private lateinit var tvTotalGames: TextView
    private lateinit var tvAverageTime: TextView
    
    // Lógica del juego
    private lateinit var game: TicTacToeGame
    private lateinit var gameTimer: GameTimer
    private lateinit var gameStatistics: GameStatistics
    private lateinit var aiGameController: AIGameController
    
    // Matriz de botones del tablero
    private lateinit var boardButtons: Array<Array<Button>>
    
    // Estado del juego
    private var isVsAIMode = false
    private var isWaitingForAI = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        setupWindowInsets()
        initializeViews()
        initializeGameLogic()
        setupEventListeners()
        setupDifficultySpinner()
        createGameBoard()
        updateUI()
        startNewGame()
    }

    /**
     * Configura los window insets para edge-to-edge
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Inicializa todas las vistas de la interfaz
     */
    private fun initializeViews() {
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer)
        tvTimer = findViewById(R.id.tvTimer)
        gridBoard = findViewById(R.id.gridBoard)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnResetStats = findViewById(R.id.btnResetStats)
        btnPlayerVsPlayer = findViewById(R.id.btnPlayerVsPlayer)
        btnPlayerVsAI = findViewById(R.id.btnPlayerVsAI)
        llDifficultySelector = findViewById(R.id.llDifficultySelector)
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty)
        tvAIStatus = findViewById(R.id.tvAIStatus)
        tvPlayerXWins = findViewById(R.id.tvPlayerXWins)
        tvPlayerOWins = findViewById(R.id.tvPlayerOWins)
        tvDraws = findViewById(R.id.tvDraws)
        tvTotalGames = findViewById(R.id.tvTotalGames)
        tvAverageTime = findViewById(R.id.tvAverageTime)
    }

    /**
     * Inicializa la lógica del juego
     */
    private fun initializeGameLogic() {
        game = TicTacToeGame()
        gameTimer = GameTimer(this)
        gameStatistics = GameStatistics()
        aiGameController = AIGameController(Player.X, DifficultyLevel.EASY)
        aiGameController.setAIGameListener(this)
    }

    /**
     * Configura los listeners de eventos
     */
    private fun setupEventListeners() {
        btnNewGame.setOnClickListener {
            startNewGame()
        }
        
        btnResetStats.setOnClickListener {
            showResetStatsConfirmation()
        }
        
        btnPlayerVsPlayer.setOnClickListener {
            setGameMode(false)
        }
        
        btnPlayerVsAI.setOnClickListener {
            setGameMode(true)
        }
        
        spinnerDifficulty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLevel = DifficultyLevel.values()[position]
                aiGameController.setDifficultyLevel(selectedLevel)
                showAIPerformanceInfo(selectedLevel)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Configura el spinner de dificultad
     */
    private fun setupDifficultySpinner() {
        val difficultyLevels = DifficultyLevel.values().map { it.displayName }
        val adapter = ArrayAdapter(this, R.layout.spinner_item, difficultyLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = adapter
    }

    /**
     * Establece el modo de juego (PvP o PvAI)
     */
    private fun setGameMode(vsAI: Boolean) {
        isVsAIMode = vsAI
        
        // Actualizar apariencia de botones
        if (vsAI) {
            btnPlayerVsAI.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))
            btnPlayerVsPlayer.setBackgroundColor(ContextCompat.getColor(this, R.color.button_disabled))
            llDifficultySelector.visibility = View.VISIBLE
        } else {
            btnPlayerVsPlayer.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))
            btnPlayerVsAI.setBackgroundColor(ContextCompat.getColor(this, R.color.button_disabled))
            llDifficultySelector.visibility = View.GONE
            tvAIStatus.visibility = View.GONE
        }
        
        startNewGame()
    }

    /**
     * Muestra información sobre el rendimiento de la IA
     */
    private fun showAIPerformanceInfo(level: DifficultyLevel) {
        val stats = aiGameController.getAIPerformanceStats()
        val message = buildString {
            appendLine("Nivel: ${stats.level}")
            appendLine("Descripción: ${stats.description}")
            appendLine("Tiempo promedio: ${stats.averageThinkingTime}")
            appendLine("Probabilidad de ganar: ${stats.winProbability}")
            appendLine("Algoritmo: ${stats.algorithm}")
        }
        
        Toast.makeText(this, "Dificultad cambiada a: ${level.displayName}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Crea dinámicamente el tablero de juego con botones
     */
    private fun createGameBoard() {
        gridBoard.removeAllViews()
        boardButtons = Array(TicTacToeGame.BOARD_SIZE) { row ->
            Array(TicTacToeGame.BOARD_SIZE) { col ->
                createBoardButton(row, col)
            }
        }
    }

    /**
     * Crea un botón individual para el tablero
     */
    private fun createBoardButton(row: Int, col: Int): Button {
        val button = Button(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(col, 1f)
                rowSpec = GridLayout.spec(row, 1f)
                setMargins(4, 4, 4, 4)
            }
            
            text = ""
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)
            setBackgroundColor(ContextCompat.getColor(context, R.color.cell_background))
            
            setOnClickListener {
                onCellClicked(row, col)
            }
        }
        
        gridBoard.addView(button)
        return button
    }

    /**
     * Maneja el click en una celda del tablero
     */
    private fun onCellClicked(row: Int, col: Int) {
        if (!gameTimer.isTimerRunning() || game.isGameFinished() || isWaitingForAI) {
            return
        }
        
        // En modo IA, solo permitir clicks del jugador humano
        if (isVsAIMode && aiGameController.isAITurn(game.getCurrentPlayer())) {
            Toast.makeText(this, "Espera el turno de la IA", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (game.makeMove(row, col)) {
            updateBoardUI()
            updateCurrentPlayerUI()
            
            if (game.isGameFinished()) {
                finishGame()
            } else if (isVsAIMode && aiGameController.isAITurn(game.getCurrentPlayer())) {
                // Es turno de la IA
                makeAIMove()
            }
        } else {
            Toast.makeText(this, "Movimiento inválido", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Hace que la IA ejecute su movimiento
     */
    private fun makeAIMove() {
        isWaitingForAI = true
        
        aiGameController.makeAIMove(game) { row, col ->
            // Callback ejecutado cuando la IA ha decidido su movimiento
            if (game.makeMove(row, col)) {
                updateBoardUI()
                updateCurrentPlayerUI()
                
                if (game.isGameFinished()) {
                    finishGame()
                }
            }
            isWaitingForAI = false
        }
    }

    /**
     * Inicia un nuevo juego
     */
    private fun startNewGame() {
        gameTimer.stopTimer()
        game.resetGame()
        isWaitingForAI = false
        
        // Cancelar cualquier cálculo de IA en progreso
        aiGameController.cancelAICalculation()
        
        updateBoardUI()
        updateCurrentPlayerUI()
        updateAIStatusUI()
        gameTimer.startTimer()
        
        // Si es modo IA y la IA juega primero (X), hacer que mueva
        if (isVsAIMode && aiGameController.isAITurn(game.getCurrentPlayer())) {
            makeAIMove()
        }
    }

    /**
     * Finaliza el juego actual
     */
    private fun finishGame() {
        gameTimer.stopTimer()
        aiGameController.cancelAICalculation()
        isWaitingForAI = false
        
        val timeElapsed = gameTimer.getElapsedTime()
        val result = game.getGameResult()
        
        gameStatistics.recordGame(result, timeElapsed.toLong())
        updateStatisticsUI()
        showGameResult(result, timeElapsed, game.getMovesCount())
    }

    /**
     * Actualiza la interfaz del tablero
     */
    private fun updateBoardUI() {
        for (row in 0 until TicTacToeGame.BOARD_SIZE) {
            for (col in 0 until TicTacToeGame.BOARD_SIZE) {
                val cellValue = game.getCellValue(row, col)
                val button = boardButtons[row][col]
                
                button.text = cellValue
                when (cellValue) {
                    Player.X.symbol -> {
                        button.setTextColor(ContextCompat.getColor(this, R.color.player_x_color))
                    }
                    Player.O.symbol -> {
                        button.setTextColor(ContextCompat.getColor(this, R.color.player_o_color))
                    }
                    else -> {
                        button.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                    }
                }
            }
        }
    }

    /**
     * Actualiza la interfaz del jugador actual
     */
    private fun updateCurrentPlayerUI() {
        val currentPlayer = game.getCurrentPlayer()
        val playerText = if (isVsAIMode) {
            when {
                aiGameController.isAITurn(currentPlayer) -> "Turno: IA (${currentPlayer.symbol})"
                else -> "Turno: Humano (${currentPlayer.symbol})"
            }
        } else {
            if (currentPlayer == Player.X) {
                getString(R.string.player_x_turn)
            } else {
                getString(R.string.player_o_turn)
            }
        }
        
        tvCurrentPlayer.text = playerText
        
        val playerColor = if (currentPlayer == Player.X) {
            R.color.player_x_color
        } else {
            R.color.player_o_color
        }
        tvCurrentPlayer.setTextColor(ContextCompat.getColor(this, playerColor))
    }

    /**
     * Actualiza la interfaz del estado de la IA
     */
    private fun updateAIStatusUI() {
        if (isVsAIMode) {
            tvAIStatus.visibility = if (isWaitingForAI) View.VISIBLE else View.GONE
        } else {
            tvAIStatus.visibility = View.GONE
        }
    }

    /**
     * Actualiza la interfaz de estadísticas
     */
    private fun updateStatisticsUI() {
        tvPlayerXWins.text = getString(R.string.player_x_wins, gameStatistics.playerXWins)
        tvPlayerOWins.text = getString(R.string.player_o_wins, gameStatistics.playerOWins)
        tvDraws.text = getString(R.string.draws, gameStatistics.draws)
        tvTotalGames.text = getString(R.string.total_games, gameStatistics.totalGames)
        tvAverageTime.text = getString(R.string.average_time, gameStatistics.getAverageTime())
    }

    /**
     * Actualiza toda la interfaz
     */
    private fun updateUI() {
        updateBoardUI()
        updateCurrentPlayerUI()
        updateStatisticsUI()
        updateAIStatusUI()
    }

    /**
     * Muestra el resultado del juego en un diálogo
     */
    private fun showGameResult(result: GameResult, timeElapsed: Int, movesCount: Int) {
        val winnerText = when (result) {
            GameResult.PLAYER_X_WINS -> {
                if (isVsAIMode && aiGameController.getAIPlayer() == Player.X) {
                    "¡La IA gana!"
                } else {
                    getString(R.string.player_x_wins_msg)
                }
            }
            GameResult.PLAYER_O_WINS -> {
                if (isVsAIMode && aiGameController.getAIPlayer() == Player.O) {
                    "¡La IA gana!"
                } else {
                    getString(R.string.player_o_wins_msg)
                }
            }
            GameResult.DRAW -> getString(R.string.game_draw_msg)
            GameResult.TIME_UP -> getString(R.string.time_up_msg)
            GameResult.ONGOING -> "Juego en curso"
        }
        
        val difficultyInfo = if (isVsAIMode) {
            "\nDificultad: ${aiGameController.getDifficultyLevel().displayName}"
        } else {
            ""
        }
        
        val message = buildString {
            appendLine(getString(R.string.winner_label, winnerText))
            appendLine(getString(R.string.time_elapsed_label, timeElapsed))
            appendLine(getString(R.string.moves_made_label, movesCount))
            appendLine(getString(R.string.game_number_label, gameStatistics.totalGames))
            append(difficultyInfo)
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_result_title))
            .setMessage(message)
            .setPositiveButton("Nuevo Juego") { _, _ ->
                startNewGame()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /**
     * Muestra confirmación para reiniciar estadísticas
     */
    private fun showResetStatsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reiniciar Estadísticas")
            .setMessage("¿Estás seguro de que quieres reiniciar todas las estadísticas?")
            .setPositiveButton("Sí") { _, _ ->
                gameStatistics.reset()
                updateStatisticsUI()
                Toast.makeText(this, "Estadísticas reiniciadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Implementación de GameTimer.GameTimerListener

    override fun onTimerUpdate(secondsRemaining: Int) {
        tvTimer.text = getString(R.string.time_remaining, secondsRemaining)
        
        // Cambiar color del temporizador cuando quede poco tiempo
        val timerColor = if (secondsRemaining <= GameTimer.WARNING_TIME_SECONDS) {
            R.color.accent_color
        } else {
            R.color.timer_color
        }
        tvTimer.setTextColor(ContextCompat.getColor(this, timerColor))
    }

    override fun onTimerWarning() {
        Toast.makeText(this, getString(R.string.time_warning_msg), Toast.LENGTH_LONG).show()
    }

    override fun onTimerFinished() {
        game.forceTimeUp()
        finishGame()
        Toast.makeText(this, getString(R.string.time_up_msg), Toast.LENGTH_LONG).show()
    }

    override fun onGameStarted() {
        Toast.makeText(this, getString(R.string.game_started_msg), Toast.LENGTH_SHORT).show()
    }

    // Implementación de AIGameController.AIGameListener

    override fun onAIThinking() {
        isWaitingForAI = true
        tvAIStatus.text = getString(R.string.ai_thinking)
        tvAIStatus.visibility = View.VISIBLE
    }

    override fun onAIMoveCompleted(row: Int, col: Int) {
        tvAIStatus.text = getString(R.string.ai_move_completed, row + 1, col + 1)
        // Ocultar el mensaje después de un breve delay
        tvAIStatus.postDelayed({
            updateAIStatusUI()
        }, 1000)
    }

    override fun onAIError(error: String) {
        isWaitingForAI = false
        tvAIStatus.text = getString(R.string.ai_error, error)
        tvAIStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    override fun onAIProgressUpdate(message: String) {
        tvAIStatus.text = getString(R.string.ai_progress, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        gameTimer.stopTimer()
        aiGameController.cleanup()
    }

    override fun onPause() {
        super.onPause()
        if (gameTimer.isTimerRunning()) {
            gameTimer.pauseTimer()
        }
        aiGameController.cancelAICalculation()
    }

    override fun onResume() {
        super.onResume()
        if (gameTimer.isTimerRunning()) {
            gameTimer.resumeTimer()
        }
    }
}