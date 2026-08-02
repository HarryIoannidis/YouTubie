package com.youtubie.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubie.app.data.model.VideoInfoResponse
import com.youtubie.app.data.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the home screen search flow.
 *
 * @param repository repository used to resolve YouTube metadata.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YoutubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState

    /**
     * Parses a YouTube URL and fetches metadata for the extracted video ID.
     *
     * @param url raw YouTube URL entered by the user.
     */
    fun fetchVideoMetadata(url: String) {
        val videoId = extractVideoId(url)
        if (videoId == null) {
            _uiState.value = HomeUiState.Error("Invalid YouTube URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            repository.getVideoMetadata(videoId).onSuccess { metadata ->
                _uiState.value = HomeUiState.Success(metadata)
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    /**
     * Extracts a video ID from supported YouTube URL shapes.
     *
     * @param url raw YouTube URL entered by the user.
     * @return video ID when the URL contains a supported pattern, otherwise null.
     */
    private fun extractVideoId(url: String): String? {
        // Basic extractor for now, can be improved
        return if (url.contains("v=")) {
            url.split("v=")[1].split("&")[0]
        } else if (url.contains("youtu.be/")) {
            url.split("youtu.be/")[1].split("?")[0]
        } else {
            null
        }
    }
}

/**
 * UI state emitted by [HomeViewModel] and rendered by the home screen.
 */
sealed class HomeUiState {
    /**
     * No request is active and no result is displayed.
     */
    object Idle : HomeUiState()

    /**
     * Metadata request is in progress.
     */
    object Loading : HomeUiState()

    /**
     * Metadata request completed successfully.
     *
     * @property metadata video metadata returned by the repository.
     */
    data class Success(val metadata: VideoInfoResponse) : HomeUiState()

    /**
     * Metadata request failed or the submitted URL was invalid.
     *
     * @property message user-visible error text.
     */
    data class Error(val message: String) : HomeUiState()
}
