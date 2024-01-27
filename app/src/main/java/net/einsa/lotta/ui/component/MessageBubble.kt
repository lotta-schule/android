package net.einsa.lotta.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.einsa.lotta.GetConversationQuery
import net.einsa.lotta.R
import net.einsa.lotta.composition.LocalUserSession
import net.einsa.lotta.model.getUrl
import net.einsa.lotta.type.FileType

@Composable
fun MessageBubble(
    message: GetConversationQuery.Message,
    fromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val session = LocalUserSession.current
    val theme = session.tenant.customTheme

    val shape = if (fromCurrentUser) {
        RoundedCornerShape(
            topStart = theme.borderRadius,
            topEnd = theme.borderRadius,
            bottomStart = theme.borderRadius
        )
    } else {
        RoundedCornerShape(
            topStart = theme.borderRadius,
            topEnd = theme.borderRadius,
            bottomEnd = theme.borderRadius
        )
    }

    Column(
        modifier = modifier
            .background(
                if (fromCurrentUser) {
                    Color(theme.primaryColor.toArgb()).copy(alpha = 0.3f)
                } else {
                    Color(
                        theme.primaryColor.toArgb()
                    ).copy(alpha = 0.08f)
                }
            )
            .border(
                width = 1.dp,
                color = Color(theme.primaryColor.toArgb()),
                shape = shape
            )
            .padding(theme.spacing)
            .clip(shape),
    ) {
        message.content?.let {
            Text(it)
        }
        message.files?.filterNotNull()?.let { files ->
            MessageBubbleFileRow(files)
        }
    }
}

@Composable
fun MessageBubbleFileRow(
    files: List<GetConversationQuery.File>,
    modifier: Modifier = Modifier
) {
    val tenant = LocalUserSession.current.tenant
    val context = LocalContext.current

    val openFileUrl = remember {
        ({ fileUrl: String, fileType: FileType? ->
            context.startActivity(getFileUrlIntent(fileUrl, fileType))
        })
    }

    Column(modifier = modifier) {
        files.filter { it.id != null }.forEach { file ->
            Row {
                file.id?.getUrl(tenant = tenant)?.let { fileUrl ->
                    val thumbnailUrl = "$fileUrl?height=250&width=250&resize=contain"
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openFileUrl(fileUrl, file.fileType) }) {
                        AsyncImage(
                            ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailUrl)
                                .error(R.drawable.wifi_off)
                                .crossfade(true)
                                .build(),
                            modifier = Modifier
                                .padding(tenant.customTheme.spacing)
                                .size(125.dp)
                                .aspectRatio(1f),
                            alignment = Alignment.CenterEnd,
                            contentScale = ContentScale.Fit,
                            contentDescription = file.filename,
                        )

                        file.filename?.let { filename ->
                            Text(
                                text = filename,
                                style = TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .padding(tenant.customTheme.spacing)
                                    .align(Alignment.CenterVertically)
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }

                    }
                }
            }
        }
    }
}

private fun getFileUrlIntent(fileUrl: String, fileType: FileType?): Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(
        Uri.parse(fileUrl),
        when (fileType) {
            FileType.PDF -> "application/pdf"
            FileType.IMAGE -> "image/*"
            FileType.AUDIO -> "audio/*"
            FileType.VIDEO -> "video/*"
            else -> "*/*"
        }
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    return intent
}