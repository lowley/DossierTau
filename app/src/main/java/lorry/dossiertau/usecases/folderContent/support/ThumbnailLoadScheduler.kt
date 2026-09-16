package lorry.dossiertau.usecases.folderContent.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Régule les traitements coûteux de miniatures.
 *
 * Le scheduler ne connaît volontairement ni Room, ni les capsules : il décide seulement
 * QUAND un travail peut être lancé. Le chargeur existant reste donc inchangé en aval.
 */
class ThumbnailLoadScheduler(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parallelism: Int = 3,
) {
    enum class Priority(val rank: Int) {
        VISIBLE(0),
        PREFETCH(1),
        BACKGROUND(2),
    }

    private data class Request(
        val key: String,
        val priority: Priority,
        val generation: Long,
        val block: suspend () -> Unit,
    )

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val signal = Channel<Unit>(Channel.CONFLATED)
    private val pending = ConcurrentHashMap<String, Request>()
    private val running = ConcurrentHashMap<String, Job>()

    @Volatile
    private var generation = 0L

    init {
        repeat(parallelism.coerceAtLeast(1)) {
            scope.launch { workerLoop() }
        }
    }

    /**
     * Nouvelle fenêtre de visibilité. Les requêtes en attente de l'ancienne fenêtre deviennent
     * obsolètes. Les travaux déjà en cours ne sont pas brutalement annulés : les lecteurs de
     * fichiers/capsules existants n'ont ainsi pas besoin d'être rendus annulables.
     */
    fun newViewportGeneration() {
        generation++
        pending.entries.removeIf { it.value.generation < generation }
        signal.trySend(Unit)
    }

    fun request(
        key: String,
        priority: Priority,
        block: suspend () -> Unit,
    ) {
        if (running.containsKey(key)) return

        val request = Request(
            key = key,
            priority = priority,
            generation = generation,
            block = block,
        )

        pending.compute(key) { _, old ->
            when {
                old == null -> request
                old.generation < request.generation -> request
                request.priority.rank < old.priority.rank -> request
                else -> old
            }
        }
        signal.trySend(Unit)
    }

    fun forget(key: String) {
        pending.remove(key)
    }

    private suspend fun workerLoop() {
        while (true) {
            signal.receive()

            while (true) {
                val next = pending.values.minWithOrNull(
                    compareBy<Request> { it.priority.rank }
                        .thenByDescending { it.generation }
                ) ?: break

                if (!pending.remove(next.key, next)) continue
                if (next.generation != generation) continue

                val job = scope.launch {
                    try {
                        next.block()
                    } finally {
                        running.remove(next.key)
                        signal.trySend(Unit)
                    }
                }
                running[next.key] = job
                break
            }
        }
    }
}
