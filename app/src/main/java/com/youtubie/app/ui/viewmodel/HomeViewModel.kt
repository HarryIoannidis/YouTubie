package com.youtubie.app.ui.viewmodel
import com.youtubie.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubie.app.data.model.VideoInfoResponse
import com.youtubie.app.data.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YoutubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState

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

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val metadata: VideoInfoResponse) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
