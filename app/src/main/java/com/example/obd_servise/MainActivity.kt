package com.example.obd_servise

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.obd_servise.databinding.ActivityMainBinding
import com.example.obd_servise.ui.connection.ConnectionState
import com.example.obd_servise.ui.home.HomeViewModel

import com.google.android.material.navigation.NavigationView
import java.util.*
import com.example.obd_servise.obd_connection.bluetooth.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val newContext = updateLocale(newBase, languageCode)
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val theme = sharedPreferences.getString("theme", "classic")

        // Инициализация SharedViewModel
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        when (theme) {
            "classic" -> setTheme(R.style.Theme_OBD_Servise)
            "yellow" -> setTheme(R.style.Theme_OBD_Servise_Yellow)
            "green" -> setTheme(R.style.Theme_OBD_Servise_Green)
            "violet" -> setTheme(R.style.Theme_OBD_Servise_Violet)

        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_dynamics,
                R.id.nav_statistics,
                R.id.nav_errors,
                R.id.nav_car,
                R.id.nav_info,
                R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val statusIcon = menu.findItem(R.id.status_icon)

        // Наблюдаем за состоянием демо-режима
        homeViewModel.isDemoActive.observe(this) { isDemo ->
            updateStatusIcon(
                statusIcon,
                isDemo,
                sharedViewModel.elmStatus.value,
                sharedViewModel.ecuStatus.value
            )
        }

        // Наблюдаем за состоянием подключения к ELM327
        sharedViewModel.elmStatus.observe(this) { elmState ->
            updateStatusIcon(
                statusIcon,
                homeViewModel.isDemoActive.value == true,
                elmState,
                sharedViewModel.ecuStatus.value
            )
        }

        // Наблюдаем за состоянием подключения к ЭБУ
        sharedViewModel.ecuStatus.observe(this) { ecuState ->
            updateStatusIcon(
                statusIcon,
                homeViewModel.isDemoActive.value == true,
                sharedViewModel.elmStatus.value,
                ecuState
            )
        }

        return true
    }

    private fun updateStatusIcon(
        menuItem: MenuItem,
        isDemo: Boolean?,
        elmState: ConnectionState?,
        ecuState: ConnectionState?
    ) {
        val iconResId = when {
            isDemo == true -> R.drawable.ic_demo_mode
            elmState == ConnectionState.CONNECTED && ecuState == ConnectionState.CONNECTED -> R.drawable.ic_connected
            else -> R.drawable.ic_status_off
        }
        menuItem.icon = ContextCompat.getDrawable(this, iconResId)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}