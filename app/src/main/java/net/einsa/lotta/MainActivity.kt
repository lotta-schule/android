package net.einsa.lotta

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import io.sentry.Sentry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.ModelData
import net.einsa.lotta.model.Theme
import net.einsa.lotta.service.PushNotificationService
import net.einsa.lotta.ui.theme.LottaTheme
import net.einsa.lotta.ui.view.RootView

class MainActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Permission for push notifications granted")
            GlobalScope.launch {
                PushNotificationService.instance.registerDeviceToken()
            }
        } else {
            Log.d("MainActivity", "Permission for push notifications denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.set(window.context.applicationContext)
        App.set(this)

        // waiting for view to draw to better represent a captured error with a screenshot
        findViewById<android.view.View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
            try {
                throw Exception("This app uses Sentry! :)")
            } catch (e: Exception) {
                Sentry.captureException(e)
            }
        }

        setContent {
            val theme = ModelData.instance.currentSession?.tenant?.customTheme ?: Theme()
            LottaTheme(theme) {
                CompositionLocalProvider(LocalModelData provides ModelData.instance) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        RootView()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.unset()
    }
}
