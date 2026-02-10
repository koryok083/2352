package com.modem.android

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.modem.android.utils.ModemManager
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.InputStreamReader

class ModemWebServer(
    port: Int,
    private val context: Context,
    private val modemManager: ModemManager
) : NanoHTTPD(port) {

    private val gson = Gson()
    private val tag = "ModemWebServer"

    override fun serve(session: IHTTPSession): Response {
        try {
            val uri = session.uri
            val method = session.method

            Log.d(tag, "Request: $method $uri")

            return when {
                uri == "/" && method == Method.GET -> serveWebUI()
                uri == "/api/status" && method == Method.GET -> getStatus()
                uri == "/api/hotspot/start" && method == Method.POST -> startHotspot()
                uri == "/api/hotspot/stop" && method == Method.POST -> stopHotspot()
                uri == "/api/usb/start" && method == Method.POST -> startUsb()
                uri == "/api/usb/stop" && method == Method.POST -> stopUsb()
                uri == "/api/devices" && method == Method.GET -> getConnectedDevices()
                uri.startsWith("/assets/") -> serveAsset(uri)
                else -> notFound()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error serving request: ${e.message}")
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    private fun serveWebUI(): Response {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Android Modem Control</title>
                <link rel="stylesheet" href="/assets/style.css">
            </head>
            <body>
                <div class="container">
                    <header>
                        <h1>📱 Android Modem Controller</h1>
                        <p>Control your Android device as a modem</p>
                    </header>

                    <main>
                        <section class="status-panel">
                            <h2>System Status</h2>
                            <div id="status" class="status-info">Loading...</div>
                        </section>

                        <section class="control-panel">
                            <h2>Tethering Controls</h2>
                            
                            <div class="control-group">
                                <h3>WiFi Hotspot</h3>
                                <button id="hotspotStart" class="btn btn-success">Start Hotspot</button>
                                <button id="hotspotStop" class="btn btn-danger">Stop Hotspot</button>
                            </div>

                            <div class="control-group">
                                <h3>USB Tethering</h3>
                                <button id="usbStart" class="btn btn-success">Start USB</button>
                                <button id="usbStop" class="btn btn-danger">Stop USB</button>
                            </div>
                        </section>

                        <section class="devices-panel">
                            <h2>Connected Devices</h2>
                            <div id="devices" class="devices-list">Loading...</div>
                        </section>

                        <section class="logs-panel">
                            <h2>Activity Log</h2>
                            <div id="logs" class="logs-container"></div>
                        </section>
                    </main>

                    <footer>
                        <p>Android Modem v1.0</p>
                    </footer>
                </div>

                <script src="/assets/script.js"></script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    private fun getStatus(): Response {
        val status = modemManager.getTetheringStatus()
        val json = gson.toJson(status)
        addLog("Status fetched")
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun startHotspot(): Response {
        val success = modemManager.startWifiHotspot()
        addLog("WiFi Hotspot ${if (success) "STARTED" else "FAILED"}")
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(mapOf("success" to success, "message" to if (success) "Hotspot started" else "Failed to start"))
        )
    }

    private fun stopHotspot(): Response {
        val success = modemManager.stopWifiHotspot()
        addLog("WiFi Hotspot STOPPED")
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(mapOf("success" to success, "message" to if (success) "Hotspot stopped" else "Failed to stop"))
        )
    }

    private fun startUsb(): Response {
        val success = modemManager.startUsbTethering()
        addLog("USB Tethering STARTED")
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(mapOf("success" to success, "message" to if (success) "USB started" else "Failed to start"))
        )
    }

    private fun stopUsb(): Response {
        val success = modemManager.stopUsbTethering()
        addLog("USB Tethering STOPPED")
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(mapOf("success" to success, "message" to if (success) "USB stopped" else "Failed to stop"))
        )
    }

    private fun getConnectedDevices(): Response {
        return try {
            val command = arrayOf("sh", "-c", "arp -a")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val devices = mutableListOf<Map<String, String>>()
            
            var line = reader.readLine()
            while (line != null) {
                if (line.isNotEmpty()) {
                    devices.add(mapOf("device" to line))
                }
                line = reader.readLine()
            }
            reader.close()

            val json = gson.toJson(mapOf("devices" to devices, "count" to devices.size))
            newFixedLengthResponse(Response.Status.OK, "application/json", json)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf("devices" to emptyList<Any>(), "count" to 0)))
        }
    }

    private fun serveAsset(uri: String): Response {
        return try {
            val assetPath = uri.removePrefix("/assets/")
            val inputStream = context.assets.open("web/$assetPath")
            val mimeType = when {
                assetPath.endsWith(".css") -> "text/css"
                assetPath.endsWith(".js") -> "application/javascript"
                assetPath.endsWith(".html") -> "text/html"
                else -> "text/plain"
            }
            newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, inputStream.available().toLong())
        } catch (e: Exception) {
            notFound()
        }
    }

    private fun notFound(): Response {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
    }

    private fun addLog(message: String) {
        Log.i(tag, message)
    }
}
