package com.youtubie.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.youtubie.app.R

private const val RAPID_API_PAGE_URL = "https://rapidapi.com/ytjar/api/yt-api"

/**
 * Shows the shared RapidAPI-key dialog used during onboarding and from the main screen.
 *
 * @param prefill optional key displayed in the input field.
 * @param cancelable whether the dialog can be dismissed without saving a key.
 * @param onDone called with the validated, trimmed key after the dialog is dismissed.
 */
fun AppCompatActivity.showApiKeyDialog(
    prefill: String? = null,
    cancelable: Boolean,
    onDone: (String) -> Unit
) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_api_key, null)
    val dialog = AlertDialog.Builder(this).create()
    val apiKeyInput = dialogView.findViewById<EditText>(R.id.apiKeyInput)
    val getApiKeyButton = dialogView.findViewById<TextView>(R.id.b1)
    val doneButton = dialogView.findViewById<TextView>(R.id.b2)

    apiKeyInput.setText(prefill.orEmpty())
    apiKeyInput.setSelection(apiKeyInput.text.length)

    getApiKeyButton.setOnClickListener {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RAPID_API_PAGE_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.toast_no_browser), Toast.LENGTH_SHORT).show()
        }
    }

    doneButton.setOnClickListener {
        val apiKey = apiKeyInput.text.toString().trim()
        if (apiKey.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_enter_api_key), Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        dialog.dismiss()
        onDone(apiKey)
    }

    dialog.setView(dialogView)
    dialog.setCancelable(cancelable)
    dialog.setCanceledOnTouchOutside(cancelable)
    dialog.show()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
}
