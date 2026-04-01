package com.example.focuskey.ui.minigames

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.focuskey.databinding.FragmentMinigamesBinding


class MinigamesActivity : Fragment() {

    private var _binding: FragmentMinigamesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinigamesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val inhaleExhale = binding.inhaleExhale
        val whackAMole = binding.whackAMole
        val memoryGame = binding.memoryGame

        inhaleExhale.setOnClickListener() {
            val intent = Intent(this.activity, InhaleExhale_game::class.java)
            startActivity(intent)
        }

        whackAMole.setOnClickListener(){
            val intent = Intent(this.activity, WhackAMoleGame::class.java)
            startActivity(intent)
        }

        memoryGame.setOnClickListener() {
            val intent = Intent(this.activity, MemoryGameActivity::class.java)
            startActivity(intent)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}