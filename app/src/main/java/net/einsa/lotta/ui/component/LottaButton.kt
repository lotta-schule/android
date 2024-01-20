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
import net.einsa.lotta.composition.LocalModelData

@Composable
fun LottaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    disabled: Boolean = false
) {
    val modelData = LocalModelData.current

    val isEnabled = !disabled && !isLoading

    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 160.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(modelData.theme.boxBackgroundColor.toArgb()),
            contentColor = Color(modelData.theme.primaryColor.toArgb()),
            disabledContainerColor = Color(modelData.theme.boxBackgroundColor.toArgb()),
            disabledContentColor = Color(modelData.theme.disabledColor.toArgb())
        ),
        shape = RoundedCornerShape(modelData.theme.borderRadius),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                colors = if (isEnabled) {
                    listOf(
                        Color(modelData.theme.primaryColor.toArgb()),
                        Color(modelData.theme.primaryColor.toArgb())
                    )
                } else {
                    listOf(
                        Color(modelData.theme.disabledColor.toArgb()),
                        Color(modelData.theme.disabledColor.toArgb())
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
            Spacer(modifier = Modifier.width(modelData.theme.spacing))
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