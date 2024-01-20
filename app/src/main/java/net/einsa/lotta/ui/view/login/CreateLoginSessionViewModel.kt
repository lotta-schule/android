package net.einsa.lotta.ui.view.login

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.einsa.lotta.api.LOTTA_API_HTTP_URL
import net.einsa.lotta.composition.UserSession
import net.einsa.lotta.model.ListTenantResult
import net.einsa.lotta.model.TenantDescriptor
import okhttp3.OkHttpClient
import okhttp3.Request

class CreateLoginSessionViewModel : ViewModel() {
    private val _availableTenantDescriptors = mutableStateListOf<TenantDescriptor>()
    val availableTenantDescriptors
        get() = _availableTenantDescriptors

    private val _isLoading = mutableStateOf(false)
    val isLoading
        get() = _isLoading.value

    private val _error = mutableStateOf<Throwable?>(null)
    val error
        get() = _error.value

    suspend fun updatePossibleTenants(email: String, excludingSessions: List<UserSession>) {
        _isLoading.value = true
        try {
            val excludingTenantIds = excludingSessions.map { it.tenant.id }
            _availableTenantDescriptors.addAll(
                fetchPossibleTenants(email).filter {
                    !excludingTenantIds.contains(it.id.toString())
                }
            )
        } catch (e: Exception) {
            _error.value = e
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun sendLoginRequest(
        tenant: TenantDescriptor,
        email: String,
        password: String
    ): UserSession? {
        _isLoading.value = true
        return withContext(Dispatchers.IO) {
            try {
                UserSession.createFromCredentials(
                    tenantSlug = tenant.slug,
                    username = email,
                    password = password
                )
            } catch (e: Exception) {
                _error.value = e
                return@withContext null
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun fetchPossibleTenants(email: String): List<TenantDescriptor> {
        return withContext(Dispatchers.IO) {
            val url = "$LOTTA_API_HTTP_URL/api/public/user-tenants?username=$email"

            val request =
                Request.Builder()
                    .url(url)
                    .build()

            val response = OkHttpClient().newCall(request).execute()

            if (response.body == null) {
                throw Exception("Could not get tenants from server")
            }

            val jsonString = response.body!!.string()
            println(jsonString)

            val result = Json.decodeFromString<ListTenantResult>(jsonString)

            if (result.tenants != null) {
                return@withContext result.tenants
            } else {
                throw Exception("Could not get tenants from server")
            }
        }
    }
}
