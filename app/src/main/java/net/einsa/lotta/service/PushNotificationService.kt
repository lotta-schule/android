package net.einsa.lotta.service

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.einsa.lotta.App
import net.einsa.lotta.composition.ModelData

class PushNotificationService {
    companion object {
        val instance = PushNotificationService()
    }

    suspend fun registerDeviceToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d("PushNotificationService", "Firebase token: $token")

        return withContext(Dispatchers.IO) {
            ModelData.instance.userSessions.map { session ->
                session.runCatching { registerDevice(token) }
            }
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun startReceivingNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    App.mainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "Permission for push notifications granted")
                GlobalScope.launch {
                    registerDeviceToken()
                }
            } else if (shouldShowRequestPermissionRationale(
                    App.mainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                AlertDialog.Builder(App.context)
                    .setTitle("Push-Benachrichtigungen")
                    .setMessage(
                        """
                        Möchtest du Push-Benachrichtigungen aktivieren?
                        Du kannst diese Einstellung später in den App-Einstellungen ändern.
                        So wirst du benachrichtigt, wenn du eine neue Nachricht erhältst.
                        """.trimIndent()
                    )
                    .setIcon(android.R.drawable.ic_notification_overlay)
                    .setPositiveButton("aktivieren") { _: DialogInterface, _: Int ->
                        App.mainActivity.requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("überspringen", null)
                    .show()
            } else {
                App.mainActivity.requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}