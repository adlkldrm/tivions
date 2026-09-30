package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure persistent storage for user credentials and IPTV sources.
 * Encrypts sensitive credentials before writing to SharedPreferences.
 * Prevents plain passwords and URLs from appearing in storage or logs.
 */
class SecureAuthPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "tivions_secure_auth"
        private const val KEY_REMEMBER = "sec_remember"
        private const val KEY_SOURCE_TYPE = "sec_source_type"
        private const val KEY_NAME = "sec_name"
        private const val KEY_SERVER = "sec_server_enc"
        private const val KEY_USER = "sec_user_enc"
        private const val KEY_PASS = "sec_pass_enc"
        private const val KEY_M3U_URL = "sec_m3u_enc"

        // Obfuscation key for local credential security
        private val AES_KEY = byteArrayOf(
            0x2A.toByte(), 0x5F.toByte(), 0x11.toByte(), 0x9C.toByte(),
            0x4B.toByte(), 0x88.toByte(), 0x72.toByte(), 0x3D.toByte(),
            0x6E.toByte(), 0x0A.toByte(), 0x44.toByte(), 0x99.toByte(),
            0x1B.toByte(), 0x7E.toByte(), 0x33.toByte(), 0x2C.toByte()
        )
        private val FIXED_IV = byteArrayOf(
            0x15.toByte(), 0x33.toByte(), 0x5A.toByte(), 0x77.toByte(),
            0x09.toByte(), 0x22.toByte(), 0x48.toByte(), 0x61.toByte(),
            0x7C.toByte(), 0x1E.toByte(), 0x3F.toByte(), 0x50.toByte(),
            0x6D.toByte(), 0x02.toByte(), 0x19.toByte(), 0x4A.toByte()
        )

        private fun encrypt(value: String): String {
            if (value.isBlank()) return ""
            return try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val keySpec = SecretKeySpec(AES_KEY, "AES")
                val ivSpec = IvParameterSpec(FIXED_IV)
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
                val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            } catch (_: Exception) {
                ""
            }
        }

        private fun decrypt(encryptedBase64: String?): String {
            if (encryptedBase64.isNullOrBlank()) return ""
            return try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val keySpec = SecretKeySpec(AES_KEY, "AES")
                val ivSpec = IvParameterSpec(FIXED_IV)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                val decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                String(cipher.doFinal(decoded), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
    }

    var isRemembered: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    var sourceType: String
        get() = prefs.getString(KEY_SOURCE_TYPE, "xtream") ?: "xtream"
        set(value) = prefs.edit().putString(KEY_SOURCE_TYPE, value).apply()

    var playlistName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var serverUrl: String
        get() = decrypt(prefs.getString(KEY_SERVER, ""))
        set(value) = prefs.edit().putString(KEY_SERVER, encrypt(value)).apply()

    var username: String
        get() = decrypt(prefs.getString(KEY_USER, ""))
        set(value) = prefs.edit().putString(KEY_USER, encrypt(value)).apply()

    var password: String
        get() = decrypt(prefs.getString(KEY_PASS, ""))
        set(value) = prefs.edit().putString(KEY_PASS, encrypt(value)).apply()

    var m3uUrl: String
        get() = decrypt(prefs.getString(KEY_M3U_URL, ""))
        set(value) = prefs.edit().putString(KEY_M3U_URL, encrypt(value)).apply()

    fun saveXtreamCredentials(name: String, host: String, user: String, pass: String, remember: Boolean) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, remember)
            .putString(KEY_SOURCE_TYPE, "xtream")
            .putString(KEY_NAME, name)
            .putString(KEY_SERVER, encrypt(host))
            .putString(KEY_USER, encrypt(user))
            .putString(KEY_PASS, encrypt(pass))
            .apply()
    }

    fun saveM3uCredentials(name: String, url: String, remember: Boolean) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, remember)
            .putString(KEY_SOURCE_TYPE, "m3u")
            .putString(KEY_NAME, name)
            .putString(KEY_M3U_URL, encrypt(url))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
