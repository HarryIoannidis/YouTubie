package com.youtubie.app.ui.home
import com.youtubie.app.R

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
import com.youtubie.app.data.download.DownloadWorker
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.content.Context
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    
    @Inject
    lateinit var repository: YoutubeRepository

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
            hideKeyboard()
            val url = binding.searchEditText.text.toString()
            if (url.isNotEmpty()) {
                viewModel.fetchVideoMetadata(url)
            } else {
                Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imageClear.setOnClickListener {
            binding.searchEditText.setText("")
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                val url = binding.searchEditText.text.toString()
                if (url.isNotEmpty()) {
                    viewModel.fetchVideoMetadata(url)
                }
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
            hideKeyboard()
            val url = binding.searchEditText.text.toString()
            if (url.isNotEmpty()) {
                viewModel.fetchVideoMetadata(url)
            }
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

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

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

    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.setColor(android.graphics.Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), android.graphics.Color.parseColor(color2))
        view.background = gd
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

    private fun checkStoragePermissionAndStart(metadata: VideoInfoResponse, url: String, formatType: String) {
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
            "thumbnailUrl" to (metadata.thumbnails?.lastOrNull()?.url ?: ""),
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

    private fun fetchAndStartDownload(videoId: String, formatType: String) {
        if (formatType == "Audio") {
            binding.progressbarAudio.visibility = View.VISIBLE
            binding.linearAudio.isEnabled = false
        } else {
            binding.progressbarVideo.visibility = View.VISIBLE
            binding.linearVideo.isEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getDownloadUrl(videoId).onSuccess { downloadResponse ->
                if (formatType == "Audio") {
                    binding.progressbarAudio.visibility = View.GONE
                    binding.linearAudio.isEnabled = true
                    val audioUrl = audioFormatUrl(downloadResponse)
                    if (audioUrl != null) {
                        checkStoragePermissionAndStart(downloadResponse, audioUrl, "Audio")
                    } else {
                        Toast.makeText(requireContext(), "No audio format available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.progressbarVideo.visibility = View.GONE
                    binding.linearVideo.isEnabled = true
                    val videoUrl = videoFormatUrl(downloadResponse)
                    if (videoUrl != null) {
                        checkStoragePermissionAndStart(downloadResponse, videoUrl, "Video")
                    } else {
                        Toast.makeText(requireContext(), "No video format available", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure { error ->
                if (formatType == "Audio") {
                    binding.progressbarAudio.visibility = View.GONE
                    binding.linearAudio.isEnabled = true
                } else {
                    binding.progressbarVideo.visibility = View.GONE
                    binding.linearVideo.isEnabled = true
                }
                Toast.makeText(requireContext(), "Failed to get download URL: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            Toast.makeText(requireContext(), "Download cancelled", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

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
    private fun View.fadeIn(duration: Long = 150) {
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(duration).start()
    }

    private fun View.fadeOut(duration: Long = 150, onEnd: () -> Unit = {}) {
        animate().alpha(0f).setDuration(duration)
            .withEndAction { visibility = View.GONE; onEnd() }
            .start()
    }
}
