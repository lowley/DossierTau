package lorry.dossiertau.data.intelligenceService.utils2.events

import arrow.core.zip
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath

class Snapshot(
    val folderPath: TauPath,
    private val entriesByName: Map<String, SnapshotElement>
) {
    fun get(name: String): SnapshotElement? = entriesByName[name]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Snapshot

        if (folderPath != other.folderPath) return false
        entries.sortedBy { it.name }.zip(other.entries.sortedBy { it.name })
            .forEach { (left, right) ->
                if (left != right) {
                    return false
                }
            }

        return true
    }

    override fun hashCode(): Int {
        var result = folderPath.hashCode()
        result = 31 * result + entriesByName.hashCode()
        return result
    }

    val names: Set<String>
        get() = entriesByName.keys

    val entries: Collection<SnapshotElement>
        get() = entriesByName.values


    companion object {

        fun FAKE(path: String) = SnapshotElement(
            name = "FAKE",
            isDir = false,
            size = 1L,
            lastModified = 1L
        ).createSnapshotWith(path)

        fun FAKE(path: TauPath) = SnapshotElement(
            name = "FAKE",
            isDir = false,
            size = 1L,
            lastModified = 1L
        ).createSnapshotWith(path)

        fun EMPTY(path: String) = Snapshot(
            folderPath = path.toTauPath(),
            entriesByName = emptyMap()
        )

        fun EMPTY(path: TauPath) = EMPTY(path.path)
    }
}

fun SnapshotElement.createSnapshotWith(path: String): Snapshot =
    Snapshot(
        folderPath = path.toTauPath(),
        entriesByName = mapOf(path to this)
    )

fun SnapshotElement.createSnapshotWith(path: TauPath): Snapshot =
    Snapshot(
        folderPath = path,
        entriesByName = mapOf(path.path to this)
    )
