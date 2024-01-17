package net.einsa.lotta.ui.view

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.einsa.lotta.composition.ModelData

class RootViewModel : ViewModel() {
    private val _isInitialized = mutableStateOf(false)
    val isInitialized: Boolean
        get() = _isInitialized.value

    suspend fun init(modelData: ModelData) {
        return withContext(Dispatchers.IO) {
            modelData.initializeSessions()
            _isInitialized.value = true
        }
    }
}