package com.example.tictactoegame

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "win") val winner: String
)

@Dao
interface GameHistoryDao {
    @Insert
    suspend fun insert(gameHistory: GameHistory)

    @Query("SELECT * FROM game_history ORDER BY timestamp DESC LIMIT 1")
    fun getRecentHistory(): Flow<List<GameHistory>>

    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<GameHistory>
}

@Database(version = 2, entities = [GameHistory::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameHistoryDao(): GameHistoryDao
}


class HistoryFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button_back = view.findViewById<View>(R.id.button_history_back)
        button_back.setOnClickListener {
            // Возвращаемся к предыдущему фрагменту
            parentFragmentManager.popBackStack()
        }
        // Получаем ссылку на базу данных
         val db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "game_history")
             .fallbackToDestructiveMigration()
             .build()

        // Получаем DAO
         val gameHistoryDao = db.gameHistoryDao()
        // Получаем данные из базы данных

        viewLifecycleOwner.lifecycleScope.launch {
            var gameHistoryList = gameHistoryDao.getAllHistory()
            // дальнейшая работа с gameHistoryList

            val listViewHistory = view.findViewById<ListView>(R.id.list_view_history)
            gameHistoryList = listOf(
                GameHistory(timestamp = 1696118400000, winner = "X"),
                GameHistory(timestamp = 1696204800000, winner = "O"),
                GameHistory(timestamp = 1696291200000, winner = "X")
            )
            val adapter = HistoryAdapter(requireContext(), gameHistoryList)
            listViewHistory.adapter = adapter
        }
    }
}