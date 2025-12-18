package lorry.dossiertau.data.intelligenceService.utils2.events

data class Snapshot(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
){

    companion object{

        val FAKE = Snapshot(
            name = "FAKE",
            isDir = false,
            size = 1L,
            lastModified = 1L
        )
    }



}