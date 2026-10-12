package com.audioonly.blackscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        if (prefs.getString(KEY_PIN, null) == null) {
            showSetPinDialog()
        }
        render()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
            setBackgroundColor(android.graphics.Color.rgb(10, 10, 10))
        }
        root.addView(TextView(this).apply {
            text = "AudioOnly"
            textSize = 34f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }, match())
        root.addView(TextView(this).apply {
            text = "Let the music play. Hide the video."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, dp(12), 0, dp(28))
        }, match())
        root.addView(button("Start black screen") { startBlackScreen() }, match())
        root.addView(button("Turn off black screen (PIN)") { openUnlock() }, match())
        root.addView(button("Allow overlay permission") { requestOverlayPermission() }, match())
        root.addView(TextView(this).apply {
            text = "How to use\n1. Start music in your usual app.\n2. Return here and start the black screen.\n3. Use the persistent notification or reopen this app to unlock with your parent PIN.\n\nTV note: overlay permission availability varies by TV brand."
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, dp(24), 0, 0)
        }, match())
        setContentView(root)
    }

    private fun startBlackScreen() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow overlay permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        val intent = Intent(this, BlackOverlayService::class.java).setAction(BlackOverlayService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Black screen activated", Toast.LENGTH_SHORT).show()
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is already enabled", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openUnlock() {
        startActivity(Intent(this, UnlockActivity::class.java))
    }

    private fun showSetPinDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Create a 4–8 digit parent PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Set parent PIN")
            .setMessage("Choose a PIN that your child will not know.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val pin = input.text.toString()
                        if (pin.length !in 4..8 || pin.any { !it.isDigit() }) {
                            input.error = "Enter 4–8 digits"
                        } else {
                            prefs.edit().putString(KEY_PIN, pin).apply()
                            dialog.dismiss()
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 16f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply {
            bottomMargin = dp(10)
        }
    }

    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "audio_only_prefs"
        const val KEY_PIN = "parent_pin"
    }
}
