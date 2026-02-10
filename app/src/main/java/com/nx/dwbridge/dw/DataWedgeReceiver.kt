package com.nx.dwbridge.dw

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject
import androidx.core.content.ContextCompat
import com.nx.dwbridge.ws.WebSocketService

class DataWedgeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DWBridge"
        const val EXTRA_WS_JSON = "extra_ws_json"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "DataWedgeReceiver received intent: ${intent.action}")

        // Try known DataWedge extras (symbol/zebra variations)
        val data = intent.getStringExtra("com.symbol.datawedge.data_string")
            ?: intent.getStringExtra("com.zebra.datawedge.data_string")
            ?: intent.getStringExtra("com.nx.dwbridge.data_string")
            ?: ""

        val labelType = intent.getStringExtra("com.symbol.datawedge.label_type")
            ?: intent.getStringExtra("com.zebra.datawedge.label_type")
            ?: intent.getStringExtra("com.nx.dwbridge.label_type")

        val timestamp = System.currentTimeMillis()

        val jsonObj = JSONObject()
        jsonObj.put("type", "scan")
        jsonObj.put("data", data)
        if (labelType != null) jsonObj.put("symbology", labelType)
        jsonObj.put("timestamp", timestamp)

        val json = jsonObj.toString()

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
