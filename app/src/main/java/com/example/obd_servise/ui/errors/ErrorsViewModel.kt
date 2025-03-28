package com.example.obd_servise.ui.errors

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ErrorItem(
    val code: String,
    val error_status: String,
    val description: String
)

class ErrorsViewModel : ViewModel() {
    private val _errors = MutableLiveData<List<ErrorItem>>(emptyList())
    val errors: LiveData<List<ErrorItem>> = _errors


    private val allErrors = mutableListOf<ErrorItem>()
    fun initializeConnection(
        bluetoothSocket: BluetoothSocket?,
        isDemoActive: Boolean,
        isConnected: Boolean,
        onConnectionInitialized: (ObdDeviceConnection?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val obdConnection = bluetoothSocket?.let {
                try {
                    ObdDeviceConnection(it.inputStream, it.outputStream)
                } catch (e: Exception) {
                    Log.e("ErrorsViewModel", "Error creating ObdDeviceConnection", e)
                    null
                }
            }
            onConnectionInitialized(obdConnection)

            loadErrors(obdConnection, isDemoActive, isConnected)
        }
    }

    fun loadErrors(
        obdDeviceConnection: ObdDeviceConnection?,
        isDemo: Boolean,
        isConnected: Boolean
    ) {
        if (!isDemo && !isConnected) {
            // Если нет соединения и не в демо-режиме, не выполняем запрос
            updateErrors(listOf(ErrorItem("NO_CONN", "Нет соединения", "OBD не подключен")))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val errorsList = when {
                    isDemo -> generateDemoErrors()
                    else -> fetchErrorsFromObd(obdDeviceConnection)
                }

                withContext(Dispatchers.Main) {
                    updateErrors(errorsList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchErrorsFromObd(obdDeviceConnection: ObdDeviceConnection?): List<ErrorItem> {
        // Реализуйте получение ошибок с устройства OBD
        // Для примера, вернем пустой список
        return emptyList()
    }

    private fun updateErrors(errorsList: List<ErrorItem>) {
        allErrors.clear()
        allErrors.addAll(errorsList)
        _errors.value = allErrors
    }

    fun addError(error: ErrorItem) {
        allErrors.add(error)
        _errors.value = allErrors
    }

    fun removeError(error: ErrorItem) {
        allErrors.remove(error)
        _errors.value = allErrors
    }

    private fun generateDemoErrors(): List<ErrorItem> {
        return listOf(
            ErrorItem("P0301", "Ошибка зажигания", "Пропуски зажигания в цилиндре 1"),
            ErrorItem(
                "P0420",
                "Проблема с катализатором",
                "Эффективность катализатора ниже порога"
            ),
            ErrorItem("P0171", "Бедная смесь", "Система топливоподачи слишком бедная (банк 1)"),
            ErrorItem(
                "P0500",
                "Ошибка датчика скорости",
                "Неисправность датчика скорости автомобиля"
            ),
            ErrorItem("C1201", "Проблема с тормозной системой", "Ошибка в системе ABS или ESC")
        )
    }

    fun clearAllErrors() {
        allErrors.clear()
        _errors.value = allErrors
    }

    fun filterErrors(query: String) {
        _errors.value = if (query.isEmpty()) allErrors else allErrors.filter {
            it.code.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
    }
}
