package com.example.obd_servise.ui.connection

enum class ConnectionState {
    DISCONNECTED, // Отключено
    CONNECTING,   // Ожидание подключения
    CONNECTED,    // Подключено
    DEMO,
    ERROR         // Ошибка подключения
}