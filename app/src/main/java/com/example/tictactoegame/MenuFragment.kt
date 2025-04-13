package com.example.tictactoegame

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appname = requireActivity().findViewById<TextView>(R.id.app_name)
        appname.visibility = View.VISIBLE
        // Set up click listeners for buttons here
        view.findViewById<View>(R.id.button_startgame).setOnClickListener {
            // Navigate to GameFragment
            parentFragmentManager.beginTransaction()
//                .setCustomAnimations(
//                    R.anim.slide_in_right,
//                    R.anim.slide_out_left,
//                    R.anim.slide_in_right,
//                    R.anim.slide_out_left
//                )
                .replace(R.id.frame_layout, GameFragment())
                .addToBackStack(null)
                .commit()
            appname.visibility = View.GONE

        }

        view.findViewById<View>(R.id.button_history).setOnClickListener {
            // Navigate to HistoryFragment
            parentFragmentManager.beginTransaction()
//                .setCustomAnimations(
//                    R.anim.slide_in_right,
//                    R.anim.slide_out_left,
//                    R.anim.slide_in_right,
//                    R.anim.slide_out_left
//                )
                .replace(R.id.frame_layout, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }
    }



}