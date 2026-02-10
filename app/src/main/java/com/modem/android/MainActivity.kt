package com.modem.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText: TextView = findViewById(R.id.statusText)
        val startServiceBtn: Button = findViewById(R.id.startServiceBtn)
        val stopServiceBtn: Button = findViewById(R.id.stopServiceBtn)
        val openWebUIBtn: Button = findViewById(R.id.openWebUIBtn)

        // Start Service
        startServiceBtn.setOnClickListener {
            val intent = Intent(this, ModemService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            statusText.text = "✓ Service started on port 8080\nAccess: http://localhost:8080"
        }

        // Stop Service
        stopServiceBtn.setOnClickListener {
            val intent = Intent(this, ModemService::class.java)
            stopService(intent)
            statusText.text = "✗ Service stopped"
        }

        // Open WebUI
        openWebUIBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("http://localhost:8080")
            }
            startActivity(intent)
        }
    }
}
