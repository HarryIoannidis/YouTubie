package com.youtubie.app.ui.about
import com.youtubie.app.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.youtubie.app.databinding.AboutActivityBinding
import com.youtubie.app.ui.intro.IntroFragment1
import com.youtubie.app.ui.intro.IntroFragment2
import com.youtubie.app.ui.intro.IntroFragment3
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: AboutActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }

    private fun setupUI() {
        binding.textviewMainTitle.text = "About YouTubie"
        
        binding.viewpagerAbout.adapter = AboutPagerAdapter(this)
        
        TabLayoutMediator(binding.tablayout, binding.viewpagerAbout) { tab, position ->
            tab.text = when (position) {
                0 -> "Instructions"
                1 -> "Tips"
                2 -> "Info"
                else -> null
            }
        }.attach()
    }
}

class AboutPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IntroFragment1()
            1 -> IntroFragment2()
            2 -> IntroFragment3()
            else -> IntroFragment1()
        }
    }
}
