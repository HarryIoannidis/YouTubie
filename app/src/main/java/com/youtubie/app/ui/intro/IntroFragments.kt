package com.youtubie.app.ui.intro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.youtubie.app.databinding.Fragment1FragmentBinding
import com.youtubie.app.databinding.Fragment2FragmentBinding
import com.youtubie.app.databinding.Fragment3FragmentBinding
import com.youtubie.app.databinding.FragmentAboutInfoBinding
import com.youtubie.app.databinding.FragmentAboutInstructionsBinding
import com.youtubie.app.databinding.FragmentAboutTipsBinding

/**
 * First onboarding page shown during the initial launch flow.
 */
class IntroFragment1 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment1FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

/**
 * Second onboarding page shown during the initial launch flow.
 */
class IntroFragment2 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment2FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

/**
 * Third onboarding page reused by onboarding and the about screen.
 */
class IntroFragment3 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = Fragment3FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}

/**
 * Static instructions page shown from the about screen.
 */
class AboutInstructionsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAboutInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }
}

/**
 * Static tips page shown from the about screen.
 */
class AboutTipsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAboutTipsBinding.inflate(inflater, container, false)
        return binding.root
    }
}

/**
 * Info page shown from the about screen with app description, developer links, and license.
 */
class AboutInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAboutInfoBinding.inflate(inflater, container, false)

        binding.linkGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HarryIoannidis")))
        }

        binding.linkLinkedin.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/harry-ioannidis-hafa/")))
        }

        return binding.root
    }
}
