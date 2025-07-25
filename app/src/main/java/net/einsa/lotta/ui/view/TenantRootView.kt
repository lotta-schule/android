package net.einsa.lotta.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.einsa.lotta.composition.LocalTheme
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.ui.theme.LottaTheme

@Composable
fun TenantRootView(
    session: UserSession
) {
    CompositionLocalProvider(LocalUserSession provides session) {
        CompositionLocalProvider(LocalTheme provides session.tenant.customTheme) {
            LottaTheme(session.tenant.customTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red)
                ) {
                    session.tenant.backgroundImageUrl?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(it)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                    MainView()
                }
            }
        }
    }
}