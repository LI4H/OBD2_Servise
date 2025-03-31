package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {

    private var sharedPreferences: SharedPreferences? = null

    // Инициализация SharedPreferences
    fun initSharedPreferences(context: Context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    //текущий язык
    fun getCurrentLanguage(): String {
        val language = sharedPreferences?.getString("language", "en") ?: "en"
        return language
    }

    fun getCurrentTheme(): String {
        val theme = sharedPreferences?.getString("theme", "classic") ?: "classic"
        return theme
    }
    // Смена языка
    fun changeLanguage(languageCode: String, callback: () -> Unit) {

        // изменение языка в фоновом потоке
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putString("language", languageCode)?.apply()

            // Обновляем конфигурацию ресурса на главном потоке
            withContext(Dispatchers.Main) {
                callback() //callback для обновления UI
            }
        }
    }

    fun setTheme(theme: String) {

        // изменение темы в фоновом потоке
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putString("theme", theme)?.apply()

            // Обновляем UI на главном потоке
            withContext(Dispatchers.Main) {}
        }
    }
}

