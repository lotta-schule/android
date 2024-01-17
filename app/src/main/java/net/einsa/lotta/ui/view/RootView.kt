package net.einsa.lotta.ui.view

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.ui.view.login.CreateLoginSessionView
import java.util.logging.Logger

@Composable
fun RootView(vm: RootViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    val modelData = LocalModelData.current
    val currentSession = modelData.currentSession

    Log.i("RootView", "currentSession: $currentSession")

    LottaLogoView()

    if (currentSession != null) {
        TenantRootView(currentSession)
    }

    if (showDialog) {
        Dialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = {}
        ) {
            CreateLoginSessionView(onLogin = { userSession ->
                showDialog = false
                modelData.add(userSession)
                Logger.getGlobal().info("Logged in as ${userSession.user.name}")
                // TODO
            })
        }
    }

    DisposableEffect(vm.isInitialized, currentSession) {
        if (vm.isInitialized && currentSession == null) {
            showDialog = true
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        vm.init(modelData)
    }
}
