package net.einsa.lotta

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.einsa.lotta.service.PushNotificationService

class LottaFirebaseMessagingService : FirebaseMessagingService() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("LottaFirebaseMessagingService", "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("LottaFirebaseMessagingService", "Message data payload: ${remoteMessage.data}")

            GlobalScope.launch {
                PushNotificationService.instance.didReceiveRemoteNotification(remoteMessage)
            }
        }

        remoteMessage.notification?.let {
            Log.d("LottaFirebaseMessagingService", "Message Notification Body: ${it.body}")
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        GlobalScope.launch {
            PushNotificationService.instance.registerDeviceToken()
        }
    }
}