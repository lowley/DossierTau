package lorry.dossiertau.usecases.folderContent.support

import kotlin.math.max
import kotlin.math.min

/**
 * Fenêtre de chargement calculée à partir des éléments réellement visibles dans la grille.
 *
 * Elle est indépendante de Compose afin que la politique de préchargement reste testable et
 * puisse évoluer sans toucher au code d'affichage.
 */
data class ThumbnailViewport(
    val visible: IntRange,
    val prefetchBefore: IntRange,
    val prefetchAfter: IntRange,
) {
    companion object {
        val EMPTY = ThumbnailViewport(
            visible = IntRange.EMPTY,
            prefetchBefore = IntRange.EMPTY,
            prefetchAfter = IntRange.EMPTY,
        )

        fun of(
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            itemCount: Int,
            prefetchBeforeCount: Int = 8,
            prefetchAfterCount: Int = 24,
        ): ThumbnailViewport {
            if (
                itemCount <= 0 ||
                firstVisibleIndex < 0 ||
                lastVisibleIndex < firstVisibleIndex ||
                firstVisibleIndex >= itemCount
            ) return EMPTY

            val lastItemIndex = itemCount - 1
            val first = firstVisibleIndex.coerceAtMost(lastItemIndex)
            val last = lastVisibleIndex.coerceAtMost(lastItemIndex)

            val beforeStart = max(0, first - prefetchBeforeCount)
            val beforeEnd = first - 1
            val afterStart = last + 1
            val afterEnd = min(lastItemIndex, last + prefetchAfterCount)

            return ThumbnailViewport(
                visible = first..last,
                prefetchBefore = rangeOrEmpty(beforeStart, beforeEnd),
                prefetchAfter = rangeOrEmpty(afterStart, afterEnd),
            )
        }

        private fun rangeOrEmpty(start: Int, endInclusive: Int): IntRange =
            if (start <= endInclusive) start..endInclusive else IntRange.EMPTY
    }

    fun priorityOf(index: Int): ThumbnailLoadScheduler.Priority? = when (index) {
        in visible -> ThumbnailLoadScheduler.Priority.VISIBLE
        in prefetchAfter -> ThumbnailLoadScheduler.Priority.PREFETCH
        in prefetchBefore -> ThumbnailLoadScheduler.Priority.BACKGROUND
        else -> null
    }
}
