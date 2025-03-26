package com.github.eltonvs.obd.command.temperature

import com.example.obd_servise.obd_connection.api.command.ObdCommand
import com.example.obd_servise.obd_connection.api.command.ObdRawResponse
import com.example.obd_servise.obd_connection.api.command.bytesToInt


private fun calculateTemperature(rawValue: IntArray): Float = bytesToInt(rawValue, bytesToProcess = 1) - 40f

class AirIntakeTemperatureCommand : ObdCommand() {
    override val tag = "AIR_INTAKE_TEMPERATURE"
    override val name = "Air Intake Temperature"
    override val mode = "01"
    override val pid = "0F"

    override val defaultUnit = "°C"
    override val handler = { it: ObdRawResponse -> "%.1f".format(calculateTemperature(it.bufferedValue)) }
}

class AmbientAirTemperatureCommand : ObdCommand() {
    override val tag = "AMBIENT_AIR_TEMPERATURE"
    override val name = "Ambient Air Temperature"
    override val mode = "01"
    override val pid = "46"

    override val defaultUnit = "°C"
    override val handler = { it: ObdRawResponse -> "%.1f".format(calculateTemperature(it.bufferedValue)) }
}

class EngineCoolantTemperatureCommand : ObdCommand() {
    override val tag = "ENGINE_COOLANT_TEMPERATURE"
    override val name = "Engine Coolant Temperature"
    override val mode = "01"
    override val pid = "05"

    override val defaultUnit = "°C"
    override val handler = { it: ObdRawResponse -> "%.1f".format(calculateTemperature(it.bufferedValue)) }
}

class OilTemperatureCommand : ObdCommand() {
    override val tag = "ENGINE_OIL_TEMPERATURE"
    override val name = "Engine Oil Temperature"
    override val mode = "01"
    override val pid = "5C"

    override val defaultUnit = "°C"
    override val handler = { it: ObdRawResponse -> "%.1f".format(calculateTemperature(it.bufferedValue)) }
}