package com.sch.sekolah_mobile_app.data.security

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object PlatformCrypto {

    actual fun verifyRsaSignature(publicKeyB64: String, data: ByteArray, signatureB64: String): Boolean {
        return try {
            val pubBytes = Base64.decode(publicKeyB64.trim(), Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(pubBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val sigBytes = Base64.decode(signatureB64.trim(), Base64.DEFAULT)
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    actual fun decryptAesGcm(secretKeyB64: String, ivB64: String, encryptedPayloadB64: String): String {
        val keyBytes = Base64.decode(secretKeyB64.trim(), Base64.DEFAULT)
        val ivBytes = Base64.decode(ivB64.trim(), Base64.DEFAULT)
        val cipherBytes = Base64.decode(encryptedPayloadB64.trim(), Base64.DEFAULT)

        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decrypted = cipher.doFinal(cipherBytes)
        return decrypted.decodeToString()
    }

    actual fun encryptAesGcm(secretKeyB64: String, plaintext: String): Pair<String, String> {
        val keyBytes = Base64.decode(secretKeyB64.trim(), Base64.DEFAULT)
        val ivBytes = ByteArray(12)
        SecureRandom().nextBytes(ivBytes)

        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherBytes = cipher.doFinal(plaintext.encodeToByteArray())

        val ivB64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return ivB64 to cipherB64
    }

    actual fun hmacSha256B64(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.encodeToByteArray(), "HmacSHA256")
        mac.init(keySpec)
        val raw = mac.doFinal(data.encodeToByteArray())
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }
}
