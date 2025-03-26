package com.example.obd_servise.obd_connection.api.command


abstract class ATCommand : ObdCommand() {
    override val mode = "AT"
    override val skipDigitCheck = true
}