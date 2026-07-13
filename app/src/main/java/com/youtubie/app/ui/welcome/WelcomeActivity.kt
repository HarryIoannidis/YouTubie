package com.youtubie.app.ui.welcome

import com.youtubie.app.R
import com.youtubie.app.ui.main.MainpageActivity
import com.youtubie.app.ui.intro.IntroFragment1
import com.youtubie.app.ui.intro.IntroFragment2
import com.youtubie.app.ui.intro.IntroFragment3
import com.youtubie.app.util.PreferenceManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.youtubie.app.databinding.WelcomeBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: WelcomeBinding
    private var current: Double = 0.0
    private lateinit var fragAdapter: FragFragmentAdapter
    private val intentObj = Intent()
    private lateinit var vibr: Vibrator

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Storage permission helps with downloading files.", Toast.LENGTH_SHORT).show()
        }
        // Proceed to main app regardless
        navigateToMainPage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        vibr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fragAdapter = FragFragmentAdapter(this, supportFragmentManager)
        binding.viewpager1.adapter = fragAdapter

        binding.viewpager1.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                current = position.toDouble()
                updateIndicators(position)
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.textviewButton.setOnClickListener {
            if (current == 2.0) {
                preferenceManager.setFirstLaunch(false)
                requestStoragePermissionAndNavigate()
            } else {
                binding.viewpager1.currentItem = (current + 1).toInt()
            }
        }
    }

    private fun requestStoragePermissionAndNavigate() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(permission)
                return
            }
        }
        // Already granted or not needed (API 29+)
        navigateToMainPage()
    }

    private fun navigateToMainPage() {
        intentObj.setClass(applicationContext, MainpageActivity::class.java)
        startActivity(intentObj)
        finish()
    }

    private fun updateIndicators(position: Int) {
        binding.imageview1.setImageResource(if (position == 0) R.drawable.ic_brightness_1_black else R.drawable.ic_panorama_fisheye_black)
        binding.imageview2.setImageResource(if (position == 1) R.drawable.ic_brightness_1_black else R.drawable.ic_panorama_fisheye_black)
        binding.imageview3.setImageResource(if (position == 2) R.drawable.ic_brightness_1_black else R.drawable.ic_panorama_fisheye_black)
        
        binding.textviewButton.text = if (position == 2) "Get Started" else "Next"
    }

    private fun initializeLogic() {
        binding.viewpager1.setPageTransformer(true, ZoomOutPageTransformer())
    }

    private inner class FragFragmentAdapter(context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 3
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> IntroFragment1()
                1 -> IntroFragment2()
                2 -> IntroFragment3()
                else -> IntroFragment1()
            }
        }
    }

    private inner class ZoomOutPageTransformer : ViewPager.PageTransformer {
        private val MIN_SCALE = 0.85f
        private val MIN_ALPHA = 0.5f

        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width
            val pageHeight = view.height

            when {
                position < -1 -> view.alpha = 0f
                position <= 1 -> {
                    val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    if (position < 0) {
                        view.translationX = horzMargin - vertMargin / 2
                    } else {
                        view.translationX = -horzMargin + vertMargin / 2
                    }
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.alpha = MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA)
                }
                else -> view.alpha = 0f
            }
        }
    }
}

