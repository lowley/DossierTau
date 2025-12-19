package lorry.dossiertau.data.intelligenceService.utils2.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DebouncedTimer(private val scope: CoroutineScope) {
    private var timerJob: Job? = null

    fun start(delayMs: Long, block: () -> Unit) {
        // si un timer existe déjà, on le remplace
        timerJob?.cancel()

        timerJob = scope.launch {
            delay(delayMs)
            block()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }

    fun isRunning(): Boolean = timerJob?.isActive == true
}