package com.example.obd_servise.ui.errors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ErrorItem(
    val code: String,
    val status: String,
    val description: String
)

class ErrorsViewModel(private val obdDeviceConnection: ObdDeviceConnection) : ViewModel() {

    private val _errors = MutableLiveData<List<ErrorItem>>().apply { value = emptyList() }
    val errors: LiveData<List<ErrorItem>> = _errors

    private var allErrors: MutableList<ErrorItem> = mutableListOf()

    private val troubleCodesManager = TroubleCodesManager(obdDeviceConnection)

    fun loadErrors(obdDeviceConnection: ObdDeviceConnection) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val codes = troubleCodesManager.getTroubleCodes()
                withContext(Dispatchers.Main) {
                    allErrors.clear()
                    allErrors.addAll(codes.map { code ->
                        ErrorItem(
                            code = code,
                            status = "Новая ошибка", // Здесь можно добавить логику для определения статуса
                            description = "Описание ошибки" // Здесь можно добавить описание ошибки
                        )
                    })
                    _errors.value = allErrors
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addError(error: ErrorItem) {
        allErrors.add(error)
        _errors.value = allErrors
    }

    fun removeError(error: ErrorItem) {
        allErrors.remove(error)
        _errors.value = allErrors
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
