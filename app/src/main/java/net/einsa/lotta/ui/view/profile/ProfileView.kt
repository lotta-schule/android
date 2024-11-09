package net.einsa.lotta.ui.view.profile

import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoorBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass.Companion.EXPANDED
import net.einsa.lotta.App
import net.einsa.lotta.api.LOTTA_API_HOST
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.LocalTheme
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.ui.component.LottaButton
import net.einsa.lotta.ui.view.login.CreateLoginSessionView

@Composable
fun ProfileView(
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
    onSwitchProfile: () -> Unit
) {
    var showLoginDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    val modelData = LocalModelData.current
    val currentSession = LocalUserSession.current
    val theme = LocalTheme.current

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .background(Color(theme.boxBackgroundColor.toArgb()))
            .fillMaxSize()
    ) {
        if (showLoginDialog) {
            Dialog(
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    usePlatformDefaultWidth = windowSizeClass.windowWidthSizeClass == EXPANDED
                ),
                onDismissRequest = { showLoginDialog = false }
            ) {
                CreateLoginSessionView(
                    onLogin = { userSession ->
                        showLoginDialog = false
                        modelData.add(userSession)
                    },
                    defaultEmail = currentSession.user.email,
                    onDismiss = { showLoginDialog = false },
                    modifier = Modifier.fillMaxHeight(.9f)
                )
            }
        }

        if (showFeedbackDialog) {
            Dialog(
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    usePlatformDefaultWidth = windowSizeClass.windowWidthSizeClass == EXPANDED
                ),
                onDismissRequest = { showFeedbackDialog = false }
            ) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false },
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .widthIn(min = 300.dp, max = 500.dp)
                .fillMaxHeight()
                .padding(theme.spacing)
                .background(Color(theme.boxBackgroundColor.toArgb()))
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 300.dp, max = 500.dp)
            ) {
                Text("Angemeldet als:", modifier = Modifier.fillMaxWidth())
                Column(Modifier.padding(top = theme.spacing)) {
                    modelData.userSessions.forEach { session ->
                        UserSessionListItem(
                            session = session,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            isSelected = session == currentSession,
                            onSelect = {
                                if (session != currentSession) {
                                    modelData.setSession(session.tenant.id)
                                    onSwitchProfile()
                                }
                            }
                        )
                    }
                }

                LottaButton(
                    onClick = {
                        showLoginDialog = true
                    },
                    text = "Account hinzufügen",
                    icon = {
                        Icon(Icons.Outlined.PersonAdd, null)
                    }
                )
                LottaButton(
                    onClick = { modelData.removeCurrentSession() },
                    text = "Abmelden",
                    icon = {
                        Icon(Icons.Outlined.DoorBack, null)
                    },
                    modifier = Modifier
                        .padding(top = 2.dp)
                )
            }

            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version:")
                    Text(getAppVersion())
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("API Endpunkt:")
                    Text(LOTTA_API_HOST)
                }
                LottaButton(
                    onClick = {
                        showFeedbackDialog = true
                    },
                    text = "Feedback",
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                )
                LottaButton(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/lotta-schule/android")
                        )
                        App.mainActivity.startActivity(browserIntent)
                    },
                    text = "Quelltext anzeigen",
                )
            }
        }

    }
}

fun getAppVersion(): String {
    val version = getPackageInfo().versionName
    val build = getPackageInfo().longVersionCode

    return "$version ($build)"
}

fun getPackageInfo(): PackageInfo {
    try {
        return App.mainActivity.packageManager.getPackageInfo(App.mainActivity.packageName, 0)
    } catch (e: Exception) {
        print(e)
        return PackageInfo()
    }
}
