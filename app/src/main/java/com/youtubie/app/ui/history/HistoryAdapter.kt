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

class HistoryAdapter(
    context: Context,
    private val items: List<DownloadHistoryItem>
) : ArrayAdapter<DownloadHistoryItem>(context, R.layout.download_history, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.download_history, parent, false)
        val item = getItem(position) ?: return view

        val linearBG = view.findViewById<android.widget.LinearLayout>(R.id.linearBG)
        _RoundAndBorder(linearBG, "#FFFFFF", 2.0, "#EEEEEE", 20.0)

        val thumbnailView = view.findViewById<ImageView>(R.id.imageview1)
        val titleView = view.findViewById<TextView>(R.id.title)
        val channelView = view.findViewById<TextView>(R.id.channel)
        val dateView = view.findViewById<TextView>(R.id.date)
        val formatView = view.findViewById<TextView>(R.id.format)

        titleView.text = item.title
        channelView.text = item.channelTitle
        dateView.text = item.downloadDate
        formatView.text = item.format

        Glide.with(context)
            .load(item.thumbnailUrl)
            .placeholder(R.drawable.youtube_thumbnail)
            .into(thumbnailView)

        return view
    }

    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.setColor(android.graphics.Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), android.graphics.Color.parseColor(color2))
        view.background = gd
    }
}
