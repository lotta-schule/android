package net.einsa.lotta.ui.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Avatar(
    url: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    contentDescription: String = "Avatar",
) {
    AsyncImage(
        ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(size)
            .build(),
        modifier = modifier.clip(CircleShape),
        contentDescription = contentDescription
    )
}