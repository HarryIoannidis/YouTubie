package com.youtubie.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.youtubie.app.databinding.MainBinding
import com.youtubie.app.ui.welcome.WelcomeActivity
import com.youtubie.app.util.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding
    
    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Splash screen logic
        Handler(Looper.getMainLooper()).postDelayed({
            val targetActivity = if (preferenceManager.isFirstLaunch()) {
                WelcomeActivity::class.java
            } else {
                MainpageActivity::class.java
            }
            
            val intent = Intent(this, targetActivity)
            startActivity(intent)
            finish()
        }, 2000)
    }
}
