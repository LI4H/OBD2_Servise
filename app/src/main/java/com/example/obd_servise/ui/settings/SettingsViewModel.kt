package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private var sharedPreferences: SharedPreferences? = null

    // Инициализация SharedPreferences
    fun initSharedPreferences(context: Context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    // Получение текущего языка
    fun getCurrentLanguage(): String {
        return sharedPreferences?.getString("language", "en") ?: "en"
    }

    // Смена языка
    fun changeLanguage(languageCode: String) {
        sharedPreferences?.edit()?.putString("language", languageCode)?.apply()
    }

    fun getCurrentTheme(): String {
        return sharedPreferences?.getString("theme", "classic") ?: "classic"
    }

    fun setTheme(theme: String) {
        sharedPreferences?.edit()?.putString("theme", theme)?.apply()
    }

}
