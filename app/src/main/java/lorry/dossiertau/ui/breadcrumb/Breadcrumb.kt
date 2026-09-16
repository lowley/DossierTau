package lorry.dossiertau.ui.breadcrumb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import lorry.dossiertau.R
import lorry.dossiertau.TauColors
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath

@Composable
fun BreadcrumbComponent.UI(
    state: BreadcrumbState?,
    onClick: (TauPath) -> Unit,
    animDuration: Int,
    modifier: Modifier = Modifier,
    onArrowClicked: () -> Unit
) {
    if (state == null || state is BreadcrumbState.LOADING)
        return

    val stateData = state as BreadcrumbState.DATA
    val segs = stateData.currentPath
        ?.split("/")
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    val scrollState = rememberScrollState()

    // Le contenu doit d'abord être mesuré avant que maxValue soit fiable.
    // Deux frames suffisent ici et évitent le défilement aléatoire observé
    // lorsque le chemin change.
    LaunchedEffect(stateData.currentPath) {
        withFrameNanos { }
        withFrameNanos { }
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier.clipToBounds(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = R.drawable.arrow2,
            contentDescription = "Flèche",
            modifier = Modifier
                .padding(end = 10.dp)
                .size(24.dp)
                .clickable {
                    onArrowClicked()
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(TauColors.Tertiary)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segs.forEachIndexed { index, seg ->
                BreadcrumbChip(
                    text = seg,
                ) {
                    onClick(("/" + segs.take(index + 1).joinToString("/")).toTauPath())
                }

                if (index < segs.lastIndex) {
                    Separator()
                }
            }
        }
    }
}

@Composable
fun BreadcrumbChip(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = modifier.clickable {
            onClick()
        },
        color = Color.DarkGray,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
fun Separator() {
    Text(
        text = "/",
        color = TauColors.Tertiary
    )
}
