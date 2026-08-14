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
import com.youtubie.app.databinding.HistoryFragmentBinding
import com.youtubie.app.util.PreferenceManager
import com.youtubie.app.data.model.DownloadHistoryItem
import com.youtubie.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment that displays completed downloads and allows users to inspect or clear history.
 */
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

    private var historyAdapter: HistoryAdapter? = null

    /**
     * Loads saved download history into the list and toggles the empty-state view.
     */
    private fun loadHistory() {
        val history = preferenceManager.getHistory()
        if (history.isEmpty()) {
            binding.linearHistory.visibility = View.GONE
            binding.linearError.visibility = View.VISIBLE
            historyAdapter = null
            binding.listviewHistory.adapter = null
        } else {
            binding.linearHistory.visibility = View.VISIBLE
            binding.linearError.visibility = View.GONE
            
            if (historyAdapter == null || binding.listviewHistory.adapter == null) {
                historyAdapter = HistoryAdapter(requireContext(), history.toMutableList())
                binding.listviewHistory.adapter = historyAdapter
            } else {
                historyAdapter?.updateItems(history)
            }
        }
    }

    /**
     * Shows a read-only detail dialog for a selected history item.
     *
     * @param item history item whose metadata and thumbnail should be displayed.
     */
    private fun showHistoryDetailDialog(item: DownloadHistoryItem) {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_history_detail, null)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setView(dialogView)

        val thumbnailView = dialogView.findViewById<ImageView>(R.id.thumbnail)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val channelView = dialogView.findViewById<TextView>(R.id.channel)
        val dateView = dialogView.findViewById<TextView>(R.id.date)
        val formatView = dialogView.findViewById<TextView>(R.id.format)
        val closeBtn = dialogView.findViewById<TextView>(R.id.b1)

        titleView.text = item.title
        channelView.text = item.channelTitle
        
        dateView.text = item.downloadDate
        val iconDate = dialogView.findViewById<ImageView>(R.id.imageviewDate)
        iconDate?.setImageResource(R.drawable.ic_access_time)
        
        formatView.text = item.format
        val formatIconView = dialogView.findViewById<ImageView>(R.id.imageviewFormat)
        val iconRes = when {
            item.format.contains("audio", ignoreCase = true) || item.format.contains("mp3", ignoreCase = true) -> R.drawable.mp3_file
            item.format.contains("image", ignoreCase = true) || item.format.contains("png", ignoreCase = true) -> R.drawable.png_file
            else -> R.drawable.mp4_file
        }
        formatIconView?.setImageResource(iconRes)

        val rawUrl = item.thumbnailUrl
        val cleanUrl = if (rawUrl.contains("ytimg.com")) {
            rawUrl.replace("/sddefault.", "/mqdefault.")
                .replace("/hqdefault.", "/mqdefault.")
                .replace("/default.", "/mqdefault.")
        } else rawUrl

        Glide.with(requireContext())
            .load(cleanUrl)
            .placeholder(R.drawable.youtube_thumbnail)
            .into(thumbnailView)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    /**
     * Confirms and performs deletion of all persisted download-history items.
     */
    private fun showClearHistoryDialog() {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_small, null)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setView(dialogView)

        val t1 = dialogView.findViewById<TextView>(R.id.t1)
        val t2 = dialogView.findViewById<TextView>(R.id.t2)
        val b1 = dialogView.findViewById<TextView>(R.id.b1)
        val b2 = dialogView.findViewById<TextView>(R.id.b2)

        t1.text = getString(R.string.clear_history_title)
        t2.text = getString(R.string.clear_history_confirm)
        b1.text = getString(R.string.action_keep)
        b2.text = getString(R.string.action_clear)

        b1.setOnClickListener {
            dialog.dismiss()
        }

        b2.setOnClickListener {
            preferenceManager.clearHistory()
            loadHistory()
            dialog.dismiss()
            Toast.makeText(requireContext(), getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
