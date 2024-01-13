package net.einsa.lotta.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import net.einsa.lotta.R

@Composable
fun LottaLogoView() {
    Image(
        painter = painterResource(id = R.drawable.wort_bild_marke_logo), contentDescription = null,
        modifier = Modifier.padding(all = Dp(50.0F))
    )
}