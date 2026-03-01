package com.traverse.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure token storage using EncryptedSharedPreferences.
 * Equivalent to iOS KeychainHelper.
 */
class TokenManager private constructor(context: Context) {
    
    // backing preferences instance. we try to use encrypted storage but fall back
    // to a plain shared preferences if something goes wrong (e.g. on devices
    // without the required crypto provider or during tests).
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                "traverse_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // encryption initialization failed, fall back to normal prefs so the
            // app can still start. this prevents the ViewModel factory from
            // crashing and gives us a chance to recover later.
            android.util.Log.e("TokenManager", "Unable to create encrypted prefs, using fallback", e)
            context.applicationContext.getSharedPreferences(
                "traverse_secure_prefs",
                Context.MODE_PRIVATE
            )
        }
    }
    
    fun saveToken(token: String): Boolean {
        return sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .commit()
    }
    
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun deleteToken() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }
    
    fun isAuthenticated(): Boolean {
        return getToken() != null
    }
    
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        
        @Volatile
        private var INSTANCE: TokenManager? = null
        
        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
