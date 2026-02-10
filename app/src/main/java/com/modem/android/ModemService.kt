package com.modem.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.modem.android.utils.ModemManager

class ModemService : Service() {

    private lateinit var webServer: ModemWebServer
    private lateinit var modemManager: ModemManager
    private val tag = "ModemService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "Service started")

        modemManager = ModemManager(this)
        webServer = ModemWebServer(8080, this, modemManager)

        try {
            webServer.start()
            Log.d(tag, "WebServer started on port 8080")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start webserver: ${e.message}")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stop()
        Log.d(tag, "Service destroyed")
    }
}
