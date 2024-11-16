package net.einsa.lotta.util

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.einsa.lotta.App


class SecretKeyStore(storeName: String = "lotta-auth") {

    private val masterKeyAlias = MasterKey.Builder(App.context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize EncryptedSharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        App.context,
        storeName,
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        val instance = SecretKeyStore()

        fun refreshTokenKey(userId: String, tenantId: String): String {
            return "$tenantId-$userId--refresh-token"
        }
    }

    fun get(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun set(key: String, value: String) {
        synchronized(sharedPreferences) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}