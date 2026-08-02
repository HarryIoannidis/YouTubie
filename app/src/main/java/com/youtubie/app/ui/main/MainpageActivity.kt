package com.youtubie.app.ui.main

import com.youtubie.app.ui.showApiKeyDialog
import com.youtubie.app.ui.home.HomeFragment
import com.youtubie.app.ui.history.HistoryFragment
import com.youtubie.app.ui.about.AboutActivity
import com.youtubie.app.util.PreferenceManager
import com.youtubie.app.R
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import javax.inject.Inject
import java.util.*

/**
 * Main activity that hosts the home and history tabs and shared overflow actions.
 */
@AndroidEntryPoint
class MainpageActivity : AppCompatActivity() {


    private lateinit var binding: MainpageBinding
    private val intentObj = Intent()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainpageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupOnBackPressed()
        initialize()
        initializeLogic()
    }

    /**
     * Registers the custom back-press behavior that asks for confirmation before exiting.
     */
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showQuitDialog()
            }
        })
    }

    /**
     * Displays the confirmation dialog used before closing the app.
     */
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
        
        t1.text = getString(R.string.quit_dialog_title)
        t2.text = getString(R.string.quit_dialog_desc)
        b1.text = getString(R.string.action_stay)
        b2.text = getString(R.string.action_quit)
        
        b1.setOnClickListener {
            dialog1.dismiss()
        }
        b2.setOnClickListener {
            finish()
        }
        dialog1.setCancelable(true)
        dialog1.show()
    }

    /**
     * Wires bottom navigation and the floating action button.
     */
    private fun initialize() {
        binding.navig.setOnNavigationItemSelectedListener { item ->
            binding.viewpager.currentItem = item.itemId
            true
        }

        binding.fab.setOnClickListener {
            showBottomSheet()
        }
    }

    /**
     * Shows the overflow bottom sheet for about and GitHub actions.
     */
    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.new_version, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)

        val b1 = bottomSheetView.findViewById<TextView>(R.id.b1)
        val b2 = bottomSheetView.findViewById<TextView>(R.id.b2)
        val b3 = bottomSheetView.findViewById<TextView>(R.id.b3)
        val i2 = bottomSheetView.findViewById<ImageView>(R.id.i2)
        val bg2 = bottomSheetView.findViewById<LinearLayout>(R.id.bg2)

        _RoundAndBorder(bg2, "#FFFFFF", 0.0, "#000000", 25.0)
        _rippleRoundStroke(b1, "#FFFFFF", "#EEEEEE", 15.0, 2.5, "#EEEEEE")
        _rippleRoundStroke(b2, "#FFFFFF", "#EEEEEE", 15.0, 2.5, "#EEEEEE")
        _rippleRoundStroke(b3, "#FFFFFF", "#EEEEEE", 15.0, 2.5, "#EEEEEE")
        _rippleRoundStroke(i2, "#FFFFFF", "#40000000", 90.0, 0.0, "#FFFFFF")
        
        i2.setImageResource(R.drawable.cross)
        
        b1.setOnClickListener {
            bottomSheetDialog.dismiss()
            intentObj.setClass(applicationContext, AboutActivity::class.java)
            startActivity(intentObj)
        }

        b2.setOnClickListener {
            bottomSheetDialog.dismiss()
            val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HarryIoannidis"))
            startActivity(githubIntent)
        }

        b3.setOnClickListener {
            bottomSheetDialog.dismiss()
            showApiKeyDialog(prefill = preferenceManager.getApiKey(), cancelable = true) { apiKey ->
                preferenceManager.setApiKey(apiKey)
            }
        }
        
        i2.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.show()
    }

    /**
     * Applies window/UI styling and tab synchronization.
     */
    private fun initializeLogic() {
        _ui()
        _viewPagerOnPageSelected()
    }

    /**
     * Applies a solid rounded rectangle background with a border.
     *
     * @param view target view to style.
     * @param color1 fill color.
     * @param border border width in pixels.
     * @param color2 border color.
     * @param round corner radius in pixels.
     */
    private fun _RoundAndBorder(view: View, color1: String, border: Double, color2: String, round: Double) {
        val gd = GradientDrawable()
        gd.setColor(Color.parseColor(color1))
        gd.cornerRadius = round.toFloat()
        gd.setStroke(border.toInt(), Color.parseColor(color2))
        view.background = gd
    }

    /**
     * Applies a rounded background with ripple feedback and outline clipping.
     *
     * @param view target view to style.
     * @param focus background color used at rest.
     * @param pressed ripple color used during touch feedback.
     * @param round corner radius in pixels.
     * @param stroke border width in pixels.
     * @param strokeclr border color, with or without a leading #.
     */
    private fun _rippleRoundStroke(view: View, focus: String, pressed: String, round: Double, stroke: Double, strokeclr: String) {
        val GG = GradientDrawable()
        GG.setColor(Color.parseColor(focus))
        GG.cornerRadius = round.toFloat()
        GG.setStroke(stroke.toInt(), Color.parseColor("#" + strokeclr.replace("#", "")))
        val mask = GradientDrawable()
        mask.setColor(Color.WHITE)
        mask.cornerRadius = round.toFloat()
        val RE = RippleDrawable(ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor(pressed))), GG, mask)
        view.background = RE
        view.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, round.toFloat())
            }
        }
        view.clipToOutline = true
    }

    /**
     * Keeps the bottom-navigation selection synchronized with ViewPager page changes.
     */
    private fun _viewPagerOnPageSelected() {
        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.navig.menu.getItem(position).isChecked = true
            }
        })
    }

    /**
     * Applies immersive window flags, navigation styling, and ViewPager setup.
     */
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
        binding.navig.menu.add(0, 0, 0, getString(R.string.nav_home)).setIcon(R.drawable.home)
        binding.navig.menu.add(0, 1, 0, getString(R.string.nav_history)).setIcon(R.drawable.time_past)

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

    /**
     * Supplies the main tabs hosted by [MainpageActivity].
     *
     * @param fragmentActivity host activity used by [FragmentStateAdapter].
     */
    class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        /**
         * Creates the fragment for a main tab.
         *
         * @param position zero-based tab index.
         * @return [HomeFragment] for index 0, [HistoryFragment] for index 1, and [HomeFragment] as fallback.
         */
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> HistoryFragment()
                else -> HomeFragment()
            }
        }

        /**
         * Returns the number of main tabs.
         *
         * @return always 2 for home and history.
         */
        override fun getItemCount(): Int = 2
    }
}
