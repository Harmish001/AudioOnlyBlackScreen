package com.audioonly.blackscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BlackOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlay: View? = null
    private var isOverlayPaused: Boolean = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlay()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseOverlay()
            }
            ACTION_RESUME -> {
                resumeOverlay()
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showOverlay()
            }
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        if (overlay != null) {
            resumeOverlay()
            return
        }

        val isTv = DeviceUtils.isTv(this)

        // Gesture detector for double-tap unhide (mobile and touch devices)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                initiateUnlock()
                return true
            }
        })

        // Root overlay container
        val root = object : FrameLayout(this) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(ev)
                super.dispatchTouchEvent(ev)
                // Always consume touches so underlying apps/videos cannot be tapped accidentally
                return true
            }

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (isTv) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP,
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        KeyEvent.KEYCODE_VOLUME_MUTE -> {
                            // Do not consume volume keys so user can adjust TV audio volume
                            return false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (event.action == KeyEvent.ACTION_UP) {
                                initiateUnlock()
                                return true
                            }
                            return true
                        }
                    }
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = isTv
            isFocusableInTouchMode = isTv
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            contentDescription = if (isTv) {
                "Black screen active. Click the cross button to unhide."
            } else {
                "Black screen active. Double tap anywhere to unhide."
            }
        }

        // On TV: add a remote-navigable Cross Button in the safe top-corner zone
        if (isTv) {
            val tvCrossContainer = createTvCrossButton()
            root.addView(tvCrossContainer)
            root.post {
                tvCrossContainer.findViewById<View>(ID_CROSS_BUTTON)?.requestFocus()
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var windowFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (!isTv) {
            // On mobile, keep it not focusable so phone hardware volume buttons operate freely
            windowFlags = windowFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            windowFlags,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(root, params)
            overlay = root
            isOverlayPaused = false
        } catch (_: Exception) {
            // Some TV firmware blocks application overlays; notification remains available.
        }
    }

    private fun createTvCrossButton(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(40)
                marginEnd = dp(40)
            }
            layoutParams = lp
        }

        val crossBtn = FrameLayout(this).apply {
            id = ID_CROSS_BUTTON
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            contentDescription = "Unhide screen"

            // Keep TV D-pad focus locked on the button
            nextFocusDownId = ID_CROSS_BUTTON
            nextFocusUpId = ID_CROSS_BUTTON
            nextFocusLeftId = ID_CROSS_BUTTON
            nextFocusRightId = ID_CROSS_BUTTON

            val btnSize = dp(56)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)

            val normalBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#44222222"))
                setStroke(dp(2), Color.parseColor("#AAFFFFFF"))
            }

            val focusedBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D7FF64"))
                setStroke(dp(3), Color.WHITE)
            }

            background = normalBg

            val crossIcon = TextView(this@BlackOverlayService).apply {
                text = "✕"
                textSize = 26f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(
                crossIcon,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            val label = TextView(this@BlackOverlayService).apply {
                text = "Unhide"
                textSize = 13f
                setTextColor(Color.parseColor("#B0FFFFFF"))
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    background = focusedBg
                    crossIcon.setTextColor(Color.BLACK)
                    label.setTextColor(Color.parseColor("#D7FF64"))
                    animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                } else {
                    background = normalBg
                    crossIcon.setTextColor(Color.WHITE)
                    label.setTextColor(Color.parseColor("#B0FFFFFF"))
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
            }

            setOnClickListener {
                initiateUnlock()
            }

            container.addView(this)
            container.addView(label)
        }

        return container
    }

    private fun initiateUnlock() {
        if (isOverlayPaused) return
        val savedPin = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
            .getString(MainActivity.KEY_PIN, null)

        if (savedPin.isNullOrEmpty()) {
            stopOverlay()
        } else {
            pauseOverlay()
            val unlockIntent = Intent(this, UnlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(unlockIntent)
        }
    }

    private fun pauseOverlay() {
        isOverlayPaused = true
        overlay?.visibility = View.INVISIBLE
    }

    private fun resumeOverlay() {
        isOverlayPaused = false
        overlay?.visibility = View.VISIBLE
        if (DeviceUtils.isTv(this)) {
            overlay?.findViewById<View>(ID_CROSS_BUTTON)?.requestFocus()
        }
    }

    private fun stopOverlay() {
        overlay?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        overlay = null
        isOverlayPaused = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val unlockIntent = Intent(this, UnlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            this,
            2,
            unlockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("AudioOnly is active")
            .setContentText("Screen is black. Tap to enter the parent PIN and restore video.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Black screen", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Controls for the AudioOnly black-screen overlay"
                }
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        overlay?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlay = null
        isOverlayPaused = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.audioonly.blackscreen.START"
        const val ACTION_STOP = "com.audioonly.blackscreen.STOP"
        const val ACTION_PAUSE = "com.audioonly.blackscreen.PAUSE"
        const val ACTION_RESUME = "com.audioonly.blackscreen.RESUME"
        private const val CHANNEL_ID = "audioonly_overlay"
        private const val NOTIFICATION_ID = 7421
        const val ID_CROSS_BUTTON = 10001
    }
}
