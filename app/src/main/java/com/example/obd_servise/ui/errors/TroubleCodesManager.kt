package com.example.obd_servise.ui.errors

import com.example.obd_servise.obd_connection.api.command.ObdCommand
import com.example.obd_servise.obd_connection.api.command.control.ResetTroubleCodesCommand
import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
import java.util.regex.Pattern

class TroubleCodesManager(private val obdInterface: ObdDeviceConnection) {

    private val testErrors = listOf("P0420", "P0171")

    suspend fun getTroubleCodes(): List<String> {
        return try {
            val response = obdInterface.run(
                TroubleCodesCommand(),
                delayTime = 1000L // Пример задержки в 1 секунду
            )
            val realCodes = TroubleCodesCommand().parseResponse(response.toString())
            realCodes + testErrors // Добавляем тестовые ошибки
        } catch (e: Exception) {
            testErrors // Если OBD не подключен, показываем только тестовые ошибки
        }
    }

    suspend fun getPendingTroubleCodes(): List<String> {
        return try {
            val response = obdInterface.run(
                PendingTroubleCodesCommand(),
                delayTime = 1000L // Пример задержки в 1 секунду
            )
            PendingTroubleCodesCommand().parseResponse(response.toString())
        } catch (e: Exception) {
            emptyList() // Если не удалось получить данные, возвращаем пустой список
        }
    }

    suspend fun getPermanentTroubleCodes(): List<String> {
        return try {
            val response = obdInterface.run(
                PermanentTroubleCodesCommand(),
                delayTime = 1000L // Пример задержки в 1 секунду
            )
            PermanentTroubleCodesCommand().parseResponse(response.toString())
        } catch (e: Exception) {
            emptyList() // Если не удалось получить данные, возвращаем пустой список
        }
    }

    suspend fun resetTroubleCodes() {
        try {
            obdInterface.run(
                ResetTroubleCodesCommand(),
                delayTime = 1000L // Пример задержки в 1 секунду
            )
        } catch (e: Exception) {
            // Логируем ошибку, если не удалось сбросить коды
        }
    }
}

abstract class BaseTroubleCodesCommand : ObdCommand() {
    override val pid = ""
    abstract val carriageNumberPattern: Pattern

    fun parseResponse(rawValue: String?): List<String> {
        val workingData = rawValue?.replace(carriageNumberPattern.toRegex(), "") ?: ""
        return workingData.chunked(4).map { "P$it" }
    }
}

class TroubleCodesCommand : BaseTroubleCodesCommand() {
    override val tag = "TROUBLE_CODES"
    override val name = "Trouble Codes"
    override val mode = "03"
    override val carriageNumberPattern: Pattern = Pattern.compile("^43|[\r\n]43|[\r\n]")
}

class PendingTroubleCodesCommand : BaseTroubleCodesCommand() {
    override val tag = "PENDING_TROUBLE_CODES"
    override val name = "Pending Trouble Codes"
    override val mode = "07"
    override val carriageNumberPattern: Pattern = Pattern.compile("^47|[\r\n]47|[\r\n]")
}

class PermanentTroubleCodesCommand : BaseTroubleCodesCommand() {
    override val tag = "PERMANENT_TROUBLE_CODES"
    override val name = "Permanent Trouble Codes"
    override val mode = "0A"
    override val carriageNumberPattern: Pattern = Pattern.compile("^4A|[\r\n]4A|[\r\n]")
}
