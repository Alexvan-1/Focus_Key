package com.example.focuskey.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.focuskey.R
import com.example.focuskey.data.Session
import com.example.focuskey.data.SessionStorage

class HistoryActivity : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var storage: SessionStorage
    private lateinit var emptyTextView: TextView
    private lateinit var sessions: List<Session>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = SessionStorage(requireContext())
        recyclerView = view.findViewById(R.id.history_recycler_view)
        emptyTextView = view.findViewById(R.id.empty_history_text)

        sessions = storage.loadSessions()
        adapter = HistoryAdapter(sessions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        updateVisibility(sessions.isEmpty())
    }

    private fun updateVisibility(isEmpty: Boolean) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(storage.loadSessions())
        adapter.updateData(sessions)
        updateVisibility(sessions.isEmpty())
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "${mins} мин"
            mins == 0 -> "${hours} ч"
            else -> "${hours} ч ${mins} мин"
        }
    }

    inner class HistoryAdapter(private var sessions: List<Session>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            holder.numberTextView.text = "Сессия №${sessions.size - position}"
            holder.dateTextView.text = session.startDate
            holder.timeTextView.text = session.startTime
            holder.durationTextView.text = formatDuration(session.durationMinutes)
            holder.tagTextView.text = session.tag
            holder.statusTextView.text = session.status
            val iconText = if (session.status == "Завершена") "✓" else "✗"
            holder.statusIconTextView.text = iconText
            holder.statusIconTextView.setTextColor(
                if (session.status == "Завершена")
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                else
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
            )

            fun updateData(newSessions: List<Session>) {
                sessions = newSessions
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = sessions.size

        fun updateData(newSessions: List<Session>) {
            sessions = newSessions
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val numberTextView: TextView = itemView.findViewById(R.id.item_number)
            val dateTextView: TextView = itemView.findViewById(R.id.item_date)
            val timeTextView: TextView = itemView.findViewById(R.id.item_time)
            val durationTextView: TextView = itemView.findViewById(R.id.item_duration)
            val tagTextView: TextView = itemView.findViewById(R.id.item_tag)
            val statusTextView: TextView = itemView.findViewById(R.id.item_status)
            val statusIconTextView: TextView = itemView.findViewById(R.id.item_status_icon)
        }
    }
}