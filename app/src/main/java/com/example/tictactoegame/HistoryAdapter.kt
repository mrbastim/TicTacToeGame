package com.example.tictactoegame

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class HistoryAdapter(context: Context, private val items: List<GameHistory>)
    : ArrayAdapter<GameHistory>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false)
        val item = items[position]

        val textViewDate = view.findViewById<TextView>(R.id.textView_date)
        val textViewResult = view.findViewById<TextView>(R.id.textView_result)

        textViewDate.text = item.timestamp.toString()
        textViewResult.text = item.winner.toString()

        return view
    }
}