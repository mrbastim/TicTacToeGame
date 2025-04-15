package com.example.tictactoegame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    // Игровое поле: 3x3. Каждая ячейка содержит символ игрока (X, O) или null.
    private var board = Array(3) { Array<Char?>(3) { null } }
    private var currentPlayer: Char = 'X'
    private lateinit var cellButtons: List<ImageButton>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeBoard(view)
        setupClickListeners()
    }

    // Инициализация списка кнопок игрового поля
    private fun initializeBoard(view: View) {
        cellButtons = listOf(
            view.findViewById(R.id.button_00),
            view.findViewById(R.id.button_01),
            view.findViewById(R.id.button_02),
            view.findViewById(R.id.button_10),
            view.findViewById(R.id.button_11),
            view.findViewById(R.id.button_12),
            view.findViewById(R.id.button_20),
            view.findViewById(R.id.button_21),
            view.findViewById(R.id.button_22)
        )
        // Очистка игровой доски (сбрасываем изображения и активность кнопок)
        cellButtons.forEach { button ->
            button.setImageDrawable(null)
            button.isEnabled = true
        }
    }

    // Установка слушателей для каждой кнопки игрового поля
    private fun setupClickListeners() {

        cellButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val row = index / 3
                val col = index % 3
                val gameStatus = view?.findViewById<TextView>(R.id.textViewGameStatus)
                if (board[row][col] == null) {
                    board[row][col] = currentPlayer
                    updateButton(button, currentPlayer)
                    val winner = checkWin()
                    if (winner != null) {
                        disableButtons() // Если победа, отключаем дальнейшие ходы
                        insertWinnerIntoDB(winner) // Добавляем победителя в БД
                        gameStatus?.text = "Победитель: $winner"
                        gameStatus?.visibility = View.VISIBLE
                    } else {
                        // Переключение игрока
                        currentPlayer = if (currentPlayer == 'X') 'O' else 'X'
                    }
                    // Проверка на ничью
                    if (board.all { row -> row.all { it != null } } and (winner == null)) {
                        disableButtons() // Если ничья, отключаем дальнейшие ходы
                        gameStatus?.text = "Ничья!"
                        gameStatus?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Обновление кнопки согласно выбору игрока
    private fun updateButton(button: ImageButton, player: Char) {
        // Предполагается, что в ресурсах есть изображения для крестика (cross) и нолика (circle)
        val resId = if (player == 'X') R.drawable.cross else R.drawable.circle
        button.setImageResource(resId)
        button.isEnabled = false
    }

    // Проверка условий победы (по строкам, столбцам и диагоналям)
    private fun checkWin(): Char? {
        // Проверка строк
        for (i in 0 until 3) {
            if (board[i][0] != null &&
                board[i][0] == board[i][1] &&
                board[i][1] == board[i][2]
            ) return board[i][0]
        }
        // Проверка столбцов
        for (j in 0 until 3) {
            if (board[0][j] != null &&
                board[0][j] == board[1][j] &&
                board[1][j] == board[2][j]
            ) return board[0][j]
        }
        // Проверка диагоналей
        if (board[0][0] != null &&
            board[0][0] == board[1][1] &&
            board[1][1] == board[2][2]
        ) return board[0][0]
        if (board[0][2] != null &&
            board[0][2] == board[1][1] &&
            board[1][1] == board[2][0]
        ) return board[0][2]
        return null
    }

    // Отключение всех кнопок после завершения игры
    private fun disableButtons() {
        cellButtons.forEach { it.isEnabled = false }
        val buttonRestart = view?.findViewById<Button>(R.id.buttonRestart)
        val buttonBackToMenu = view?.findViewById<Button>(R.id.buttonBackToMenu)
        val gameStatus = view?.findViewById<TextView>(R.id.textViewGameStatus)
        buttonRestart?.visibility = View.VISIBLE
        buttonBackToMenu?.visibility = View.VISIBLE

        buttonBackToMenu?.setOnClickListener {
            // Возвращаемся к предыдущему фрагменту
            parentFragmentManager.popBackStack()
        }
        buttonRestart?.setOnClickListener {
            // Сброс игры
            board = Array(3) { Array<Char?>(3) { null } }
            currentPlayer = 'X'
            initializeBoard(requireView())
            buttonRestart.visibility = View.GONE
            buttonBackToMenu?.visibility = View.GONE
            gameStatus?.visibility = View.GONE
            gameStatus?.text = ""
        }
    }
    private fun insertWinnerIntoDB(winner: Char) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "game_history")
                .fallbackToDestructiveMigration()
                .build()
            db.gameHistoryDao().insert(GameHistory(timestamp = System.currentTimeMillis(), winner = winner.toString()))
        }
    }
}