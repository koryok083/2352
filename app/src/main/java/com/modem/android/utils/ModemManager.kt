package com.modem.android.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ModemManager(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val tag = "ModemManager"

    // Status Tethering
    fun getTetheringStatus(): Map<String, Any> {
        return mapOf(
            "wifiHotspot" to isWifiHotspotEnabled(),
            "usbTethering" to isUsbTetheringEnabled(),
            "bluetoothTethering" to isBluetoothTetheringEnabled(),
            "connectedDevices" to getConnectedDevicesCount(),
            "ipAddress" to getLocalIPAddress()
        )
    }

    // Cek WiFi Hotspot aktif
    private fun isWifiHotspotEnabled(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("isTetheringSupported")
            method.isAccessible = true
            val result = method.invoke(connectivityManager) as Boolean
            result
        } catch (e: Exception) {
            Log.e(tag, "Error checking hotspot: ${e.message}")
            false
        }
    }

    // Cek USB Tethering aktif
    private fun isUsbTetheringEnabled(): Boolean {
        return try {
            val command = arrayOf("sh", "-c", "cat /sys/class/net/usb0/operstate")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val state = reader.readLine()
            reader.close()
            state == "up"
        } catch (e: Exception) {
            false
        }
    }

    // Cek Bluetooth Tethering
    private fun isBluetoothTetheringEnabled(): Boolean {
        // Implementasi untuk Bluetooth tethering
        return false
    }

    // Hitung perangkat yang terhubung
    private fun getConnectedDevicesCount(): Int {
        return try {
            val command = arrayOf("sh", "-c", "cat /proc/net/arp | grep -v IP | wc -l")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val count = reader.readLine()?.toIntOrNull() ?: 0
            reader.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    // Dapatkan IP Address lokal
    private fun getLocalIPAddress(): String {
        return try {
            val command = arrayOf("sh", "-c", "hostname -I")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val ip = reader.readLine()
            reader.close()
            ip ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Start WiFi Hotspot (memerlukan root/system privilege)
    fun startWifiHotspot(): Boolean {
        return try {
            val command = arrayOf("su", "-c", "svc wifi enable && svc tether start")
            Runtime.getRuntime().exec(command)
            Thread.sleep(2000)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error starting hotspot: ${e.message}")
            false
        }
    }

    // Stop WiFi Hotspot
    fun stopWifiHotspot(): Boolean {
        return try {
            val command = arrayOf("su", "-c", "svc tether stop")
            Runtime.getRuntime().exec(command)
            Thread.sleep(2000)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error stopping hotspot: ${e.message}")
            false
        }
    }

    // Aktifkan USB Tethering
    fun startUsbTethering(): Boolean {
        return try {
            val command = arrayOf("su", "-c", "service call connectivity 30 i32 1 i32 1 i32 1")
            Runtime.getRuntime().exec(command)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error starting USB tethering: ${e.message}")
            false
        }
    }

    // Matikan USB Tethering
    fun stopUsbTethering(): Boolean {
        return try {
            val command = arrayOf("su", "-c", "service call connectivity 30 i32 0 i32 0 i32 0")
            Runtime.getRuntime().exec(command)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error stopping USB tethering: ${e.message}")
            false
        }
    }
}
