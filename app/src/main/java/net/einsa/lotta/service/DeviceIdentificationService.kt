package net.einsa.lotta.service

import android.content.res.Configuration
import android.os.Build
import com.google.firebase.installations.FirebaseInstallations
import net.einsa.lotta.App
import java.util.Locale

class DeviceIdentificationService {
    companion object {
        val instance = DeviceIdentificationService()
    }

    fun getDeviceType(): String {
        val xlarge = App.context.resources
            .configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == 4
        val large = App.context.resources
            .configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE

        return if (xlarge || large) {
            "tablet"
        } else {
            "phone"
        }
    }

    fun getModelName(): String {
        val manufacturer = Build.MANUFACTURER;
        val model = Build.MODEL;
        return if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            model
        } else {
            "$manufacturer $model";
        }
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    suspend fun getDeviceIdentifier(): String? {
        return try {
            FirebaseInstallations.getInstance().id.result
        } catch (e: Exception) {
            null
        }
    }
}