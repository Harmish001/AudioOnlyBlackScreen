package com.audioonly.blackscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast

class UnlockActivity : Activity() {
    private var attempts = 0
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Temporarily pause the overlay so this unlock prompt is fully visible
        startService(Intent(this, BlackOverlayService::class.java).setAction(BlackOverlayService.ACTION_PAUSE))
        askPin()
    }

    private fun askPin() {
        val input = EditText(this).apply {
            hint = "Parent PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
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
            input.requestFocus()

            val unlockAction = {
                val saved = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                    .getString(MainActivity.KEY_PIN, null)
                val entered = input.text.toString()

                if (saved != null && entered == saved) {
                    unlocked = true
                    startService(Intent(this, BlackOverlayService::class.java).setAction(BlackOverlayService.ACTION_STOP))
                    dialog.dismiss()
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

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                unlockAction()
            }

            input.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    unlockAction()
                    true
                } else {
                    false
                }
            }
        }

        dialog.setOnCancelListener {
            finish()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!unlocked) {
            // Restore the black overlay if the user exited without unlocking
            startService(Intent(this, BlackOverlayService::class.java).setAction(BlackOverlayService.ACTION_RESUME))
        }
    }
}
