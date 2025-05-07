package com.example.obd_servise.ui.settings

import android.content.Context
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

    private val sectionLayouts by lazy {
        listOf(
            binding.layoutTheme,
            binding.layoutLanguage,
            binding.layoutStatistics,
            binding.layoutNotifications
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
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        carViewModel = ViewModelProvider(this)[CarViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        settingsViewModel.initSharedPreferences(requireContext())

        setupLanguageSelection()
        setupThemeSelection()
        setupCarSelection()
        setupSectionNavigation()

        currentSectionIndex = settingsViewModel.getLastSectionIndex()
        showSection(currentSectionIndex)

        return root
    }

    private fun setupSectionNavigation() {
        binding.btnLeft.setOnClickListener {
            currentSectionIndex =
                (currentSectionIndex - 1 + sectionLayouts.size) % sectionLayouts.size
            showSection(currentSectionIndex)
        }

        binding.btnCenter.setOnClickListener {
            // Центр — это текущая секция, ничего не делаем
        }

        binding.btnRight.setOnClickListener {
            currentSectionIndex = (currentSectionIndex + 1) % sectionLayouts.size
            showSection(currentSectionIndex)
        }

        showSectionButtons(currentSectionIndex)
    }


    private fun showSection(index: Int) {
        sectionLayouts.forEachIndexed { i, layout ->
            layout.visibility = if (i == index) View.VISIBLE else View.GONE
        }

        currentSectionIndex = index
        settingsViewModel.setLastSectionIndex(index)
        showSectionButtons(index)
    }

    private fun showSectionButtons(index: Int) {
        val total = sectionTitles.size
        val leftIndex = (index - 1 + total) % total
        val rightIndex = (index + 1) % total

        with(binding) {
            btnLeft.text = sectionTitles[leftIndex]
            btnLeft.visibility = View.VISIBLE

            btnCenter.text = sectionTitles[index]

            btnRight.text = sectionTitles[rightIndex]
            btnRight.visibility = View.VISIBLE
        }
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
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                binding.radioClassic.id -> "classic"
                binding.radioYellow.id -> "yellow"
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
