package lorry.dossiertau.ui.displayedItem

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import lorry.dossiertau.MainActivity
import lorry.dossiertau.R
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name
import lorry.dossiertau.data.model.picture
import lorry.dossiertau.support.littleClasses.TauPath


@Composable
fun MainActivity.DisplayedItem(
    item: TauItem,
    setCurrentFolder: (TauPath) -> Unit
) {
    val borderSize = 160
    val borderSizeDp = borderSize.dp

    Column(
        modifier = Modifier
            .width(borderSizeDp)
            .padding(horizontal = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (item.isFolder())
                    setCurrentFolder(item.fullPath)
            }
//            .background(Color.Blue)
    ) {
        ////////////////
        // zone image //
        ////////////////
        var imageSize by remember { mutableStateOf<IntSize?>(null) }
        var containerSize = IntSize(borderSize, borderSize)

        // Le calcul reste le même, il sera relancé quand imageSize changera
        val shouldShowMesh = remember(imageSize /*, scale */) {
            val size = imageSize
            if (size != null) {
                !doesImageFillBox(
                    containerWidth = containerSize.width,
                    containerHeight = containerSize.height,
                    imageWidth = size.width,
                    imageHeight = size.height,
                    contentScale = ContentScale.Companion.Fit
                )
            } else {
                false // On ne montre pas le maillage avant de connaître la taille
            }
        }

        Box(
            modifier = Modifier
                .height(borderSizeDp)
                .padding(vertical = 5.dp)
                .onSizeChanged { containerSize = it }
                .align(Alignment.CenterHorizontally)
//                .background(Color.Red)

        ) {

            if (shouldShowMesh) {
                Icon(
                    painter = painterResource(id = R.drawable.diagos),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                        .clip(shape = RoundedCornerShape(8.dp))
                        .scale(1.2f),
                    tint = Color.DarkGray
                )
            }

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.picture.toBitmap())
                    .crossfade(false)
                    .build(),
                contentDescription = "miniature",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(borderSizeDp)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .then( if (!shouldShowMesh) Modifier.border(1.dp, Color.DarkGray, shape = RoundedCornerShape(8.dp)) else Modifier),
                loading = { /*affiche un loader*/ },
                success = { successState ->
                    val drawable = successState.result.drawable
                    imageSize = IntSize(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight
                    )

                    Box(
                        modifier = Modifier.matchParentSize()

                    ) {
                        Image(
                            painter = successState.painter,
                            contentDescription = "Miniature",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .matchParentSize(),
                            colorFilter = null
                        )
                    }

                },
                error = { /* fallback en cas d'erreur */ }
            )
        }

        ////////////////
        // zone texte //
        ////////////////
        Text(
            modifier = Modifier
                .width(borderSizeDp)
                .align(Alignment.CenterHorizontally),
//                .background(Color.Green),
            text = item.name.value,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
    }
}


fun doesImageFillBox(
    containerWidth: Int,
    containerHeight: Int,
    imageWidth: Int,
    imageHeight: Int,
    contentScale: ContentScale
): Boolean {
    if (imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0 || containerHeight <= 0)
        return false

    val containerRatio = containerWidth.toFloat() / containerHeight
    val imageRatio = imageWidth.toFloat() / imageHeight

    return when (contentScale) {
        ContentScale.Companion.Crop,
        ContentScale.Companion.FillBounds -> true

        ContentScale.Companion.Fit,
        ContentScale.Companion.Inside -> {
            if (imageRatio > containerRatio) {
                (containerWidth / imageRatio) >= containerHeight
            } else {
                (containerHeight * imageRatio) >= containerWidth
            }
        }

        ContentScale.Companion.FillWidth -> imageRatio <= containerRatio
        ContentScale.Companion.FillHeight -> imageRatio >= containerRatio
        ContentScale.Companion.None -> false
        else -> false
    }
}