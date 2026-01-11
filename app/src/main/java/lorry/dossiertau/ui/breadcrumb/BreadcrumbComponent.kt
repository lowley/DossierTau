package lorry.dossiertau.ui.breadcrumb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import lorry.dossiertau.support.littleClasses.TauPath
import javax.inject.Singleton

@Singleton
class BreadcrumbComponent @Inject constructor() {

    val breadcrumbState = MutableStateFlow<BreadcrumbState>(BreadcrumbState.LOADING)
    val scope = CoroutineScope(Dispatchers.IO + Job())

    @Composable
    fun Breadcrumb(
        path: List<String>,
        onClick: (path: TauPath) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val state = breadcrumbState.collectAsState()
        var previousPath = (state.value as? BreadcrumbState.DATA)?.let {
            if (it.animation == Animation.DISAPPEAR) it.currentPath?.split("/")?.dropLast(1)?.joinToString("/") else it.currentPath
        }

        val animDuration = 500

        LaunchedEffect(path) {
            val newPath = path.joinToString("/")
            if (newPath != previousPath) {
                breadcrumbStateRegulate(
                    currentPath = newPath,
                    previousPath = previousPath,
                    state = breadcrumbState
                )

                previousPath = newPath
            }
        }

        UI(
            state = state.value,
            onClick = { path ->
                onClick(path)
            },
            animDuration = animDuration,
            modifier = modifier
        )
    }

    suspend fun breadcrumbStateRegulate(
        currentPath: String?,
        previousPath: String?,
        state: MutableStateFlow<BreadcrumbState>,
        duration: Long = 500L
    ) {
        val previousList = previousPath.splitWithSlash()
        val currentList = currentPath.splitWithSlash()

        if (previousList == currentList)
            return

        //! agrandissement
        if (previousList.isLowerThan(currentList))
            extend(previousList, currentList, state, duration)
        else
        //! réduction
            if (previousList.isBiggerThan(currentList))
                shrink(previousList, currentList, state, duration)
            else {
                //! cas général
                twoways(previousList, currentList, state, duration)
            }
    }


    private suspend fun twoways(
        previousList: List<String>,
        currentList: List<String>,
        state: MutableStateFlow<BreadcrumbState>,
        duration: Long
    ) {
        // -1 si previous et current commencent par élément différent
        // tous cas génants déjà gérés avant
        val maxCommonIndex = previousList.zip(currentList).indexOfFirst {
            it.first != it.second
        } - 1

        val (shrinkDuration, extendDuration) = computeDurations(
            duration, previousList, maxCommonIndex, currentList
        )

        shrink(
            previousList = previousList,
            currentList = if (maxCommonIndex < 0) emptyList() else previousList.slice(0..maxCommonIndex),
            state = state,
            duration = shrinkDuration
        )

        extend(
            previousList = if (maxCommonIndex < 0) emptyList() else previousList.slice(0..maxCommonIndex),
            currentList = currentList,
            state = state,
            duration = extendDuration
        )
    }

    private fun computeDurations(
        duration: Long,
        previousList: List<String>,
        maxCommonIndex: Int,
        currentList: List<String>
    ): Pair<Long, Long> {
        val atomicDuration =
            duration / (previousList.size - maxCommonIndex.let { if (it < 0) 0 else it }
                    + currentList.size - maxCommonIndex.let { if (it < 0) 0 else it })

        val shrinkDuration =
            atomicDuration * (previousList.size - maxCommonIndex.let { if (it < 0) 0 else it })
        val extendDuration =
            atomicDuration * (currentList.size - maxCommonIndex.let { if (it < 0) 0 else it })
        return Pair(shrinkDuration, extendDuration)
    }

    /**
     * extends previous to current
     */
    private suspend fun extend(
        previousList: List<String>,
        currentList: List<String>,
        state: MutableStateFlow<BreadcrumbState>,
        duration: Long,
    ) {
        val atomicDuration =
            if (previousList == currentList) duration else duration / (currentList.size - previousList.size)
        val suffixes = if (previousList == currentList) listOf(currentList.last()) else
            currentList.slice(previousList.size until currentList.size)

        var actual = if (previousList == currentList) previousList.dropLast(1) else previousList
        for (suffix in suffixes) {
            actual = actual.plus(suffix)
            state.update {
                it.with(
                    actual.joinToString("/"),
                    animation = Animation.APPEAR
                )
            }

            delay(atomicDuration)
        }
    }

    /**
     * shrinks previous to current
     */
    private suspend fun shrink(
        previousList: List<String>,
        currentList: List<String>,
        state: MutableStateFlow<BreadcrumbState>,
        duration: Long
    ) {
        val atomicDuration = duration / (previousList.size - currentList.size)
        val suffixes = previousList.slice(currentList.size until previousList.size)
            .reversed()

        var actual = previousList
        for (suffix in suffixes) {
            state.update {
                it.with(
                    actual.joinToString("/"),
                    animation = Animation.DISAPPEAR
                )
            }
            actual = actual.dropLast(1)

            delay(atomicDuration)
        }
    }
}

private fun List<String>.isBiggerThan(otherList: List<String>): Boolean =
    otherList.size <= this.size &&
            (otherList.size == 0 || this.slice(0 until otherList.size) == otherList)

private fun List<String>.isLowerThan(otherList: List<String>): Boolean =
    otherList.isBiggerThan(this)

sealed class BreadcrumbState() {
    data class DATA(
        val currentPath: String?,
        val animation: Animation,

        ) : BreadcrumbState()

    object LOADING : BreadcrumbState()

    fun with(
        currentPath: String? = (this as? DATA)?.currentPath,
        animation: Animation = (this as? DATA)?.animation ?: Animation.APPEAR
    ): BreadcrumbState {

        if (this is DATA)
            return this.copy(
                currentPath = currentPath,
                animation = animation
            )

        if (this is LOADING)
            return DATA(
                currentPath = currentPath,
                animation = animation
            )

        return DATA(
            currentPath = currentPath,
            animation = animation
        )
    }
}

enum class Animation {
    APPEAR,
    DISAPPEAR,
}

private fun String?.splitWithSlash(): List<String> =
    if (this == "") emptyList() else this?.split("/") ?: emptyList()

//data class BreadcrumbState(
//    val currentPath: String,
//
//
//){
//    object INIT: BreadcrumbState("kbfdbù$$")
//}