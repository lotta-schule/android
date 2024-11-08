package net.einsa.lotta.service

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.einsa.lotta.App
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.GetConversationsQuery
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

    suspend fun didReceiveRemoteNotification(remoteMessage: RemoteMessage) {
        Log.d("PushNotificationService", "Received remote notification: $remoteMessage")

        return withContext(Dispatchers.IO) {
            if (remoteMessage.data["category"] == "receive_message") {
                ModelData.instance.userSessions
                    .firstOrNull { it.tenant.id == remoteMessage.data["tenant_id"] }
                    ?.let { session ->
                        runCatching { session.forceReloadConversations() }
                        remoteMessage.data["conversation_id"]?.let { conversationId ->
                            runCatching { session.forceReloadConversation(conversationId) }
                        }
                    }
            } else if (remoteMessage.data["category"] == "read_conversation") {
                ModelData.instance.userSessions
                    .firstOrNull { it.tenant.id == remoteMessage.data["tenant_id"] }
                    ?.let { session ->
                        runCatching {
                            session.api.apollo.apolloStore.readOperation(GetConversationsQuery()).conversations?.let { conversations ->
                                session.api.apollo.apolloStore.writeOperation(
                                    GetConversationsQuery(),
                                    GetConversationsQuery.Data(conversations.map { conversation ->
                                        if (conversation?.id == remoteMessage.data["conversation_id"]) {
                                            conversation?.copy(unreadMessages = 0)
                                        } else {
                                            conversation
                                        }
                                    })
                                )
                            }
                        }

                        remoteMessage.data["conversation_id"]?.let { conversationId ->
                            runCatching {
                                session.api.apollo.apolloStore.readOperation(
                                    GetConversationQuery(
                                        conversationId
                                    )
                                ).conversation?.let { conversation ->
                                    session.api.apollo.apolloStore.writeOperation(
                                        GetConversationQuery(conversationId),
                                        GetConversationQuery.Data(conversation.copy(unreadMessages = 0))
                                    )
                                }
                            }
                        }
                    }

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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
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
                }
            } else {
                App.mainActivity.requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

}