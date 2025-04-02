package com.example.obd_servise.ui.dynamics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.obd_servise.R
import com.example.obd_servise.obd_connection.bluetooth.ObdManager
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import kotlinx.coroutines.*

class DynamicsFragment : Fragment() {
    private lateinit var speedTextView: TextView
    private lateinit var rpmTextView: TextView
    private lateinit var obdManager: ObdManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dynamics, container, false)
        speedTextView = view.findViewById(R.id.speedTextView)
        rpmTextView = view.findViewById(R.id.rpmTextView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        obdManager = ObdManager(requireContext())
        startReadingObdData()
    }

    private fun startReadingObdData() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // Отправляем команды и получаем ответы
                    val speedResponse = obdManager.sendCommand(SpeedCommand(), delay(200))
                    val rpmResponse = obdManager.sendCommand(RPMCommand(), delay(200))

                    // Обрабатываем ответы
                    val speed =
                        if (speedResponse?.value != null && !speedResponse.value.contains("ERROR")) {
                            "${speedResponse.value} ${speedResponse.unit ?: "км/ч"}"
                        } else {
                            "N/A"
                        }

                    val rpm =
                        if (rpmResponse?.value != null && !rpmResponse.value.contains("ERROR")) {
                            "${rpmResponse.value} ${rpmResponse.unit ?: "об/мин"}"
                        } else {
                            "N/A"
                        }

                    // Обновляем UI
                    withContext(Dispatchers.Main) {
                        speedTextView.text = "Скорость: $speed"
                        rpmTextView.text = "Обороты: $rpm"
                    }
                } catch (e: Exception) {
                    // Логируем ошибку и обновляем UI значением "N/A"
                    Log.e("DynamicsFragment", "Error reading OBD data", e)
                    withContext(Dispatchers.Main) {
                        speedTextView.text = "Скорость: N/A"
                        rpmTextView.text = "Обороты: N/A"
                    }
                }

                // Задержка перед следующим запросом
                delay(1000)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        obdManager.disconnect()
    }
}