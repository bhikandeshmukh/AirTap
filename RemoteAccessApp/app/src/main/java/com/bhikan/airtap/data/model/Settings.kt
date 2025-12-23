package com.bhikan.airtap.data.model

data class AppSettings(
    val serverPort: Int = 8080,
    val autoStart: Boolean = false,
    val password: String = "",
    val remoteAccessEnabled: Boolean = false,
    val tunnelUrl: String = ""
)
