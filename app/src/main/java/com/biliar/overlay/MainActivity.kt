package com.biliar.overlay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val rcOverlay = 1
    private val rcCapture = 2

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        val tv = TextView(this).apply {
            text = "Biliar Overlay\n\n1. Tekan Mulai, izinkan \"Tampil di atas aplikasi lain\".\n" +
                "2. Izinkan tangkap layar.\n3. Buka game, tekan tombol ◎ yang melayang untuk memindai meja.\n" +
                "Tahan tombol ◎ lama untuk menghentikan."
            setPadding(48, 96, 48, 48)
        }
        val btn = Button(this).apply { text = "Mulai"; setOnClickListener { start() } }
        setContentView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(tv); addView(btn) })
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), rcOverlay)
            return
        }
        val mpm = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(mpm.createScreenCaptureIntent(), rcCapture)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data)
        if (rc == rcOverlay) start()
        else if (rc == rcCapture && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, OverlayService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}
