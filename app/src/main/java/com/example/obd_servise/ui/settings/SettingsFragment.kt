package com.example.obd_servise.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.R
import com.example.obd_servise.databinding.FragmentSettingsBinding
import com.example.obd_servise.ui.car.CarViewModel
import java.util.Locale


class SettingsFragment : Fragment() {
    private lateinit var carViewModel: CarViewModel
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsViewModel: SettingsViewModel

    private var currentSectionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация ViewModel в onCreate
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        carViewModel = ViewModelProvider(this)[CarViewModel::class.java]
    }

    private val sectionLayouts by lazy {
        listOf(
            binding.layoutStatistics,    // 0 - Статистика
            binding.layoutTheme,         // 1 - Темы
            binding.layoutLanguage,      // 2 - Язык
            binding.layoutNotifications  // 3 - Уведомления
        )
    }

    private val sectionTitles by lazy {
        listOf("Тема", "Язык", "Статистика", "Уведомления")
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        // Инициализация SharedPreferences после создания ViewModel
        settingsViewModel.initSharedPreferences(requireContext())

        // Настройка интерфейса
        setupUI()

        return root
    }

    private fun setupUI() {
        currentSectionIndex = settingsViewModel.getLastSectionIndex()
        setupSectionNavigation()
        showSection(currentSectionIndex)
        setupLanguageSelection()
        setupThemeSelection()
        setupCarSelection()
        setupNotificationSettings()
    }

    private fun setupNotificationSettings() {

        Log.d(
            "SettingsFragment",
            "Initial notifications enabled: ${settingsViewModel.getNotificationsEnabled()}"
        )
        // Инициализация текущих настроек
        binding.switchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            Log.d("SettingsFragment", "Notifications switch changed to: $isChecked")
            settingsViewModel.setNotificationsEnabled(isChecked)
            Log.d(
                "SettingsFragment",
                "Value after save: ${settingsViewModel.getNotificationsEnabled()}"
            )
            updateNotificationOptionsVisibility()
        }

        val currentMethods = settingsViewModel.getNotificationMethods()
        binding.checkboxPhone.isChecked = currentMethods.contains("phone")
        binding.checkboxEmail.isChecked = currentMethods.contains("email")

        binding.etNotificationEmail.setText(settingsViewModel.getNotificationEmail())
        updateEmailInputVisibility()

        // Обработчики событий
        binding.switchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setNotificationsEnabled(isChecked)
            updateNotificationOptionsVisibility()
        }

        binding.switchSelectedCarOnly.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setSelectedCarOnly(isChecked)
        }

        binding.checkboxPhone.setOnCheckedChangeListener { _, isChecked ->
            val methods = settingsViewModel.getNotificationMethods().toMutableSet()
            if (isChecked) {
                methods.add("phone")
                requestNotificationPermission()
            } else {
                methods.remove("phone")
            }
            settingsViewModel.setNotificationMethods(methods)
        }

        binding.checkboxEmail.setOnCheckedChangeListener { _, isChecked ->
            val methods = settingsViewModel.getNotificationMethods().toMutableSet()
            if (isChecked) {
                methods.add("email")
            } else {
                methods.remove("email")
            }
            settingsViewModel.setNotificationMethods(methods)
            updateEmailInputVisibility()
        }

        binding.etNotificationEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                settingsViewModel.setNotificationEmail(binding.etNotificationEmail.text.toString())
            }
        }
    }

    private fun updateNotificationOptionsVisibility() {
        val isEnabled = binding.switchNotificationsEnabled.isChecked
        binding.notificationOptionsContainer.visibility = if (isEnabled) View.VISIBLE else View.GONE
        binding.selectedCarOnlyContainer.visibility = if (isEnabled) View.VISIBLE else View.GONE

        // Отключаем/включаем чекбоксы в зависимости от состояния переключателя
        binding.checkboxPhone.isEnabled = isEnabled
        binding.checkboxEmail.isEnabled = isEnabled
        binding.switchSelectedCarOnly.isEnabled = isEnabled
    }


    private fun updateEmailInputVisibility() {
        binding.emailInputContainer.visibility =
            if (binding.checkboxEmail.isChecked) View.VISIBLE else View.GONE
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение получено
                } else {
                    // Разрешение не получено, можно показать объяснение
                    binding.checkboxPhone.isChecked = false
                    val methods = settingsViewModel.getNotificationMethods().toMutableSet()
                    methods.remove("phone")
                    settingsViewModel.setNotificationMethods(methods)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
    private fun setupSectionNavigation() {
        // Устанавливаем начальную выбранную кнопку
        when (currentSectionIndex) {
            0 -> binding.radioStatistics.isChecked = true
            1 -> binding.radioTheme.isChecked = true
            2 -> binding.radioLanguage.isChecked = true
            3 -> binding.radioNotifications.isChecked = true
        }

        binding.navigationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentSectionIndex = when (checkedId) {
                binding.radioStatistics.id -> 0    // Статистика
                binding.radioTheme.id -> 1        // Темы
                binding.radioLanguage.id -> 2     // Язык
                binding.radioNotifications.id -> 3 // Уведомления
                else -> 0
            }
            showSection(currentSectionIndex)
        }
    }

    private fun showSection(index: Int) {
        binding.layoutStatistics.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.layoutTheme.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.layoutLanguage.visibility = if (index == 2) View.VISIBLE else View.GONE
        binding.layoutNotifications.visibility = if (index == 3) View.VISIBLE else View.GONE

        currentSectionIndex = index
        settingsViewModel.setLastSectionIndex(index)
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

    private fun setupThemeSelection() {
        val currentTheme = settingsViewModel.getCurrentTheme()
        when (currentTheme) {
            "classic" -> binding.radioClassic.isChecked = true
            "yellow" -> binding.radioYellow.isChecked = true
            "green" -> binding.radioGreen.isChecked = true
            "violet" -> binding.radioViolet.isChecked = true
            "black" -> binding.radioBlack.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                binding.radioClassic.id -> "classic"
                binding.radioYellow.id -> "yellow"
                binding.radioGreen.id -> "green"
                binding.radioViolet.id -> "violet"
                binding.radioBlack.id -> "black"
                else -> "classic"
            }

            settingsViewModel.setTheme(newTheme)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            requireActivity().recreate()
        }
    }

    private fun changeLanguage(languageCode: String) {
        settingsViewModel.changeLanguage(languageCode) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            settingsViewModel.setLastSectionIndex(1)
            requireActivity().recreate()
        }
    }

//    private var isSpinnerInitialized = false

    private fun setupCarSelection() {
        carViewModel.carList.observe(viewLifecycleOwner) { carList ->
            binding.carListContainer.removeAllViews()

            // Сортируем: выбранный авто — первым
            val sortedCars = carList.sortedByDescending { it.isSelected }

            // Обновляем заголовок
            val selectedCar = sortedCars.firstOrNull { it.isSelected == 1 }
            binding.tvSelectedCar.text =
                "Выбранный автомобиль: ${selectedCar?.name ?: "Не выбрано"}"

            // Добавляем кнопки
            sortedCars.forEach { car ->
                val carButton = Button(requireContext()).apply {
                    text = car.name
                    textSize = 16f

                    // Цвет фона
                    val bgColor = requireContext().getColorFromAttr(
                        if (car.isSelected == 1) R.attr.colorPrimary
                        else R.attr.colorSecondary
                    )

                    setBackgroundColor(bgColor)
                    // Цвет текста
                    val textColor = requireContext().getColorFromAttr(R.attr.colorOnPrimary)
                    setTextColor(textColor)


                    setOnClickListener {
                        carViewModel.selectCarForStats(
                            car.id,
                            onSuccess = {
                                // Обновление через LiveData
                            },
                            onFailure = {
                                Log.e("SettingsFragment", "Ошибка выбора авто: ${it.message}")
                            }
                        )
                    }
                }
                binding.carListContainer.addView(carButton)
            }
        }
    }

    private fun Context.getColorFromAttr(attrResId: Int): Int {
        val typedValue = TypedValue()
        val theme = this.theme
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }







    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
