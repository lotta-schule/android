package net.einsa.lotta.ui.view

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.einsa.lotta.composition.ModelData

class RootViewModel : ViewModel() {
    private val _didStartInitialization = mutableStateOf(false)
    val didStartInitialization: Boolean
        get() = _didStartInitialization.value

    suspend fun init(modelData: ModelData) {
        _didStartInitialization.value = true
        return withContext(Dispatchers.IO) {
            modelData.initializeSessions()
        }
    }
}