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

    fun initSharedPreferences(context: Context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    fun getCurrentLanguage(): String {
        return sharedPreferences?.getString("language", "en") ?: "en"
    }

    fun getCurrentTheme(): String {
        return sharedPreferences?.getString("theme", "classic") ?: "classic"
    }

    fun changeLanguage(languageCode: String, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putString("language", languageCode)?.apply()
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putString("theme", theme)?.apply()
            withContext(Dispatchers.Main) {}
        }
    }

    // ✅ Сохраняем индекс текущей секции
    fun setLastSectionIndex(index: Int) {
        sharedPreferences?.edit()?.putInt("last_section_index", index)?.apply()
    }

    // ✅ Получаем индекс текущей секции
    fun getLastSectionIndex(): Int {
        return sharedPreferences?.getInt("last_section_index", 0) ?: 0
    }
}
