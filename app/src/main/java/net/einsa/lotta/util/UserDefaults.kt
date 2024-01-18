package net.einsa.lotta.util

import android.content.SharedPreferences
import net.einsa.lotta.App
import net.einsa.lotta.model.ID

class UserDefaults {
    companion object {
        var instance: UserDefaults = UserDefaults()
    }

    private val sharedPreferences: SharedPreferences = App.get().getSharedPreferences("lotta", 0)

    fun setTenantId(tenantId: ID) {
        sharedPreferences.edit().apply {
            putString("tenantId", tenantId)
            apply()
        }
    }

    fun getTenantId(): ID? {
        return sharedPreferences.getString("tenantId", null)
    }

    fun removeTenantId() {
        sharedPreferences.edit().apply {
            remove("tenantId")
            apply()
        }
    }
}