package com.youtubie.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.youtubie.app.databinding.HomeFragmentBinding
import com.youtubie.app.ui.viewmodel.HomeUiState
import com.youtubie.app.ui.viewmodel.HomeViewModel
import com.youtubie.app.data.repository.YoutubeRepository
import com.youtubie.app.data.model.VideoInfoResponse
import com.youtubie.app.data.model.VideoFormat
import com.youtubie.app.data.model.AdaptiveVideoFormat
import com.youtubie.app.data.download.DownloadWorker
import com.youtubie.app.R
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.content.Context
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Main search and download screen.
 *
 * The fragment resolves YouTube metadata, lets the user choose audio/video/image downloads,
 * manages runtime permissions, and observes WorkManager progress.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    
    @Inject
    lateinit var repository: YoutubeRepository

    @Inject
    lateinit var preferenceManager: com.youtubie.app.util.PreferenceManager

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

    // Track active download for re-showing progress dialog
    private var activeDownloadWorkId: UUID? = null
    private var activeDownloadTitle: String? = null
    private var isDownloadToastShown = false

    // Pending download info for permission flow
    private var pendingDownloadMetadata: VideoInfoResponse? = null
    private var pendingDownloadUrl: String? = null
    private var pendingDownloadFormatType: String? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continue flow regardless of notification permission result
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Auto-resume the pending download
            val metadata = pendingDownloadMetadata
            val url = pendingDownloadUrl
            val formatType = pendingDownloadFormatType
            if (metadata != null && url != null && formatType != null) {
                performStartDownload(metadata, url, formatType)
            }
        } else {
            Toast.makeText(requireContext(), "Storage permission is required to download files.", Toast.LENGTH_SHORT).show()
        }
        pendingDownloadMetadata = null
        pendingDownloadUrl = null
        pendingDownloadFormatType = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeState()
    }

    /**
     * Initializes static styling and click listeners for searching, result actions, and dialogs.
     */
    private fun setupUI() {
        binding.progressbarAudio.visibility = View.GONE
        binding.progressbarVideo.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.progressbarRefresh.visibility = View.GONE

        // Apply card and pill styles programmatically to match design
        _RoundAndBorder(binding.linearForSearchMain, "#FAFAFA", 2.0, "#DDDDDD", 30.0)
        _RoundAndBorder(binding.linearSearchResult, "#FAFAFA", 2.0, "#DDDDDD", 30.0)
        _RoundAndBorder(binding.linearEditText, "#FFFFFF", 2.0, "#CCCCCC", 90.0)
        _rippleRoundStroke(binding.linearSearch, "#FAFAFA", "#EEEEEE", 45.0, 2.0, "#DDDDDD")
        _rippleRoundStroke(binding.linearRefresh, "#FAFAFA", "#EEEEEE", 45.0, 2.0, "#DDDDDD")
        _rippleRoundStroke(binding.linearSearchAgain, "#FAFAFA", "#EEEEEE", 45.0, 2.0, "#DDDDDD")

        // Reduce thumbnail radius as requested
        binding.cardview.radius = 20f

        // Style the tips and instructions links as underlined black text
        binding.textviewTips.paintFlags = binding.textviewTips.paintFlags.or(android.graphics.Paint.UNDERLINE_TEXT_FLAG)
        binding.textviewInstructions.paintFlags = binding.textviewInstructions.paintFlags.or(android.graphics.Paint.UNDERLINE_TEXT_FLAG)

        binding.linearSearch.isClickable = true
        binding.linearSearch.setOnClickListener {
            val url = binding.searchEditText.text.toString()
            performSearch(url)
        }

        binding.imageClear.setOnClickListener {
            showSearchHistoryBottomSheet()
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val url = binding.searchEditText.text.toString()
                performSearch(url)
                true
            } else {
                false
            }
        }

        binding.linearSearchAgain.setOnClickListener {
            binding.linearResult.fadeOut {
                binding.linearForSearch.fadeIn()
            }
            binding.linearError.visibility = View.GONE
            binding.searchEditText.setText("")
        }

        binding.linearRefresh.setOnClickListener {
            val url = binding.searchEditText.text.toString()
            performSearch(url)
        }

        binding.textviewTips.setOnClickListener {
            showTipsDialog()
        }

        binding.textviewInstructions.setOnClickListener {
            showInstructionsDialog()
        }

        binding.linearAudio.setOnClickListener {
            // If a download is active, re-show the progress dialog
            val activeId = activeDownloadWorkId
            val activeTitle = activeDownloadTitle
            if (activeId != null && activeTitle != null) {
                showDownloadProgressDialog(activeId, activeTitle)
                return@setOnClickListener
            }
            val state = viewModel.uiState.value
            if (state is HomeUiState.Success) {
                val metadata = state.metadata
                val audioUrl = audioFormatUrl(metadata)
                if (audioUrl != null) {
                    checkStoragePermissionAndStart(metadata, audioUrl, "Audio")
                } else {
                    fetchAndStartDownload(metadata.id ?: "", "Audio")
                }
            }
        }

        binding.linearVideo.setOnClickListener {
            // If a download is active, re-show the progress dialog
            val activeId = activeDownloadWorkId
            val activeTitle = activeDownloadTitle
            if (activeId != null && activeTitle != null) {
                showDownloadProgressDialog(activeId, activeTitle)
                return@setOnClickListener
            }
            val state = viewModel.uiState.value
            if (state is HomeUiState.Success) {
                val metadata = state.metadata
                val videoUrl = videoFormatUrl(metadata)
                if (videoUrl != null) {
                    checkStoragePermissionAndStart(metadata, videoUrl, "Video")
                } else {
                    fetchAndStartDownload(metadata.id ?: "", "Video")
                }
            }
        }

        binding.thumbnail.setOnLongClickListener {
            val state = viewModel.uiState.value
            if (state is HomeUiState.Success) {
                val metadata = state.metadata
                val thumbnailUrl = metadata.thumbnails?.lastOrNull()?.url
                if (thumbnailUrl != null) {
                    checkStoragePermissionAndStart(metadata, thumbnailUrl, "Image")
                }
            }
            true
        }
    }

    /**
     * Hides the soft keyboard after the user submits a search.
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    /**
     * Selects the best available audio URL from adaptive formats, falling back to progressive formats.
     *
     * @param metadata API metadata response containing media formats.
     * @return direct audio URL when available, otherwise a progressive fallback URL, or null.
     */
    private fun audioFormatUrl(metadata: VideoInfoResponse): String? {
        val adaptiveAudio = metadata.adaptiveFormats?.filter {
            it.mimeType?.contains("audio") == true
        }?.maxByOrNull {
            when (it.audioQuality) {
                "AUDIO_QUALITY_HIGH" -> 3
                "AUDIO_QUALITY_MEDIUM" -> 2
                "AUDIO_QUALITY_LOW" -> 1
                else -> 0
            }
        }
        if (adaptiveAudio?.url != null) return adaptiveAudio.url
        return metadata.formats?.firstOrNull()?.url
    }

    /**
     * Selects the highest labeled progressive video URL available in the metadata.
     *
     * @param metadata API metadata response containing media formats.
     * @return direct video URL, first available fallback URL, or null when no format has a URL.
     */
    private fun videoFormatUrl(metadata: VideoInfoResponse): String? {
        val progressiveVideo = metadata.formats?.maxByOrNull {
            when (it.qualityLabel) {
                "1080p" -> 1080
                "720p" -> 720
                "480p" -> 480
                "360p" -> 360
                "240p" -> 240
                "144p" -> 144
                else -> 0
            }
        }
        return progressiveVideo?.url ?: metadata.formats?.firstOrNull()?.url
    }

    /**
     * Applies a solid rounded rectangle background with a border.
     *
     * @param view target view to style.
     * @param color1 fill color.
     * @param border border width in pixels.
     * @param color2 border color.
     * @param round corner radius in pixels.
     */
    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.setColor(android.graphics.Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), android.graphics.Color.parseColor(color2))
        view.background = gd
    }

    /**
     * Applies a rounded background with ripple feedback and outline clipping.
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

    /**
     * Requests any needed runtime permissions before starting a download.
     *
     * @param metadata video metadata used to name and record the download.
     * @param url direct URL to download.
     * @param formatType asset type requested by the user, such as Audio, Video, or Image.
     */
    private fun checkStoragePermissionAndStart(metadata: VideoInfoResponse, url: String, formatType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Store pending download info so we can auto-resume on grant
                pendingDownloadMetadata = metadata
                pendingDownloadUrl = url
                pendingDownloadFormatType = formatType
                storagePermissionLauncher.launch(permission)
                return
            }
        }
        performStartDownload(metadata, url, formatType)
    }

    /**
     * Builds and enqueues the WorkManager request that performs a download.
     *
     * @param metadata video metadata saved into the worker input data.
     * @param url direct media or image URL to download.
     * @param formatType asset type requested by the user, such as Audio, Video, or Image.
     */
    private fun performStartDownload(metadata: VideoInfoResponse, url: String, formatType: String) {
        val extension = when (formatType) {
            "Audio" -> ".mp3"
            "Video" -> ".mp4"
            else -> ".png"
        }
        val mimeType = when (formatType) {
            "Audio" -> "audio/mpeg"
            "Video" -> "video/mp4"
            else -> "image/png"
        }
        val title = metadata.title ?: "download"
        val fileName = "${title.replace(Regex("[\\\\/:*?\"<>|]"), "_")}$extension"

        val workInputData = androidx.work.workDataOf(
            "url" to url,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "videoId" to (metadata.id ?: ""),
            "title" to title,
            "channelTitle" to (metadata.channelTitle ?: "Unknown"),
            "thumbnailUrl" to run {
                val rawUrl = metadata.thumbnails?.lastOrNull()?.url ?: ""
                if (rawUrl.contains("ytimg.com")) {
                    rawUrl.replace("/sddefault.", "/mqdefault.")
                        .replace("/hqdefault.", "/mqdefault.")
                        .replace("/default.", "/mqdefault.")
                } else rawUrl
            },
            "formatType" to formatType,
            "viewCount" to (metadata.viewCount ?: "0"),
            "duration" to formatDuration(metadata.durationSeconds, metadata.durationText)
        )

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workInputData)
            .build()

        val workManager = androidx.work.WorkManager.getInstance(requireContext())
        workManager.enqueue(workRequest)

        activeDownloadWorkId = workRequest.id
        activeDownloadTitle = title
        showDownloadProgressDialog(workRequest.id, title)
    }

    /**
     * Fetches fresh download URLs when the metadata response does not contain a usable URL.
     *
     * @param videoId YouTube video identifier.
     * @param formatType asset type requested by the user, such as Audio or Video.
     */
    private fun fetchAndStartDownload(videoId: String, formatType: String) {
        if (formatType == "Audio") {
            binding.progressbarAudio.visibility = View.VISIBLE
            binding.linearAudio.isEnabled = false
        } else {
            binding.progressbarVideo.visibility = View.VISIBLE
            binding.linearVideo.isEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            var validUrl: String? = null
            var lastResponse: VideoInfoResponse? = null
            var lastErrorMessage: String? = null

            // Try up to 2 attempts
            for (attempt in 1..2) {
                val result = repository.getDownloadUrl(videoId)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response != null) {
                        lastResponse = response
                        val url = if (formatType == "Audio") audioFormatUrl(response) else videoFormatUrl(response)
                        if (url != null) {
                            validUrl = url
                            break // Success!
                        }
                    }
                } else {
                    lastErrorMessage = result.exceptionOrNull()?.message
                }

                // If first attempt failed to return valid URL, wait briefly before retrying
                if (attempt == 1) {
                    kotlinx.coroutines.delay(500)
                }
            }

            if (formatType == "Audio") {
                binding.progressbarAudio.visibility = View.GONE
                binding.linearAudio.isEnabled = true
            } else {
                binding.progressbarVideo.visibility = View.GONE
                binding.linearVideo.isEnabled = true
            }

            if (validUrl != null && lastResponse != null) {
                checkStoragePermissionAndStart(lastResponse, validUrl, formatType)
            } else {
                val errorMsg = lastErrorMessage ?: "Unable to obtain a valid $formatType download link after 2 attempts."
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Displays the static tips dialog.
     */
    private fun showTipsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tips, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.b1).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * Displays the static instructions dialog.
     */
    private fun showInstructionsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_instructions, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.b1).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * Shows a modal progress dialog for an active WorkManager download.
     *
     * @param workId ID of the enqueued download work.
     * @param title video title displayed in the progress dialog.
     */
    private fun showDownloadProgressDialog(workId: java.util.UUID, title: String) {
        isDownloadToastShown = false
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val t1 = dialogView.findViewById<TextView>(R.id.t1)
        val t2 = dialogView.findViewById<TextView>(R.id.t2)
        val progressbar = dialogView.findViewById<ProgressBar>(R.id.progressbar)
        val b1 = dialogView.findViewById<TextView>(R.id.b1)
        val b2 = dialogView.findViewById<TextView>(R.id.b2)

        t1.text = "Downloading $title"
        t2.text = "Starting download...\nProgress: 0%"
        progressbar.max = 100
        progressbar.progress = 0

        val workManager = androidx.work.WorkManager.getInstance(requireContext())
        workManager.getWorkInfoByIdLiveData(workId).observe(viewLifecycleOwner) { workInfo ->
            if (workInfo != null) {
                val progress = workInfo.progress.getInt("progress", 0)
                progressbar.progress = progress
                t2.text = "Progress: $progress%"

                if (workInfo.state.isFinished && !isDownloadToastShown) {
                    isDownloadToastShown = true
                    activeDownloadWorkId = null
                    activeDownloadTitle = null
                    if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(requireContext(), "Download complete!", Toast.LENGTH_SHORT).show()
                    } else if (workInfo.state == androidx.work.WorkInfo.State.FAILED) {
                        Toast.makeText(requireContext(), "Download failed!", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
            }
        }

        b1.setOnClickListener {
            Toast.makeText(requireContext(), "Download continuing in background", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        b2.setOnClickListener {
            activeDownloadWorkId = null
            activeDownloadTitle = null
            workManager.cancelWorkById(workId)
            dialog.dismiss()
            Toast.makeText(requireContext(), getString(R.string.download_cancelled), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    /**
     * Observes [HomeUiState] and renders loading, success, and error states.
     */
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.textviewSearch.visibility = View.VISIBLE
                            binding.linearResult.visibility = View.GONE
                            binding.linearError.visibility = View.GONE
                        }
                        is HomeUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.textviewSearch.visibility = View.GONE
                            binding.linearError.visibility = View.GONE
                        }
                        is HomeUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.textviewSearch.visibility = View.VISIBLE
                            binding.linearError.visibility = View.GONE
                            
                            // Fade out search, fade in result
                            binding.linearForSearch.fadeOut {
                                val metadata = state.metadata
                                val searchedUrl = binding.searchEditText.text.toString().trim()
                                if (searchedUrl.isNotEmpty() && !metadata.title.isNullOrEmpty()) {
                                    preferenceManager.updateSearchTitle(searchedUrl, metadata.title)
                                }
                                binding.textviewTitle.text = metadata.title ?: "No Title"
                                binding.textviewChannel.text = metadata.channelTitle ?: "Unknown Channel"
                                binding.textviewViews.text = metadata.viewCount ?: "0"
                                binding.textviewDuration.text = formatDuration(metadata.durationSeconds, metadata.durationText)
                                
                                val thumbnailUrl = metadata.thumbnails?.lastOrNull()?.url
                                Glide.with(this@HomeFragment)
                                    .load(thumbnailUrl)
                                    .placeholder(R.drawable.youtube_thumbnail)
                                    .into(binding.thumbnail)
                                
                                binding.linearResult.fadeIn()
                            }
                        }
                        is HomeUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.textviewSearch.visibility = View.VISIBLE
                            binding.linearForSearch.fadeOut {
                                binding.linearError.fadeIn()
                            }
                            binding.linearResult.visibility = View.GONE
                            binding.errorText.text = state.message
                        }
                    }
                }
            }
        }
    }

    /**
     * Formats a duration from either preformatted text or a raw seconds value.
     *
     * @param secondsStr duration in seconds as returned by the API.
     * @param text preformatted duration text returned by the API.
     * @return preformatted text when present, otherwise an m:ss or h:mm:ss string.
     */
    private fun formatDuration(secondsStr: String?, text: String?): String {
        if (!text.isNullOrEmpty()) return text
        val seconds = secondsStr?.toLongOrNull() ?: 0L
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Fade animation utilities
    /**
     * Fades a view from transparent to visible.
     *
     * @param duration animation duration in milliseconds.
     */
    private fun View.fadeIn(duration: Long = 150) {
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(duration).start()
    }

    /**
     * Fades a view out and hides it after the animation finishes.
     *
     * @param duration animation duration in milliseconds.
     * @param onEnd callback invoked after the view is hidden.
     */
    private fun View.fadeOut(duration: Long = 150, onEnd: () -> Unit = {}) {
        animate().alpha(0f).setDuration(duration)
            .withEndAction { visibility = View.GONE; onEnd() }
            .start()
    }

    /**
     * Validates and submits a search URL, saving it to recent searches first.
     *
     * @param url raw text entered by the user.
     */
    private fun performSearch(url: String) {
        hideKeyboard()
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) {
            preferenceManager.addSearchQuery(trimmed)
            viewModel.fetchVideoMetadata(trimmed)
        } else {
            Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Public entry point called by [MainpageActivity] to paste a URL and auto-search.
     * Used for clipboard auto-paste and share intent flows.
     *
     * @param url YouTube URL to search.
     */
    fun pasteAndSearch(url: String) {
        if (_binding == null) return
        binding.searchEditText.setText(url)
        performSearch(url)
    }

    /**
     * Displays saved search history and lets the user rerun or clear previous searches.
     */
    private fun showSearchHistoryBottomSheet() {
        val searchHistory = preferenceManager.getSearchHistory()

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_search_history, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val bg2 = bottomSheetView.findViewById<LinearLayout>(R.id.bg2)
        _RoundAndBorder(bg2, "#FFFFFF", 0.0, "#000000", 25.0)

        val listview = bottomSheetView.findViewById<ListView>(R.id.listviewSearchHistory)
        val textviewEmpty = bottomSheetView.findViewById<TextView>(R.id.textviewEmpty)
        val imageClearHistory = bottomSheetView.findViewById<ImageView>(R.id.imageClearHistory)
        val imageClose = bottomSheetView.findViewById<ImageView>(R.id.imageClose)

        // Limit maximum height to half the screen height
        val maxScreenHeight = resources.displayMetrics.heightPixels / 2
        val bottomSheetInternal = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheetInternal?.setBackgroundResource(android.R.color.transparent)

        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                behavior.maxHeight = maxScreenHeight
                behavior.peekHeight = maxScreenHeight
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            }
        }

        if (searchHistory.isEmpty()) {
            listview.visibility = View.GONE
            textviewEmpty.visibility = View.VISIBLE
        } else {
            listview.visibility = View.VISIBLE
            textviewEmpty.visibility = View.GONE

            val adapter = object : ArrayAdapter<com.youtubie.app.data.model.SearchHistoryItem>(requireContext(), R.layout.item_search_history, searchHistory) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false)
                    val item = getItem(position)
                    val tvQuery = view.findViewById<TextView>(R.id.textviewQuery)
                    val tvTitle = view.findViewById<TextView>(R.id.textviewTitle)

                    if (item != null) {
                        tvQuery.text = item.query
                        if (!item.title.isNullOrEmpty()) {
                            tvTitle.text = item.title
                            tvTitle.visibility = View.VISIBLE
                        } else {
                            tvTitle.visibility = View.GONE
                        }
                    }
                    return view
                }
            }
            listview.adapter = adapter

            listview.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = searchHistory[position]
                binding.searchEditText.setText(selectedItem.query)
                bottomSheetDialog.dismiss()
                performSearch(selectedItem.query)
            }
        }

        imageClearHistory.setOnClickListener {
            preferenceManager.clearSearchHistory()
            bottomSheetDialog.dismiss()
            Toast.makeText(requireContext(), "Search history cleared", Toast.LENGTH_SHORT).show()
        }

        imageClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
}
