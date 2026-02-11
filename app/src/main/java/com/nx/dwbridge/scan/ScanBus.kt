package com.nx.dwbridge.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nx.dwbridge.ws.WsMessage

object ScanBus {
    private val _scan = MutableLiveData<WsMessage?>()
    val scan: LiveData<WsMessage?> = _scan

    fun post(msg: WsMessage) {
        _scan.postValue(msg)
    }

    fun clear() {
        _scan.postValue(null)
    }
}

