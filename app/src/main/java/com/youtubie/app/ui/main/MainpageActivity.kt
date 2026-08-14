package com.youtubie.app.ui.main

import com.youtubie.app.ui.showApiKeyDialog
import com.youtubie.app.ui.home.HomeFragment
import com.youtubie.app.ui.history.HistoryFragment
import com.youtubie.app.ui.about.AboutActivity
import com.youtubie.app.util.PreferenceManager
import com.youtubie.app.R
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    companion object {
        /** Regex matching common YouTube URL patterns. */
        private val YOUTUBE_URL_REGEX = Regex(
            "(https?://)?(www\\.|m\\.)?(youtube\\.com/(watch\\?.*v=|shorts/)|youtu\\.be/)",
            RegexOption.IGNORE_CASE
        )
    }

    private lateinit var binding: MainpageBinding
    private val intentObj = Intent()

    /** Tracks the last clipboard text handled this session to avoid re-prompting. */
    private var lastSessionClipboardText: String? = null

    /** Stores a YouTube URL received via share intent, to be processed once the UI is ready. */
    private var pendingShareUrl: String? = null

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnBackPressed()
        initialize()
        initializeLogic()

        // Handle share intent if the activity was launched via ACTION_SEND
        handleShareIntent(intent)
    }

    /**
     * Called when the activity is re-launched while already running (singleTop).
     * Handles share intents arriving while the activity is in the foreground.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * Checks the clipboard for a YouTube link whenever the activity gains window focus.
     * This is the reliable way to read the clipboard on Android 10+.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return

        // If there is a pending share URL, process it now that we have focus
        val shareUrl = pendingShareUrl
        if (shareUrl != null) {
            pendingShareUrl = null
            pasteUrlToHomeFragment(shareUrl)
            return
        }

        // Only check clipboard if auto-paste is enabled
        if (!preferenceManager.isAutoClipboardEnabled()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return

        val clipText = clip.getItemAt(0).text?.toString()?.trim() ?: return
        if (!isYouTubeUrl(clipText)) return

        // Don't re-prompt for the same link within the same session
        if (clipText == lastSessionClipboardText) return
        if (clipText == preferenceManager.getLastClipboardText()) return

        lastSessionClipboardText = clipText
        preferenceManager.setLastClipboardText(clipText)

        if (preferenceManager.isDontAskClipboardAgain()) {
            // Silently paste and search
            pasteUrlToHomeFragment(clipText)
        } else {
            showClipboardDialog(clipText)
        }
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

    private fun initialize() {
        binding.navig.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewpager.currentItem = 0
                R.id.nav_history -> binding.viewpager.currentItem = 1
            }
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
            showPreferencesSheet()
        }
        
        i2.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.show()
    }

    /**
     * Extracts and processes a YouTube URL from a share intent.
     *
     * @param intent the incoming intent to inspect.
     */
    private fun handleShareIntent(intent: Intent) {
        if (Intent.ACTION_SEND != intent.action) return
        if ("text/plain" != intent.type) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return

        if (isYouTubeUrl(sharedText)) {
            // Store the URL to be processed once the UI has focus
            pendingShareUrl = sharedText
        } else {
            Toast.makeText(this, getString(R.string.toast_not_youtube_link), Toast.LENGTH_SHORT).show()
        }

        // Clear the action so it doesn't re-trigger on config changes
        intent.action = null
    }

    /**
     * Tests whether a string looks like a YouTube URL.
     *
     * @param text candidate string to check.
     * @return true when the string matches common YouTube URL patterns.
     */
    private fun isYouTubeUrl(text: String): Boolean {
        return YOUTUBE_URL_REGEX.containsMatchIn(text)
    }

    /**
     * Shows the clipboard detection dialog with Cancel/Paste buttons and a "Don't ask again" checkbox.
     *
     * @param clipText the YouTube URL found in the clipboard.
     */
    private fun showClipboardDialog(clipText: String) {
        val dialog = AlertDialog.Builder(this).create()
        val inflate = layoutInflater.inflate(R.layout.dialog_clipboard, null)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setView(inflate)

        val t3 = inflate.findViewById<TextView>(R.id.t3)
        val checkboxDontAsk = inflate.findViewById<CheckBox>(R.id.checkboxDontAsk)
        val b1 = inflate.findViewById<TextView>(R.id.b1)
        val b2 = inflate.findViewById<TextView>(R.id.b2)

        t3.text = clipText

        b1.setOnClickListener {
            dialog.dismiss()
        }

        b2.setOnClickListener {
            if (checkboxDontAsk.isChecked) {
                preferenceManager.setDontAskClipboardAgain(true)
            }
            dialog.dismiss()
            pasteUrlToHomeFragment(clipText)
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    /**
     * Navigates to the home tab, populates the search field, and triggers a search.
     *
     * @param url the YouTube URL to search.
     */
    private fun pasteUrlToHomeFragment(url: String) {
        // Switch to the home tab
        binding.viewpager.currentItem = 0
        binding.navig.menu.getItem(0).isChecked = true

        // Wait for the fragment to be ready, then set the URL and search
        binding.viewpager.post {
            val fragment = supportFragmentManager.findFragmentByTag("f0")
                ?: supportFragmentManager.fragments.firstOrNull { it is HomeFragment }
            if (fragment is HomeFragment) {
                fragment.pasteAndSearch(url)
            }
        }
    }

    /**
     * Shows the preferences bottom sheet with API key change and auto-paste toggle.
     */
    private fun showPreferencesSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_preferences, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)

        val b1 = bottomSheetView.findViewById<TextView>(R.id.b1)
        val linearToggle = bottomSheetView.findViewById<LinearLayout>(R.id.linearToggle)
        val toggleStatus = bottomSheetView.findViewById<TextView>(R.id.toggleStatus)
        val i2 = bottomSheetView.findViewById<ImageView>(R.id.i2)

        // Initialize toggle state
        updateToggleStatus(toggleStatus, preferenceManager.isAutoClipboardEnabled())

        b1.setOnClickListener {
            bottomSheetDialog.dismiss()
            showApiKeyDialog(prefill = preferenceManager.getApiKey(), cancelable = true) { apiKey ->
                preferenceManager.setApiKey(apiKey)
            }
        }

        linearToggle.setOnClickListener {
            val newState = !preferenceManager.isAutoClipboardEnabled()
            preferenceManager.setAutoClipboardEnabled(newState)
            if (!newState) {
                // If auto-paste is turned off, also reset "don't ask again"
                preferenceManager.setDontAskClipboardAgain(false)
            }
            updateToggleStatus(toggleStatus, newState)
        }

        i2.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.show()
    }

    /**
     * Updates the toggle status text and color for the auto-paste preference row.
     *
     * @param textView the status TextView to update.
     * @param enabled current toggle state.
     */
    private fun updateToggleStatus(textView: TextView, enabled: Boolean) {
        if (enabled) {
            textView.text = getString(R.string.pref_on)
            textView.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textView.text = getString(R.string.pref_off)
            textView.setTextColor(Color.parseColor("#F44336"))
        }
    }

    /**
     * Applies window/UI styling and tab synchronization.
     */
    private fun initializeLogic() {
        _ui()
        _viewPagerOnPageSelected()
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

    private fun _ui() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        @Suppress("DEPRECATION")
        window.getDecorView().systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
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
