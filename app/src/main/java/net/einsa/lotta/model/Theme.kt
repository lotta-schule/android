package net.einsa.lotta.model

import android.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import net.einsa.lotta.serializer.ColorSerializer
import net.einsa.lotta.serializer.DpSerializer
import net.einsa.lotta.serializer.toColor

@Serializable
class Theme(
    @Serializable(with = ColorSerializer::class)
    var primaryColor: Color = Color.valueOf(1.0f, 0.34f, 0.13f),

    @Serializable(with = ColorSerializer::class)
    var navigationBackgroundColor: Color = Color.valueOf(0.2f, 0.2f, 0.2f),

    @Serializable(with = ColorSerializer::class)
    var errorColor: Color = Color.valueOf(1.0f, 0.0f, 0.0f),

    @Serializable(with = ColorSerializer::class)
    var successColor: Color = Color.valueOf(0.04f, 0.32f, 0.15f),

    @Serializable(with = ColorSerializer::class)
    var navigationColor: Color = Color.valueOf(0.02f, 0.02f, 0.02f),

    @Serializable(with = ColorSerializer::class)
    var disabledColor: Color = Color.valueOf(0.38f, 0.38f, 0.38f),

    @Serializable(with = ColorSerializer::class)
    var textColor: Color = Color.valueOf(0.13f, 0.13f, 0.13f),

    @Serializable(with = ColorSerializer::class)
    var labelTextColor: Color = Color.valueOf(0.62f, 0.62f, 0.62f),

    @Serializable(with = ColorSerializer::class)
    var navigationContrastTextColor: Color = Color.valueOf(1f, 1f, 1f),

    @Serializable(with = ColorSerializer::class)
    var primaryContrastTextColor: Color = Color.valueOf(1f, 1f, 1f),

    @Serializable(with = ColorSerializer::class)
    var boxBackgroundColor: Color = Color.valueOf(1f, 1f, 1f),

    @Serializable(with = ColorSerializer::class)
    var pageBackgroundColor: Color = Color.valueOf(0.79f, 0.8f, 0.84f),

    @Serializable(with = ColorSerializer::class)
    var dividerColor: Color = Color.valueOf(0.88f, 0.88f, 0.88f),

    @Serializable(with = ColorSerializer::class)
    var highlightColor: Color = Color.valueOf(0.88f, 0.88f, 0.88f),

    @Serializable(with = ColorSerializer::class)
    var bannerBackgroundColor: Color = Color.valueOf(0.21f, 0.48f, 0.94f),

    @Serializable(with = ColorSerializer::class)
    var accentGreyColor: Color = Color.valueOf(0.89f, 0.89f, 0.89f),


    @Serializable(with = DpSerializer::class)
    var spacing: Dp = 8.dp,

    @Serializable(with = DpSerializer::class)
    var borderRadius: Dp = 4.dp,
) {
    constructor(overrides: Any?) : this() {
        if (overrides is Map<*, *>) {
            overrides["primaryColor"]?.let {
                if (it is String) {
                    try {
                        primaryColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["navigationBackgroundColor"]?.let {
                if (it is String) {
                    try {
                        navigationBackgroundColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["errorColor"]?.let {
                if (it is String) {
                    try {
                        errorColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["successColor"]?.let {
                if (it is String) {
                    try {
                        successColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["navigationColor"]?.let {
                if (it is String) {
                    try {
                        navigationColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["disabledColor"]?.let {
                if (it is String) {
                    try {
                        disabledColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["textColor"]?.let {
                if (it is String) {
                    try {
                        textColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["labelTextColor"]?.let {
                if (it is String) {
                    try {
                        labelTextColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["navigationContrastTextColor"]?.let {
                if (it is String) {
                    try {
                        navigationContrastTextColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["primaryContrastTextColor"]?.let {
                if (it is String) {
                    try {
                        primaryContrastTextColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["boxBackgroundColor"]?.let {
                if (it is String) {
                    try {
                        boxBackgroundColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["pageBackgroundColor"]?.let {
                if (it is String) {
                    try {
                        pageBackgroundColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["dividerColor"]?.let {
                if (it is String) {
                    try {
                        dividerColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["highlightColor"]?.let {
                if (it is String) {
                    try {
                        highlightColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["bannerBackgroundColor"]?.let {
                if (it is String) {
                    try {
                        bannerBackgroundColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["accentGreyColor"]?.let {
                if (it is String) {
                    try {
                        accentGreyColor = it.toColor()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["spacing"]?.let {
                if (it is String) {
                    try {
                        spacing = it.toFloat().dp
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            overrides["borderRadius"]?.let {
                if (it is String) {
                    try {
                        borderRadius = it.toFloat().dp
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }
}

