package com.nx.dwbridge.dw

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject
import androidx.core.content.ContextCompat
import com.nx.dwbridge.scan.ScanBus
import com.nx.dwbridge.ws.WebSocketService
import com.nx.dwbridge.ws.WsMessage

class DataWedgeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DWBridge"
        const val EXTRA_WS_JSON = "extra_ws_json"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Primary brief log for quick filtering
        Log.d(TAG, "DataWedgeReceiver received intent: action=${intent.action} component=${intent.component} package=${intent.`package`} flags=${intent.flags}")

        // Verbose dump of all extras to aid debugging (will show keys and values)
        val extras = intent.extras
        if (extras != null && !extras.isEmpty) {
            for (key in extras.keySet()) {
                try {
                    val value = extras.get(key)
                    Log.d(TAG, "Intent extra: key=$key value=$value")
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to read extra $key", t)
                }
            }
        } else {
            Log.d(TAG, "Intent has no extras")
        }

        // Try known DataWedge extras (symbol/zebra variations)
        val data = intent.getStringExtra("com.symbol.datawedge.data_string")
            ?: intent.getStringExtra("com.zebra.datawedge.data_string")
            ?: intent.getStringExtra("com.nx.dwbridge.data_string")
            ?: intent.getStringExtra("com.motorolasolutions.emdk.datawedge.data_string")
            ?: ""

        val labelType = intent.getStringExtra("com.symbol.datawedge.label_type")
            ?: intent.getStringExtra("com.zebra.datawedge.label_type")
            ?: intent.getStringExtra("com.nx.dwbridge.label_type")
            ?: intent.getStringExtra("com.motorolasolutions.emdk.datawedge.label_type")

        val timestamp = System.currentTimeMillis()

        val jsonObj = JSONObject()
        jsonObj.put("type", "scan")
        jsonObj.put("data", data)
        if (labelType != null) jsonObj.put("symbology", labelType)
        jsonObj.put("timestamp", timestamp)

        val json = jsonObj.toString()

        // Post to ScanBus for UI
        try {
            val msg = WsMessage(type = "scan", data = data, symbology = labelType, timestamp = timestamp)
            ScanBus.post(msg)
            Log.d(TAG, "Posted scan to ScanBus: $msg")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to post scan to ScanBus", t)
        }

        // Start or deliver to the WebSocketService with the JSON
        val svcIntent = Intent(context, WebSocketService::class.java).apply {
            putExtra(EXTRA_WS_JSON, json)
        }
        try {
            ContextCompat.startForegroundService(context, svcIntent)
            Log.d(TAG, "Started WebSocketService with scan JSON: $json")
        } catch (e: Exception) {
            // Fallback to startService if foreground start fails
            try {
                context.startService(svcIntent)
                Log.d(TAG, "Started WebSocketService (fallback) with scan JSON: $json")
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to deliver scan to WebSocketService", ex)
            }
        }
    }
}
