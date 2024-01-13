package net.einsa.lotta.ui.view.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import net.einsa.lotta.R
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.Tenant
import net.einsa.lotta.model.TenantDescriptor
import net.einsa.lotta.model.getUrl


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLoginSessionView(
    createLoginSessionViewModel: CreateLoginSessionViewModel = viewModel(),
    onLogin: (UserSession) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedTenantDescriptor by rememberSaveable() { mutableStateOf<TenantDescriptor?>(null) }
    val scope = rememberCoroutineScope()

    fun onSubmit() {
        scope.launch {
            try {
                val userSession = createLoginSessionViewModel.sendLoginRequest(
                    tenant = selectedTenantDescriptor!!,
                    email = email,
                    password = password
                )

                println("Login successful")
                println(userSession)
                onLogin(userSession)
            } catch (e: Exception) {
                println("Login failed")
                println(e)
                // TODO: Show some message
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Anmelden")
                }
            )
        },
    ) { innerPadding ->

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
        ) {
            selectedTenantDescriptor?.logoImageFileId?.let { logoImageFileId ->
                val descriptor = selectedTenantDescriptor!!
                val url = logoImageFileId.getUrl(
                    Tenant(
                        id = descriptor.id.toString(),
                        slug = descriptor.slug,
                        title = descriptor.title,
                    ), mapOf(
                        "width" to "200"
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
                        .padding(bottom = Dp(8.0F))
                        .heightIn(
                            max = Dp(175.0F)
                        )
                        .widthIn(max = Dp(200.0F))
                        .fillMaxWidth(),
                    contentScale = ContentScale.Inside
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "E-Mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions(
                    onDone = {
                        scope.launch {
                            createLoginSessionViewModel.updatePossibleTenants(email)
                        }
                    }
                ),
                modifier = Modifier
                    .padding(horizontal = Dp(8.0F))
                    .fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key.keyCode == Key.Enter.keyCode) {
                            scope.launch {
                                createLoginSessionViewModel.updatePossibleTenants(email)
                            }
                            return@onKeyEvent true
                        } else {
                            return@onKeyEvent false
                        }
                    }
            )

            if (createLoginSessionViewModel.availableTenantDescriptors.isNotEmpty() && selectedTenantDescriptor == null) {
                ModalBottomSheet(onDismissRequest = { email = "" }) {
                    Column {
                        createLoginSessionViewModel.availableTenantDescriptors.forEach { tenantDescriptor ->
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
                    onSubmit = { onSubmit() })
            }

            Button(
                enabled = selectedTenantDescriptor != null && email.isNotEmpty() && password.isNotEmpty(),
                modifier = Modifier
                    .padding(Dp(8.0F)),
                onClick = {
                    onSubmit()
                }) {
                Text("Anmelden")
            }
        }

    }
}


@Composable
fun UserAuthPasswordTextField(
    password: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
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
        modifier = Modifier
            .padding(horizontal = Dp(8.0F))
            .fillMaxWidth()
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


@Preview()
@Composable()
fun Preview_CreateLoginSessionView() {
    CreateLoginSessionView()
}

