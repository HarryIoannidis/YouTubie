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
        _rippleRoundStroke(linearBG, "#FFFFFF", "#EEEEEE", 20.0, 2.0, "EEEEEE")

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

    private fun _rippleRoundStroke(view: View, focus: String, pressed: String, round: Double, stroke: Double, strokeclr: String) {
        val GG = android.graphics.drawable.GradientDrawable()
        GG.setColor(android.graphics.Color.parseColor(focus))
        GG.cornerRadius = round.toFloat()
        GG.setStroke(stroke.toInt(), android.graphics.Color.parseColor("#" + strokeclr.replace("#", "")))
        val RE = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList(arrayOf(intArrayOf()), intArrayOf(android.graphics.Color.parseColor(pressed))),
            GG,
            null
        )
        view.background = RE
    }
}
