package net.einsa.lotta.ui.view

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.einsa.lotta.MainActivity
import net.einsa.lotta.composition.ModelData

class RootViewModel : ViewModel() {
    private val _didStartInitialization = mutableStateOf(false)
    val didStartInitialization: Boolean
        get() = _didStartInitialization.value

    private val _initialized = mutableStateOf(false)
    val initialized: Boolean
        get() = _initialized.value

    suspend fun init(activity: MainActivity, modelData: ModelData) {
        _didStartInitialization.value = true
        return withContext(Dispatchers.IO) {
            modelData.initializeSessions(activity)
            _initialized.value = true
        }
    }
}