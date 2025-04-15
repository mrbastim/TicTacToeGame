package com.example.tictactoegame

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appNameTextView = requireActivity().findViewById<TextView>(R.id.app_name)
        val lastResult = requireActivity().findViewById<TextView>(R.id.textView_history)
        appNameTextView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "game_history")
                .fallbackToDestructiveMigration()
                .build()
            val lastGame = withContext(Dispatchers.IO) { db.gameHistoryDao().getRecentHistory() }
            if (lastGame != null) {
                lastResult?.text = "победа за ${lastGame.winner}"
            } else {
                lastResult?.text = "История отсутствует"
            }
        }

        val buttonStartGame = view.findViewById<Button>(R.id.button_startgame)
        val spinnerGameMode = view.findViewById<Spinner>(R.id.spinner_game_mode)
        val spinnerDifficulty = view.findViewById<Spinner>(R.id.spinner_difficulty)

        buttonStartGame.setOnClickListener {
            val selectedPosition = spinnerGameMode.selectedItemPosition
            val computerGame = (selectedPosition == 1) // Проверяем позицию
            val gameFragment = GameFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("computer_game", computerGame)
                    putString("difficulty", spinnerDifficulty.selectedItem.toString())
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, gameFragment)
                .addToBackStack(null)
                .commit()
            appNameTextView.visibility = View.GONE
        }

        view.findViewById<View>(R.id.button_history).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        val gameModesArray = resources.getStringArray(R.array.game_modes).toMutableList()
        val gameModesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            gameModesArray
        )
        gameModesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGameMode.adapter = gameModesAdapter

        val difficultyArray = resources.getStringArray(R.array.difficulty_levels).toMutableList()
        val difficultyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            difficultyArray
        )
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter

        spinnerGameMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                spinnerDifficulty.visibility =
                    if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
