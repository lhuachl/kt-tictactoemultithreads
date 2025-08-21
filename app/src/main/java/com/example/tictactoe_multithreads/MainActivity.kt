package com.example.tictactoe_multithreads

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tictactoe_multithreads.data.GameResult
import com.example.tictactoe_multithreads.data.GameStatistics
import com.example.tictactoe_multithreads.data.Player
import com.example.tictactoe_multithreads.game.TicTacToeGame
import com.example.tictactoe_multithreads.timer.GameTimer

/**
 * Actividad principal que maneja la interfaz y lógica del juego Tic Tac Toe
 * Implementa multithreading para el temporizador y manejo de eventos
 */
class MainActivity : AppCompatActivity(), GameTimer.GameTimerListener {
    
    // Componentes de la interfaz
    private lateinit var tvCurrentPlayer: TextView
    private lateinit var tvTimer: TextView
    private lateinit var gridBoard: GridLayout
    private lateinit var btnNewGame: Button
    private lateinit var btnResetStats: Button
    private lateinit var tvPlayerXWins: TextView
    private lateinit var tvPlayerOWins: TextView
    private lateinit var tvDraws: TextView
    private lateinit var tvTotalGames: TextView
    private lateinit var tvAverageTime: TextView
    
    // Lógica del juego
    private lateinit var game: TicTacToeGame
    private lateinit var gameTimer: GameTimer
    private lateinit var gameStatistics: GameStatistics
    
    // Matriz de botones del tablero
    private lateinit var boardButtons: Array<Array<Button>>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        setupWindowInsets()
        initializeViews()
        initializeGameLogic()
        setupEventListeners()
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
        if (!gameTimer.isTimerRunning() || game.isGameFinished()) {
            return
        }
        
        if (game.makeMove(row, col)) {
            updateBoardUI()
            updateCurrentPlayerUI()
            
            if (game.isGameFinished()) {
                finishGame()
            }
        } else {
            Toast.makeText(this, "Movimiento inválido", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inicia un nuevo juego
     */
    private fun startNewGame() {
        gameTimer.stopTimer()
        game.resetGame()
        updateBoardUI()
        updateCurrentPlayerUI()
        gameTimer.startTimer()
    }

    /**
     * Finaliza el juego actual
     */
    private fun finishGame() {
        gameTimer.stopTimer()
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
        val playerText = if (currentPlayer == Player.X) {
            getString(R.string.player_x_turn)
        } else {
            getString(R.string.player_o_turn)
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
    }

    /**
     * Muestra el resultado del juego en un diálogo
     */
    private fun showGameResult(result: GameResult, timeElapsed: Int, movesCount: Int) {
        val winnerText = when (result) {
            GameResult.PLAYER_X_WINS -> getString(R.string.player_x_wins_msg)
            GameResult.PLAYER_O_WINS -> getString(R.string.player_o_wins_msg)
            GameResult.DRAW -> getString(R.string.game_draw_msg)
            GameResult.TIME_UP -> getString(R.string.time_up_msg)
            GameResult.ONGOING -> "Juego en curso"
        }
        
        val message = buildString {
            appendLine(getString(R.string.winner_label, winnerText))
            appendLine(getString(R.string.time_elapsed_label, timeElapsed))
            appendLine(getString(R.string.moves_made_label, movesCount))
            appendLine(getString(R.string.game_number_label, gameStatistics.totalGames))
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

    override fun onDestroy() {
        super.onDestroy()
        gameTimer.stopTimer()
    }

    override fun onPause() {
        super.onPause()
        if (gameTimer.isTimerRunning()) {
            gameTimer.pauseTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (gameTimer.isTimerRunning()) {
            gameTimer.resumeTimer()
        }
    }
}