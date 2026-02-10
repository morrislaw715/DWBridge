package com.nx.dwbridge.ws

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class WebSocketService : Service() {
    companion object {
        private const val TAG = "DWBridge"
        private const val CHANNEL_ID = "dwbridge.ws.channel"
        const val EXTRA_PORT = "extra_port"
        const val DEFAULT_PORT = 8080
        const val EXTRA_WS_JSON = "extra_ws_json"
    }

    private var server: SimpleWsServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        // Start foreground with notification. We removed the connectedDevice
        // foregroundServiceType in the manifest, so this should not require the
        // FOREGROUND_SERVICE_CONNECTED_DEVICE permission. Wrap in try/catch to
        // avoid an unexpected SecurityException causing app crash.
        val notification = buildNotification("WebSocket server running on port $port")
        try {
            startForeground(1, notification)
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while starting foreground service", se)
            stopSelf()
            return START_NOT_STICKY
        }
        startServer(port)

        // If started with a scan JSON, forward it to clients
        val incomingJson = intent?.getStringExtra(EXTRA_WS_JSON)
        if (!incomingJson.isNullOrEmpty()) {
            try {
                server?.broadcast(incomingJson)
                Log.d(TAG, "Forwarded incoming JSON to clients: $incomingJson")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to forward incoming JSON", e)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    private fun startServer(port: Int) {
        if (server != null) return
        server = SimpleWsServer(InetSocketAddress(port)).also { ws ->
            thread(start = true) {
                try {
                    ws.start()
                    Log.d(TAG, "WebSocket server started on port $port")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start WebSocket server", e)
                }
            }
        }
    }

    private fun stopServer() {
        try {
            server?.stop(1000)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping server", e)
        }
        server = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "DWBridge WebSocket", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(content: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DWBridge WebSocket")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        return builder.build()
    }

    // Simple WebSocketServer implementation
    private class SimpleWsServer(address: InetSocketAddress) : WebSocketServer(address) {
        private val clients = ConcurrentHashMap<WebSocket, Unit>()

        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            conn?.let {
                clients[it] = Unit
                Log.d(TAG, "Client connected: ${it.remoteSocketAddress}")
            }
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            conn?.let {
                clients.remove(it)
                Log.d(TAG, "Client disconnected: ${it.remoteSocketAddress}")
            }
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            Log.d(TAG, "Received from client: $message")
            // For now, echo back
            conn?.send(message ?: "")
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.w(TAG, "WebSocket error", ex)
        }

        override fun onStart() {
            Log.d(TAG, "SimpleWsServer started")
        }
    }
}
