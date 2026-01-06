package lorry.dossiertau.ui

import kotlinx.coroutines.flow.MutableSharedFlow

object AppBus {
    val lines = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    val summary = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
    )
}