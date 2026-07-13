package com.youtubie.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.youtubie.app.R
import com.youtubie.app.databinding.HistoryFragmentBinding
import com.youtubie.app.util.PreferenceManager
import com.youtubie.app.data.model.DownloadHistoryItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private var _binding: HistoryFragmentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HistoryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun setupUI() {
        binding.imageviewHistory.setOnClickListener {
            val history = preferenceManager.getHistory()
            if (history.isEmpty()) {
                Toast.makeText(requireContext(), "History is already empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showClearHistoryDialog()
        }

        binding.listviewHistory.setOnItemClickListener { parent, view, position, id ->
            val item = parent.getItemAtPosition(position) as DownloadHistoryItem
            showHistoryDetailDialog(item)
        }
    }

    private fun loadHistory() {
        val history = preferenceManager.getHistory()
        if (history.isEmpty()) {
            binding.linearHistory.visibility = View.GONE
            binding.linearError.visibility = View.VISIBLE
        } else {
            binding.linearHistory.visibility = View.VISIBLE
            binding.linearError.visibility = View.GONE
            
            val adapter = HistoryAdapter(requireContext(), history)
            binding.listviewHistory.adapter = adapter
        }
    }

    private fun showHistoryDetailDialog(item: DownloadHistoryItem) {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_history_detail, null)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setView(dialogView)

        val linearSearchResult = dialogView.findViewById<LinearLayout>(R.id.linearSearchResult)
        _RoundAndBorder(linearSearchResult, "#FAFAFA", 2.0, "#DDDDDD", 30.0)

        val thumbnailView = dialogView.findViewById<ImageView>(R.id.thumbnail)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val channelView = dialogView.findViewById<TextView>(R.id.channel)
        val dateView = dialogView.findViewById<TextView>(R.id.date)
        val formatView = dialogView.findViewById<TextView>(R.id.format)
        val closeBtn = dialogView.findViewById<TextView>(R.id.b1)

        titleView.text = item.title
        channelView.text = item.channelTitle
        
        // Use duration if available, otherwise show date
        if (!item.duration.isNullOrEmpty()) {
            dateView.text = item.duration
            val iconDate = dialogView.findViewById<ImageView>(R.id.imageviewDate)
            iconDate.setImageResource(R.drawable.ic_access_time_black)
        } else {
            dateView.text = item.downloadDate
        }
        
        formatView.text = item.format

        Glide.with(requireContext())
            .load(item.thumbnailUrl)
            .placeholder(R.drawable.youtube_thumbnail)
            .into(thumbnailView)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.setColor(android.graphics.Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), android.graphics.Color.parseColor(color2))
        view.background = gd
    }

    private fun showClearHistoryDialog() {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_small, null)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setView(dialogView)

        val t1 = dialogView.findViewById<TextView>(R.id.t1)
        val t2 = dialogView.findViewById<TextView>(R.id.t2)
        val b1 = dialogView.findViewById<TextView>(R.id.b1)
        val b2 = dialogView.findViewById<TextView>(R.id.b2)

        t1.text = "Clear History?"
        t2.text = "Are you sure you want to clear your download history?"
        b1.text = "Keep"
        b2.text = "Clear"

        b1.setOnClickListener {
            dialog.dismiss()
        }

        b2.setOnClickListener {
            preferenceManager.clearHistory()
            loadHistory()
            dialog.dismiss()
            Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
