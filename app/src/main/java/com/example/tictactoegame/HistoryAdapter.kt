package com.example.tictactoegame

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(context: Context, private val items: List<GameHistory>)
    : ArrayAdapter<GameHistory>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false)
        val item = items[position]

        val textViewDate = view.findViewById<TextView>(R.id.textView_date)
        val textViewResult = view.findViewById<TextView>(R.id.textView_result)
        val textViewType = view.findViewById<TextView>(R.id.textView_type)

        val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        if (item.winner == "No history") {
            textViewResult.text = item.winner
            textViewDate.visibility = View.GONE
            textViewType.visibility = View.GONE
        } else {
            textViewDate.visibility = View.VISIBLE
            textViewType.visibility = View.VISIBLE
            textViewResult.text = item.winner.toString()
            textViewDate.text = df.format(item.timestamp)
            textViewType.text = item.type.toString()
        }

        return view
    }
}