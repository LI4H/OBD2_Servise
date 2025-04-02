package com.example.obd_servise.obd_connection.bluetooth

import android.util.Log
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.ObdResponse
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DeviceConnection(
    private val inputStream: InputStream,
    private val outputStream: OutputStream
) {

    /**
     * Выполняет OBD команду и возвращает результат.
     */
    suspend fun run(command: ObdCommand): ObdResponse? {
        return try {
            // Отправляем команду
            withContext(Dispatchers.IO) {
                outputStream.write("${command.rawCommand}\r".toByteArray())
                outputStream.flush()
            }

            // Читаем ответ
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis() // Время начала выполнения команды
            val bytesRead = withContext(Dispatchers.IO) {
                inputStream.read(buffer)
            }
            val rawResponse = String(buffer, 0, bytesRead)

            // Создаем объект ObdRawResponse
            val elapsedTime = System.currentTimeMillis() - startTime // Вычисляем время выполнения
            val obdRawResponse = ObdRawResponse(
                value = rawResponse.trim(), // Убираем лишние пробелы из сырого ответа
                elapsedTime = elapsedTime
            )

            // Используем метод handleResponse из ObdCommand для обработки ответа
            command.handleResponse(obdRawResponse)
        } catch (e: IOException) {
            Log.e("DeviceConnection", "Error running command: ${command.tag}", e)
            null
        }
    }

    /**
     * Закрывает потоки ввода/вывода.
     */
    fun close() {
        try {
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            Log.e("DeviceConnection", "Error closing streams", e)
        }
    }
}