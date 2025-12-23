package com.bhikan.airtap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val email: String,
    val deviceName: String,
    val createdAt: Long = System.currentTimeMillis()
)
