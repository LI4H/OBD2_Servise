package com.example.obd_servise

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
import com.example.obd_servise.ui.home.HomeViewModel
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
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

        homeViewModel.isDemoActive.observe(this) { isDemo ->
            updateStatusIcon(statusIcon, isDemo, homeViewModel.connectionStatus.value == true)
        }
        homeViewModel.connectionStatus.observe(this) { isConnected ->
            updateStatusIcon(statusIcon, homeViewModel.isDemoActive.value == true, isConnected)
        }

        return true
    }

    private fun updateStatusIcon(menuItem: MenuItem, isDemo: Boolean?, isConnected: Boolean?) {
        menuItem.icon = ContextCompat.getDrawable(
            this, when {
                isDemo == true -> R.drawable.ic_demo_mode
                isConnected == true -> R.drawable.ic_connected
                else -> R.drawable.ic_status_off
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}