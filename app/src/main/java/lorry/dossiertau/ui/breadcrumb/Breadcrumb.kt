package lorry.dossiertau.ui.breadcrumb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath

@Composable
fun BreadcrumbComponent.UI(
    state: BreadcrumbState?,
    onClick: (TauPath) -> Unit,
    animDuration: Int,
    modifier: Modifier = Modifier
) {
    if (state == null || state is BreadcrumbState.LOADING)
        return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stateData = state as BreadcrumbState.DATA
        val segs = stateData.currentPath?.split("/")

        Text(
            text = " ▶ ",
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )

        // éléments non animés
        segs?.dropLast(1)?.forEachIndexed { i, seg ->
            BreadcrumbChip(
                text = seg,
            ) { onClick(("/" + segs.slice(0..i).joinToString("/")).toTauPath()) }

            if (i < segs.size - 1) Separator()
        }

        //  dernier élément animé
        val lastSeg = segs?.lastOrNull()

        val lastSegIndex = (segs?.size ?: 0) - 1
        val show = stateData.animation == Animation.APPEAR
        val visibleState = MutableTransitionState(
            initialState = !show,
        )
        visibleState.targetState = show

        key("$lastSeg-$lastSegIndex") {
            AnimatedVisibility(
                visibleState = visibleState, enter = expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(durationMillis = animDuration)
                ), exit = shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = tween(durationMillis = animDuration)
                )
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth()
                ) {
                    BreadcrumbChip(
                        text = lastSeg ?: "",
                    ) { }
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
        text = text, modifier = modifier.clickable {
            onClick()
        })
}

@Composable
fun Separator() {
    Text(text = "/")
}


