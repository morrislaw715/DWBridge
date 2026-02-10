package com.nx.dwbridge.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nx.dwbridge.ws.WsMessage

class ServerViewModel : ViewModel() {
    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _clientCount = MutableLiveData(0)
    val clientCount: LiveData<Int> = _clientCount

    private val _messages = MutableLiveData<List<WsMessage>>(emptyList())
    val messages: LiveData<List<WsMessage>> = _messages

    fun setRunning(running: Boolean) {
        _isRunning.postValue(running)
    }

    fun setClientCount(count: Int) {
        _clientCount.postValue(count)
    }

    fun addMessage(msg: WsMessage) {
        val current = _messages.value ?: emptyList()
        _messages.postValue((listOf(msg) + current).take(100))
    }
}

