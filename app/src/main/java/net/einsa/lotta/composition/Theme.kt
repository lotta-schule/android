package net.einsa.lotta.composition

import androidx.compose.runtime.compositionLocalOf
import net.einsa.lotta.model.Theme

val LocalTheme = compositionLocalOf {
    Theme()
}
