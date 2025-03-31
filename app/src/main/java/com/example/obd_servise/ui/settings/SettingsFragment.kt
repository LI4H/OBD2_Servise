package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.MainActivity
import com.example.obd_servise.databinding.FragmentSettingsBinding
import java.util.Locale


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Инициализация SharedPreferences в ViewModel
        settingsViewModel.initSharedPreferences(requireContext())

        setupLanguageSelection()
        setupThemeSelection()

        return root
    }

    private fun setupLanguageSelection() {
        val currentLanguage = settingsViewModel.getCurrentLanguage()

        when (currentLanguage) {
            "en" -> binding.radioEnglish.isChecked = true
            "ru" -> binding.radioRussian.isChecked = true
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioEnglish.id ->
                    changeLanguage("en")

                binding.radioRussian.id ->
                    changeLanguage("ru")

            }
        }
    }

    private fun setupThemeSelection() {
        val currentTheme = settingsViewModel.getCurrentTheme()
        when (currentTheme) {
            "classic" -> binding.radioClassic.isChecked = true
            "yellow" -> binding.radioYellow.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                binding.radioClassic.id -> "classic"
                binding.radioYellow.id -> "yellow"
                else -> "classic"
            }
            settingsViewModel.setTheme(newTheme)

            // Изменение темы
            if (newTheme == "classic") {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // Обновить UI
            activity?.recreate()
        }
    }

    private fun changeLanguage(languageCode: String) {

        settingsViewModel.changeLanguage(languageCode) {
            // Изменение языка
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            // Обновить UI
            activity?.recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

//private fun setupLanguageSelection() {
//    val currentLanguage = settingsViewModel.getCurrentLanguage()
//
//    when (currentLanguage) {
//        "en" -> binding.radioEnglish.isChecked = true
//        "ru" -> binding.radioRussian.isChecked = true
//    }
//
//    binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
//        val newLanguage = when (checkedId) {
//            binding.radioEnglish.id ->
//                changeLanguage("en")
//
//            binding.radioRussian.id ->
//                changeLanguage("ru")
//            else -> ("en")
//        }
//        settingsViewModel.setLanguage(newLanguage)
//    }
//}
