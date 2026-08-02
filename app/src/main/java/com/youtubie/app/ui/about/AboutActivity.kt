package com.youtubie.app.ui.about

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.youtubie.app.R
import com.youtubie.app.databinding.AboutActivityBinding
import com.youtubie.app.ui.intro.AboutInstructionsFragment
import com.youtubie.app.ui.intro.AboutTipsFragment
import com.youtubie.app.ui.intro.AboutInfoFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that hosts the app's about pages in a tabbed ViewPager.
 */
@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: AboutActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                )

        binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    )
        }
    }

    private fun setupUI() {
        binding.textviewMainTitle.text = getString(R.string.title_about_youtubie)

        binding.viewpagerAbout.adapter = AboutPagerAdapter(this)

        TabLayoutMediator(binding.tablayout, binding.viewpagerAbout) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_info)
                1 -> getString(R.string.tab_tips)
                2 -> getString(R.string.tab_instructions)
                else -> null
            }
        }.attach()
    }
}

/**
 * Supplies the static about fragments used by [AboutActivity].
 *
 * @param activity host activity used by [FragmentStateAdapter].
 */
class AboutPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    /**
     * Returns the number of pages shown in the about screen.
     *
     * @return always 3 for instructions, tips, and app info.
     */
    override fun getItemCount(): Int = 3

    /**
     * Creates the fragment for the requested about tab.
     *
     * @param position zero-based page index.
     * @return fragment matching the requested tab; defaults to instructions for invalid positions.
     */
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AboutInfoFragment()
            1 -> AboutTipsFragment()
            2 -> AboutInstructionsFragment()
            else -> AboutInstructionsFragment()
        }
    }
}
