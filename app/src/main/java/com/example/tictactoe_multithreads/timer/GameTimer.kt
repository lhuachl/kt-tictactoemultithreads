package com.example.tictactoe_multithreads.timer

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Clase que maneja el temporizador del juego usando hilos y Handler
 */
class GameTimer(private val listener: GameTimerListener) {
    companion object {
        const val GAME_DURATION_SECONDS = 60 // Duración del juego en segundos
        const val WARNING_TIME_SECONDS = 10 // Tiempo de advertencia
        
        // Mensajes para el Handler
        const val MSG_TIMER_UPDATE = 1
        const val MSG_TIMER_WARNING = 2
        const val MSG_TIMER_FINISHED = 3
        const val MSG_GAME_STARTED = 4
    }

    private val timeRemaining = AtomicInteger(GAME_DURATION_SECONDS)
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var timerThread: Thread? = null
    private var startTime: Long = 0

    /**
     * Handler para comunicación entre el hilo del temporizador y el hilo principal
     */
    private val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TIMER_UPDATE -> {
                    val seconds = msg.arg1
                    listener.onTimerUpdate(seconds)
                }
                MSG_TIMER_WARNING -> {
                    listener.onTimerWarning()
                }
                MSG_TIMER_FINISHED -> {
                    listener.onTimerFinished()
                }
                MSG_GAME_STARTED -> {
                    listener.onGameStarted()
                }
            }
        }
    }

    /**
     * Inicia el temporizador
     */
    fun startTimer() {
        if (isRunning.get()) {
            return
        }

        isRunning.set(true)
        isPaused.set(false)
        timeRemaining.set(GAME_DURATION_SECONDS)
        startTime = System.currentTimeMillis()

        // Enviar mensaje de juego iniciado
        timerHandler.sendMessage(
            Message.obtain(timerHandler, MSG_GAME_STARTED)
        )

        // Crear y ejecutar el hilo del temporizador
        timerThread = Thread {
            runTimerLoop()
        }.apply {
            name = "GameTimerThread"
            start()
        }
    }

    /**
     * Pausa el temporizador
     */
    fun pauseTimer() {
        isPaused.set(true)
    }

    /**
     * Reanuda el temporizador
     */
    fun resumeTimer() {
        isPaused.set(false)
    }

    /**
     * Detiene el temporizador
     */
    fun stopTimer() {
        isRunning.set(false)
        isPaused.set(false)
        timerThread?.interrupt()
        timerThread = null
    }

    /**
     * Obtiene el tiempo transcurrido en segundos
     */
    fun getElapsedTime(): Int {
        return GAME_DURATION_SECONDS - timeRemaining.get()
    }

    /**
     * Obtiene el tiempo restante en segundos
     */
    fun getTimeRemaining(): Int {
        return timeRemaining.get()
    }

    /**
     * Verifica si el temporizador está ejecutándose
     */
    fun isTimerRunning(): Boolean {
        return isRunning.get()
    }

    /**
     * Bucle principal del temporizador ejecutado en un hilo separado
     */
    private fun runTimerLoop() {
        var warningMessageSent = false

        try {
            while (isRunning.get() && timeRemaining.get() > 0) {
                // Verificar si el hilo ha sido interrumpido
                if (Thread.currentThread().isInterrupted) {
                    break
                }

                // Si no está pausado, decrementar el tiempo
                if (!isPaused.get()) {
                    val currentTime = timeRemaining.get()

                    // Enviar actualización del temporizador
                    val message = Message.obtain(timerHandler, MSG_TIMER_UPDATE)
                    message.arg1 = currentTime
                    timerHandler.sendMessage(message)

                    // Enviar advertencia cuando queden 10 segundos
                    if (currentTime == WARNING_TIME_SECONDS && !warningMessageSent) {
                        timerHandler.sendMessage(
                            Message.obtain(timerHandler, MSG_TIMER_WARNING)
                        )
                        warningMessageSent = true
                    }

                    // Decrementar el tiempo
                    timeRemaining.decrementAndGet()
                }

                // Esperar 1 segundo
                Thread.sleep(1000)
            }

            // Si el tiempo se agotó y el temporizador sigue ejecutándose
            if (isRunning.get() && timeRemaining.get() <= 0) {
                timerHandler.sendMessage(
                    Message.obtain(timerHandler, MSG_TIMER_FINISHED)
                )
            }

        } catch (e: InterruptedException) {
            // El hilo fue interrumpido, salir limpiamente
            Thread.currentThread().interrupt()
        } finally {
            isRunning.set(false)
        }
    }

    /**
     * Interfaz para escuchar eventos del temporizador
     */
    interface GameTimerListener {
        /**
         * Llamado cuando se actualiza el temporizador
         * @param secondsRemaining Segundos restantes
         */
        fun onTimerUpdate(secondsRemaining: Int)

        /**
         * Llamado cuando quedan pocos segundos (advertencia)
         */
        fun onTimerWarning()

        /**
         * Llamado cuando el tiempo se agota
         */
        fun onTimerFinished()

        /**
         * Llamado cuando el juego se inicia
         */
        fun onGameStarted()
    }
}