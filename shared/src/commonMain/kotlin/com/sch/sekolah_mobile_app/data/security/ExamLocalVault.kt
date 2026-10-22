package com.sch.sekolah_mobile_app.data.security

import com.sch.sekolah_mobile_app.data.model.UjianQuestionMobile
import com.sch.sekolah_mobile_app.data.storage.PlatformStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EncryptedVaultRecord(
    val iv: String,
    val cipher: String
)

@Serializable
data class SignedExamProgressPayload(
    val scheduleId: String,
    val ujianId: String? = null,
    val studentId: String,
    val startedAtMillis: Long,
    val lastSavedAtMillis: Long,
    val durationSeconds: Long = 0L,
    val answersByIndex: Map<String, String> = emptyMap(),
    val mappedAnswers: Map<String, String> = emptyMap(),
    val signature: String
)

object ExamLocalVault {
    // 256-bit AES key (32 bytes encoded in Base64) for local AES-GCM storage encryption
    private const val EXAM_VAULT_AES_KEY_B64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    // Shared HMAC-SHA256 secret matching ExamResultService on backend
    private const val EXAM_VAULT_HMAC_SECRET = "SekolahExamVaultHmacKey_2026_v1_9f8e7d6c5b4a3f2e1d0c"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun vaultKey(scheduleId: String, studentId: String): String =
        "exam_vault_${scheduleId}_${studentId}"

    private fun legacyAnswersKey(scheduleId: String, studentId: String): String =
        "exam_answers_${scheduleId}_${studentId}"

    private fun startKey(scheduleId: String, studentId: String): String =
        "exam_start_time_${scheduleId}_${studentId}"

    private fun flaggedKey(scheduleId: String, studentId: String): String =
        "exam_flagged_${scheduleId}_${studentId}"

    fun canonicalizeMap(map: Map<String, String>): String {
        if (map.isEmpty()) return ""
        return map.keys.sorted().joinToString("&") { k -> "$k=${map[k] ?: ""}" }
    }

    fun computePayloadSignature(
        scheduleId: String,
        studentId: String,
        clientSavedAtMillis: Long,
        mappedAnswers: Map<String, String>
    ): String {
        val canonical = canonicalizeMap(mappedAnswers)
        val data = "${scheduleId.trim()}|${studentId.trim()}|$clientSavedAtMillis|$canonical"
        return PlatformCrypto.hmacSha256B64(EXAM_VAULT_HMAC_SECRET, data)
    }

    private fun computeVaultInternalSignature(
        scheduleId: String,
        studentId: String,
        startedAtMillis: Long,
        lastSavedAtMillis: Long,
        answersByIndex: Map<String, String>,
        mappedAnswers: Map<String, String>
    ): String {
        val canonicalIdx = canonicalizeMap(answersByIndex)
        val canonicalMapped = canonicalizeMap(mappedAnswers)
        val data = "${scheduleId.trim()}|${studentId.trim()}|$startedAtMillis|$lastSavedAtMillis|$canonicalIdx|$canonicalMapped"
        return PlatformCrypto.hmacSha256B64(EXAM_VAULT_HMAC_SECRET, data)
    }

    fun buildMappedAnswers(
        questions: List<UjianQuestionMobile>,
        studentAnswers: Map<Int, String>
    ): Map<String, String> {
        val mapped = mutableMapOf<String, String>()
        if (questions.isNotEmpty()) {
            questions.forEachIndexed { idx, q ->
                val ans = studentAnswers[idx]
                if (!ans.isNullOrBlank()) {
                    val key = q.id ?: (idx + 1).toString()
                    mapped[key] = ans
                    mapped[(idx + 1).toString()] = ans
                }
            }
        } else {
            studentAnswers.forEach { (idx, ans) ->
                if (ans.isNotBlank()) {
                    mapped[(idx + 1).toString()] = ans
                    mapped[idx.toString()] = ans
                }
            }
        }
        return mapped
    }

    fun saveSignedProgress(
        storage: PlatformStorage,
        scheduleId: String,
        ujianId: String?,
        studentId: String,
        startedAtMillis: Long,
        lastSavedAtMillis: Long,
        durationSeconds: Long,
        studentAnswers: Map<Int, String>,
        questions: List<UjianQuestionMobile>
    ) {
        if (scheduleId.isBlank() || studentId.isBlank()) return
        try {
            val answersByIndex = studentAnswers.mapKeys { it.key.toString() }
            val mappedAnswers = buildMappedAnswers(questions, studentAnswers)
            val sig = computeVaultInternalSignature(
                scheduleId = scheduleId,
                studentId = studentId,
                startedAtMillis = startedAtMillis,
                lastSavedAtMillis = lastSavedAtMillis,
                answersByIndex = answersByIndex,
                mappedAnswers = mappedAnswers
            )
            val payload = SignedExamProgressPayload(
                scheduleId = scheduleId,
                ujianId = ujianId,
                studentId = studentId,
                startedAtMillis = startedAtMillis,
                lastSavedAtMillis = lastSavedAtMillis,
                durationSeconds = durationSeconds,
                answersByIndex = answersByIndex,
                mappedAnswers = mappedAnswers,
                signature = sig
            )
            val plainJson = json.encodeToString(payload)
            val (ivB64, cipherB64) = PlatformCrypto.encryptAesGcm(EXAM_VAULT_AES_KEY_B64, plainJson)
            val record = EncryptedVaultRecord(iv = ivB64, cipher = cipherB64)
            storage.setString(vaultKey(scheduleId, studentId), json.encodeToString(record))
            // Hapus key plain-text lama agar tidak bisa diedit manual di SharedPreferences
            storage.remove(legacyAnswersKey(scheduleId, studentId))
        } catch (_: Exception) {}
    }

    fun loadSignedProgress(
        storage: PlatformStorage,
        scheduleId: String,
        studentId: String
    ): SignedExamProgressPayload? {
        if (scheduleId.isBlank() || studentId.isBlank()) return null

        // 1. Prioritaskan baca dari Encrypted + Signed Vault
        val encryptedStr = storage.getString(vaultKey(scheduleId, studentId))
        if (!encryptedStr.isNullOrBlank()) {
            return try {
                val record = json.decodeFromString<EncryptedVaultRecord>(encryptedStr)
                val decryptedJson = PlatformCrypto.decryptAesGcm(EXAM_VAULT_AES_KEY_B64, record.iv, record.cipher)
                val payload = json.decodeFromString<SignedExamProgressPayload>(decryptedJson)

                // Verifikasi tanda tangan digital HMAC-SHA256 untuk mendeteksi tampering
                val expectedSig = computeVaultInternalSignature(
                    scheduleId = payload.scheduleId,
                    studentId = payload.studentId,
                    startedAtMillis = payload.startedAtMillis,
                    lastSavedAtMillis = payload.lastSavedAtMillis,
                    answersByIndex = payload.answersByIndex,
                    mappedAnswers = payload.mappedAnswers
                )
                if (expectedSig.isNotEmpty() && payload.signature != expectedSig) {
                    // Terdeteksi manipulasi pada vault lokal!
                    null
                } else {
                    payload
                }
            } catch (_: Exception) {
                // Gagal dekripsi AES-GCM (Authentication Tag mismatch / data dimodifikasi)
                null
            }
        }

        // 2. Fallback migrasi 1x untuk data lama (legacy exam_answers_...) jika masih ada di HP siswa
        val legacySaved = storage.getString(legacyAnswersKey(scheduleId, studentId))
        if (!legacySaved.isNullOrBlank()) {
            return try {
                val map = json.decodeFromString<Map<String, String>>(legacySaved)
                if (map.isEmpty()) return null
                val startedAt = storage.getString(startKey(scheduleId, studentId))?.toLongOrNull() ?: 0L
                val intMap = mutableMapOf<Int, String>()
                map.forEach { (k, v) -> k.toIntOrNull()?.let { intMap[it] = v } }
                val mapped = buildMappedAnswers(emptyList(), intMap)
                val sig = computeVaultInternalSignature(
                    scheduleId = scheduleId,
                    studentId = studentId,
                    startedAtMillis = startedAt,
                    lastSavedAtMillis = startedAt,
                    answersByIndex = map,
                    mappedAnswers = mapped
                )
                val migrated = SignedExamProgressPayload(
                    scheduleId = scheduleId,
                    ujianId = null,
                    studentId = studentId,
                    startedAtMillis = startedAt,
                    lastSavedAtMillis = startedAt,
                    durationSeconds = 0L,
                    answersByIndex = map,
                    mappedAnswers = mapped,
                    signature = sig
                )
                // Segel langsung ke Encrypted Vault dan hapus plain JSON lama
                saveSignedProgress(
                    storage = storage,
                    scheduleId = scheduleId,
                    ujianId = null,
                    studentId = studentId,
                    startedAtMillis = startedAt,
                    lastSavedAtMillis = startedAt,
                    durationSeconds = 0L,
                    studentAnswers = intMap,
                    questions = emptyList()
                )
                migrated
            } catch (_: Exception) {
                null
            }
        }

        return null
    }

    fun clearExamProgress(storage: PlatformStorage, scheduleId: String, studentId: String) {
        storage.remove(vaultKey(scheduleId, studentId))
        storage.remove(legacyAnswersKey(scheduleId, studentId))
        storage.remove(startKey(scheduleId, studentId))
        storage.remove(flaggedKey(scheduleId, studentId))
    }
}

