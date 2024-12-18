package net.einsa.lotta

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.ModelData
import net.einsa.lotta.model.Theme
import net.einsa.lotta.service.PushNotificationService
import net.einsa.lotta.ui.theme.LottaTheme
import net.einsa.lotta.ui.view.RootView
import net.einsa.lotta.util.UserDefaults

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

        intent.extras?.getString("tenant_id")?.let { tenantId ->
            ModelData.instance.apply {
                if (ModelData.instance.initialized) {
                    setSession(tenantId)
                } else {
                    UserDefaults.instance.setTenantId(tenantId)
                }
            }
        }

        setContent {
            LottaTheme(Theme()) {
                CompositionLocalProvider(LocalModelData provides ModelData.instance) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White
                    ) {
                        RootView(mainActivity = this)
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
