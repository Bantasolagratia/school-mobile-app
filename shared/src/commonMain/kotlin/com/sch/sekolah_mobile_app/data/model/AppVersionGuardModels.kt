package com.sch.sekolah_mobile_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedVersionGuardResponse(
    val payload: String = "",
    val iv: String = "",
    val signature: String = "",
    val timestamp: Long = 0L
)

@Serializable
data class DecryptedAppVersionPayload(
    val nonce: String = "",
    val platform: String = "",
    val minVersionCode: Long = 1L,
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val updateUrl: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

sealed class VersionCheckResult {
    object Idle : VersionCheckResult()
    object Checking : VersionCheckResult()
    object UpToDate : VersionCheckResult()
    data class UpdateOptional(val payload: DecryptedAppVersionPayload) : VersionCheckResult()
    data class ForceUpdateRequired(val payload: DecryptedAppVersionPayload) : VersionCheckResult()
    data class TamperingDetected(val reason: String) : VersionCheckResult()
    data class NetworkError(val error: String) : VersionCheckResult()
}
