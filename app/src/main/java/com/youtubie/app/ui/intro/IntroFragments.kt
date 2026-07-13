package com.youtubie.app.ui.intro
import com.youtubie.app.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.youtubie.app.databinding.Fragment1FragmentBinding
import com.youtubie.app.databinding.Fragment2FragmentBinding
import com.youtubie.app.databinding.Fragment3FragmentBinding
import com.youtubie.app.databinding.FragmentAboutInstructionsBinding
import com.youtubie.app.databinding.FragmentAboutTipsBinding

class IntroFragment1 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment1FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

class IntroFragment2 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment2FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

class IntroFragment3 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment3FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

class AboutInstructionsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAboutInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }
}

class AboutTipsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAboutTipsBinding.inflate(inflater, container, false)
        return binding.root
    }
}
