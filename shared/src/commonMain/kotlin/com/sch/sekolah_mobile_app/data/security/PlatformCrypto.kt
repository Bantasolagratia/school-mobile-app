package com.sch.sekolah_mobile_app.data.security

expect object PlatformCrypto {
    fun verifyRsaSignature(publicKeyB64: String, data: ByteArray, signatureB64: String): Boolean
    fun decryptAesGcm(secretKeyB64: String, ivB64: String, encryptedPayloadB64: String): String
}
