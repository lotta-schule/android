package net.einsa.lotta.util

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.einsa.lotta.App

/** Key/value store for secrets like refresh tokens. */
interface SecretStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

class SecretKeyStore(storeName: String = "lotta-auth") : SecretStore {

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
        val instance: SecretKeyStore by lazy { SecretKeyStore() }

        fun refreshTokenKey(userId: String, tenantId: String): String {
            return "$tenantId-$userId--refresh-token"
        }
    }

    override fun get(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun set(key: String, value: String) {
        synchronized(sharedPreferences) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    override fun remove(key: String) {
        synchronized(sharedPreferences) {
            sharedPreferences.edit().remove(key).apply()
        }
    }
}
