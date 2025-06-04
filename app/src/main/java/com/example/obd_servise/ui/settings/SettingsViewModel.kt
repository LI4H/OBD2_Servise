package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_servise.ui.statistics.TripEntity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {

    private var sharedPreferences: SharedPreferences? = null
    private val PREF_SELECTED_CAR_ONLY = "selected_car_only"
    //----------------------------------------------------------------------------------------
    fun getNotificationsEnabled(): Boolean {
        return sharedPreferences?.getBoolean("notifications_enabled", true) ?: true
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences?.edit()?.putBoolean("notifications_enabled", enabled)?.apply()
        // Или используйте commit() для синхронного сохранения:
        // sharedPreferences?.edit()?.putBoolean("notifications_enabled", enabled)?.commit()
    }

    fun setSelectedCarOnly(enabled: Boolean) {
        sharedPreferences?.edit()?.putBoolean(PREF_SELECTED_CAR_ONLY, enabled)?.apply()
    }

    fun getSelectedCarOnly(): Boolean {
        return sharedPreferences?.getBoolean(PREF_SELECTED_CAR_ONLY, false) == true
    }

    fun getNotificationMethods(): Set<String> {
        return sharedPreferences?.getStringSet("notification_methods", setOf()) ?: setOf()
    }

    fun setNotificationMethods(methods: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putStringSet("notification_methods", methods)?.apply()
        }
    }

    fun getNotificationEmail(): String {
        return sharedPreferences?.getString("notification_email", "") ?: ""
    }

    fun setNotificationEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences?.edit()?.putString("notification_email", email)?.apply()
        }
    }

    //----------------------------------------------------------------------------------------
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

