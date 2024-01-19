package net.einsa.lotta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import io.sentry.Sentry
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.ModelData
import net.einsa.lotta.model.Theme
import net.einsa.lotta.ui.theme.LottaTheme
import net.einsa.lotta.ui.view.RootView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // waiting for view to draw to better represent a captured error with a screenshot
        findViewById<android.view.View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
            try {
                throw Exception("This app uses Sentry! :)")
            } catch (e: Exception) {
                Sentry.captureException(e)
            }
        }

        App.set(window.context.applicationContext)
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
}
