package net.einsa.lotta.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.einsa.lotta.composition.LocalTheme

@Composable
fun LottaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    disabled: Boolean = false
) {
    val theme = LocalTheme.current

    val isEnabled = !disabled && !isLoading

    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 160.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(theme.boxBackgroundColor.toArgb()),
            contentColor = Color(theme.primaryColor.toArgb()),
            disabledContainerColor = Color(theme.boxBackgroundColor.toArgb()),
            disabledContentColor = Color(theme.disabledColor.toArgb())
        ),
        shape = RoundedCornerShape(theme.borderRadius),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                colors = if (isEnabled) {
                    listOf(
                        Color(theme.primaryColor.toArgb()),
                        Color(theme.primaryColor.toArgb())
                    )
                } else {
                    listOf(
                        Color(theme.disabledColor.toArgb()),
                        Color(theme.disabledColor.toArgb())
                    )
                }
            )
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(ButtonDefaults.IconSize),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(theme.spacing))
        }
        Text(text)
    }
}

@Preview
@Composable
fun LottaButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LottaButton(onClick = {}, text = "Button")
        LottaButton(onClick = {}, text = "I'm disabled", disabled = true)
        LottaButton(onClick = {}, text = "Loading", isLoading = true)
    }
}