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
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.download_history, parent, false).also { v ->
            val linearBG = v.findViewById<android.widget.LinearLayout>(R.id.linearBG)
            _rippleRoundStroke(linearBG, "#FFFFFF", "#EEEEEE", 20.0, 2.0, "EEEEEE")
        }
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

    /**
     * Applies a rounded background with ripple feedback and an outline clip.
     *
     * @param view target view to style.
     * @param focus background color used at rest.
     * @param pressed ripple color used during touch feedback.
     * @param round corner radius in pixels.
     * @param stroke border width in pixels.
     * @param strokeclr border color, with or without a leading #.
     */
    private fun _rippleRoundStroke(view: View, focus: String, pressed: String, round: Double, stroke: Double, strokeclr: String) {
        val GG = android.graphics.drawable.GradientDrawable()
        GG.setColor(android.graphics.Color.parseColor(focus))
        GG.cornerRadius = round.toFloat()
        GG.setStroke(stroke.toInt(), android.graphics.Color.parseColor("#" + strokeclr.replace("#", "")))

        val mask = android.graphics.drawable.GradientDrawable()
        mask.setColor(android.graphics.Color.WHITE)
        mask.cornerRadius = round.toFloat()

        val RE = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList(arrayOf(intArrayOf()), intArrayOf(android.graphics.Color.parseColor(pressed))),
            GG,
            mask
        )
        view.background = RE
        view.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, round.toFloat())
            }
        }
        view.clipToOutline = true
    }
}
