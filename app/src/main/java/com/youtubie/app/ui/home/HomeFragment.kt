package com.youtubie.app.ui.home
import com.youtubie.app.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.youtubie.app.databinding.HomeFragmentBinding
import com.youtubie.app.ui.viewmodel.HomeUiState
import com.youtubie.app.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

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
        binding.linearSearch.isClickable = true
        binding.linearSearch.setOnClickListener {
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
        
        binding.linearSearchAgain.setOnClickListener {
            binding.linearForSearch.visibility = View.VISIBLE
            binding.linearResult.visibility = View.GONE
            binding.linearError.visibility = View.GONE
            binding.searchEditText.setText("")
        }

        binding.linearRefresh.setOnClickListener {
            val url = binding.searchEditText.text.toString()
            if (url.isNotEmpty()) {
                viewModel.fetchVideoMetadata(url)
            }
        }
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
                            binding.linearForSearch.visibility = View.GONE
                            binding.linearResult.visibility = View.VISIBLE
                            binding.linearError.visibility = View.GONE
                            
                            val metadata = state.metadata
                            binding.textviewTitle.text = metadata.title ?: "No Title"
                            binding.textviewChannel.text = metadata.channelTitle ?: "Unknown Channel"
                            binding.textviewViews.text = metadata.viewCount ?: "0"
                            binding.textviewDuration.text = metadata.duration ?: "0:00"
                            
                            val thumbnailUrl = metadata.thumbnails?.lastOrNull()?.url
                            Glide.with(this@HomeFragment)
                                .load(thumbnailUrl)
                                .placeholder(R.drawable.youtube_thumbnail)
                                .into(binding.thumbnail)
                        }
                        is HomeUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.textviewSearch.visibility = View.VISIBLE
                            binding.linearForSearch.visibility = View.GONE
                            binding.linearResult.visibility = View.GONE
                            binding.linearError.visibility = View.VISIBLE
                            binding.errorText.text = state.message
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
