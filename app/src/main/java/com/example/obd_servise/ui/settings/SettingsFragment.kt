package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.MainActivity
import com.example.obd_servise.databinding.FragmentSettingsBinding


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
                binding.radioEnglish.id -> changeLanguage("en")
                binding.radioRussian.id -> changeLanguage("ru")
            }
        }
    }

    private fun changeLanguage(languageCode: String) {
        settingsViewModel.changeLanguage(languageCode)

        // Перезапуск MainActivity для применения изменений
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//    private fun changeLanguage(languageCode: String) {
//        settingsViewModel.changeLanguage(languageCode)
//
//        // Перезапуск MainActivity для применения изменений
//        val intent = Intent(requireActivity(), MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//
//        // Отключение стандартной анимации перехода
//        requireActivity().overridePendingTransition(0, 0)
//        // Уникальная анимация перехода (закомментировано)
//        // requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
//        requireActivity().finish()
//    }