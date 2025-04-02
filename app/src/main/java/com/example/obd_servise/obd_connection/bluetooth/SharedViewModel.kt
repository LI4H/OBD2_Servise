package com.example.obd_servise.obd_connection.bluetooth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_servise.ui.connection.ConnectionState

class SharedViewModel : ViewModel() {
    private val _elmStatus = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val elmStatus: LiveData<ConnectionState> get() = _elmStatus

    private val _ecuStatus = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val ecuStatus: LiveData<ConnectionState> get() = _ecuStatus

    fun updateElmStatus(state: ConnectionState) {
        _elmStatus.value = state
    }

    fun updateEcuStatus(state: ConnectionState) {
        _ecuStatus.value = state
    }
}