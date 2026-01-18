package lorry.dossiertau.ui.displayedItem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.MainActivity
import lorry.dossiertau.R
import lorry.dossiertau.TauApp
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name
import lorry.dossiertau.data.model.picture
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.ui.displayedItem.support.DisplayItemRepo
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.dsl.koinApplication

@Composable
fun MainActivity.DisplayedItem(
    item: TauItem,
    setCurrentFolder: (TauPath) -> Unit,
    onClick: (TauPath) -> Unit,
    onLongClick: (TauItem) -> Unit
) {
    val borderSize = 160
    val borderSizeDp = borderSize.dp
    val displayRepo: DisplayItemRepo by inject()

    Column(
        modifier = Modifier
            .width(borderSizeDp)
            .padding(horizontal = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    if (item.isFolder())
                        setCurrentFolder(item.fullPath)
                    else if (
                        item.name.value.substringAfterLast('.') in
                        listOf("avi", "mp4", "mkv", "ts", "mpg", "html")
                    )
                        onClick(item.fullPath)
                },
                onLongClick = { onLongClick(item) }
            )
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
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape = RoundedCornerShape(8.dp))
                        .scale(1.2f),
                    tint = lerp(
                        Color.DarkGray,
                        Color.LightGray,
                        0.3f
                    )
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
                    .then(
                        if (!shouldShowMesh) Modifier.border(
                            1.dp,
                            Color.DarkGray,
                            shape = RoundedCornerShape(8.dp)
                        ) else Modifier
                    ),
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

            CornerSupplement(
                item = item,
                modifier = Modifier,
                getInfoSup = { item ->
                    displayRepo.getInfoSup(item)
                },
                getInfoInf = { item ->
                    displayRepo.getInfoInf(item)
                },
                onTopLeftPanelClick = { item -> },
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
            color = Color.Black,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
    }
}

context(BoxScope)
@Composable
fun CornerSupplement(
    modifier: Modifier = Modifier,
    item: TauItem,
    getInfoSup: suspend (TauItem) -> String?,
    getInfoInf: suspend (TauItem) -> String?,
    onTopLeftPanelClick: (TauItem) -> Unit,
) {
    //Ajout à l'image
    val infoSup = produceState<String?>(initialValue = null, item) {
        value = withContext(Dispatchers.IO) { getInfoSup(item) }
    }.value

    val infoInf = produceState<String?>(initialValue = null, item) {
        value = withContext(Dispatchers.IO) { getInfoInf(item) }
    }.value
//
    if (infoSup == null || infoInf == null) {
//                        CircularProgressIndicator()
    } else {
        val boxWidth = 45.dp
        val shapeForInsert = RoundedCornerShape(
            topStart = 8.dp,
            bottomEnd = 8.dp
        )

        //l'ajout à l'image proprement dit: encart supérieur gauche
        Box(
            modifier = modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    shape = shapeForInsert
                    clip = true
                    shadowElevation = 0f
                }
                .background(Color.DarkGray)
                .width(boxWidth)
                .border(
                    1.dp, Color.DarkGray,
                    shape = shapeForInsert
                )
                .clickable {
                    onTopLeftPanelClick(item)
                }
        ) {
//                     Couche 2 (Conditionnelle) : Le maillage, dessiné par-dessus le fond
//            if (!memoEmpty) {
//                Image(
//                    painter = painterResource(id = R.drawable.obliques4), // Remplacez par votre fichier
//                    contentDescription = "Maillage de fond",
//                    contentScale = ContentScale.Companion.Crop, // Assure que l'image remplit l'espace
//                    modifier = modifier.matchParentSize() // Fait en sorte que l'image prenne toute la taille de la Box
//                )
//            }

            Column(
                modifier = modifier
                    .align(Alignment.Companion.TopStart)
                    .padding(start = 0.dp, top = 0.dp)
                    .width(boxWidth)
            ) {
                val textHeight = 18.dp

                Text(
                    modifier = modifier
                        .align(Alignment.Companion.CenterHorizontally)
                        .padding(0.dp)
                        .height(textHeight),
                    text = infoSup,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )

                Text(
                    modifier = modifier
                        .align(Alignment.Companion.CenterHorizontally)
                        .padding(
                            top = 0.dp, start = 0.dp, bottom = 5.dp, end = 0.dp
                        )
                        .height(textHeight),
                    text = infoInf,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
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
