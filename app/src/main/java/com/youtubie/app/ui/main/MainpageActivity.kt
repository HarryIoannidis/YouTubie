package com.youtubie.app.ui.main

import com.youtubie.app.R
import com.youtubie.app.ui.home.HomeFragment
import com.youtubie.app.ui.history.HistoryFragment
import com.youtubie.app.ui.about.AboutActivity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.youtubie.app.databinding.MainpageBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainpageActivity : AppCompatActivity() {

    private lateinit var binding: MainpageBinding
    private val intentObj = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainpageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupOnBackPressed()
        initialize()
        initializeLogic()
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showQuitDialog()
            }
        })
    }

    private fun showQuitDialog() {
        val dialog1 = AlertDialog.Builder(this).create()
        val inflate = layoutInflater.inflate(R.layout.dialog_small, null)
        dialog1.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog1.setView(inflate)
        
        val t1 = inflate.findViewById<TextView>(R.id.t1)
        val t2 = inflate.findViewById<TextView>(R.id.t2)
        val b1 = inflate.findViewById<TextView>(R.id.b1)
        val b2 = inflate.findViewById<TextView>(R.id.b2)
        val bg = inflate.findViewById<LinearLayout>(R.id.bg)
        
        t1.text = "Leave?"
        t2.text = "Are you sure you want to quit the app? Your current search will not be saved."
        b1.text = "Stay"
        b2.text = "Quit"
        
        b1.setOnClickListener {
            dialog1.dismiss()
        }
        b2.setOnClickListener {
            finish()
        }
        dialog1.setCancelable(true)
        dialog1.show()
    }

    private fun initialize() {
        binding.navig.setOnNavigationItemSelectedListener { item ->
            binding.viewpager.currentItem = item.itemId
            true
        }

        binding.fab.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.new_version, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)

        val b1 = bottomSheetView.findViewById<TextView>(R.id.b1)
        val b2 = bottomSheetView.findViewById<TextView>(R.id.b2)
        val i2 = bottomSheetView.findViewById<ImageView>(R.id.i2)
        val bg2 = bottomSheetView.findViewById<LinearLayout>(R.id.bg2)

        _RoundAndBorder(bg2, "#FFFFFF", 0.0, "#000000", 25.0)
        _rippleRoundStroke(b1, "#FFFFFF", "#EEEEEE", 15.0, 2.5, "#EEEEEE")
        _rippleRoundStroke(b2, "#FFFFFF", "#EEEEEE", 15.0, 2.5, "#EEEEEE")
        _rippleRoundStroke(i2, "#FFFFFF", "#40000000", 90.0, 0.0, "#FFFFFF")
        
        i2.setImageResource(R.drawable.cross)
        
        b1.setOnClickListener {
            intentObj.setClass(applicationContext, AboutActivity::class.java)
            startActivity(intentObj)
        }
        
        i2.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.show()
    }

    private fun initializeLogic() {
        _ui()
        _viewPagerOnPageSelected()
    }

    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = GradientDrawable()
        gd.setColor(Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), Color.parseColor(color2))
        view.background = gd
    }

    private fun _rippleRoundStroke(view: View, focus: String, pressed: String, round: Double, stroke: Double, strokeclr: String) {
        val GG = GradientDrawable()
        GG.setColor(Color.parseColor(focus))
        GG.cornerRadius = round.toFloat()
        GG.setStroke(stroke.toInt(), Color.parseColor("#" + strokeclr.replace("#", "")))
        val RE = RippleDrawable(ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor(pressed))), GG, null)
        view.background = RE
    }

    private fun _viewPagerOnPageSelected() {
        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.navig.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun _ui() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        @Suppress("DEPRECATION")
        window.getDecorView().systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        binding.fab.background = GradientDrawable().apply {
            setStroke(10, Color.BLACK)
            setColor(Color.RED)
        }
        
        binding.navig.elevation = 0f
        binding.navig.itemIconSize = 36
        binding.fab.setImageResource(R.drawable.menu_burger)
        binding.navig.menu.clear()
        binding.navig.menu.add(0, 0, 0, "Home").setIcon(R.drawable.home)
        binding.navig.menu.add(0, 1, 0, "History").setIcon(R.drawable.time_past)

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(Color.BLACK, Color.GRAY)
        )
        binding.navig.itemTextColor = colorStateList
        binding.navig.itemIconTintList = colorStateList
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.navig.setItemBackgroundResource(R.drawable.selector_ripple_item)
        }
        binding.viewpager.adapter = PagerAdapter(this)
    }

    class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> HistoryFragment()
                else -> HomeFragment()
            }
        }

        override fun getItemCount(): Int = 2
    }
}
