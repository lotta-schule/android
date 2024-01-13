package net.einsa.lotta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.ModelData
import net.einsa.lotta.ui.theme.LottaTheme
import net.einsa.lotta.ui.view.RootView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.set(window.context.applicationContext)
        setContent {
            LottaTheme {
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
