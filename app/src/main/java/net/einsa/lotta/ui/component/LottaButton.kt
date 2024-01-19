package net.einsa.lotta.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.einsa.lotta.composition.LocalModelData

@Composable
fun LottaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = false
) {
    val modelData = LocalModelData.current

    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .width(160.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(modelData.theme.pageBackgroundColor.toArgb()),
            contentColor = Color(modelData.theme.primaryColor.toArgb())
        ),
        shape = RoundedCornerShape(modelData.theme.borderRadius),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(modelData.theme.primaryColor.toArgb()),
                    Color(modelData.theme.primaryColor.toArgb())
                )
            )
        )
    ) {
        if (isLoading) {
            Text("L")
        } else {
            Text(text)
        }
    }
}