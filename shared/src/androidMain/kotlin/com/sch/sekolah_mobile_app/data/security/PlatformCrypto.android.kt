package com.sch.sekolah_mobile_app.data.security

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
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
}
