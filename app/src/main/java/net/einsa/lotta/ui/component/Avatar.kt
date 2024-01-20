package net.einsa.lotta.ui.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Avatar(
    url: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    contentDescription: String = "Profilbild",
) {
    AsyncImage(
        ImageRequest.Builder(LocalContext.current)
            .data(url + "?aspectRatio=1&resize=cover&width=${size * 2}")
            .fallback(android.R.drawable.ic_menu_report_image)
            .size(size)
            .build(),
        modifier = modifier
            .clip(CircleShape),
        contentScale = ContentScale.FillBounds,
        contentDescription = contentDescription
    )
}