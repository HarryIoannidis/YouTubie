package com.youtubie.app.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.youtubie.app.R
import com.youtubie.app.data.model.DownloadHistoryItem

/**
 * Adapter that renders persisted download history rows.
 *
 * @param context context used for inflating rows and loading thumbnails.
 * @param items initial history items displayed by the list.
 */
class HistoryAdapter(
    context: Context,
    private val items: List<DownloadHistoryItem>
) : ArrayAdapter<DownloadHistoryItem>(context, R.layout.download_history, items) {

    /**
     * Binds a download-history item to a recycled or newly inflated row view.
     *
     * @param position adapter position to render.
     * @param convertView recycled row supplied by the ListView, or null when a row must be inflated.
     * @param parent parent view group used for layout inflation.
     * @return row view populated with title, channel, date, format, and thumbnail.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.download_history, parent, false)
        val item = getItem(position) ?: return view

        val thumbnailView = view.findViewById<ImageView>(R.id.imageview1)
        val titleView = view.findViewById<TextView>(R.id.title)
        val channelView = view.findViewById<TextView>(R.id.channel)
        val dateView = view.findViewById<TextView>(R.id.date)
        val formatView = view.findViewById<TextView>(R.id.format)

        titleView.text = item.title
        channelView.text = item.channelTitle
        dateView.text = item.downloadDate
        formatView.text = item.format

        val rawUrl = item.thumbnailUrl
        val cleanUrl = if (rawUrl.contains("ytimg.com")) {
            rawUrl.replace("/sddefault.", "/mqdefault.")
                .replace("/hqdefault.", "/mqdefault.")
                .replace("/default.", "/mqdefault.")
        } else rawUrl

        Glide.with(context)
            .load(cleanUrl)
            .placeholder(R.drawable.youtube_thumbnail)
            .dontAnimate()
            .into(thumbnailView)

        return view
    }

    /**
     * Replaces the adapter contents and refreshes the attached ListView.
     *
     * @param newItems history items to display in newest-first order.
     */
    fun updateItems(newItems: List<DownloadHistoryItem>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }
}