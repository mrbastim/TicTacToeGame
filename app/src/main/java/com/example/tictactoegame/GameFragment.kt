package com.example.tictactoegame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.util.Log


class GameFragment : Fragment() {

    // Игровое поле: 3x3. Каждая ячейка содержит символ игрока (X, O) или null.
    private var board = Array(3) { Array<Char?>(3) { null } }
    private var currentPlayer: Char = 'X'
    private lateinit var cellButtons: List<ImageButton>
    private var isComputerGame = false
    private var difficultyLevel: String = "Лёгкий"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Считываем выбранный режим игры из аргументов
        arguments?.let {
            isComputerGame = it.getBoolean("computer_game", false)
            // Получаем строку сложности, если она есть, иначе используем дефолтную "Лёгкий"
            difficultyLevel = it.getString("difficulty", "Лёгкий")
        }

        Log.d("GameFragment", "isComputerGame = $isComputerGame")
        Log.d("GameFragment", "Difficulty Level = $difficultyLevel")

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
                // val gameStatus = requireView().findViewById<TextView>(R.id.textViewGameStatus) // Перенесено в makeMove

                // Ход игрока 'X' (условие currentPlayer == 'X' добавлено для ясности)
                if (board[row][col] == null && currentPlayer == 'X') {
                    makeMove(row, col, 'X') // Используем новую функцию для хода

                    // Если игра с компьютером и ход теперь у 'O' и игра не окончена
                    if (isComputerGame && currentPlayer == 'O' && checkWin() == null && !isBoardFull()) {
                        // Небольшая задержка для имитации "раздумий" компьютера
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Доп. проверка перед ходом компа, т.к. состояние могло измениться за время задержки
                            if (currentPlayer == 'O' && checkWin() == null && !isBoardFull()) {
                                computerMove()
                            }
                        }, 500) // Задержка 500 мс
                    }
                }
                // Если игра НЕ с компьютером, клик по пустой клетке должен обработать ход 'O'
                // (Это не было явно в вашем коде, но добавляю на всякий случай, если нужен режим 2-х игроков)
                else if (!isComputerGame && board[row][col] == null && currentPlayer == 'O') {
                    makeMove(row, col, 'O')
                }
            }
        }
    }

    private fun isBoardFull(): Boolean {
        return board.all { row -> row.all { it != null } }
    }

    private fun makeMove(row: Int, col: Int, player: Char) {
        if (board[row][col] == null) {
            board[row][col] = player
            val index = row * 3 + col
            updateButton(cellButtons[index], player)

            val winner = checkWin()
            val gameStatus = requireView().findViewById<TextView>(R.id.textViewGameStatus)

            if (winner != null) {
                disableButtons()
                insertWinnerIntoDB(winner)
                gameStatus.text = "Победитель: $winner"
                gameStatus.visibility = View.VISIBLE
            } else if (isBoardFull()) {
                disableButtons()
                gameStatus.text = "Ничья!"
                gameStatus.visibility = View.VISIBLE
            } else {
                // Переключаем игрока ТОЛЬКО если игра не окончена
                currentPlayer = if (player == 'X') 'O' else 'X'
                Log.d("GameFragment", "Player switched to: $currentPlayer") // Логгирование смены игрока
            }
        }
    }

    // Автоматический ход компьютера (простейшая реализация — выбирается первая свободная ячейка)
    private fun computerMove() {
        // Доп. проверка, что сейчас действительно ход компьютера ('O')
        if (currentPlayer != 'O' || !isComputerGame) {
            Log.w("GameFragment", "computerMove called but it's not O's turn or not a computer game.")
            return
        }

        Log.d("GameFragment", "Computer ($difficultyLevel) is thinking...")
        val bestMove = findComputerMove() // Выбираем ход по сложности

        if (bestMove != null) {
            Log.d("GameFragment", "Computer chose move: (${bestMove.first}, ${bestMove.second})")
            makeMove(bestMove.first, bestMove.second, 'O') // Выполняем ход через общую функцию
        } else {
            // Эта ситуация не должна возникать, если есть пустые клетки
            Log.e("GameFragment", "Computer couldn't find a move, but board is not full?")
            // Можно добавить аварийный случайный ход, если такое вдруг произойдет
            val emptyCells = findEmptyCells()
            if(emptyCells.isNotEmpty()){
                val fallbackMove = emptyCells.random()
                makeMove(fallbackMove.first, fallbackMove.second, 'O')
            }
        }
    }

    // --- ДОБАВЛЕНО: Функции выбора хода и стратегии ИИ ---
    private fun findComputerMove(): Pair<Int, Int>? {
        val emptyCells = findEmptyCells()
        if (emptyCells.isEmpty()) return null

        return when (difficultyLevel) {
            "Лёгкий" -> findEasyMove(emptyCells)
            "Средний" -> findMediumMove(emptyCells)
            "Сложный" -> findHardMove(emptyCells)
            else -> {
                Log.w("GameFragment", "Unknown difficulty '$difficultyLevel', defaulting to Easy.")
                findEasyMove(emptyCells) // По умолчанию легкий
            }
        }
    }

    // Лёгкий: Случайный ход
    private fun findEasyMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int> {
        return emptyCells.random()
    }

    // Средний: Выиграть -> Блокировать -> Случайный
    private fun findMediumMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int> {
        findWinningMove(emptyCells, 'O')?.let { return it } // 1. Выиграть
        findWinningMove(emptyCells, 'X')?.let { return it } // 2. Блокировать
        return findEasyMove(emptyCells)                     // 3. Случайный
    }

    // Сложный: Набор правил (приоритеты сверху вниз)
    private fun findHardMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int> {
        findWinningMove(emptyCells, 'O')?.let { return it } // 1. Выиграть
        findWinningMove(emptyCells, 'X')?.let { return it } // 2. Блокировать
        findForkMove(emptyCells, 'O')?.let { return it }      // 3. Создать вилку
        findForkMove(emptyCells, 'X')?.let { return it }      // 4. Блокировать вилку противника (заняв клетку вилки)
        if (board[1][1] == null && emptyCells.contains(Pair(1,1))) { return Pair(1, 1) } // 5. Центр
        findOppositeCornerMove(emptyCells)?.let { return it } // 6. Противоположный угол
        findEmptyCornerMove(emptyCells)?.let { return it }    // 7. Пустой угол
        findEmptySideMove(emptyCells)?.let { return it }      // 8. Пустая сторона

        // Если ничего не подошло (не должно случиться)
        Log.w("HardMove", "Strategy: Fallback to Random (should not happen)")
        return findEasyMove(emptyCells)
    }

    private fun findEmptyCells(): List<Pair<Int, Int>> {
    /* ... реализация ... */
        val cells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j] == null) {
                    cells.add(Pair(i, j))
                }
            }
        }
        return cells
    }
    private fun findWinningMove(emptyCells: List<Pair<Int, Int>>, player: Char): Pair<Int, Int>? {
    /* ... реализация ... */
        for ((r, c) in emptyCells) {
            board[r][c] = player // Временно ставим символ
            val wins = (checkWin() == player)
            board[r][c] = null // Убираем символ (отменяем ход)
            if (wins) {
                return Pair(r, c) // Нашли выигрышный ход
            }
        }
        return null // Не нашли выигрышный ход
    }
    private fun findForkMove(emptyCells: List<Pair<Int, Int>>, player: Char): Pair<Int, Int>? {
    /* ... реализация ... */
        for ((r, c) in emptyCells) {
            board[r][c] = player // Делаем ход
            // Считаем, сколько выигрышных ходов стало доступно ПОСЛЕ этого хода
            val winningOpportunities = findEmptyCells().count { (next_r, next_c) ->
                board[next_r][next_c] = player
                val wins = checkWin() == player
                board[next_r][next_c] = null // Отменяем второй ход
                wins
            }
            board[r][c] = null // Отменяем первый ход

            if (winningOpportunities >= 2) {
                return Pair(r, c) // Этот ход создает вилку
            }
        }
        return null
    }
    private fun findOppositeCornerMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int>? {
    /* ... реализация ... */
        val opponent = 'X'
        val corners = listOf(Pair(0,0), Pair(0,2), Pair(2,0), Pair(2,2))
        val opposites = mapOf(
            Pair(0,0) to Pair(2,2), Pair(0,2) to Pair(2,0),
            Pair(2,0) to Pair(0,2), Pair(2,2) to Pair(0,0)
        )

        for (corner in corners) {
            if (board[corner.first][corner.second] == opponent) {
                val opposite = opposites[corner]
                if (opposite != null && emptyCells.contains(opposite)) {
                    return opposite
                }
            }
        }
        return null
    }
    private fun findEmptyCornerMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int>? {
    /* ... реализация ... */
        val corners = listOf(Pair(0,0), Pair(0,2), Pair(2,0), Pair(2,2))
        val availableCorners = emptyCells.filter { it in corners }
        return availableCorners.randomOrNull()
    }
    private fun findEmptySideMove(emptyCells: List<Pair<Int, Int>>): Pair<Int, Int>? {
    /* ... реализация ... */
        val sides = listOf(Pair(0,1), Pair(1,0), Pair(1,2), Pair(2,1))
        val availableSides = emptyCells.filter { it in sides }
        return availableSides.randomOrNull()
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
        val buttonRestart = requireView().findViewById<Button>(R.id.buttonRestart)
        val buttonBackToMenu = requireView().findViewById<Button>(R.id.buttonBackToMenu)
        val gameStatus = requireView().findViewById<TextView>(R.id.textViewGameStatus)
        buttonRestart.visibility = View.VISIBLE
        buttonBackToMenu.visibility = View.VISIBLE

        buttonBackToMenu.setOnClickListener {
            // Возвращаемся к предыдущему фрагменту
            parentFragmentManager.popBackStack()
        }
        buttonRestart.setOnClickListener {
            // Сброс игры
            board = Array(3) { Array<Char?>(3) { null } }
            currentPlayer = 'X'
            initializeBoard(requireView())
            setupClickListeners()
            buttonRestart.visibility = View.GONE
            buttonBackToMenu.visibility = View.GONE
            gameStatus.visibility = View.GONE
            gameStatus.text = ""
        }
    }
    private fun insertWinnerIntoDB(winner: Char) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = Room.databaseBuilder(
                    requireContext().applicationContext,
                    AppDatabase::class.java,
                    "game_history"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                // Добавляем проверку на одиночный режим, если требуется
                val gameType = if (isComputerGame) {
                    "Сложность: $difficultyLevel"
                } else {
                    // Если планируете отображать победителя и для одиночной игры,
                    // укажите соответствующий тип. По умолчанию сейчас используется "2 игрока"
                    "2 игрока"
                }
                db.gameHistoryDao().insert(GameHistory(
                    timestamp = System.currentTimeMillis(),
                    winner = winner.toString(),
                    type = gameType
                ))
                Log.d("DB", "Winner $winner inserted successfully.")
            } catch (e: Exception) {
                Log.e("DB", "Error inserting winner", e)
            }
        }
    }
}
