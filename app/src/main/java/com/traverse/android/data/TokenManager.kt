package com.traverse.android.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Device-bound secret storage backed by Android Keystore AES-GCM. */
internal interface AuthTokenStore {
    fun saveToken(token: String): Boolean
    fun getToken(): String?
    fun deleteToken()
    fun isAuthenticated(): Boolean
}

internal class TokenManager private constructor(context: Context) : AuthTokenStore {
    private val appContext = context.applicationContext
    private val ciphertextPreferences: SharedPreferences = appContext.getSharedPreferences(
        CIPHERTEXT_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val legacyPreferences: SharedPreferences? = openLegacyPreferences(appContext)

    @Synchronized
    override fun saveToken(token: String): Boolean = saveSecret(KEY_AUTH_TOKEN, token)

    @Synchronized
    override fun getToken(): String? = getSecret(KEY_AUTH_TOKEN)

    @Synchronized
    override fun deleteToken() = deleteSecret(KEY_AUTH_TOKEN)

    override fun isAuthenticated(): Boolean = !getToken().isNullOrBlank()

    @Synchronized
    fun saveSecret(key: String, value: String): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val payload = ByteBuffer.allocate(1 + cipher.iv.size + ciphertext.size)
                .put(cipher.iv.size.toByte())
                .put(cipher.iv)
                .put(ciphertext)
                .array()
            val stored = Base64.encodeToString(payload, Base64.NO_WRAP)
            if (!ciphertextPreferences.edit().putString(key, stored).commit()) return false
            legacyPreferences?.edit()?.remove(key)?.commit()
            true
        } catch (error: Exception) {
            Log.e(TAG, "Unable to store an encrypted secret", error)
            false
        }
    }

    @Synchronized
    fun getSecret(key: String): String? {
        val stored = ciphertextPreferences.getString(key, null)
        if (stored != null) {
            return runCatching { decrypt(stored) }
                .onFailure { Log.e(TAG, "Unable to decrypt a stored secret", it) }
                .getOrNull()
        }

        // One-time migration from the old EncryptedSharedPreferences file. New
        // writes use Android Keystore directly; the old file is never a fallback.
        val legacy = runCatching { legacyPreferences?.getString(key, null) }
            .onFailure { Log.e(TAG, "Unable to read legacy encrypted secret", it) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        if (!saveSecret(key, legacy)) return legacy
        return legacy
    }

    @Synchronized
    fun deleteSecret(key: String) {
        ciphertextPreferences.edit().remove(key).commit()
        legacyPreferences?.edit()?.remove(key)?.commit()
        deleteKeyWhenEmpty()
    }

    @Synchronized
    fun clearAllSecrets() {
        ciphertextPreferences.edit().clear().commit()
        legacyPreferences?.edit()?.clear()?.commit()
        deleteKeystoreKey()
    }

    private fun decrypt(stored: String): String {
        val payload = Base64.decode(stored, Base64.NO_WRAP)
        require(payload.isNotEmpty()) { "Invalid encrypted secret" }
        val buffer = ByteBuffer.wrap(payload)
        val ivSize = buffer.get().toInt() and 0xff
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "Invalid encrypted secret" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun deleteKeyWhenEmpty() {
        if (ciphertextPreferences.all.isEmpty() && legacyPreferences?.all.isNullOrEmpty()) {
            deleteKeystoreKey()
        }
    }

    private fun deleteKeystoreKey() {
        runCatching {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }.onFailure { Log.w(TAG, "Unable to remove the local encryption key", it) }
    }

    companion object {
        private const val TAG = "TokenManager"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val CIPHERTEXT_PREFERENCES = "traverse_secure_ciphertext"
        private const val LEGACY_PREFERENCES = "traverse_secure_prefs"
        private const val KEY_ALIAS = "com.traverse.android.account-secrets.v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun openLegacyPreferences(context: Context): SharedPreferences? {
            val file = File(context.applicationInfo.dataDir, "shared_prefs/$LEGACY_PREFERENCES.xml")
            if (!file.exists()) return null
            return runCatching {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    LEGACY_PREFERENCES,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.onFailure { Log.w(TAG, "Legacy encrypted preferences could not be migrated", it) }
                .getOrNull()
        }
    }
}
