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
        val notificationsViewModel =
            ViewModelProvider(this).get(MinigamesLayout::class.java)

        _binding = FragmentMinigamesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val InhaleExhale_bt = binding.inhaleExhale
        val WhackAMole_bt = binding.whackAMole

        InhaleExhale_bt.setOnClickListener() {
            val intent = Intent(this.activity, InhaleExhale_game::class.java)
            startActivity(intent)
        }

        WhackAMole_bt.setOnClickListener(){
            val intent = Intent(this.activity, WhackAMoleGame::class.java)
            startActivity(intent)
        }



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}