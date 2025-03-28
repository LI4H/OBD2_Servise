package com.example.obd_servise

enum class ObdProtocols(val displayName: String, internal val command: String) {
    AUTO("Auto", "0"),
    SAE_J1850_PWM("SAE J1850 PWM", "1"),
    ISO_9141_2("ISO 9141-2", "3"),
    // другие протоколы
}

// Использование в коде для выбора нужного протокола:
//val selectedProtocol = ObdProtocols.ISO_15765_4_CAN
