package com.example.obd_servise.obd_connection.api.command.egr

import com.example.obd_servise.obd_connection.api.command.ObdCommand
import com.example.obd_servise.obd_connection.api.command.ObdRawResponse
import com.example.obd_servise.obd_connection.api.command.bytesToInt
import com.example.obd_servise.obd_connection.api.command.calculatePercentage

class CommandedEgrCommand : ObdCommand() {
    override val tag = "COMMANDED_EGR"
    override val name = "Commanded EGR"
    override val mode = "01"
    override val pid = "2C"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(calculatePercentage(it.bufferedValue, bytesToProcess = 1)) }
}

class EgrErrorCommand : ObdCommand() {
    override val tag = "EGR_ERROR"
    override val name = "EGR Error"
    override val mode = "01"
    override val pid = "2D"

    override val defaultUnit = "%"
    override val handler = { it: ObdRawResponse -> "%.1f".format(bytesToInt(it.bufferedValue, bytesToProcess = 1) * (100f / 128) - 100) }
}
