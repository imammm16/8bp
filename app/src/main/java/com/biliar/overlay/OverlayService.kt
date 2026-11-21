package com.biliar.overlay

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Base64
import android.util.DisplayMetrics
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.TextView
import java.io.ByteArrayOutputStream

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private val h = Handler(Looper.getMainLooper())
    private var web: WebView? = null
    private var btn: TextView? = null
    private var mp: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private var sw = 0
    private var sh = 0

    override fun onBind(i: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startFg()
        val data = i?.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        mp = getSystemService(MediaProjectionManager::class.java).getMediaProjection(i.getIntExtra("code", 0), data)
        mp!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, h)
        val dm = DisplayMetrics(); wm.defaultDisplay.getRealMetrics(dm)
        sw = dm.widthPixels; sh = dm.heightPixels
        reader = ImageReader.newInstance(sw, sh, PixelFormat.RGBA_8888, 2)
        vd = mp!!.createVirtualDisplay("cap", sw, sh, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, h)
        addWeb(); addButton()
        return START_NOT_STICKY
    }

    private fun startFg() {
        val ch = "ovl"
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(ch, "Overlay", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, ch).setContentTitle("Biliar Overlay aktif").setSmallIcon(android.R.drawable.ic_menu_compass).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1, n)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun addWeb() {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT)
        web = WebView(this).apply {
            setBackgroundColor(0)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(object { @JavascriptInterface fun ok() = true }, "Android")
            loadUrl("file:///android_asset/index.html")
        }
        wm.addView(web, lp)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addButton() {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 240 }
        btn = TextView(this).apply {
            text = "◎"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            setBackgroundColor(0x88000000.toInt()); setPadding(26, 10, 26, 10)
        }
        var dx = 0f; var dy = 0f; var sx = 0; var sy = 0; var moved = false; var t0 = 0L
        btn!!.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; sx = lp.x; sy = lp.y; moved = false; t0 = e.eventTime }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawX - dx) > 12 || Math.abs(e.rawY - dy) > 12) moved = true
                    if (moved) { lp.x = sx + (e.rawX - dx).toInt(); lp.y = sy + (e.rawY - dy).toInt(); wm.updateViewLayout(btn, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) { if (e.eventTime - t0 > 900) stopSelf() else scan() }
            }
            true
        }
        wm.addView(btn, lp)
    }

    private fun scan() {
        web?.visibility = View.INVISIBLE; btn?.visibility = View.INVISIBLE
        h.postDelayed({
            val b64 = grab()
            web?.visibility = View.VISIBLE; btn?.visibility = View.VISIBLE
            if (b64 != null) web?.evaluateJavascript("window.onFrame('data:image/jpeg;base64,$b64')", null)
        }, 300)
    }

    private fun grab(): String? {
        val img = reader?.acquireLatestImage() ?: return null
        try {
            val p = img.planes[0]
            val rowPad = p.rowStride - p.pixelStride * sw
            val bmp = Bitmap.createBitmap(sw + rowPad / p.pixelStride, sh, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(p.buffer)
            val crop = Bitmap.createBitmap(bmp, 0, 0, sw, sh)
            val k = minOf(1f, 1280f / sw)
            val out = if (k < 1f) Bitmap.createScaledBitmap(crop, (sw * k).toInt(), (sh * k).toInt(), true) else crop
            val bo = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 85, bo)
            return Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
        } finally { img.close() }
    }

    override fun onDestroy() {
        web?.let { wm.removeView(it); it.destroy() }
        btn?.let { wm.removeView(it) }
        vd?.release(); reader?.close(); mp?.stop()
        super.onDestroy()
    }
}
