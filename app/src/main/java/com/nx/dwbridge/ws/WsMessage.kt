package com.nx.dwbridge.ws

data class WsMessage(
    val type: String,
    val data: String,
    val symbology: String? = null,
    val timestamp: Long
)

