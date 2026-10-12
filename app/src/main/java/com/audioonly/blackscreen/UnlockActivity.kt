package com.audioonly.blackscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast

class UnlockActivity : Activity() {
    private var attempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        askPin()
    }

    private fun askPin() {
        val input = EditText(this).apply {
            hint = "Parent PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Restore video")
            .setMessage("Enter the parent PIN to turn off the black screen.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Unlock", null)
            .setNegativeButton("Keep black screen") { _, _ -> finish() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val saved = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).getString(MainActivity.KEY_PIN, null)
                if (saved != null && input.text.toString() == saved) {
                    startService(Intent(this, BlackOverlayService::class.java).setAction(BlackOverlayService.ACTION_STOP))
                    finish()
                } else {
                    attempts++
                    input.error = "Incorrect PIN"
                    if (attempts >= 5) {
                        Toast.makeText(this, "Too many attempts. Black screen remains active.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        finish()
                    }
                }
            }
        }
        dialog.show()
    }
}
