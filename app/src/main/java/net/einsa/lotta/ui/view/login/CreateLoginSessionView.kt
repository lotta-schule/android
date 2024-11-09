package net.einsa.lotta.ui.view.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import net.einsa.lotta.R
import net.einsa.lotta.composition.LocalModelData
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.TenantDescriptor
import net.einsa.lotta.model.Theme
import net.einsa.lotta.model.getUrl
import net.einsa.lotta.ui.component.LottaButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLoginSessionView(
    modifier: Modifier = Modifier,
    vm: CreateLoginSessionViewModel = viewModel(),
    defaultEmail: String? = null,
    onLogin: (UserSession) -> Unit = {},
    onDismiss: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val modelData = LocalModelData.current

    val theme = remember { Theme() }

    var email by remember { mutableStateOf(defaultEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var selectedTenantDescriptor by remember { mutableStateOf<TenantDescriptor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onSubmit() {
        focusManager.clearFocus()
        scope.launch {
            vm.sendLoginRequest(
                tenant = selectedTenantDescriptor!!,
                email = email,
                password = password
            )?.let(onLogin)
        }
    }

    LaunchedEffect(key1 = vm.error) {
        vm.error?.let { error ->
            snackbarHostState.showSnackbar(
                error.message ?: "Ein Fehler ist aufgetreten.",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(vertical = theme.spacing)
            .background(Color(theme.boxBackgroundColor.toArgb()))
            .fillMaxSize()
    ) {
        Scaffold(
            modifier = modifier.widthIn(max = 500.dp),
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = Color(theme.boxBackgroundColor.toArgb()),
                        titleContentColor = Color(theme.textColor.toArgb()),
                    ),
                    title = {
                        Text("Anmelden")
                    },
                    actions = {
                        onDismiss?.let {
                            IconButton(onClick = { it.invoke() }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Schließen"
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color(theme.boxBackgroundColor.toArgb()),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
        ) { innerPadding ->

            Box(
                propagateMinConstraints = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(innerPadding)
                        .widthIn(max = 600.dp)
                ) {
                    if (selectedTenantDescriptor?.logoImageFileId != null) {
                        val descriptor = selectedTenantDescriptor!!
                        val url = selectedTenantDescriptor!!.logoImageFileId!!.getUrl(
                            Tenant(
                                id = descriptor.id.toString(),
                                slug = descriptor.slug,
                                title = descriptor.title,
                            ), mapOf(
                                "width" to "400",
                            )
                        )

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo von ${descriptor.title}",
                            placeholder = painterResource(id = R.drawable.wort_bild_marke_logo),
                            modifier = Modifier
                                .padding(bottom = theme.spacing)
                                .heightIn(
                                    max = 200.dp
                                )
                                .fillMaxWidth(0.75f),
                            contentScale = ContentScale.Fit
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-Mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                scope.launch {
                                    vm.updatePossibleTenants(
                                        email,
                                        excludingSessions = modelData.userSessions
                                    )
                                }
                            }
                        ),
                        modifier = Modifier
                            .padding(horizontal = Dp(8.0F))
                            .fillMaxWidth(.85F)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key.keyCode == Key.Enter.keyCode) {
                                    scope.launch {
                                        vm.updatePossibleTenants(
                                            email,
                                            excludingSessions = modelData.userSessions
                                        )
                                    }
                                    return@onKeyEvent true
                                } else {
                                    return@onKeyEvent false
                                }
                            }
                    )

                    if (selectedTenantDescriptor == null) {
                        LottaButton(
                            disabled = email.isEmpty(),
                            modifier = Modifier
                                .padding(theme.spacing)
                                .fillMaxWidth(.85F),
                            isLoading = vm.isLoading,
                            text = "weiter",
                            onClick = {
                                focusManager.clearFocus()
                                scope.launch {
                                    vm.updatePossibleTenants(
                                        email,
                                        excludingSessions = modelData.userSessions
                                    )
                                }
                            }
                        )
                    }

                    if (vm.availableTenantDescriptors.isNotEmpty() && selectedTenantDescriptor == null) {
                        ModalBottomSheet(onDismissRequest = { email = "" }) {
                            Column {
                                vm.availableTenantDescriptors.forEach { tenantDescriptor ->
                                    TextButton(
                                        onClick = {
                                            selectedTenantDescriptor = tenantDescriptor
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(tenantDescriptor.title)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTenantDescriptor != null) {
                        UserAuthPasswordTextField(
                            password,
                            onValueChange = { password = it },
                            onSubmit = { onSubmit() },
                            modifier = Modifier
                                .padding(horizontal = Dp(8.0F))
                                .fillMaxWidth(.85F)
                        )
                    }

                    if (selectedTenantDescriptor != null) {
                        LottaButton(
                            disabled = email.isEmpty() || password.isEmpty(),
                            isLoading = vm.isLoading,
                            modifier = Modifier
                                .padding(theme.spacing),
                            text = "Anmelden",
                            onClick = {
                                onSubmit()
                            }
                        )
                    }
                }
            }

        }
    }
}


@Composable
fun UserAuthPasswordTextField(
    password: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passwordFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        passwordFieldFocusRequester.requestFocus()
    }

    OutlinedTextField(
        value = password,
        onValueChange = onValueChange,
        label = { Text(text = "Passwort") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardActions = KeyboardActions(
            onGo = {
                onSubmit()
            },
        ),
        singleLine = true,
        modifier = modifier
            .onKeyEvent { keyEvent ->
                if (keyEvent.key.keyCode == Key.Enter.keyCode) {
                    onSubmit()
                    return@onKeyEvent true
                } else {
                    return@onKeyEvent false
                }
            }
            .focusRequester(passwordFieldFocusRequester),
    )
}


@Preview
@Composable
fun Preview_CreateLoginSessionView() {
    CreateLoginSessionView()
}

