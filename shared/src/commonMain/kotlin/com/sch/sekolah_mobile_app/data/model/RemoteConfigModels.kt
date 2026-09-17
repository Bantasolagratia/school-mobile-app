package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfigResponse(
    val success: Boolean = true,
    val statusCode: Int = 200,
    val version: Long = 1,
    val etag: String = "",
    val features: Map<String, Boolean> = emptyMap()
)

@Serializable
data class FeatureFlagUpdateEvent(
    val flagKey: String = "",
    val isEnabled: Boolean = false,
    val version: Long = 1,
    val etag: String = "",
    val reason: String = ""
)

